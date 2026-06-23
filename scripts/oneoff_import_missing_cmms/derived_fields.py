"""Расчёт полей как в DataTransferService (date, shift, production_day, cause, staff)."""

from __future__ import annotations

import re
from datetime import date, datetime, time, timedelta
from typing import Any, Optional

MONTH_NAMES = {
    1: "Январь",
    2: "Февраль",
    3: "Март",
    4: "Апрель",
    5: "Май",
    6: "Июнь",
    7: "Июль",
    8: "Август",
    9: "Сентябрь",
    10: "Октябрь",
    11: "Ноябрь",
    12: "Декабрь",
}


def production_calendar_date(dt: Optional[datetime]) -> Optional[date]:
    if dt is None:
        return None
    if dt.time() >= time(8, 0):
        return dt.date()
    return dt.date() - timedelta(days=1)


def calc_production_day(t1: Optional[datetime], t4: Optional[datetime]) -> Optional[str]:
    """Логика UPDATE production_day из DataTransferService."""
    if t1 is None:
        return None
    if t4 is None:
        d = production_calendar_date(t1)
        return d.strftime("%d.%m.%Y") if d else None
    d_start = production_calendar_date(t1)
    d_end = production_calendar_date(t4)
    if d_start is None or d_end is None:
        d = d_end or d_start
        return d.strftime("%d.%m.%Y") if d else None
    d = d_end if d_start < d_end else d_start
    return d.strftime("%d.%m.%Y")


def calc_date_field(t1: Optional[datetime]) -> Optional[str]:
    if t1 is None:
        return None
    return t1.strftime("%d.%m.%Y")


def calc_shift(t1: Optional[datetime]) -> Optional[str]:
    if t1 is None:
        return None
    h = t1.hour
    if 8 <= h <= 19:
        return "1"
    if h >= 20 or h <= 7:
        return "2"
    return "Не определено"


def calc_week_number(date_str: Optional[str]) -> Optional[int]:
    if not date_str:
        return None
    try:
        dt = datetime.strptime(date_str, "%d.%m.%Y")
        return dt.isocalendar()[1]
    except ValueError:
        return None


def calc_month_name(date_str: Optional[str]) -> Optional[str]:
    if not date_str:
        return None
    try:
        dt = datetime.strptime(date_str, "%d.%m.%Y")
        return MONTH_NAMES.get(dt.month)
    except ValueError:
        return None


def extract_cause(comments: Optional[str]) -> str:
    if not comments or "Cause:" not in comments:
        return ""
    idx = comments.index("Cause:") + len("Cause:")
    rest = comments[idx:]
    for sep in ("¶", "\n", "\r", "[", ";"):
        if sep in rest:
            rest = rest.split(sep, 1)[0]
    cause = rest.strip()
    for ch in ("#", ";", "---"):
        cause = cause.replace(ch, "")
    return cause.replace("Cause:", "").strip()


def extract_staff(comments: Optional[str]) -> str:
    if not comments:
        return ""
    m = re.search(r"\(([^)]+)\)\s*\]", comments)
    if m:
        return m.group(1).strip()
    m = re.search(r"\(([^)]+)\)", comments)
    return m.group(1).strip() if m else ""


def build_derived_row(
    comments: Optional[str],
    start_bd_t1: Optional[datetime],
    stop_bd_t4: Optional[datetime],
) -> dict[str, Any]:
    date_s = calc_date_field(start_bd_t1)
    cause = extract_cause(comments)
    return {
        "cause": cause or None,
        "staff": extract_staff(comments) or None,
        "date": date_s,
        "week_number": calc_week_number(date_s),
        "month_name": calc_month_name(date_s),
        "shift": calc_shift(start_bd_t1),
        "production_day": calc_production_day(start_bd_t1, stop_bd_t4),
    }
