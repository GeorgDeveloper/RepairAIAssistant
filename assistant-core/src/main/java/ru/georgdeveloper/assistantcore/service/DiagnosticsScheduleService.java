package ru.georgdeveloper.assistantcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.georgdeveloper.assistantcore.model.*;
import ru.georgdeveloper.assistantcore.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiagnosticsScheduleService {

    @Autowired
    private DiagnosticsScheduleRepository scheduleRepository;

    @Autowired
    private DiagnosticsScheduleEntryRepository entryRepository;

    @Autowired
    private DiagnosticsTypeRepository typeRepository;

    /**
     * Создает график диагностики на год
     * @param year год
     * @param workersCount количество человек
     * @param shiftDurationHours длительность смены в часах (по умолчанию 7)
     * @param equipmentList список оборудования с количеством диагностик каждого типа
     * @param startDate дата начала распределения (если null, используется 1 января)
     * @return созданный график
     */
    @Transactional
    public DiagnosticsSchedule createYearlySchedule(
            Integer year,
            Integer workersCount,
            Integer shiftDurationHours,
            List<EquipmentDiagnosticsRequest> equipmentList,
            LocalDate startDate) {

        // Проверяем, существует ли уже график на этот год
        Optional<DiagnosticsSchedule> existing = scheduleRepository.findByYear(year);
        if (existing.isPresent()) {
            throw new RuntimeException("График на " + year + " год уже существует");
        }

        // Создаем график
        DiagnosticsSchedule schedule = new DiagnosticsSchedule();
        schedule.setYear(year);
        schedule.setWorkersCount(workersCount);
        schedule.setShiftDurationHours(shiftDurationHours != null ? shiftDurationHours : 7);
        schedule = scheduleRepository.save(schedule);

        // Получаем все активные типы диагностики
        List<DiagnosticsType> types = typeRepository.findByIsActiveTrue();
        Map<String, DiagnosticsType> typeMap = types.stream()
                .collect(Collectors.toMap(DiagnosticsType::getCode, t -> t));

        // Вычисляем общее количество рабочих часов в году
        LocalDate scheduleStartDate = startDate != null ? startDate : LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        List<LocalDate> workingDays = getWorkingDays(scheduleStartDate, endDate);
        int totalWorkingDays = workingDays.size();
        int totalWorkingHours = totalWorkingDays * schedule.getShiftDurationHours() * workersCount;

        // Подсчитываем общее количество часов диагностики и создаем задачи с учетом периода
        int totalDiagnosticsMinutes = 0;
        List<DiagnosticsTask> allTasks = new ArrayList<>();
        
        for (EquipmentDiagnosticsRequest eq : equipmentList) {
            for (Map.Entry<String, Number> entry : eq.getDiagnosticsCounts().entrySet()) {
                String typeCode = entry.getKey();
                // Период в месяцах (может быть дробным: 0.5, 1, 2, 6, 12 и т.д.)
                double periodMonths = entry.getValue().doubleValue();
                
                if (periodMonths < 0.5 || periodMonths > 12) {
                    throw new RuntimeException("Период диагностики должен быть от 0.5 до 12 месяцев для типа '" + typeCode + "'");
                }
                
                DiagnosticsType type = typeMap.get(typeCode);
                if (type == null) {
                    throw new RuntimeException("Тип диагностики с кодом '" + typeCode + "' не найден");
                }
                
                // Используем переданную длительность, если она указана
                Integer durationMinutes = eq.getDiagnosticsDurations() != null 
                    ? eq.getDiagnosticsDurations().get(typeCode) 
                    : null;
                if (durationMinutes == null || durationMinutes <= 0) {
                    durationMinutes = type.getDurationMinutes();
                }
                
                // Вычисляем количество диагностик в год: 12 месяцев / период
                int diagnosticsPerYear = (int) Math.round(12.0 / periodMonths);
                
                // Создаем временный тип с переопределенной длительностью
                DiagnosticsType taskType = new DiagnosticsType();
                taskType.setId(type.getId());
                taskType.setCode(type.getCode());
                taskType.setName(type.getName());
                taskType.setDurationMinutes(durationMinutes);
                taskType.setColorCode(type.getColorCode());
                taskType.setIsActive(type.getIsActive());
                
                // Создаем задачи с указанием периода для равномерного распределения
                for (int i = 0; i < diagnosticsPerYear; i++) {
                    DiagnosticsTask task = new DiagnosticsTask(eq.getEquipment(), eq.getArea(), taskType);
                    task.setPeriodMonths(periodMonths);
                    task.setSequenceNumber(i);
                    task.setTotalCount(diagnosticsPerYear);
                    allTasks.add(task);
                    totalDiagnosticsMinutes += durationMinutes;
                }
            }
        }

        // Проверяем, помещается ли все в рабочие часы
        int totalDiagnosticsHours = (int) Math.ceil(totalDiagnosticsMinutes / 60.0);
        if (totalDiagnosticsHours > totalWorkingHours) {
            throw new RuntimeException(
                    String.format("Недостаточно рабочих часов. Требуется: %d, доступно: %d",
                            totalDiagnosticsHours, totalWorkingHours));
        }

        // Группируем задачи по оборудованию и типу диагностики для равномерного распределения
        Map<String, List<DiagnosticsTask>> tasksByEquipmentAndType = new HashMap<>();
        for (DiagnosticsTask task : allTasks) {
            String key = task.getEquipment() + "|" + task.getType().getCode();
            tasksByEquipmentAndType.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }
        
        // Распределяем задачи по дням с учетом периода и равномерного распределения
        Map<LocalDate, List<DiagnosticsTask>> dailyTasks = new HashMap<>();
        
        for (Map.Entry<String, List<DiagnosticsTask>> entry : tasksByEquipmentAndType.entrySet()) {
            List<DiagnosticsTask> tasks = entry.getValue();
            if (tasks.isEmpty()) continue;
            
            DiagnosticsTask firstTask = tasks.get(0);
            double periodMonths = firstTask.getPeriodMonths();
            int totalCount = firstTask.getTotalCount();
            int taskDurationMinutes = firstTask.getType().getDurationMinutes();
            
            // Вычисляем идеальные даты для диагностик с учетом периода и даты старта
            // Для периода 6 месяцев: первые задачи в первой половине года, вторые - во второй
            List<LocalDate> idealDates = calculateIdealDatesByPeriod(year, periodMonths, tasks.size(), scheduleStartDate);
            
            // Распределяем задачи по идеальным датам с учетом равномерности
            for (int i = 0; i < tasks.size() && i < idealDates.size(); i++) {
                DiagnosticsTask task = tasks.get(i);
                LocalDate idealDate = idealDates.get(i);
                
                // Ищем лучший рабочий день в диапазоне ±2 недели с учетом равномерного распределения
                LocalDate scheduledDate = findBestDateNearIdealFromStart(idealDate, workingDays, dailyTasks, 
                        new HashMap<>(), workersCount, schedule.getShiftDurationHours(), taskDurationMinutes, 
                        task.getEquipment());
                
                if (scheduledDate != null) {
                    dailyTasks.computeIfAbsent(scheduledDate, k -> new ArrayList<>()).add(task);
                } else {
                    // Если не нашли в диапазоне ±2 недели, ищем ближайший свободный день от начала года
                    LocalDate fallbackDate = findBestDateFromStart(workingDays, dailyTasks, 
                            new HashMap<>(), workersCount, schedule.getShiftDurationHours(), 
                            taskDurationMinutes, task.getEquipment());
                    if (fallbackDate != null) {
                        dailyTasks.computeIfAbsent(fallbackDate, k -> new ArrayList<>()).add(task);
                    } else {
                        throw new RuntimeException("Не удалось разместить диагностику для " + task.getEquipment());
                    }
                }
            }
        }

        // Сохраняем записи в базу данных
        List<DiagnosticsScheduleEntry> entries = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DiagnosticsTask>> entry : dailyTasks.entrySet()) {
            for (DiagnosticsTask task : entry.getValue()) {
                DiagnosticsScheduleEntry scheduleEntry = new DiagnosticsScheduleEntry();
                scheduleEntry.setSchedule(schedule);
                scheduleEntry.setEquipment(task.getEquipment());
                scheduleEntry.setArea(task.getArea());
                scheduleEntry.setDiagnosticsType(task.getType());
                scheduleEntry.setScheduledDate(entry.getKey());
                scheduleEntry.setIsCompleted(false);
                entries.add(scheduleEntry);
            }
        }
        
        entryRepository.saveAll(entries);

        return schedule;
    }

    /**
     * Добавляет оборудование в существующий график
     * @param scheduleId ID графика
     * @param equipmentList список оборудования с количеством диагностик каждого типа
     * @param startDate дата начала распределения (если null, используется 1 января)
     * @return обновленный график
     */
    @Transactional
    public DiagnosticsSchedule addEquipmentToSchedule(
            Long scheduleId,
            List<EquipmentDiagnosticsRequest> equipmentList,
            LocalDate startDate) {

        DiagnosticsSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("График не найден"));

        // Получаем все активные типы диагностики
        List<DiagnosticsType> types = typeRepository.findByIsActiveTrue();
        Map<String, DiagnosticsType> typeMap = types.stream()
                .collect(Collectors.toMap(DiagnosticsType::getCode, t -> t));

        // Получаем рабочие дни года
        LocalDate scheduleStartDate = startDate != null ? startDate : LocalDate.of(schedule.getYear(), 1, 1);
        LocalDate endDate = LocalDate.of(schedule.getYear(), 12, 31);
        List<LocalDate> workingDays = getWorkingDays(scheduleStartDate, endDate);

        // Получаем существующие записи для расчета загрузки и проверки дубликатов
        List<DiagnosticsScheduleEntry> existingEntries = entryRepository.findByScheduleId(scheduleId);
        Map<LocalDate, List<DiagnosticsScheduleEntry>> existingByDate = existingEntries.stream()
                .collect(Collectors.groupingBy(DiagnosticsScheduleEntry::getScheduledDate));
        
        // Группируем существующие записи по оборудованию и типу диагностики
        // Ключ: "оборудование|тип диагностики"
        Map<String, List<DiagnosticsScheduleEntry>> existingByEquipmentAndType = existingEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getEquipment() + "|" + e.getDiagnosticsType().getCode()));

        // Создаем список задач для нового оборудования
        List<DiagnosticsTask> allTasks = new ArrayList<>();
        
        for (EquipmentDiagnosticsRequest eq : equipmentList) {
            for (Map.Entry<String, Number> entry : eq.getDiagnosticsCounts().entrySet()) {
                String typeCode = entry.getKey();
                // Период в месяцах (может быть дробным: 0.5, 1, 2, 6, 12 и т.д.)
                double periodMonths = entry.getValue().doubleValue();
                
                if (periodMonths < 0.5 || periodMonths > 12) {
                    throw new RuntimeException("Период диагностики должен быть от 0.5 до 12 месяцев для типа '" + typeCode + "'");
                }
                
                DiagnosticsType type = typeMap.get(typeCode);
                if (type == null) {
                    throw new RuntimeException("Тип диагностики с кодом '" + typeCode + "' не найден");
                }
                
                // Используем переданную длительность, если она указана
                Integer durationMinutes = eq.getDiagnosticsDurations() != null 
                    ? eq.getDiagnosticsDurations().get(typeCode) 
                    : null;
                if (durationMinutes == null || durationMinutes <= 0) {
                    durationMinutes = type.getDurationMinutes();
                }
                
                // Вычисляем количество диагностик в год: 12 месяцев / период
                int diagnosticsPerYear = (int) Math.round(12.0 / periodMonths);
                
                // Создаем временный тип с переопределенной длительностью
                DiagnosticsType taskType = new DiagnosticsType();
                taskType.setId(type.getId());
                taskType.setCode(type.getCode());
                taskType.setName(type.getName());
                taskType.setDurationMinutes(durationMinutes);
                taskType.setColorCode(type.getColorCode());
                taskType.setIsActive(type.getIsActive());
                
                // Создаем задачи с указанием периода
                for (int i = 0; i < diagnosticsPerYear; i++) {
                    DiagnosticsTask task = new DiagnosticsTask(eq.getEquipment(), eq.getArea(), taskType);
                    task.setPeriodMonths(periodMonths);
                    task.setSequenceNumber(i);
                    task.setTotalCount(diagnosticsPerYear);
                    allTasks.add(task);
                }
            }
        }

        // Группируем задачи по оборудованию и типу диагностики
        Map<String, List<DiagnosticsTask>> tasksByEquipmentAndType = new HashMap<>();
        for (DiagnosticsTask task : allTasks) {
            String key = task.getEquipment() + "|" + task.getType().getCode();
            tasksByEquipmentAndType.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }
        
        // Удаляем существующие записи для оборудования и типов диагностики, которые перезаписываются
        List<Long> entriesToDelete = new ArrayList<>();
        for (Map.Entry<String, List<DiagnosticsTask>> entry : tasksByEquipmentAndType.entrySet()) {
            String key = entry.getKey();
            List<DiagnosticsScheduleEntry> existingForEquipmentAndType = existingByEquipmentAndType.get(key);
            if (existingForEquipmentAndType != null && !existingForEquipmentAndType.isEmpty()) {
                // Если оборудование и тип диагностики уже есть - удаляем старые записи
                for (DiagnosticsScheduleEntry existingEntry : existingForEquipmentAndType) {
                    entriesToDelete.add(existingEntry.getId());
                }
            }
        }
        
        if (!entriesToDelete.isEmpty()) {
            entryRepository.deleteAllById(entriesToDelete);
            // Обновляем карту существующих записей после удаления
            existingEntries = entryRepository.findByScheduleId(scheduleId);
            existingByDate = existingEntries.stream()
                    .collect(Collectors.groupingBy(DiagnosticsScheduleEntry::getScheduledDate));
        }
        
        // Распределяем задачи по дням с приоритетом от начала года
        Map<LocalDate, List<DiagnosticsTask>> dailyTasks = new HashMap<>();
        
        for (Map.Entry<String, List<DiagnosticsTask>> entry : tasksByEquipmentAndType.entrySet()) {
            List<DiagnosticsTask> tasks = entry.getValue();
            if (tasks.isEmpty()) continue;
            
            DiagnosticsTask firstTask = tasks.get(0);
            double periodMonths = firstTask.getPeriodMonths();
            int totalCount = firstTask.getTotalCount();
            
            // Вычисляем идеальные даты для диагностик с учетом периода и даты старта
            List<LocalDate> idealDates = calculateIdealDatesByPeriod(schedule.getYear(), periodMonths, tasks.size(), scheduleStartDate);
            
            // Распределяем задачи по идеальным датам с учетом равномерности
            for (int i = 0; i < tasks.size() && i < idealDates.size(); i++) {
                DiagnosticsTask task = tasks.get(i);
                LocalDate idealDate = idealDates.get(i);
                int taskDurationMinutes = task.getType().getDurationMinutes();
                
                // Ищем лучший рабочий день в диапазоне ±2 недели с учетом равномерного распределения
                LocalDate scheduledDate = findBestDateNearIdealFromStart(idealDate, workingDays, dailyTasks, 
                        existingByDate, schedule.getWorkersCount(), schedule.getShiftDurationHours(), 
                        taskDurationMinutes, task.getEquipment());
                
                if (scheduledDate != null) {
                    dailyTasks.computeIfAbsent(scheduledDate, k -> new ArrayList<>()).add(task);
                } else {
                    // Если не нашли в диапазоне ±2 недели, ищем ближайший свободный день от начала года
                    LocalDate fallbackDate = findBestDateFromStart(workingDays, dailyTasks, existingByDate, 
                            schedule.getWorkersCount(), schedule.getShiftDurationHours(), 
                            task.getType().getDurationMinutes(), task.getEquipment());
                    if (fallbackDate != null) {
                        dailyTasks.computeIfAbsent(fallbackDate, k -> new ArrayList<>()).add(task);
                    } else {
                        throw new RuntimeException("Не удалось разместить диагностику для " + task.getEquipment() + ". Возможно, недостаточно свободного времени.");
                    }
                }
            }
        }

        // Сохраняем новые записи в базу данных
        List<DiagnosticsScheduleEntry> entries = new ArrayList<>();
        for (Map.Entry<LocalDate, List<DiagnosticsTask>> entry : dailyTasks.entrySet()) {
            for (DiagnosticsTask task : entry.getValue()) {
                DiagnosticsScheduleEntry scheduleEntry = new DiagnosticsScheduleEntry();
                scheduleEntry.setSchedule(schedule);
                scheduleEntry.setEquipment(task.getEquipment());
                scheduleEntry.setArea(task.getArea());
                scheduleEntry.setDiagnosticsType(task.getType());
                scheduleEntry.setScheduledDate(entry.getKey());
                scheduleEntry.setIsCompleted(false);
                entries.add(scheduleEntry);
            }
        }
        
        entryRepository.saveAll(entries);

        return schedule;
    }

    /**
     * Находит лучший рабочий день в диапазоне ±2 недели от идеальной даты с учетом существующей загрузки
     * @deprecated Используется findBestDateNearIdealFromStart для приоритета от начала года
     */
    @Deprecated
    private LocalDate findBestDateNearIdealForAdding(LocalDate idealDate, List<LocalDate> workingDays,
                                                     Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                                     Map<LocalDate, List<DiagnosticsScheduleEntry>> existingByDate,
                                                     int workersCount, int shiftHours, int taskMinutes) {
        LocalDate bestDate = null;
        int minScore = Integer.MAX_VALUE;
        
        // Диапазон ±2 недели (14 дней)
        LocalDate minDate = idealDate.minusDays(14);
        LocalDate maxDate = idealDate.plusDays(14);
        
        for (LocalDate date : workingDays) {
            if (date.isBefore(minDate) || date.isAfter(maxDate)) {
                continue;
            }
            
            int existingDayMinutes = existingByDate.getOrDefault(date, Collections.emptyList())
                    .stream()
                    .mapToInt(e -> e.getDiagnosticsType().getDurationMinutes())
                    .sum();
            int newDayMinutes = dailyTasks.getOrDefault(date, Collections.emptyList())
                    .stream()
                    .mapToInt(t -> t.getType().getDurationMinutes())
                    .sum();
            int totalDayMinutes = existingDayMinutes + newDayMinutes;
            int availableMinutes = workersCount * shiftHours * 60;
            
            if (totalDayMinutes + taskMinutes <= availableMinutes) {
                // Предпочитаем даты ближе к идеальной
                long daysFromIdeal = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(idealDate, date));
                int score = totalDayMinutes + (int) (daysFromIdeal * 10); // Штраф за удаление от идеальной даты
                
                if (score < minScore) {
                    minScore = score;
                    bestDate = date;
                }
            }
        }
        
        return bestDate;
    }
    
    /**
     * Находит лучший день для размещения задачи с учетом существующей загрузки
     * @deprecated Используется findBestDateFromStart для приоритета от начала года
     */
    @Deprecated
    private LocalDate findBestDateForAdding(Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                           Map<LocalDate, List<DiagnosticsScheduleEntry>> existingByDate,
                                           List<LocalDate> workingDays,
                                           int workersCount,
                                           int shiftHours,
                                           int taskMinutes) {
        LocalDate bestDate = null;
        int minLoad = Integer.MAX_VALUE;

        for (LocalDate date : workingDays) {
            int existingDayMinutes = existingByDate.getOrDefault(date, Collections.emptyList())
                    .stream()
                    .mapToInt(e -> e.getDiagnosticsType().getDurationMinutes())
                    .sum();
            int newDayMinutes = dailyTasks.getOrDefault(date, Collections.emptyList())
                    .stream()
                    .mapToInt(t -> t.getType().getDurationMinutes())
                    .sum();
            int totalDayMinutes = existingDayMinutes + newDayMinutes;
            int availableMinutes = workersCount * shiftHours * 60;
            
            if (totalDayMinutes + taskMinutes <= availableMinutes && totalDayMinutes < minLoad) {
                minLoad = totalDayMinutes;
                bestDate = date;
            }
        }

        return bestDate;
    }
    
    /**
     * Находит лучший день для размещения задачи с приоритетом от начала года
     */
    private LocalDate findBestDateFromStart(List<LocalDate> workingDays,
                                           Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                           Map<LocalDate, List<DiagnosticsScheduleEntry>> existingByDate,
                                           int workersCount,
                                           int shiftHours,
                                           int taskMinutes,
                                           String equipment) {
        // Ищем первый доступный день от начала года с правильной проверкой доступности
        for (LocalDate date : workingDays) {
            if (canScheduleTask(date, dailyTasks, existingByDate, workersCount, shiftHours, taskMinutes, equipment)) {
                return date;
            }
        }
        
        return null;
    }
    
    /**
     * Находит лучший рабочий день в диапазоне ±2 недели от идеальной даты с приоритетом от начала года
     */
    /**
     * Находит лучший рабочий день в диапазоне ±2 недели от идеальной даты
     * Учитывает равномерность распределения по месяцам и неделям
     */
    private LocalDate findBestDateNearIdealFromStart(LocalDate idealDate, List<LocalDate> workingDays,
                                                     Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                                     Map<LocalDate, List<DiagnosticsScheduleEntry>> existingByDate,
                                                     int workersCount, int shiftHours, int taskMinutes,
                                                     String equipment) {
        LocalDate bestDate = null;
        double minScore = Double.MAX_VALUE;
        
        // Диапазон ±2 недели (14 дней)
        LocalDate minDate = idealDate.minusDays(14);
        LocalDate maxDate = idealDate.plusDays(14);
        
        // Вычисляем среднюю загрузку по месяцам и неделям для ВСЕГО года (по количеству задач)
        Map<Integer, Integer> monthlyLoad = new HashMap<>();
        Map<Integer, Integer> weeklyLoad = new HashMap<>();
        
        // Подсчитываем текущую загрузку по месяцам и неделям для всего года (количество задач)
        for (LocalDate date : workingDays) {
            int month = date.getMonthValue();
            int weekOfYear = date.get(java.time.temporal.WeekFields.ISO.weekOfYear());
            
            int existingTasksCount = existingByDate.getOrDefault(date, Collections.emptyList()).size();
            int newTasksCount = dailyTasks.getOrDefault(date, Collections.emptyList()).size();
            int totalTasksCount = existingTasksCount + newTasksCount;
            
            monthlyLoad.put(month, monthlyLoad.getOrDefault(month, 0) + totalTasksCount);
            weeklyLoad.put(weekOfYear, weeklyLoad.getOrDefault(weekOfYear, 0) + totalTasksCount);
        }
        
        // Вычисляем среднюю загрузку (по количеству задач)
        double avgMonthlyLoad = monthlyLoad.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double avgWeeklyLoad = weeklyLoad.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
        // Сортируем кандидатов по близости к идеальной дате
        List<LocalDate> candidateDays = workingDays.stream()
                .filter(date -> !date.isBefore(minDate) && !date.isAfter(maxDate))
                .sorted((d1, d2) -> {
                    long diff1 = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(idealDate, d1));
                    long diff2 = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(idealDate, d2));
                    return Long.compare(diff1, diff2);
                })
                .collect(Collectors.toList());
        
        for (LocalDate date : candidateDays) {
            // Проверяем доступность с учетом параллельной работы нескольких человек
            // и ограничения: в один день на одном оборудовании может быть только одна диагностика
            if (canScheduleTask(date, dailyTasks, existingByDate, workersCount, shiftHours, taskMinutes, equipment)) {
                // Вычисляем загрузку месяца и недели с учетом новой задачи
                int month = date.getMonthValue();
                int weekOfYear = date.get(java.time.temporal.WeekFields.ISO.weekOfYear());
                
                // Подсчитываем количество задач (не минуты) для равномерного распределения
                int existingTasksCount = existingByDate.getOrDefault(date, Collections.emptyList()).size();
                int newTasksCount = dailyTasks.getOrDefault(date, Collections.emptyList()).size();
                int totalTasksCount = existingTasksCount + newTasksCount;
                
                int monthTasksWithNew = monthlyLoad.getOrDefault(month, 0) + 1; // +1 задача
                int weekTasksWithNew = weeklyLoad.getOrDefault(weekOfYear, 0) + 1; // +1 задача
                
                // Штраф за удаление от идеальной даты
                long daysFromIdeal = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(idealDate, date));
                double idealDistancePenalty = daysFromIdeal * 5.0;
                
                // Штраф за неравномерность загрузки месяца (предпочитаем месяцы с меньшей загрузкой)
                double monthLoadPenalty = 0;
                if (avgMonthlyLoad > 0) {
                    double monthLoadRatio = monthTasksWithNew / avgMonthlyLoad;
                    // Штрафуем месяцы, которые уже перегружены
                    if (monthLoadRatio > 1.2) {
                        monthLoadPenalty = (monthLoadRatio - 1.2) * 1000;
                    } else if (monthLoadRatio < 0.8) {
                        // Поощряем месяцы с меньшей загрузкой (отрицательный штраф)
                        monthLoadPenalty = (0.8 - monthLoadRatio) * -500;
                    }
                }
                
                // Штраф за неравномерность загрузки недели
                double weekLoadPenalty = 0;
                if (avgWeeklyLoad > 0) {
                    double weekLoadRatio = weekTasksWithNew / avgWeeklyLoad;
                    if (weekLoadRatio > 1.3) {
                        weekLoadPenalty = (weekLoadRatio - 1.3) * 500;
                    } else if (weekLoadRatio < 0.7) {
                        weekLoadPenalty = (0.7 - weekLoadRatio) * -200;
                    }
                }
                
                // Общий счет: чем меньше, тем лучше
                // Приоритет: близость к идеальной дате > равномерность загрузки > текущая загрузка дня
                double score = idealDistancePenalty + monthLoadPenalty + weekLoadPenalty + totalTasksCount * 10;
                
                if (score < minScore) {
                    minScore = score;
                    bestDate = date;
                }
            }
        }
        
        return bestDate;
    }

    /**
     * Проверяет, можно ли разместить задачу на указанную дату с учетом параллельной работы нескольких человек
     * и ограничения: в один день на одном оборудовании может быть только одна диагностика
     * Если 2 человека работают 60 минут, они могут выполнить 2 задачи по 60 минут параллельно
     */
    private boolean canScheduleTask(LocalDate date, 
                                    Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                    Map<LocalDate, List<DiagnosticsScheduleEntry>> existingByDate,
                                    int workersCount, int shiftHours, int taskMinutes,
                                    String equipment) {
        // Проверяем, нет ли уже диагностик для этого оборудования на эту дату
        // Проверяем существующие записи
        for (DiagnosticsScheduleEntry entry : existingByDate.getOrDefault(date, Collections.emptyList())) {
            if (entry.getEquipment().equals(equipment)) {
                return false; // На это оборудование уже запланирована диагностика на эту дату
            }
        }
        
        // Проверяем новые задачи
        for (DiagnosticsTask task : dailyTasks.getOrDefault(date, Collections.emptyList())) {
            if (task.getEquipment().equals(equipment)) {
                return false; // На это оборудование уже запланирована диагностика на эту дату
            }
        }
        
        // Получаем все задачи на этот день
        List<Integer> allTaskDurations = new ArrayList<>();
        
        // Добавляем существующие задачи
        for (DiagnosticsScheduleEntry entry : existingByDate.getOrDefault(date, Collections.emptyList())) {
            allTaskDurations.add(entry.getDiagnosticsType().getDurationMinutes());
        }
        
        // Добавляем новые задачи
        for (DiagnosticsTask task : dailyTasks.getOrDefault(date, Collections.emptyList())) {
            allTaskDurations.add(task.getType().getDurationMinutes());
        }
        
        // Добавляем проверяемую задачу
        allTaskDurations.add(taskMinutes);
        
        // Группируем задачи по длительности для расчета параллельной работы
        Map<Integer, Integer> tasksByDuration = new HashMap<>();
        for (Integer duration : allTaskDurations) {
            tasksByDuration.put(duration, tasksByDuration.getOrDefault(duration, 0) + 1);
        }
        
        // Проверяем, можно ли выполнить все задачи параллельно
        int totalShiftMinutes = shiftHours * 60;
        
        // Проверяем общее количество минут работы
        return canExecuteTasksInParallel(tasksByDuration, workersCount, totalShiftMinutes);
    }
    
    /**
     * Проверяет, можно ли выполнить все задачи параллельно с учетом количества работников и длительности смены
     * Правило: общее количество минут работы не должно превышать workersCount * shiftMinutes
     * Если 1 человек работает 7 часов (420 минут), он может выполнить максимум 420 минут работы
     * Если 2 человека работают 60 минут, они могут выполнить 2 задачи по 60 минут параллельно (120 минут работы)
     */
    private boolean canExecuteTasksInParallel(Map<Integer, Integer> tasksByDuration, 
                                              int workersCount, 
                                              int shiftMinutes) {
        // Подсчитываем общее количество минут работы
        int totalMinutes = 0;
        for (Map.Entry<Integer, Integer> entry : tasksByDuration.entrySet()) {
            int duration = entry.getKey();
            int taskCount = entry.getValue();
            totalMinutes += duration * taskCount;
        }
        
        // Максимальное количество минут работы = workersCount * shiftMinutes
        int maxWorkMinutes = workersCount * shiftMinutes;
        
        // Проверяем, что общее количество минут работы не превышает доступное
        return totalMinutes <= maxWorkMinutes;
    }

    /**
     * Результат попытки обновления даты диагностической записи
     */
    public static class UpdateDateResult {
        private boolean success;
        private String message;
        private DiagnosticsScheduleEntry entry;
        
        public UpdateDateResult(boolean success, String message, DiagnosticsScheduleEntry entry) {
            this.success = success;
            this.message = message;
            this.entry = entry;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public DiagnosticsScheduleEntry getEntry() { return entry; }
    }
    
    /**
     * Обновляет дату диагностической записи
     * @param entryId ID записи
     * @param newDate новая дата
     * @return результат обновления (успех или ошибка с сообщением)
     */
    @Transactional
    public UpdateDateResult updateEntryDate(Long entryId, LocalDate newDate) {
        DiagnosticsScheduleEntry entry = entryRepository.findById(entryId)
                .orElse(null);
        
        if (entry == null) {
            return new UpdateDateResult(false, "Запись не найдена", null);
        }
        
        DiagnosticsSchedule schedule = entry.getSchedule();
        
        // Проверяем, что новая дата в пределах года графика
        if (newDate.getYear() != schedule.getYear()) {
            return new UpdateDateResult(false, "Новая дата должна быть в пределах года графика", entry);
        }
        
        // Проверяем, что на новую дату нет других диагностик для этого оборудования
        List<DiagnosticsScheduleEntry> existingOnNewDate = entryRepository
                .findByScheduleIdAndScheduledDate(schedule.getId(), newDate);
        
        for (DiagnosticsScheduleEntry existing : existingOnNewDate) {
            if (!existing.getId().equals(entryId) && existing.getEquipment().equals(entry.getEquipment())) {
                return new UpdateDateResult(false, 
                    "На дату " + newDate + " уже запланирована диагностика для оборудования " + entry.getEquipment(), 
                    entry);
            }
        }
        
        // Проверяем доступность новой даты с учетом параллельной работы
        // Исключаем текущую запись из проверки, так как она перемещается
        List<Integer> taskDurations = new ArrayList<>();
        for (DiagnosticsScheduleEntry existing : existingOnNewDate) {
            if (!existing.getId().equals(entryId)) {
                taskDurations.add(existing.getDiagnosticsType().getDurationMinutes());
            }
        }
        // Добавляем перемещаемую задачу
        taskDurations.add(entry.getDiagnosticsType().getDurationMinutes());
        
        // Группируем задачи по длительности для расчета параллельной работы
        Map<Integer, Integer> tasksByDuration = new HashMap<>();
        for (Integer duration : taskDurations) {
            tasksByDuration.put(duration, tasksByDuration.getOrDefault(duration, 0) + 1);
        }
        
        // Проверяем доступность с учетом параллельной работы
        int totalShiftMinutes = schedule.getShiftDurationHours() * 60;
        
        // Вычисляем общее количество минут работы (включая перемещаемую задачу)
        int totalMinutes = 0;
        for (Map.Entry<Integer, Integer> durationEntry : tasksByDuration.entrySet()) {
            totalMinutes += durationEntry.getKey() * durationEntry.getValue();
        }
        
        int maxWorkMinutes = schedule.getWorkersCount() * totalShiftMinutes;
        int requiredMinutes = entry.getDiagnosticsType().getDurationMinutes();
        
        // Доступное время = максимальное время минус уже запланированное (без перемещаемой задачи)
        int existingMinutes = totalMinutes - requiredMinutes;
        int availableMinutes = maxWorkMinutes - existingMinutes;
        
        if (!canExecuteTasksInParallel(tasksByDuration, schedule.getWorkersCount(), totalShiftMinutes)) {
            return new UpdateDateResult(false, String.format(
                "Недостаточно свободного времени на дату %s. Требуется: %d минут, доступно: %d минут (работников: %d, смена: %d часов)",
                newDate, requiredMinutes, availableMinutes, 
                schedule.getWorkersCount(), schedule.getShiftDurationHours()), entry);
        }
        
        // Обновляем дату
        entry.setScheduledDate(newDate);
        DiagnosticsScheduleEntry savedEntry = entryRepository.save(entry);
        return new UpdateDateResult(true, "Дата успешно обновлена", savedEntry);
    }

    /**
     * Обновляет статус диагностической записи
     * @param entryId ID записи
     * @param isCompleted выполнена ли диагностика
     * @param hasDefect обнаружен ли дефект
     * @return обновленная запись
     */
    @Transactional
    public DiagnosticsScheduleEntry updateEntryStatus(Long entryId, boolean isCompleted, boolean hasDefect) {
        DiagnosticsScheduleEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Запись не найдена"));
        
        entry.setIsCompleted(isCompleted);
        if (isCompleted) {
            entry.setCompletedDate(LocalDate.now());
            if (hasDefect) {
                // Помечаем в notes, что обнаружен дефект
                String notes = entry.getNotes();
                if (notes == null || notes.isEmpty()) {
                    notes = "Обнаружен дефект";
                } else if (!notes.contains("Обнаружен дефект")) {
                    notes = "Обнаружен дефект. " + notes;
                }
                entry.setNotes(notes);
            }
        } else {
            entry.setCompletedDate(null);
        }
        
        return entryRepository.save(entry);
    }

    /**
     * Обновляет график для конкретного месяца
     */
    @Transactional
    public void updateMonthSchedule(Long scheduleId, int month, List<DiagnosticsScheduleEntry> entries) {
        DiagnosticsSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("График не найден"));

        // Удаляем существующие записи за месяц
        LocalDate startDate = LocalDate.of(schedule.getYear(), month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<DiagnosticsScheduleEntry> existingEntries = entryRepository
                .findByScheduleIdAndScheduledDateBetween(scheduleId, startDate, endDate);
        entryRepository.deleteAll(existingEntries);

        // Сохраняем новые записи
        for (DiagnosticsScheduleEntry entry : entries) {
            entry.setSchedule(schedule);
            // Проверяем ограничения
            validateDayCapacity(schedule, entry.getScheduledDate(), entry.getDiagnosticsType().getDurationMinutes());
        }
        
        entryRepository.saveAll(entries);
    }

    /**
     * Получает график на месяц
     */
    public Map<String, Object> getMonthSchedule(Long scheduleId, int month) {
        DiagnosticsSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("График не найден"));

        LocalDate startDate = LocalDate.of(schedule.getYear(), month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<DiagnosticsScheduleEntry> entries = entryRepository
                .findByScheduleIdAndScheduledDateBetween(scheduleId, startDate, endDate);

        // Группируем по оборудованию и дате
        Map<String, Map<String, List<Map<String, Object>>>> scheduleMap = new LinkedHashMap<>();
        Set<String> equipmentSet = new TreeSet<>();
        
        for (DiagnosticsScheduleEntry entry : entries) {
            equipmentSet.add(entry.getEquipment());
        }

        // Создаем структуру для всех дат месяца
        for (String equipment : equipmentSet) {
            Map<String, List<Map<String, Object>>> dateMap = new LinkedHashMap<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dateMap.put(date.toString(), new ArrayList<>());
            }
            scheduleMap.put(equipment, dateMap);
        }

        // Заполняем записи
        for (DiagnosticsScheduleEntry entry : entries) {
            Map<String, Object> entryData = new HashMap<>();
            entryData.put("id", entry.getId());
            entryData.put("equipment", entry.getEquipment());
            entryData.put("area", entry.getArea());
            entryData.put("diagnosticsType", Map.of(
                "id", entry.getDiagnosticsType().getId(),
                "code", entry.getDiagnosticsType().getCode(),
                "name", entry.getDiagnosticsType().getName(),
                "durationMinutes", entry.getDiagnosticsType().getDurationMinutes(),
                "colorCode", entry.getDiagnosticsType().getColorCode() != null 
                    ? entry.getDiagnosticsType().getColorCode() : "#90EE90"
            ));
            entryData.put("scheduledDate", entry.getScheduledDate().toString());
            entryData.put("isCompleted", entry.getIsCompleted());
            entryData.put("completedDate", entry.getCompletedDate() != null 
                ? entry.getCompletedDate().toString() : null);
            entryData.put("notes", entry.getNotes());
            
            scheduleMap.get(entry.getEquipment())
                .get(entry.getScheduledDate().toString())
                .add(entryData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("schedule", Map.of(
            "id", schedule.getId(),
            "year", schedule.getYear(),
            "workersCount", schedule.getWorkersCount(),
            "shiftDurationHours", schedule.getShiftDurationHours()
        ));
        result.put("entries", scheduleMap);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("workingDays", getWorkingDays(startDate, endDate).stream()
            .map(LocalDate::toString)
            .collect(Collectors.toList()));

        return result;
    }

    /**
     * Вычисляет идеальные даты для диагностик на основе периода
     * Период в месяцах: 0.5 = 2 раза в месяц, 1 = раз в месяц, 6 = раз в полгода, 12 = раз в год
     * @deprecated Используется calculateIdealDatesFromStart для приоритета от начала года
     */
    @Deprecated
    private List<LocalDate> calculateIdealDates(int year, double periodMonths, int totalCount) {
        List<LocalDate> idealDates = new ArrayList<>();
        
        if (totalCount <= 0) {
            return idealDates;
        }
        
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        // Вычисляем интервал между диагностиками в днях
        // Для равномерного распределения по году
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate.plusDays(1));
        double daysBetween = totalDays / (double) totalCount;
        
        // Первая дата - середина первого интервала
        LocalDate firstDate = startDate.plusDays((long) (daysBetween / 2.0));
        idealDates.add(firstDate);
        
        // Добавляем остальные даты с интервалом daysBetween
        for (int i = 1; i < totalCount; i++) {
            long daysOffset = (long) (daysBetween * i);
            LocalDate nextDate = startDate.plusDays((long) (daysBetween / 2.0) + daysOffset);
            
            // Если вышли за пределы года, корректируем
            if (nextDate.isAfter(endDate)) {
                nextDate = endDate.minusDays(7); // За неделю до конца года
            }
            
            idealDates.add(nextDate);
        }
        
        return idealDates;
    }
    
    /**
     * Вычисляет идеальные даты для диагностик начиная с начала года
     * Приоритет распределения от начала года
     */
    /**
     * Вычисляет идеальные даты для диагностик с учетом периода
     * Пример: период 6 месяцев, 120 станков:
     * - Первые 60 станков распределяются с января по июнь (по 10 в месяц)
     * - Вторые 60 станков распределяются с июля по декабрь (по 10 в месяц)
     * Внутри каждого месяца равномерно по неделям (по 5 в неделю, если 20 в месяц)
     */
    private List<LocalDate> calculateIdealDatesByPeriod(int year, double periodMonths, int totalTasks, LocalDate startDate) {
        List<LocalDate> idealDates = new ArrayList<>();
        
        if (totalTasks <= 0) {
            return idealDates;
        }
        
        // Используем переданную дату старта или начало года
        LocalDate actualStartDate = startDate != null ? startDate : LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        // Вычисляем количество периодов в году от даты старта
        int periodsPerYear = (int) Math.round(12.0 / periodMonths);
        
        // Количество задач на период (равномерно распределяем)
        int tasksPerPeriod = (int) Math.ceil((double) totalTasks / periodsPerYear);
        
        // Распределяем задачи по периодам, начиная с даты старта
        int startMonth = actualStartDate.getMonthValue();
        int startDay = actualStartDate.getDayOfMonth();
        
        for (int periodIndex = 0; periodIndex < periodsPerYear; periodIndex++) {
            // Вычисляем границы периода от даты старта
            int periodStartMonth = startMonth + (int)(periodIndex * periodMonths);
            // Корректируем месяц, если вышли за пределы года
            while (periodStartMonth > 12) {
                periodStartMonth -= 12;
            }
            int periodEndMonth = (int)Math.min(12, periodStartMonth + (int)periodMonths - 1);
            
            // Количество задач в этом периоде
            int tasksInPeriod = Math.min(tasksPerPeriod, totalTasks - periodIndex * tasksPerPeriod);
            
            // Количество месяцев в периоде
            int monthsInPeriod = periodEndMonth - periodStartMonth + 1;
            
            // Распределяем задачи по месяцам периода
            int tasksPerMonth = (int) Math.ceil((double) tasksInPeriod / monthsInPeriod);
            
            for (int monthOffset = 0; monthOffset < monthsInPeriod; monthOffset++) {
                int targetMonth = periodStartMonth + monthOffset;
                // Корректируем месяц, если вышли за пределы года
                while (targetMonth > 12) {
                    targetMonth -= 12;
                }
                if (targetMonth < 1 || targetMonth > 12) break;
                
                // Количество задач в этом месяце
                int tasksInMonth = Math.min(tasksPerMonth, tasksInPeriod - monthOffset * tasksPerMonth);
                
                // Распределяем задачи внутри месяца равномерно по неделям
                // Для первого месяца периода используем дату старта, для остальных - начало месяца
                LocalDate monthStart = (periodIndex == 0 && monthOffset == 0) 
                    ? actualStartDate 
                    : LocalDate.of(year, targetMonth, 1);
                LocalDate monthEnd = LocalDate.of(year, targetMonth, 1).withDayOfMonth(
                    LocalDate.of(year, targetMonth, 1).lengthOfMonth());
                
                // Получаем рабочие дни месяца
                List<LocalDate> monthWorkingDays = getWorkingDays(monthStart, monthEnd);
                if (monthWorkingDays.isEmpty()) continue;
                
                // Распределяем задачи по неделям месяца
                int weeksInMonth = (int) Math.ceil(monthWorkingDays.size() / 5.0); // Примерно 5 рабочих дней в неделю
                int tasksPerWeek = (int) Math.ceil((double) tasksInMonth / weeksInMonth);
                
                for (int taskIndex = 0; taskIndex < tasksInMonth; taskIndex++) {
                    int weekIndex = taskIndex / tasksPerWeek;
                    int taskInWeek = taskIndex % tasksPerWeek;
                    
                    // Вычисляем идеальную дату в неделе
                    int weekStartDayIndex = weekIndex * tasksPerWeek;
                    if (weekStartDayIndex >= monthWorkingDays.size()) {
                        weekStartDayIndex = monthWorkingDays.size() - 1;
                    }
                    
                    int dayIndex = weekStartDayIndex + (taskInWeek % tasksPerWeek);
                    if (dayIndex >= monthWorkingDays.size()) {
                        dayIndex = monthWorkingDays.size() - 1;
                    }
                    
                    LocalDate idealDate = monthWorkingDays.get(dayIndex);
                    idealDates.add(idealDate);
                }
            }
        }
        
        // Сортируем даты
        idealDates.sort(LocalDate::compareTo);
        
        return idealDates;
    }
    
    /**
     * Находит лучший рабочий день в диапазоне ±2 недели от идеальной даты
     * @deprecated Используется findBestDateNearIdealFromStart для приоритета от начала года
     */
    @Deprecated
    private LocalDate findBestDateNearIdeal(LocalDate idealDate, List<LocalDate> workingDays,
                                           Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                           int workersCount, int shiftHours, int taskMinutes) {
        LocalDate bestDate = null;
        int minScore = Integer.MAX_VALUE;
        
        // Диапазон ±2 недели (14 дней)
        LocalDate minDate = idealDate.minusDays(14);
        LocalDate maxDate = idealDate.plusDays(14);
        
        for (LocalDate date : workingDays) {
            if (date.isBefore(minDate) || date.isAfter(maxDate)) {
                continue;
            }
            
            List<DiagnosticsTask> dayTasks = dailyTasks.getOrDefault(date, new ArrayList<>());
            int dayMinutes = dayTasks.stream()
                    .mapToInt(t -> t.getType().getDurationMinutes())
                    .sum();
            int availableMinutes = workersCount * shiftHours * 60;
            
            if (dayMinutes + taskMinutes <= availableMinutes) {
                // Предпочитаем даты ближе к идеальной
                long daysFromIdeal = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(idealDate, date));
                int score = dayMinutes + (int) (daysFromIdeal * 10); // Штраф за удаление от идеальной даты
                
                if (score < minScore) {
                    minScore = score;
                    bestDate = date;
                }
            }
        }
        
        return bestDate;
    }
    
    /**
     * Получает список рабочих дней (исключая выходные)
     */
    private List<LocalDate> getWorkingDays(LocalDate start, LocalDate end) {
        List<LocalDate> workingDays = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                workingDays.add(current);
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Находит лучший день для размещения задачи
     */
    private LocalDate findBestDate(Map<LocalDate, List<DiagnosticsTask>> dailyTasks,
                                   List<LocalDate> workingDays,
                                   int workersCount,
                                   int shiftHours,
                                   int taskMinutes) {
        LocalDate bestDate = null;
        int minLoad = Integer.MAX_VALUE;

        for (LocalDate date : workingDays) {
            List<DiagnosticsTask> dayTasks = dailyTasks.getOrDefault(date, new ArrayList<>());
            int dayMinutes = dayTasks.stream()
                    .mapToInt(t -> t.getType().getDurationMinutes())
                    .sum();
            int availableMinutes = workersCount * shiftHours * 60;
            
            if (dayMinutes + taskMinutes <= availableMinutes && dayMinutes < minLoad) {
                minLoad = dayMinutes;
                bestDate = date;
            }
        }

        return bestDate;
    }

    /**
     * Проверяет, не превышает ли загрузка дня допустимую
     */
    private void validateDayCapacity(DiagnosticsSchedule schedule, LocalDate date, int additionalMinutes) {
        List<DiagnosticsScheduleEntry> dayEntries = entryRepository
                .findByScheduleIdAndScheduledDate(schedule.getId(), date);
        
        int totalMinutes = dayEntries.stream()
                .mapToInt(e -> e.getDiagnosticsType().getDurationMinutes())
                .sum();
        
        int availableMinutes = schedule.getWorkersCount() * schedule.getShiftDurationHours() * 60;
        
        if (totalMinutes + additionalMinutes > availableMinutes) {
            throw new RuntimeException(
                    String.format("Превышена допустимая загрузка на %s. Доступно: %d минут, требуется: %d минут",
                            date, availableMinutes, totalMinutes + additionalMinutes));
        }
    }

    /**
     * Внутренний класс для представления задачи диагностики
     */
    private static class DiagnosticsTask {
        private final String equipment;
        private final String area;
        private final DiagnosticsType type;
        private double periodMonths = 1.0; // Период в месяцах
        private int sequenceNumber = 0; // Номер в последовательности (0, 1, 2...)
        private int totalCount = 1; // Общее количество диагностик в год

        public DiagnosticsTask(String equipment, String area, DiagnosticsType type) {
            this.equipment = equipment;
            this.area = area;
            this.type = type;
        }

        public String getEquipment() { return equipment; }
        public String getArea() { return area; }
        public DiagnosticsType getType() { return type; }
        public double getPeriodMonths() { return periodMonths; }
        public void setPeriodMonths(double periodMonths) { this.periodMonths = periodMonths; }
        @SuppressWarnings("unused")
        public int getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }

    /**
     * Класс для запроса создания графика
     */
    public static class EquipmentDiagnosticsRequest {
        private String equipment;
        private String area;
        private Map<String, Number> diagnosticsCounts; // код типа -> период в месяцах (0.5-12)
        private Map<String, Integer> diagnosticsDurations; // код типа -> длительность в минутах

        public String getEquipment() { return equipment; }
        public void setEquipment(String equipment) { this.equipment = equipment; }
        public String getArea() { return area; }
        public void setArea(String area) { this.area = area; }
        public Map<String, Number> getDiagnosticsCounts() { return diagnosticsCounts; }
        public void setDiagnosticsCounts(Map<String, Number> diagnosticsCounts) { 
            this.diagnosticsCounts = diagnosticsCounts; 
        }
        public Map<String, Integer> getDiagnosticsDurations() { return diagnosticsDurations; }
        public void setDiagnosticsDurations(Map<String, Integer> diagnosticsDurations) {
            this.diagnosticsDurations = diagnosticsDurations;
        }
    }
}

