package ru.georgdeveloper.assistantbaseupdate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.BatchUpdateException;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Сервис для ежедневного переноса и обновления данных о плановых работах из SQL Server в MySQL
 * Выполняется каждый день в 6:00 утра
 */
@Service
public class PreventiveMaintenanceTransferService {

    private static final Logger logger = LoggerFactory.getLogger(PreventiveMaintenanceTransferService.class);

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Moscow");

    /**
     * Опорная дата для "периода обновления" в MySQL.
     * Используем первую непустую из плановых/фактических дат, чтобы корректно ограничивать обновления по годам.
     */
    private static final String MYSQL_PM_DATE_REF =
        "COALESCE(scheduled_date, scheduled_proposed_date, date_start_work_order, date_stop_work_order)";

    private static final class PeriodRange {
        private final Timestamp startInclusive;
        private final Timestamp endExclusive;

        private PeriodRange(Timestamp startInclusive, Timestamp endExclusive) {
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }
    }

    /**
     * Период обновления: прошлый год + текущий год (Europe/Moscow).
     * [01.01.(year-1) 00:00 .. 01.01.(year+1) 00:00)
     */
    private PeriodRange getUpdatePeriodRange() {
        int year = LocalDate.now(BUSINESS_ZONE).getYear();
        LocalDateTime start = LocalDate.of(year - 1, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        return new PeriodRange(Timestamp.valueOf(start), Timestamp.valueOf(end));
    }

    /**
     * Период первичного добавления: только текущий год (Europe/Moscow).
     * [01.01.year .. 01.01.(year+1))
     */
    private PeriodRange getImportCurrentYearRange() {
        int year = LocalDate.now(BUSINESS_ZONE).getYear();
        LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        return new PeriodRange(Timestamp.valueOf(start), Timestamp.valueOf(end));
    }

    /**
     * Размер батча для SQL Server запросов с IN (...).
     * Важно: SQL Server имеет лимит ~2100 параметров на запрос.
     */
    private static final int SQLSERVER_IN_BATCH_SIZE = 2000;

    /**
     * Размер батча для массовых UPDATE в MySQL (JdbcTemplate.batchUpdate).
     * Подбирается для разумного баланса памяти/скорости.
     */
    private static final int MYSQL_BATCH_UPDATE_SIZE = 1000;

    @Autowired
    @Qualifier("sqlServerJdbcTemplate")
    private JdbcTemplate sqlServerJdbcTemplate;

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    /**
     * Маппинг статусов с английского на русский
     */
    private static final Map<String, String> STATUS_TRANSLATION_MAP = Map.of(
        "To Be Planned", "Необходимо запланировать",
        "Closed", "Закрыто",
        "Executed", "Выполнено",
        "Scheduled", "Запланированно"
    );

    private static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
        List<List<T>> result = new ArrayList<>((list.size() + batchSize - 1) / batchSize);
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return result;
    }

    private static String placeholders(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        return String.join(",", Collections.nCopies(count, "?"));
    }

    /**
     * Выполняет batch update с обработкой deadlock и retry механизмом.
     * Сортирует записи по id (если это UPDATE по id) для предотвращения deadlock.
     */
    private void mysqlBatchUpdateChunked(String sql, List<Object[]> batchArgs) {
        if (batchArgs == null || batchArgs.isEmpty()) {
            return;
        }
        
        // Сортируем записи по id (последний параметр обычно id) для предотвращения deadlock
        // Это гарантирует, что все транзакции блокируют строки в одинаковом порядке
        List<Object[]> sortedArgs = new ArrayList<>(batchArgs);
        if (sql.contains("WHERE id = ?") && !batchArgs.isEmpty() && batchArgs.get(0).length > 1) {
            // Сортируем по последнему параметру (id)
            sortedArgs.sort((a, b) -> {
                Object idA = a[a.length - 1];
                Object idB = b[b.length - 1];
                if (idA instanceof Comparable && idB instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> compA = (Comparable<Object>) idA;
                    return compA.compareTo(idB);
                }
                return 0;
            });
        }
        
        for (int i = 0; i < sortedArgs.size(); i += MYSQL_BATCH_UPDATE_SIZE) {
            List<Object[]> chunk = sortedArgs.subList(i, Math.min(i + MYSQL_BATCH_UPDATE_SIZE, sortedArgs.size()));
            executeBatchUpdateWithRetry(sql, chunk, i / MYSQL_BATCH_UPDATE_SIZE + 1);
        }
    }
    
