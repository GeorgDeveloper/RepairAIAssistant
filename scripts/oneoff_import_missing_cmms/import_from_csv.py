#!/usr/bin/env python3
"""
Разовая загрузка нарядов из CSV (bd-missing-in-mysql-*.csv / tag-missing-in-mysql-*.csv)
в MySQL с расчётом production_day и прочих полей как в DataTransferService / TagTransferService.

Использование:
  pip install -r requirements.txt
  copy config.example.env config.env   # заполнить MYSQL_*

  python import_from_csv.py --csv bd-missing-in-mysql-2026.csv --kind bd
  python import_from_csv.py --csv tag-missing-in-mysql-2026.csv --kind tag
  python import_from_csv.py --csv bd-missing-in-mysql-2026.csv --kind bd --dry-run

После загрузки НЕ запускается cleanup (удаление «отфильтрованных») — записи остаются в БД.
"""

from __future__ import annotations

import argparse
import csv
import logging
import os
import sys
from dataclasses import dataclass
from datetime import datetime, timedelta, time
from pathlib import Path
from typing import Any, Iterator, Optional

try:
    import pymysql
except ImportError:
    print("Установите зависимости: pip install -r requirements.txt", file=sys.stderr)
    sys.exit(1)

from derived_fields import build_derived_row

# Настройка логирования
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

TABLE_BD = "equipment_maintenance_records"
TABLE_TAG = "tag_maintenance_records"

INSERT_SQL = """
INSERT INTO {table} (
    machine_name, mechanism_node, additional_kit, description, code,
    hp_bd, start_bd_t1, start_maint_t2, stop_maint_t3, stop_bd_t4,
    machine_downtime, ttr, t2_minus_t1, status, maintainers, comments, area,
    created_at
) VALUES (
    %s, %s, %s, %s, %s,
    %s, %s, %s, %s, %s,
    %s, %s, %s, %s, %s, %s, %s,
    %s
)
"""

EXISTS_SQL = """
SELECT COUNT(*) FROM {table}
WHERE code = %s AND (start_bd_t1 <=> %s) AND (stop_bd_t4 <=> %s)
"""


@dataclass
class CsvRow:
    code: str
    type_wo: str
    machine_name: str
    status: str
    area: str
    date_t1: Optional[datetime]
    date_t2: Optional[datetime]
    date_t3: Optional[datetime]
    date_t4: Optional[datetime]
    sduration: str
    duration_minutes: int
    comment: str
    note: str
    # из CMMS при --enrich-from-cmms
    mechanism_node: str = ""
    additional_kit: str = ""
    description: str = ""
    maintainers: str = ""
    sttr: str = ""
    logistic: str = ""


def load_env_file(path: Path) -> None:
    if not path.is_file():
        return
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def as_datetime(value: Any) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    if isinstance(value, str):
        return parse_dt(value)
    return parse_dt(str(value))


def parse_dt(value: str) -> Optional[datetime]:
    value = (value or "").strip()
    if not value:
        return None
    # Support several common formats including exported Excel-like `DD.MM.YYYY HH:MM`
    for fmt in (
        "%Y-%m-%dT%H:%M:%S.%f",
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%d %H:%M:%S.%f",
        "%Y-%m-%d %H:%M:%S",
        "%d.%m.%Y %H:%M:%S.%f",
        "%d.%m.%Y %H:%M:%S",
        "%d.%m.%Y %H:%M",
    ):
        try:
            return datetime.strptime(value, fmt)
        except ValueError:
            continue
    return None


def parse_duration_minutes(duration_str: str) -> int:
    duration_str = (duration_str or "").strip()
    if not duration_str:
        return 0
    if ":" in duration_str:
        parts = duration_str.split(":")
        if len(parts) == 2:
            try:
                return int(parts[0]) * 60 + int(parts[1])
            except ValueError:
                pass
    return 0


def minutes_to_time(minutes: int) -> Optional[str]:
    if minutes <= 0:
        return "00:00:00"
    if minutes > 1439:
        minutes = 1439
    h, m = divmod(minutes, 60)
    return f"{h:02d}:{m:02d}:00"