    /**
     * Выполняет batch update с retry механизмом для обработки deadlock
     */
    private void executeBatchUpdateWithRetry(String sql, List<Object[]> chunk, int chunkNumber) {
        int maxRetries = 3;
        long retryDelay = 100; // Начальная задержка 100мс
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                mysqlJdbcTemplate.batchUpdate(sql, chunk);
                return; // Успешно выполнено
            } catch (CannotAcquireLockException e) {
                if (attempt == maxRetries) {
                    logger.error("Критическая ошибка deadlock при batch update (chunk {}): исчерпаны все попытки ({}). SQL: {}", 
                                chunkNumber, maxRetries, sql, e);
                    throw new RuntimeException("Deadlock после " + maxRetries + " попыток", e);
                }
                logger.warn("Deadlock при batch update (chunk {}, попытка {}/{}), повтор через {} мс. SQL: {}", 
                           chunkNumber, attempt, maxRetries, retryDelay, sql);
                sleepWithInterruptHandling(retryDelay);
                retryDelay *= 2; // Экспоненциальная задержка для следующей попытки
            } catch (Exception e) {
                // Проверяем, является ли это deadlock исключением
                if (isDeadlockException(e)) {
                    if (attempt == maxRetries) {
                        logger.error("Критическая ошибка deadlock при batch update (chunk {}): исчерпаны все попытки ({}). SQL: {}", 
                                    chunkNumber, maxRetries, sql, e);
                        throw new RuntimeException("Deadlock после " + maxRetries + " попыток", e);
                    }
                    logger.warn("Deadlock при batch update (chunk {}, попытка {}/{}), повтор через {} мс. SQL: {}", 
                               chunkNumber, attempt, maxRetries, retryDelay, sql);
                    sleepWithInterruptHandling(retryDelay);
                    retryDelay *= 2; // Экспоненциальная задержка для следующей попытки
                } else {
                    // Это не deadlock, пробрасываем исключение дальше
                    throw e;
                }
            }
        }
    }
    
    /**
     * Выполняет задержку с обработкой прерывания
     */
    private void sleepWithInterruptHandling(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано во время ожидания повтора при deadlock", ie);
        }
    }
    
    /**
     * Проверяет, является ли исключение deadlock
     */
    private boolean isDeadlockException(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("Deadlock")) {
            return true;
        }
        
        // Проверяем вложенные исключения
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof BatchUpdateException || 
                cause instanceof SQLException) {
                String causeMessage = cause.getMessage();
                if (causeMessage != null && causeMessage.contains("Deadlock")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        
        return false;
    }
    

    /**
     * Ежедневный перенос данных в 6:01 утра (Europe/Moscow).
     * Запуск в 06:01, а не в 06:00, чтобы не пересекаться с синхронизацией ключевых линий и data-sync
     * (cron "0 *&#42;/&#47;3 * * * ?" — каждые 3 минуты в :00, :03, :06…), иначе возможны deadlock и откат транзакции.
     */
    @Scheduled(cron = "0 1 6 * * *", zone = "Europe/Moscow")
    @Transactional
    public void transferDataDaily() {
        try {
            LocalDateTime triggerTime = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
            logger.info("=== Начало ежедневного переноса данных о плановых работах... Trigger at {} (zone Europe/Moscow)", triggerTime);
            
            // Проверяем, что находимся в активной транзакции
            if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                logger.error("❌ КРИТИЧЕСКАЯ ОШИБКА: Нет активной транзакции для @Scheduled метода!");
                throw new RuntimeException("No active transaction for scheduled method - check SchedulerConfig");
            }
            logger.debug("✓ Транзакция активна, threadName={}", Thread.currentThread().getName());
            
            // Выполняем все шаги обработки
            importPmWorkOrdersForCurrentYear();
            updateDifferentStatuses();
            updateDateStartWorkOrder();
            updateDateStopWorkOrder();
            calculatePreventiveMaintenanceDurationMin();
            transferCommentsSafe();
            transferMaintainersSafe();
            transferScheduledProposedDate();
            transferScheduledDate();
            calculateDeltaSchedulingDays();
            calculatePmReportDelayDays();
            transferEstimatedDuration();
            transferOperationsNokDetailed();
            transferOperationsOk();
            calculateOperationsAll();
            
            // Проверяем, что транзакция всё ещё активна перед выходом (данные будут зафиксированы при выходе из метода)
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                logger.info("✓ Транзакция активна - данные будут зафиксированы в БД при завершении метода");
            } else {
                logger.warn("⚠️  ВНИМАНИЕ: Транзакция была закрыта во время выполнения!");
            }
            
            logger.info("=== Ежедневный перенос данных о плановых работах завершен успешно");
            
        } catch (Exception e) {
            logger.error("Критическая ошибка при переносе данных о плановых работах: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Первичное заполнение: добавляет в MySQL новые ППР-наряды за текущий год (Europe/Moscow),
     * которых ещё нет в таблице pm_maintenance_records.
     *
     * Источник: SQL Server WOM_WorkOrder (фильтры по ScheduledTime и CodeName LIKE '%PM%').
     *
     * Важно: этот шаг добавляет только IDCode (остальные поля будут заполнены последующими шагами обновления).
     */
    public void importPmWorkOrdersForCurrentYear() {
        try {
            PeriodRange period = getImportCurrentYearRange();
            int year = LocalDate.now(BUSINESS_ZONE).getYear();

            logger.info("Начало первичного заполнения pm_maintenance_records за {} год ({} .. {})",
                year, period.startInclusive, period.endExclusive);

            // 1) Берём все IDCode, которые уже есть в MySQL
            List<String> existingIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL",
                String.class
            );
            Set<String> existingSet = new HashSet<>(Math.max(16, existingIdcodes.size() * 2));
            existingSet.addAll(existingIdcodes);

            // 2) Берём все PM IDCode за текущий год из SQL Server
            String sql = """
                SELECT DISTINCT wo.IDCode, wo.ScheduledTime, wo.ScheduledTimeProposed
                FROM WOM_WorkOrder wo
                WHERE wo.ScheduledTime >= ? AND wo.ScheduledTime < ?
                  AND wo.CodeName LIKE '%PM%'
                  AND wo.IDCode IS NOT NULL
                """;

            List<Map<String, Object>> rows = sqlServerJdbcTemplate.queryForList(sql, period.startInclusive, period.endExclusive);
            logger.info("Найдено {} PM-нарядов в SQL Server за {} год", rows.size(), year);

            // 3) Вставляем отсутствующие
            List<Object[]> insertArgs = new ArrayList<>();
            Set<String> alreadyPrepared = new HashSet<>();
            int skippedExisting = 0;
            for (Map<String, Object> row : rows) {
                String idcode = (String) row.get("IDCode");
                if (idcode == null || idcode.isBlank()) {
                    continue;
                }
                if (existingSet.contains(idcode)) {
                    skippedExisting++;
                    continue;
                }
                if (!alreadyPrepared.add(idcode)) {
                    continue;
                }

                // Сразу кладём плановые даты, чтобы новые записи попадали в "период обновления" и фильтры UI.
                Timestamp scheduled = (Timestamp) row.get("ScheduledTime");
                Timestamp proposed = (Timestamp) row.get("ScheduledTimeProposed");

                insertArgs.add(new Object[]{idcode, scheduled, proposed});
            }

            if (!insertArgs.isEmpty()) {
                mysqlBatchUpdateChunked(
                    "INSERT INTO pm_maintenance_records (IDCode, scheduled_date, scheduled_proposed_date) VALUES (?, ?, ?)",
                    insertArgs
                );
            }

            logger.info("Первичное заполнение завершено: добавлено {} новых записей, пропущено (уже были) {}", insertArgs.size(), skippedExisting);
        } catch (Exception e) {
            logger.error("Ошибка при первичном заполнении pm_maintenance_records: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Ручной запуск переноса данных
     */
    @Transactional
    public void runTransfer() {
        try {
            logger.info("=== Ручной запуск переноса данных о плановых работах...");
            transferDataDaily();
        } catch (Exception e) {
            logger.error("Критическая ошибка при ручном переносе данных: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перевод статуса с английского на русский
     */
    private String translateStatus(String englishStatus) {
        if (englishStatus == null) {
            return null;
        }
        return STATUS_TRANSLATION_MAP.getOrDefault(englishStatus, englishStatus);
    }

    /**
     * Получение последних статусов из SQL Server батчами (оптимально для больших объемов).
     */
    private Map<String, String> getLatestStatusesFromSqlServerBatch(List<String> idcodesBatch) {
        if (idcodesBatch == null || idcodesBatch.isEmpty()) {
            return Collections.emptyMap();
        }

        String inPlaceholders = placeholders(idcodesBatch.size());
        String sql = ("""
            WITH LatestStatus AS (
                SELECT
                    sh.WOM_WorkOrder_IDCode AS IDCode,
                    ws.CodeName AS CodeName,
                    sh.TransitionDate,
                    ROW_NUMBER() OVER (PARTITION BY sh.WOM_WorkOrder_IDCode
                                     ORDER BY sh.TransitionDate DESC) AS rn
                FROM WOM_StatusHistory sh
                INNER JOIN WOM_WorkOrderStatus ws ON sh.Status = ws.ID
                WHERE sh.WOM_WorkOrder_IDCode IN (%s)
            )
            SELECT IDCode, CodeName
            FROM LatestStatus
            WHERE rn = 1
            """).formatted(inPlaceholders);

        List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
        Map<String, String> idcodeToStatus = new HashMap<>(Math.max(16, results.size()));
        for (Map<String, Object> row : results) {
            String idcode = (String) row.get("IDCode");
            String englishStatus = (String) row.get("CodeName");
            idcodeToStatus.put(idcode, translateStatus(englishStatus));
        }
        return idcodeToStatus;
    }

    /**
     * Обновление отличающихся статусов
     */
    public void updateDifferentStatuses() {
        try {
            logger.info("Начало обновления отличающихся статусов...");

            PeriodRange period = getUpdatePeriodRange();
            
            List<String> finalStatuses = Arrays.asList("Закрыто", "Выполнено");
            String placeholders = String.join(",", Collections.nCopies(finalStatuses.size(), "?"));
            
            // Получаем записи из MySQL
            String selectSql = "SELECT id, IDCode, status " +
                              "FROM pm_maintenance_records " +
                              "WHERE IDCode IS NOT NULL " +
                              "AND (status IS NULL OR status NOT IN (" + placeholders + ")) " +
                              "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            List<Object> params = new ArrayList<>(finalStatuses);
            params.add(period.startInclusive);
            params.add(period.endExclusive);
            List<Map<String, Object>> mysqlRecords = mysqlJdbcTemplate.queryForList(selectSql, params.toArray());
            
            logger.info("Найдено {} записей для проверки (исключая финальные статусы)", mysqlRecords.size());
            
            if (mysqlRecords.isEmpty()) {
                logger.warn("Нет записей для обработки");
                return;
            }
            
            // Батч-обработка: получаем статусы из SQL Server пакетами (лимит 2100 параметров)
            int updatedCount = 0;
            int sameStatusCount = 0;
            int noStatusCount = 0;
            int errorCount = 0;

            List<Object[]> updateArgs = new ArrayList<>();

            for (List<Map<String, Object>> recordsBatch : partition(mysqlRecords, SQLSERVER_IN_BATCH_SIZE)) {
                List<String> idcodesBatch = new ArrayList<>(recordsBatch.size());
                for (Map<String, Object> record : recordsBatch) {
                    String idcode = (String) record.get("IDCode");
                    if (idcode != null) {
                        idcodesBatch.add(idcode);
                    }
                }

                Map<String, String> latestStatuses = getLatestStatusesFromSqlServerBatch(idcodesBatch);

                for (Map<String, Object> record : recordsBatch) {
                    Integer recordId = (Integer) record.get("id");
                    String idcode = (String) record.get("IDCode");
                    String currentStatus = (String) record.get("status");

                    try {
                        String sqlserverStatus = latestStatuses.get(idcode);
                        if (sqlserverStatus == null) {
                            noStatusCount++;
                            logger.debug("Не найден статус в SQL Server для IDCode: {}", idcode);
                            continue;
                        }

                        if (!Objects.equals(currentStatus, sqlserverStatus)) {
                            updateArgs.add(new Object[]{sqlserverStatus, recordId});
                            updatedCount++;
                        } else {
                            sameStatusCount++;
                        }
                    } catch (Exception e) {
                        errorCount++;
                        logger.error("Ошибка при обработке записи ID {}, IDCode {}: {}",
                            recordId, idcode, e.getMessage());
                    }
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET status = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("==================================================");
            logger.info("ИТОГОВАЯ СТАТИСТИКА ОБНОВЛЕНИЯ СТАТУСОВ:");
            logger.info("✓ Обновлено записей: {}", updatedCount);
            logger.info("✓ Статусы совпадают: {}", sameStatusCount);
            logger.info("✓ Не найдено статусов в SQL Server: {}", noStatusCount);
            logger.info("✓ Ошибок обработки: {}", errorCount);
            logger.info("==================================================");
            
        } catch (Exception e) {
            logger.error("Ошибка при обновлении отличающихся статусов: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос даты начала работ
     */
    public void updateDateStartWorkOrder() {
        try {
            logger.info("Начало переноса даты начала работ...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем даты из SQL Server батчами (лимит 2100 параметров)
            Map<String, Timestamp> idcodeToDate = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.IDCode, wo.ActualStartTime " +
                    "FROM WOM_WorkOrder wo " +
                    "WHERE wo.IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.ActualStartTime IS NOT NULL";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToDate.put((String) row.get("IDCode"), (Timestamp) row.get("ActualStartTime"));
                }
            }
            
            logger.info("Найдено {} записей с датами начала работ в SQL Server", idcodeToDate.size());
            
            if (idcodeToDate.isEmpty()) {
                logger.warn("Нет данных с датами начала работ в SQL Server");
                return;
            }
            
            // Обновляем данные в MySQL (batchUpdate)
            int updatedCount = 0;
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );

            List<Object[]> updateArgs = new ArrayList<>();
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcodeValue = (String) record.get("IDCode");

                Timestamp startDate = idcodeToDate.get(idcodeValue);
                if (startDate != null) {
                    updateArgs.add(new Object[]{startDate, recordId});
                    updatedCount++;
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET date_start_work_order = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("✓ Успешно обновлено {} записей в MySQL", updatedCount);
            logDateStatistics(idcodeToDate);
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе даты начала работ: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос даты окончания работ
     */
    public void updateDateStopWorkOrder() {
        try {
            logger.info("Начало переноса даты окончания работ...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем даты из SQL Server батчами (лимит 2100 параметров)
            Map<String, Timestamp> idcodeToDate = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.IDCode, wo.ActualEndTime " +
                    "FROM WOM_WorkOrder wo " +
                    "WHERE wo.IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.ActualEndTime IS NOT NULL";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToDate.put((String) row.get("IDCode"), (Timestamp) row.get("ActualEndTime"));
                }
            }
            
            logger.info("Найдено {} записей с датами окончания работ в SQL Server", idcodeToDate.size());
            
            if (idcodeToDate.isEmpty()) {
                logger.warn("Нет данных с датами окончания работ в SQL Server");
                return;
            }
            
            // Обновляем данные в MySQL (batchUpdate)
            int updatedCount = 0;
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );

            List<Object[]> updateArgs = new ArrayList<>();
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcodeValue = (String) record.get("IDCode");

                Timestamp endDate = idcodeToDate.get(idcodeValue);
                if (endDate != null) {
                    updateArgs.add(new Object[]{endDate, recordId});
                    updatedCount++;
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET date_stop_work_order = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("✓ Успешно обновлено {} записей в MySQL", updatedCount);
            logDateStatistics(idcodeToDate);
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе даты окончания работ: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Расчет длительности профилактического обслуживания
     */
    public void calculatePreventiveMaintenanceDurationMin() {
        try {
            logger.info("Начало расчета длительности профилактического обслуживания...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Расчет для записей с новыми датами (без длительности)
            String query1 = "UPDATE pm_maintenance_records " +
                          "SET preventive_maintenance_duration_min = " +
                          "TIMESTAMPDIFF(MINUTE, date_start_work_order, date_stop_work_order) " +
                          "WHERE date_start_work_order IS NOT NULL " +
                          "AND date_stop_work_order IS NOT NULL " +
                          "AND preventive_maintenance_duration_min IS NULL " +
                          "AND date_stop_work_order >= date_start_work_order " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int newCalculated = mysqlJdbcTemplate.update(query1, period.startInclusive, period.endExclusive);
            
            // Пересчет для записей с измененными датами
            String query2 = "UPDATE pm_maintenance_records " +
                          "SET preventive_maintenance_duration_min = " +
                          "TIMESTAMPDIFF(MINUTE, date_start_work_order, date_stop_work_order) " +
                          "WHERE date_start_work_order IS NOT NULL " +
                          "AND date_stop_work_order IS NOT NULL " +
                          "AND preventive_maintenance_duration_min IS NOT NULL " +
                          "AND preventive_maintenance_duration_min != TIMESTAMPDIFF(MINUTE, date_start_work_order, date_stop_work_order) " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int recalculated = mysqlJdbcTemplate.update(query2, period.startInclusive, period.endExclusive);
            
            // Обнуление для записей где даты стали NULL
            String query3 = "UPDATE pm_maintenance_records " +
                          "SET preventive_maintenance_duration_min = NULL " +
                          "WHERE preventive_maintenance_duration_min IS NOT NULL " +
                          "AND (date_start_work_order IS NULL OR date_stop_work_order IS NULL) " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int nullified = mysqlJdbcTemplate.update(query3, period.startInclusive, period.endExclusive);
            
            logger.info("📊 РЕЗУЛЬТАТЫ РАСЧЕТА ДЛИТЕЛЬНОСТИ:");
            logger.info("  ✅ Новых рассчитано: {}", newCalculated);
            logger.info("  🔄 Пересчитано: {}", recalculated);
            logger.info("  🗑️  Обнулено: {}", nullified);
            logger.info("  📈 Всего обработано: {}", (newCalculated + recalculated + nullified));
            
        } catch (Exception e) {
            logger.error("Ошибка при расчете длительности: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Безопасный перенос комментариев
     */
    public void transferCommentsSafe() {
        try {
            logger.info("Начало безопасного переноса комментариев...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем комментарии из SQL Server батчами (лимит 2100 параметров)
            Map<String, String> idcodeToComment = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.IDCode, wo.Comment " +
                    "FROM WOM_WorkOrder wo " +
                    "WHERE wo.IDCode IN (" + inPlaceholders + ")";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToComment.put((String) row.get("IDCode"), (String) row.get("Comment"));
                }
            }
            
            logger.info("Найдено {} записей в SQL Server", idcodeToComment.size());
            
            // Обновляем данные в MySQL с безопасным сравнением
            int newComments = 0;
            int updatedComments = 0;
            int unchangedComments = 0;

            List<Object[]> updateArgs = new ArrayList<>();
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode, comment FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );
            
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcode = (String) record.get("IDCode");
                String currentComment = (String) record.get("comment");
                
                if (idcodeToComment.containsKey(idcode)) {
                    String newComment = idcodeToComment.get(idcode);
                    
                    // Безопасное сравнение с учетом NULL
                    if (!Objects.equals(currentComment, newComment)) {
                        updateArgs.add(new Object[]{newComment, recordId}); // newComment может быть NULL
                        if (currentComment == null && newComment != null) {
                            newComments++;
                        } else {
                            updatedComments++;
                        }
                    } else {
                        unchangedComments++;
                    }
                } else {
                    unchangedComments++;
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET comment = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ПЕРЕНОСА КОММЕНТАРИЕВ:");
            logger.info("  ✅ Новых комментариев: {}", newComments);
            logger.info("  🔄 Обновленных комментариев: {}", updatedComments);
            logger.info("  ⏸️  Без изменений: {}", unchangedComments);
            logger.info("  📈 Всего обработано: {}", (newComments + updatedComments + unchangedComments));
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе комментариев: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Безопасный перенос ремонтников
     */
    public void transferMaintainersSafe() {
        try {
            logger.info("Начало переноса ремонтников...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем ремонтников из SQL Server батчами (лимит 2100 параметров)
            Map<String, String> idcodeToMaintainers = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT swt.IDCode, swt.Maintainers " +
                    "FROM SYS_Flat_WOWorkingTime swt " +
                    "WHERE swt.IDCode IN (" + inPlaceholders + ")";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToMaintainers.put((String) row.get("IDCode"), (String) row.get("Maintainers"));
                }
            }
            
            logger.info("Найдено {} записей в SQL Server", idcodeToMaintainers.size());
            
            if (idcodeToMaintainers.isEmpty()) {
                logger.warn("Нет данных о ремонтниках в SQL Server");
                return;
            }
            
            // Обновляем данные в MySQL с безопасным сравнением
            int newMaintainers = 0;
            int updatedMaintainers = 0;
            int unchangedMaintainers = 0;

            List<Object[]> updateArgs = new ArrayList<>();
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode, maintainers FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );
            
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcode = (String) record.get("IDCode");
                String currentMaintainers = (String) record.get("maintainers");
                
                if (idcodeToMaintainers.containsKey(idcode)) {
                    String newMaintainersValue = idcodeToMaintainers.get(idcode);
                    
                    // Безопасное сравнение с учетом NULL
                    if (!Objects.equals(currentMaintainers, newMaintainersValue)) {
                        updateArgs.add(new Object[]{newMaintainersValue, recordId}); // значение может быть NULL
                        if (currentMaintainers == null && newMaintainersValue != null) {
                            newMaintainers++;
                        } else {
                            updatedMaintainers++;
                        }
                    } else {
                        unchangedMaintainers++;
                    }
                } else {
                    unchangedMaintainers++;
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET maintainers = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ПЕРЕНОСА РЕМОНТНИКОВ:");
            logger.info("  ✅ Новых записей: {}", newMaintainers);
            logger.info("  🔄 Обновленных записей: {}", updatedMaintainers);
            logger.info("  ⏸️  Без изменений: {}", unchangedMaintainers);
            logger.info("  📈 Всего обработано: {}", (newMaintainers + updatedMaintainers + unchangedMaintainers));
            
            logMaintainersStatistics(idcodeToMaintainers);
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе ремонтников: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос предлагаемой планируемой даты
     */
    public void transferScheduledProposedDate() {
        try {
            logger.info("Начало переноса предлагаемой планируемой даты...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем предлагаемые даты из SQL Server батчами (лимит 2100 параметров)
            Map<String, Timestamp> idcodeToProposedDate = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.IDCode, wo.ScheduledTimeProposed " +
                    "FROM WOM_WorkOrder wo " +
                    "WHERE wo.IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.ScheduledTimeProposed IS NOT NULL";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToProposedDate.put((String) row.get("IDCode"), (Timestamp) row.get("ScheduledTimeProposed"));
                }
            }
            
            logger.info("Найдено {} записей с предлагаемыми датами в SQL Server", idcodeToProposedDate.size());
            
            if (idcodeToProposedDate.isEmpty()) {
                logger.warn("Нет предлагаемых дат для переноса");
                return;
            }
            
            // Обновляем данные в MySQL с обработкой изменений
            int newDates = 0;
            int updatedDates = 0;
            int unchangedDates = 0;
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode, scheduled_proposed_date FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );

            List<Object[]> updateArgs = new ArrayList<>();

            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcode = (String) record.get("IDCode");
                Timestamp currentDate = convertToTimestamp(record.get("scheduled_proposed_date"));
                
                if (idcodeToProposedDate.containsKey(idcode)) {
                    Timestamp newDateValue = idcodeToProposedDate.get(idcode);
                    
                    // Безопасное сравнение дат (игнорируя время)
                    if (currentDate == null && newDateValue != null) {
                        updateArgs.add(new Object[]{newDateValue, recordId});
                        newDates++;
                    } else if (currentDate != null && newDateValue != null) {
                        if (!formatDate(currentDate).equals(formatDate(newDateValue))) {
                            updateArgs.add(new Object[]{newDateValue, recordId});
                            updatedDates++;
                        } else {
                            unchangedDates++;
                        }
                    } else {
                        unchangedDates++;
                    }
                } else {
                    unchangedDates++;
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET scheduled_proposed_date = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ПЕРЕНОСА ПРЕДЛАГАЕМЫХ ДАТ:");
            logger.info("  ✅ Новых дат: {}", newDates);
            logger.info("  🔄 Обновленных дат: {}", updatedDates);
            logger.info("  ⏸️  Без изменений: {}", unchangedDates);
            logger.info("  📈 Всего обработано: {}", (newDates + updatedDates + unchangedDates));
            
            logDatesStatistics(idcodeToProposedDate);
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе предлагаемых дат: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос планируемой даты
     */
    public void transferScheduledDate() {
        try {
            logger.info("Начало переноса планируемой даты...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем планируемые даты из SQL Server батчами (лимит 2100 параметров)
            Map<String, Timestamp> idcodeToScheduledDate = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.IDCode, wo.ScheduledTime " +
                    "FROM WOM_WorkOrder wo " +
                    "WHERE wo.IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.ScheduledTime IS NOT NULL";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToScheduledDate.put((String) row.get("IDCode"), (Timestamp) row.get("ScheduledTime"));
                }
            }
            
            logger.info("Найдено {} записей с планируемыми датами в SQL Server", idcodeToScheduledDate.size());
            
            if (idcodeToScheduledDate.isEmpty()) {
                logger.warn("Нет планируемых дат для переноса");
                return;
            }
            
            // Обновляем данные в MySQL с обработкой изменений
            int newDates = 0;
            int updatedDates = 0;
            int unchangedDates = 0;
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode, scheduled_date FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );

            List<Object[]> updateArgs = new ArrayList<>();

            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcode = (String) record.get("IDCode");
                Timestamp currentDate = convertToTimestamp(record.get("scheduled_date"));
                
                if (idcodeToScheduledDate.containsKey(idcode)) {
                    Timestamp newDateValue = idcodeToScheduledDate.get(idcode);
                    
                    // Безопасное сравнение дат (игнорируя время)
                    if (currentDate == null && newDateValue != null) {
                        updateArgs.add(new Object[]{newDateValue, recordId});
                        newDates++;
                    } else if (currentDate != null && newDateValue != null) {
                        if (!formatDate(currentDate).equals(formatDate(newDateValue))) {
                            updateArgs.add(new Object[]{newDateValue, recordId});
                            updatedDates++;
                        } else {
                            unchangedDates++;
                        }
                    } else {
                        unchangedDates++;
                    }
                } else {
                    // Поведение как в улучшенном скрипте: если в SQL Server нет планируемой даты, а в MySQL она есть — очищаем.
                    if (currentDate != null) {
                        updateArgs.add(new Object[]{null, recordId});
                        updatedDates++;
                    } else {
                        unchangedDates++;
                    }
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET scheduled_date = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ПЕРЕНОСА ПЛАНИРУЕМЫХ ДАТ:");
            logger.info("  ✅ Новых дат: {}", newDates);
            logger.info("  🔄 Обновленных дат: {}", updatedDates);
            logger.info("  ⏸️  Без изменений: {}", unchangedDates);
            logger.info("  📈 Всего обработано: {}", (newDates + updatedDates + unchangedDates));
            
            logDatesStatistics(idcodeToScheduledDate);
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе планируемых дат: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Расчет разницы в днях между планируемой и предлагаемой датами
     */
    public void calculateDeltaSchedulingDays() {
        try {
            logger.info("Начало расчета разницы в днях между планируемой и предлагаемой датами...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Вычисляем разницу в днях и обновляем записи
            String query1 = "UPDATE pm_maintenance_records " +
                          "SET delta_scheduling_days = " +
                          "DATEDIFF(scheduled_date, scheduled_proposed_date) " +
                          "WHERE scheduled_date IS NOT NULL " +
                          "AND scheduled_proposed_date IS NOT NULL " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int calculatedCount = mysqlJdbcTemplate.update(query1, period.startInclusive, period.endExclusive);
            
            // Обнуляем разницу для записей, где одна из дат стала NULL
            String query2 = "UPDATE pm_maintenance_records " +
                          "SET delta_scheduling_days = NULL " +
                          "WHERE delta_scheduling_days IS NOT NULL " +
                          "AND (scheduled_date IS NULL OR scheduled_proposed_date IS NULL) " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int nullifiedCount = mysqlJdbcTemplate.update(query2, period.startInclusive, period.endExclusive);
            
            logger.info("📊 РЕЗУЛЬТАТЫ РАСЧЕТА РАЗНИЦЫ ДАТ:");
            logger.info("  ✅ Рассчитано записей: {}", calculatedCount);
            logger.info("  🗑️  Обнулено записей: {}", nullifiedCount);
            
            if (calculatedCount > 0) {
                // Получаем статистику по рассчитанным разницам
                Map<String, Object> stats = mysqlJdbcTemplate.queryForMap(
                    "SELECT " +
                    "COUNT(*) as total_calculated, " +
                    "MIN(delta_scheduling_days) as min_delta, " +
                    "MAX(delta_scheduling_days) as max_delta, " +
                    "AVG(delta_scheduling_days) as avg_delta, " +
                    "SUM(CASE WHEN delta_scheduling_days > 0 THEN 1 ELSE 0 END) as positive_delta, " +
                    "SUM(CASE WHEN delta_scheduling_days < 0 THEN 1 ELSE 0 END) as negative_delta, " +
                    "SUM(CASE WHEN delta_scheduling_days = 0 THEN 1 ELSE 0 END) as zero_delta " +
                    "FROM pm_maintenance_records " +
                    "WHERE delta_scheduling_days IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                    period.startInclusive,
                    period.endExclusive
                );
                
                logger.info("📈 СТАТИСТИКА РАЗНИЦЫ В ДНЯХ:");
                logger.info("  Всего рассчитано: {} записей", stats.get("total_calculated"));
                logger.info("  Минимальная разница: {} дней", stats.get("min_delta"));
                logger.info("  Максимальная разница: {} дней", stats.get("max_delta"));
                logger.info("  Средняя разница: {} дней", 
                          stats.get("avg_delta") != null ? String.format("%.2f", ((Number) stats.get("avg_delta")).doubleValue()) : "0");
                logger.info("  Планируемая > Предлагаемой: {} записей", stats.get("positive_delta"));
                logger.info("  Планируемая < Предлагаемой: {} записей", stats.get("negative_delta"));
                logger.info("  Даты равны: {} записей", stats.get("zero_delta"));
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при расчете разницы дат: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Расчет задержки отчета ППР
     */
    public void calculatePmReportDelayDays() {
        try {
            logger.info("Начало расчета задержки отчета ППР...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Вычисляем разницу в днях между датой начала и планируемой датой
            String query1 = "UPDATE pm_maintenance_records " +
                          "SET pm_report_delay_days = " +
                          "DATEDIFF(date_start_work_order, scheduled_date) " +
                          "WHERE date_start_work_order IS NOT NULL " +
                          "AND scheduled_date IS NOT NULL " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int calculatedCount = mysqlJdbcTemplate.update(query1, period.startInclusive, period.endExclusive);
            
            // Обнуляем задержку для записей, где одна из дат стала NULL
            String query2 = "UPDATE pm_maintenance_records " +
                          "SET pm_report_delay_days = NULL " +
                          "WHERE pm_report_delay_days IS NOT NULL " +
                          "AND (date_start_work_order IS NULL OR scheduled_date IS NULL) " +
                          "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int nullifiedCount = mysqlJdbcTemplate.update(query2, period.startInclusive, period.endExclusive);
            
            logger.info("📊 РЕЗУЛЬТАТЫ РАСЧЕТА ЗАДЕРЖКИ ППР:");
            logger.info("  ✅ Рассчитано записей: {}", calculatedCount);
            logger.info("  🗑️  Обнулено записей: {}", nullifiedCount);
            
            // Получаем статистику по рассчитанным задержкам
            if (calculatedCount > 0) {
                Map<String, Object> stats = mysqlJdbcTemplate.queryForMap(
                    "SELECT " +
                    "COUNT(*) as total_calculated, " +
                    "MIN(pm_report_delay_days) as min_delay, " +
                    "MAX(pm_report_delay_days) as max_delay, " +
                    "AVG(pm_report_delay_days) as avg_delay, " +
                    "SUM(CASE WHEN pm_report_delay_days > 0 THEN 1 ELSE 0 END) as delayed_count, " +
                    "SUM(CASE WHEN pm_report_delay_days < 0 THEN 1 ELSE 0 END) as early_count, " +
                    "SUM(CASE WHEN pm_report_delay_days = 0 THEN 1 ELSE 0 END) as on_time_count " +
                    "FROM pm_maintenance_records " +
                    "WHERE pm_report_delay_days IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                    period.startInclusive,
                    period.endExclusive
                );
                
                logger.info("📈 СТАТИСТИКА ЗАДЕРЖКИ ОТЧЕТОВ:");
                logger.info("  Всего рассчитано: {} записей", stats.get("total_calculated"));
                logger.info("  Минимальная задержка: {} дней", stats.get("min_delay"));
                logger.info("  Максимальная задержка: {} дней", stats.get("max_delay"));
                logger.info("  Средняя задержка: {} дней", 
                          stats.get("avg_delay") != null ? String.format("%.2f", ((Number) stats.get("avg_delay")).doubleValue()) : "0");
                logger.info("  🔴 С опозданием: {} записей", stats.get("delayed_count"));
                logger.info("  🟢 Досрочно: {} записей", stats.get("early_count"));
                logger.info("  ⚪ В срок: {} записей", stats.get("on_time_count"));
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при расчете задержки ППР: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос предполагаемой продолжительности
     */
    public void transferEstimatedDuration() {
        try {
            logger.info("Начало переноса предполагаемой продолжительности...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем предполагаемую продолжительность из SQL Server батчами (лимит 2100 параметров)
            Map<String, Integer> idcodeToDuration = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.IDCode, wo.Duration " +
                    "FROM WOM_WorkOrder wo " +
                    "WHERE wo.IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.Duration IS NOT NULL";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToDuration.put((String) row.get("IDCode"), ((Number) row.get("Duration")).intValue());
                }
            }
            
            logger.info("Найдено {} записей с предполагаемой продолжительностью в SQL Server", idcodeToDuration.size());
            
            if (idcodeToDuration.isEmpty()) {
                logger.warn("Нет данных о предполагаемой продолжительности в SQL Server");
                return;
            }
            
            // Обновляем данные в MySQL с обработкой изменений
            int newDurations = 0;
            int updatedDurations = 0;
            int unchangedDurations = 0;

            List<Object[]> updateArgs = new ArrayList<>();
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode, wo_estimated_duration_min FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );
            
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcode = (String) record.get("IDCode");
                Integer currentDuration = record.get("wo_estimated_duration_min") != null ? 
                                        ((Number) record.get("wo_estimated_duration_min")).intValue() : null;
                
                if (idcodeToDuration.containsKey(idcode)) {
                    Integer newDurationValue = idcodeToDuration.get(idcode);
                    
                    // Безопасное сравнение продолжительности
                    if (!Objects.equals(currentDuration, newDurationValue)) {
                        updateArgs.add(new Object[]{newDurationValue, recordId});
                        if (currentDuration == null && newDurationValue != null) {
                            newDurations++;
                        } else {
                            updatedDurations++;
                        }
                    } else {
                        unchangedDurations++;
                    }
                } else {
                    unchangedDurations++;
                }
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET wo_estimated_duration_min = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ПЕРЕНОСА ПРЕДПОЛАГАЕМОЙ ПРОДОЛЖИТЕЛЬНОСТИ:");
            logger.info("  ✅ Новых записей: {}", newDurations);
            logger.info("  🔄 Обновленных записей: {}", updatedDurations);
            logger.info("  ⏸️  Без изменений: {}", unchangedDurations);
            logger.info("  📈 Всего обработано: {}", (newDurations + updatedDurations + unchangedDurations));
            
            logDurationStatistics(idcodeToDuration);
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе предполагаемой продолжительности: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Детальный перенос информации о невыполненных операциях
     */
    public void transferOperationsNokDetailed() {
        try {
            logger.info("Начало детального переноса информации о невыполненных операциях...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем количество невыполненных операций для каждого IDCode из SQL Server батчами
            Map<String, Integer> idcodeToNokCount = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.WOM_WorkOrder_IDCode AS IDCode, COUNT(*) AS nok_count " +
                    "FROM WOM_WOOperation wo " +
                    "WHERE wo.WOM_WorkOrder_IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.IsNotDone = 1 " +
                    "GROUP BY wo.WOM_WorkOrder_IDCode";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToNokCount.put((String) row.get("IDCode"),
                        ((Number) row.get("nok_count")).intValue());
                }
            }
            
            logger.info("Найдено {} записей с невыполненными операциями в SQL Server", idcodeToNokCount.size());
            
            // Обновляем данные в MySQL (batchUpdate)
            int updatedCount = 0;
            int totalNokOperations = 0;
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );

            List<Object[]> updateArgs = new ArrayList<>(records.size());
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcodeValue = (String) record.get("IDCode");

                int nokCount = idcodeToNokCount.getOrDefault(idcodeValue, 0);
                updateArgs.add(new Object[]{nokCount, recordId});
                updatedCount++;
                totalNokOperations += nokCount;
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET operations_nok = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ДЕТАЛЬНОГО ПЕРЕНОСА:");
            logger.info("  ✅ Обновлено записей: {}", updatedCount);
            logger.info("  🔴 Всего невыполненных операций: {}", totalNokOperations);
            if (updatedCount > 0) {
                logger.info("  📈 Среднее количество невыполненных операций на запись: {}", 
                          String.format("%.2f", (double) totalNokOperations / updatedCount));
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при детальном переносе информации о невыполненных операциях: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Перенос информации о успешно выполненных операциях
     */
    public void transferOperationsOk() {
        try {
            logger.info("Начало переноса информации о успешно выполненных операциях...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Получаем IDCode из MySQL
            List<String> mysqlIdcodes = mysqlJdbcTemplate.queryForList(
                "SELECT DISTINCT IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                String.class,
                period.startInclusive,
                period.endExclusive
            );
            
            logger.info("Найдено {} уникальных IDCode в MySQL", mysqlIdcodes.size());
            
            if (mysqlIdcodes.isEmpty()) {
                logger.warn("Нет IDCode для обработки");
                return;
            }
            
            // Получаем информацию о успешно выполненных операциях из SQL Server батчами
            Map<String, Integer> idcodeToOkCount = new HashMap<>();
            for (List<String> idcodesBatch : partition(mysqlIdcodes, SQLSERVER_IN_BATCH_SIZE)) {
                String inPlaceholders = placeholders(idcodesBatch.size());
                String sql = "SELECT wo.WOM_WorkOrder_IDCode AS IDCode, COUNT(*) AS ok_count " +
                    "FROM WOM_WOOperation wo " +
                    "WHERE wo.WOM_WorkOrder_IDCode IN (" + inPlaceholders + ") " +
                    "AND wo.IsDone = 1 " +
                    "GROUP BY wo.WOM_WorkOrder_IDCode";

                List<Map<String, Object>> results = sqlServerJdbcTemplate.queryForList(sql, idcodesBatch.toArray());
                for (Map<String, Object> row : results) {
                    idcodeToOkCount.put((String) row.get("IDCode"),
                        ((Number) row.get("ok_count")).intValue());
                }
            }
            
            logger.info("Найдено {} записей с успешно выполненными операциями в SQL Server", idcodeToOkCount.size());
            
            // Обновляем данные в MySQL (batchUpdate)
            int updatedCount = 0;
            int totalOkOperations = 0;
            
            List<Map<String, Object>> records = mysqlJdbcTemplate.queryForList(
                "SELECT id, IDCode FROM pm_maintenance_records WHERE IDCode IS NOT NULL " +
                    "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?",
                period.startInclusive,
                period.endExclusive
            );

            List<Object[]> updateArgs = new ArrayList<>(records.size());
            for (Map<String, Object> record : records) {
                Integer recordId = (Integer) record.get("id");
                String idcodeValue = (String) record.get("IDCode");

                int okCount = idcodeToOkCount.getOrDefault(idcodeValue, 0);
                updateArgs.add(new Object[]{okCount, recordId});
                updatedCount++;
                totalOkOperations += okCount;
            }

            if (!updateArgs.isEmpty()) {
                mysqlBatchUpdateChunked("UPDATE pm_maintenance_records SET operations_ok = ? WHERE id = ?", updateArgs);
            }
            
            logger.info("📊 РЕЗУЛЬТАТЫ ПЕРЕНОСА УСПЕШНО ВЫПОЛНЕННЫХ ОПЕРАЦИЙ:");
            logger.info("  ✅ Обновлено записей: {}", updatedCount);
            logger.info("  🟢 Всего успешно выполненных операций: {}", totalOkOperations);
            if (updatedCount > 0) {
                logger.info("  📈 Среднее количество успешных операций на запись: {}", 
                          String.format("%.2f", (double) totalOkOperations / updatedCount));
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при переносе информации о успешно выполненных операциях: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Расчет общего количества операций
     */
    public void calculateOperationsAll() {
        try {
            logger.info("Начало расчета общего количества операций...");

            PeriodRange period = getUpdatePeriodRange();
            
            // Вычисляем сумму operations_ok и operations_nok
            String updateQuery = "UPDATE pm_maintenance_records " +
                              "SET operations_all = " +
                              "COALESCE(operations_ok, 0) + COALESCE(operations_nok, 0) " +
                              "WHERE (operations_ok IS NOT NULL OR operations_nok IS NOT NULL) " +
                              "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int calculatedCount = mysqlJdbcTemplate.update(updateQuery, period.startInclusive, period.endExclusive);
            
            // Обнуляем общее количество для записей, где обе колонки NULL
            String nullifyQuery = "UPDATE pm_maintenance_records " +
                                "SET operations_all = NULL " +
                                "WHERE operations_all IS NOT NULL " +
                                "AND operations_ok IS NULL " +
                                "AND operations_nok IS NULL " +
                                "AND " + MYSQL_PM_DATE_REF + " >= ? AND " + MYSQL_PM_DATE_REF + " < ?";
            
            int nullifiedCount = mysqlJdbcTemplate.update(nullifyQuery, period.startInclusive, period.endExclusive);
            
            logger.info("📊 РЕЗУЛЬТАТЫ РАСЧЕТА ОБЩЕГО КОЛИЧЕСТВА ОПЕРАЦИЙ:");
            logger.info("  ✅ Рассчитано записей: {}", calculatedCount);
            logger.info("  🗑️  Обнулено записей: {}", nullifiedCount);
            
        } catch (Exception e) {
            logger.error("Ошибка при расчете общего количества операций: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Вспомогательные методы для статистики

    private void logDateStatistics(Map<String, Timestamp> dateDict) {
        if (dateDict.isEmpty()) return;
        
        List<Timestamp> dates = new ArrayList<>(dateDict.values());
        Timestamp minDate = Collections.min(dates);
        Timestamp maxDate = Collections.max(dates);
        
        logger.info("Статистика дат:");
        logger.info("  Самая ранняя дата: {}", minDate);
        logger.info("  Самая поздняя дата: {}", maxDate);
        logger.info("  Всего уникальных дат: {}", new HashSet<>(dates).size());
    }

    private void logMaintainersStatistics(Map<String, String> maintainersDict) {
        long maintainersWithData = maintainersDict.values().stream()
                .filter(maintainers -> maintainers != null)
                .count();
        
        logger.info("📈 СТАТИСТИКА РЕМОНТНИКОВ:");
        logger.info("  Всего записей: {}", maintainersDict.size());
        logger.info("  С данными о ремонтниках: {}", maintainersWithData);
        logger.info("  Пустых записей: {}", (maintainersDict.size() - maintainersWithData));
    }

    private void logDatesStatistics(Map<String, Timestamp> datesDict) {
        List<Timestamp> datesWithData = new ArrayList<>();
        for (Timestamp date : datesDict.values()) {
            if (date != null) {
                datesWithData.add(date);
            }
        }
        
        if (!datesWithData.isEmpty()) {
            Timestamp minDate = Collections.min(datesWithData);
            Timestamp maxDate = Collections.max(datesWithData);
            
            long daysRange = (maxDate.getTime() - minDate.getTime()) / (1000 * 60 * 60 * 24);
            
            logger.info("📅 СТАТИСТИКА ДАТ:");
            logger.info("  Всего дат: {}", datesWithData.size());
            logger.info("  Самая ранняя дата: {}", minDate);
            logger.info("  Самая поздняя дата: {}", maxDate);
            logger.info("  Диапазон: {} дней", daysRange);
        }
    }

    private void logDurationStatistics(Map<String, Integer> durationDict) {
        List<Integer> durationsWithData = new ArrayList<>();
        for (Integer duration : durationDict.values()) {
            if (duration != null) {
                durationsWithData.add(duration);
            }
        }
        
        if (!durationsWithData.isEmpty()) {
            int minDuration = Collections.min(durationsWithData);
            int maxDuration = Collections.max(durationsWithData);
            double avgDuration = durationsWithData.stream().mapToInt(Integer::intValue).average().orElse(0);
            
            // Конвертируем минуты в часы для лучшего восприятия
            double minHours = minDuration / 60.0;
            double maxHours = maxDuration / 60.0;
            double avgHours = avgDuration / 60.0;
            
            logger.info("⏱️ СТАТИСТИКА ПРЕДПОЛАГАЕМОЙ ПРОДОЛЖИТЕЛЬНОСТИ:");
            logger.info("  Всего записей: {}", durationsWithData.size());
            logger.info("  Минимальная: {} мин ({} ч)", minDuration, String.format("%.1f", minHours));
            logger.info("  Максимальная: {} мин ({} ч)", maxDuration, String.format("%.1f", maxHours));
            logger.info("  Средняя: {} мин ({} ч)", String.format("%.1f", avgDuration), String.format("%.1f", avgHours));
        }
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) return "";
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(timestamp.getTime()));
    }

    /**
     * Безопасное преобразование Date в Timestamp
     * Обрабатывает случаи, когда поле в MySQL имеет тип DATE (возвращает java.sql.Date)
     * или TIMESTAMP/DATETIME (возвращает java.sql.Timestamp)
     */
    private Timestamp convertToTimestamp(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        if (dateValue instanceof Timestamp) {
            return (Timestamp) dateValue;
        }
        if (dateValue instanceof Date) {
            return new Timestamp(((Date) dateValue).getTime());
        }
        if (dateValue instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) dateValue).getTime());
        }
        // Если это строка, пытаемся распарсить
        if (dateValue instanceof String) {
            try {
                java.util.Date parsedDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse((String) dateValue);
                return new Timestamp(parsedDate.getTime());
            } catch (Exception e) {
                logger.warn("Не удалось преобразовать строку в дату: {}", dateValue);
                return null;
            }
        }
        logger.warn("Неизвестный тип даты: {}", dateValue.getClass().getName());
        return null;
    }
}