def duration_between(start: Optional[datetime], end: Optional[datetime]) -> int:
    if start is None or end is None:
        return 0
    delta = int((end - start).total_seconds() // 60)
    return max(0, delta)


def get_shift_boundaries(start: datetime, end: datetime) -> list[datetime]:
    boundaries: list[datetime] = []
    current_day = start.date()
    first = datetime.combine(current_day, time(8, 0))
    if start < first < end:
        boundaries.append(first)
    current_day = current_day + timedelta(days=1)
    while True:
        boundary = datetime.combine(current_day, time(8, 0))
        if boundary < end:
            boundaries.append(boundary)
            current_day += timedelta(days=1)
        else:
            break
    boundaries.sort()
    return boundaries


def iter_parts(
    t1: datetime,
    t2: Optional[datetime],
    t3: Optional[datetime],
    t4: datetime,
) -> Iterator[tuple[datetime, Optional[datetime], Optional[datetime], datetime]]:
    boundaries = get_shift_boundaries(t1, t4)
    split_points = [t1] + boundaries + [t4]
    for i in range(len(split_points) - 1):
        part_start = split_points[i]
        part_end = split_points[i + 1]
        if i == 0:
            part_t2 = t2 if (t2 is not None and t2 >= part_start) else part_start
            part_t3 = t3 if (t3 is not None and t3 < part_end) else part_end
        else:
            part_t2 = part_start
            part_t3 = part_end
        yield part_start, part_t2, part_t3, part_end


def read_csv(path: Path) -> list[CsvRow]:
    rows: list[CsvRow] = []
    raw_head = path.read_text(encoding="utf-8-sig", errors="replace").splitlines()
    if not raw_head:
        return rows
    delim = ";" if raw_head[0].count(";") >= raw_head[0].count(",") else ","
    with path.open(encoding="utf-8-sig", errors="replace", newline="") as f:
        reader = csv.DictReader(f, delimiter=delim)
        # CSV может содержать BOM в имени первого поля (встречается при экспорте из Excel)
        if reader.fieldnames:
            reader.fieldnames = [fn.lstrip('\ufeff') if fn else fn for fn in reader.fieldnames]
        for raw in reader:
            sd = raw.get("sduration", "") or ""
            dm = raw.get("duration_minutes", "")
            try:
                duration_min = int(dm) if dm else parse_duration_minutes(sd)
            except ValueError:
                duration_min = parse_duration_minutes(sd)
            rows.append(
                CsvRow(
                    code=(raw.get("code") or "").strip(),
                    type_wo=(raw.get("type_wo") or "").strip(),
                    machine_name=(raw.get("machine_name") or "").strip(),
                    status=(raw.get("status") or "").strip(),
                    area=(raw.get("area") or "").strip(),
                    date_t1=parse_dt(raw.get("date_t1", "")),
                    date_t2=parse_dt(raw.get("date_t2", "")),
                    date_t3=parse_dt(raw.get("date_t3", "")),
                    date_t4=parse_dt(raw.get("date_t4", "")),
                    sduration=sd,
                    duration_minutes=duration_min,
                    comment=(raw.get("comment") or "").strip(),
                    note=(raw.get("note") or "").strip(),
                )
            )
    return rows


def resolve_kind(kind: str, row: CsvRow) -> str:
    if kind in ("bd", "tag"):
        return kind
    if "tag" in row.type_wo.lower():
        return "tag"
    return "bd"


def table_for_kind(kind: str) -> str:
    return TABLE_TAG if kind == "tag" else TABLE_BD


def _pick_sql_server_odbc_driver() -> str:
    import pyodbc

    configured = os.environ.get("CMMS_ODBC_DRIVER", "").strip()
    candidates = [
        configured,
        "ODBC Driver 18 for SQL Server",
        "ODBC Driver 17 for SQL Server",
        "ODBC Driver 13 for SQL Server",
        "SQL Server Native Client 11.0",
        "SQL Server",
    ]
    installed = list(pyodbc.drivers())
    for name in candidates:
        if name and name in installed:
            return name
    raise RuntimeError(
        "Не найден ODBC-драйвер для SQL Server. Установите "
        "'ODBC Driver 17 for SQL Server' или задайте CMMS_ODBC_DRIVER в config.env. "
        f"Сейчас установлены драйверы: {installed}"
    )


def connect_cmms():
    """Подключение к CMMS через pyodbc (стабильно на Windows)."""
    host = os.environ.get("CMMS_HOST", "").strip()
    if not host:
        logger.error("CMMS_HOST не задан в config.env")
        return None

    try:
        import pyodbc
    except ImportError:
        logger.error(
            "Модуль pyodbc не установлен. Выполните: pip install -r requirements.txt"
        )
        return None

    port = os.environ.get("CMMS_PORT", "1433")
    database = os.environ.get("CMMS_DATABASE", "CMMS")
    user = os.environ.get("CMMS_USER", "")
    password = os.environ.get("CMMS_PASSWORD", "")

    if not user or not password:
        logger.error("Задайте CMMS_USER и CMMS_PASSWORD в config.env")
        return None

    try:
        driver = _pick_sql_server_odbc_driver()
    except RuntimeError as e:
        logger.error("%s", e)
        return None

    conn_str = (
        f"DRIVER={{{driver}}};"
        f"SERVER={host},{port};"
        f"DATABASE={database};"
        f"UID={user};"
        f"PWD={password};"
        "Encrypt=no;"
        "TrustServerCertificate=yes;"
        "Connection Timeout=30;"
    )
    try:
        conn = pyodbc.connect(conn_str)
        logger.info("CMMS: подключено (%s:%s, driver=%s, db=%s)", host, port, driver, database)
        return conn
    except pyodbc.Error as e:
        logger.error("CMMS: ошибка подключения к %s:%s — %s", host, port, e)
        return None


def test_cmms_connection() -> int:
    conn = connect_cmms()
    if conn is None:
        return 1
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT TOP 1 WOCodeName FROM REP_BreakdownReport")
            row = cur.fetchone()
        logger.info("CMMS: тест OK, пример кода наряда: %s", row[0] if row else "нет строк")
        return 0
    except Exception as e:
        logger.error("CMMS: тест запроса не прошёл: %s", e)
        return 1
    finally:
        conn.close()


def fetch_cmms_row(conn_cmms: Any, code: str, t1: datetime, t4: datetime) -> Optional[dict]:
    """Полная строка из REP_BreakdownReport (допуск ±5 мин по T1/T4)."""
    sql = """
    SELECT TOP 1
        MachineName, Assembly, SubAssembly, InitialComment, Maintainers,
        comment,
        CAST(SDuration AS VARCHAR(20)), CAST(STTR AS VARCHAR(20)), CAST(SLogisticTimeMin AS VARCHAR(20)),
        Date_T2, Date_T3
    FROM REP_BreakdownReport
    WHERE WOCodeName = ?
      AND ABS(DATEDIFF(SECOND, Date_T1, ?)) <= 300
      AND ABS(DATEDIFF(SECOND, Date_T4, ?)) <= 300
    ORDER BY ABS(DATEDIFF(SECOND, Date_T1, ?)) + ABS(DATEDIFF(SECOND, Date_T4, ?))
    """
    with conn_cmms.cursor() as cur:
        cur.execute(sql, (code, t1, t4, t1, t4))
        r = cur.fetchone()
    if not r:
        return None
    return {
        "machine_name": (r[0] or "").strip(),
        "mechanism_node": (r[1] or "").strip(),
        "additional_kit": (r[2] or "").strip(),
        "description": (r[3] or "").strip(),
        "maintainers": (r[4] or "").strip(),
        "comment": (r[5] or "").strip(),
        "sduration": str(r[6] or "").strip(),
        "sttr": str(r[7] or "").strip(),
        "logistic": str(r[8] or "").strip(),
        "date_t2": r[9],
        "date_t3": r[10],
    }


def apply_cmms_to_csv_row(row: CsvRow, cmms: dict) -> None:
    if cmms.get("machine_name"):
        row.machine_name = cmms["machine_name"]
    row.mechanism_node = cmms.get("mechanism_node") or row.mechanism_node
    row.additional_kit = cmms.get("additional_kit") or row.additional_kit
    row.description = cmms.get("description") or row.description
    row.maintainers = cmms.get("maintainers") or row.maintainers
    if cmms.get("comment"):
        row.comment = cmms["comment"]
    if cmms.get("sduration"):
        row.sduration = cmms["sduration"]
    row.sttr = cmms.get("sttr") or row.sttr
    row.logistic = cmms.get("logistic") or row.logistic
    if row.date_t2 is None and cmms.get("date_t2"):
        row.date_t2 = parse_dt(str(cmms["date_t2"]))
    if row.date_t3 is None and cmms.get("date_t3"):
        row.date_t3 = parse_dt(str(cmms["date_t3"]))


def enrich_from_cmms(rows: list[CsvRow], conn_cmms: Any) -> int:
    enriched = 0
    for row in rows:
        if not row.code or row.date_t1 is None or row.date_t4 is None:
            continue
        cmms = fetch_cmms_row(conn_cmms, row.code, row.date_t1, row.date_t4)
        if not cmms:
            logger.warning("CMMS: не найден %s T1=%s T4=%s", row.code, row.date_t1, row.date_t4)
            continue
        apply_cmms_to_csv_row(row, cmms)
        enriched += 1
    return enriched


def build_insert_payload(row: CsvRow, t1, t2, t3, t4) -> tuple:
    downtime_min = parse_duration_minutes(row.sduration) or duration_between(t1, t4)
    ttr_min = parse_duration_minutes(row.sttr) if row.sttr else duration_between(t1, t4)
    t2t1_min = (
        parse_duration_minutes(row.logistic)
        if row.logistic
        else duration_between(t1, t2)
    )
    now = datetime.now()
    return (
        row.machine_name or None,
        row.mechanism_node or None,
        row.additional_kit or None,
        row.description or None,
        row.code,
        row.type_wo or None,
        t1,
        t2,
        t3,
        t4,
        minutes_to_time(downtime_min),
        minutes_to_time(ttr_min),
        minutes_to_time(t2t1_min),
        row.status or None,
        row.maintainers or None,
        row.comment or None,
        row.area or None,
        now,
    )


def record_exists(cur, table: str, code: str, t1, t4) -> bool:
    cur.execute(EXISTS_SQL.format(table=table), (code, t1, t4))
    return cur.fetchone()[0] > 0


def post_process_by_ids(conn, table: str, ids: list[int]) -> None:
    """Заполнение derived-полей по id (Python) + JOIN-ы staff_technical / график."""
    if not ids:
        return
    placeholders = ",".join(["%s"] * len(ids))
    scope = f"id IN ({placeholders})"
    params = tuple(ids)

    update_derived = f"""
        UPDATE {table}
        SET cause=%s, staff=%s, date=%s, week_number=%s, month_name=%s, shift=%s, production_day=%s
        WHERE id=%s
    """

    with conn.cursor() as cur:
        cur.execute(
            f"SELECT id, comments, start_bd_t1, stop_bd_t4 FROM {table} WHERE {scope}",
            params,
        )
        for row_id, comments, t1, t4 in cur.fetchall():
            d = build_derived_row(comments, t1, t4)
            cur.execute(
                update_derived,
                (
                    d["cause"],
                    d["staff"],
                    d["date"],
                    d["week_number"],
                    d["month_name"],
                    d["shift"],
                    d["production_day"],
                    row_id,
                ),
            )
        logger.info("  post-process [Python derived]: %s строк", len(ids))

        join_updates = [
            (
                "failure_type из staff_technical",
                f"""
                UPDATE {table} rp
                JOIN staff_technical rpp ON rp.staff = rpp.staff
                SET rp.failure_type = rpp.directivity
                WHERE {scope} AND (rp.failure_type IS NULL OR rp.failure_type = '')
                """,
            ),
            (
                "crew_de_facto",
                f"""
                UPDATE {table} rp
                JOIN staff_technical rpp ON rp.staff = rpp.staff
                SET rp.crew_de_facto = rpp.shift
                WHERE {scope} AND (rp.crew_de_facto IS NULL OR rp.crew_de_facto = '')
                """,
            ),
            (
                "crew",
                f"""
                UPDATE {table} AS emr
                JOIN график_работы_104 AS g ON g.Дата = emr.date AND CAST(g.Смена AS CHAR) = emr.shift
                SET emr.crew = g.Бригада
                WHERE {scope} AND (emr.crew IS NULL OR emr.crew = '')
                  AND emr.date IS NOT NULL AND emr.shift IS NOT NULL
                """,
            ),
            (
                "failure_type спец. причины",
                f"""
                UPDATE {table}
                SET failure_type = 'Другие'
                WHERE {scope} AND (
                    cause LIKE '%%Наладка%%'
                    OR cause LIKE '%%Простой по вине производства%%'
                    OR cause LIKE '%%Простой по вине с. качества%%'
                )
                """,
            ),
        ]
        conn.commit()
        logger.info("  post-process [Python derived]: закоммичено")

        for name, sql in join_updates:
            n = cur.execute(sql, params)
            logger.info("  post-process [%s]: ~%s строк", name, n)
    conn.commit()


def repair_rows_in_mysql(
    conn,
    table: str,
    id_min: int,
    id_max: int,
    conn_cmms: Any,
) -> None:
    """Исправление уже вставленных строк: CMMS + derived-поля."""
    with conn.cursor() as cur:
        cur.execute(
            f"""
            SELECT id, code, start_bd_t1, stop_bd_t4, comments
            FROM {table}
            WHERE id BETWEEN %s AND %s
            ORDER BY id
            """,
            (id_min, id_max),
        )
        rows = cur.fetchall()

    ids: list[int] = []
    for row_id, code, t1, t4, comments in rows:
        ids.append(row_id)
        if not code or t1 is None or t4 is None:
            logger.warning("Пропуск id=%s: нет code/T1/T4", row_id)
            continue

        updates: dict[str, Any] = {}
        if conn_cmms:
            cmms = fetch_cmms_row(conn_cmms, code, t1, t4)
            if cmms:
                downtime = minutes_to_time(parse_duration_minutes(cmms["sduration"]))
                ttr = minutes_to_time(
                    parse_duration_minutes(cmms["sttr"])
                    or duration_between(t1, t4)
                )
                t2_dt = as_datetime(cmms.get("date_t2"))
                t3_dt = as_datetime(cmms.get("date_t3"))
                t2t1 = minutes_to_time(
                    parse_duration_minutes(cmms["logistic"]) or duration_between(t1, t2_dt)
                )
                updates = {
                    "machine_name": cmms["machine_name"] or None,
                    "mechanism_node": cmms["mechanism_node"] or None,
                    "additional_kit": cmms["additional_kit"] or None,
                    "description": cmms["description"] or None,
                    "maintainers": cmms["maintainers"] or None,
                    "comments": cmms["comment"] or comments,
                    "machine_downtime": downtime,
                    "ttr": ttr,
                    "t2_minus_t1": t2t1,
                }
                if t2_dt:
                    updates["start_maint_t2"] = t2_dt
                if t3_dt:
                    updates["stop_maint_t3"] = t3_dt
            else:
                logger.warning("CMMS: не найден %s для id=%s", code, row_id)
                updates["comments"] = comments
        else:
            updates["comments"] = comments

        d = build_derived_row(updates.get("comments", comments), t1, t4)
        updates.update(d)

        set_clause = ", ".join(f"{k}=%s" for k in updates)
        sql = f"UPDATE {table} SET {set_clause} WHERE id=%s"
        with conn.cursor() as cur:
            cur.execute(sql, tuple(updates.values()) + (row_id,))
        logger.info("repair id=%s code=%s production_day=%s", row_id, code, d.get("production_day"))

    conn.commit()
    if ids:
        post_process_by_ids(conn, table, ids)


def main() -> int:
    parser = argparse.ArgumentParser(description="Разовая загрузка нарядов из CSV в MySQL")
    parser.add_argument("--csv", type=Path, help="Путь к CSV из /api/reconciliation/.../missing.csv")
    parser.add_argument(
        "--kind",
        choices=("bd", "tag", "auto"),
        default="auto",
        help="Целевая таблица (auto: Tag если type_wo содержит Tag)",
    )
    parser.add_argument("--config", type=Path, default=Path(__file__).parent / "config.env")
    parser.add_argument("--dry-run", action="store_true", help="Только показать план, без INSERT")
    parser.add_argument(
        "--no-split-long",
        action="store_true",
        help="Не разбивать длинные наряды (>24ч) по границам смен 08:00",
    )
    parser.add_argument(
        "--no-enrich-cmms",
        action="store_true",
        help="Не догружать поля из SQL Server (по умолчанию догрузка включена, если задан CMMS_HOST)",
    )
    parser.add_argument("--skip-post-process", action="store_true", help="Не запускать UPDATE полей после вставки")
    parser.add_argument("--repair-id-min", type=int, help="Исправить уже вставленные строки (id от)")
    parser.add_argument("--repair-id-max", type=int, help="Исправить уже вставленные строки (id до)")
    parser.add_argument(
        "--repair-table",
        choices=("bd", "tag"),
        default="bd",
        help="Таблица для --repair-id-min/max",
    )
    parser.add_argument(
        "--post-process-only",
        action="store_true",
        help="Только постобработка для id из --repair-id-min/max (без INSERT/CMMS repair)",
    )
    parser.add_argument(
        "--test-cmms",
        action="store_true",
        help="Проверить подключение к SQL Server (CMMS) и выйти",
    )
    args = parser.parse_args()

    load_env_file(args.config)

    if args.test_cmms:
        return test_cmms_connection()

    mysql_cfg = {
        "host": os.environ.get("MYSQL_HOST", "localhost"),
        "port": int(os.environ.get("MYSQL_PORT", "3306")),
        "user": os.environ.get("MYSQL_USER", "dba"),
        "password": os.environ.get("MYSQL_PASSWORD", ""),
        "database": os.environ.get("MYSQL_DATABASE", "monitoring_bd"),
        "charset": "utf8mb4",
        "autocommit": False,
    }
    if not mysql_cfg["password"] and not args.dry_run:
        print("Задайте MYSQL_PASSWORD в config.env", file=sys.stderr)
        return 1

    conn_cmms = None if args.no_enrich_cmms else connect_cmms()
    if conn_cmms is None and not args.no_enrich_cmms:
        logger.warning(
            "CMMS недоступен. Проверьте config.env и выполните: "
            "python import_from_csv.py --test-cmms"
        )

    if args.repair_id_min is not None and args.repair_id_max is not None:
        table = table_for_kind(args.repair_table)
        conn = pymysql.connect(**mysql_cfg)
        try:
            if args.post_process_only:
                with conn.cursor() as cur:
                    cur.execute(
                        f"SELECT id FROM {table} WHERE id BETWEEN %s AND %s ORDER BY id",
                        (args.repair_id_min, args.repair_id_max),
                    )
                    ids = [r[0] for r in cur.fetchall()]
                logger.info(
                    "post-process-only: %s строк id %s..%s",
                    len(ids),
                    args.repair_id_min,
                    args.repair_id_max,
                )
                post_process_by_ids(conn, table, ids)
            else:
                repair_rows_in_mysql(conn, table, args.repair_id_min, args.repair_id_max, conn_cmms)
                logger.info(
                    "Repair завершён для id %s..%s в %s",
                    args.repair_id_min,
                    args.repair_id_max,
                    table,
                )
        finally:
            conn.close()
            if conn_cmms:
                conn_cmms.close()
        return 0

    if not args.csv:
        print("Укажите --csv или --repair-id-min/--repair-id-max", file=sys.stderr)
        return 1

    csv_rows = read_csv(args.csv)
    if not csv_rows:
        print("CSV пуст")
        return 1

    if conn_cmms:
        n = enrich_from_cmms(csv_rows, conn_cmms)
        logger.info("CMMS: обогащено %s из %s строк CSV", n, len(csv_rows))

    stats = {"insert": 0, "skip_dup": 0, "skip_bad": 0, "split_parts": 0}

    conn = None if args.dry_run else pymysql.connect(**mysql_cfg)
    inserted_ids_by_table: dict[str, list[int]] = {}

    try:
        cur = conn.cursor() if conn else None
        for row in csv_rows:
            kind = resolve_kind(args.kind, row)
            table = table_for_kind(kind)

            if not row.code:
                logger.debug(f"SKIP: пустой код для строки")
                stats["skip_bad"] += 1
                continue

            t1, t2, t3, t4 = row.date_t1, row.date_t2, row.date_t3, row.date_t4
            if t1 is None and t4 is None:
                logger.debug(f"SKIP {row.code}: нет date_t1 и date_t4")
                stats["skip_bad"] += 1
                continue

            parts: list[tuple] = []
            if (
                not args.no_split_long
                and row.duration_minutes > 1439
                and t1 is not None
                and t4 is not None
            ):
                for ps in iter_parts(t1, t2, t3, t4):
                    parts.append(ps)
                stats["split_parts"] += max(0, len(parts) - 1)
            else:
                parts.append((t1, t2, t3, t4))

            for part in parts:
                pt1, pt2, pt3, pt4 = part
                if cur and record_exists(cur, table, row.code, pt1, pt4):
                    logger.debug(f"SKIP {row.code}: запись уже существует в БД (дубликат)")
                    stats["skip_dup"] += 1
                    continue

                payload = build_insert_payload(row, pt1, pt2, pt3, pt4)
                if args.dry_run:
                    d = build_derived_row(row.comment, pt1, pt4)
                    logger.info(
                        "DRY-RUN INSERT %s | %s | T1=%s T4=%s | production_day=%s",
                        table,
                        row.code,
                        pt1,
                        pt4,
                        d.get("production_day"),
                    )
                    stats["insert"] += 1
                    continue

                try:
                    cur.execute(INSERT_SQL.format(table=table), payload)
                    inserted_ids_by_table.setdefault(table, []).append(cur.lastrowid)
                    stats["insert"] += 1
                    logger.debug("INSERT id=%s code=%s", cur.lastrowid, row.code)
                except Exception as e:
                    logger.error("INSERT FAILED для %s: %s", row.code, e)

        if conn:
            conn.commit()
            logger.info(f"Коммит: вставлено операций {stats['insert']}")

        if conn and not args.skip_post_process and inserted_ids_by_table:
            for tbl, ids in inserted_ids_by_table.items():
                logger.info("Постобработка %s: %s записей (id от %s)", tbl, len(ids), min(ids))
                post_process_by_ids(conn, tbl, ids)

    finally:
        if conn:
            conn.close()
        if conn_cmms:
            conn_cmms.close()

    logger.info(f"\nИтог: insert={stats['insert']}, skip_duplicate={stats['skip_dup']}, skip_invalid={stats['skip_bad']}, extra_split_parts={stats['split_parts']}")
    return 0


def _preview_production_day(t1: Optional[datetime], t4: Optional[datetime]) -> str:
    """Упрощённый предпросмотр (точное значение задаёт SQL post_process)."""
    ref = t4 or t1
    if ref is None:
        return "?"
    d = ref.date() if ref.hour >= 8 else (ref.date() - timedelta(days=1))
    return d.strftime("%d.%m.%Y")


if __name__ == "__main__":
    sys.exit(main())