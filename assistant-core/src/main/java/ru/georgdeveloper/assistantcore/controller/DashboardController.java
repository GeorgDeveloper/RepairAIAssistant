package ru.georgdeveloper.assistantcore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import ru.georgdeveloper.assistantcore.repository.MonitoringRepository;
import ru.georgdeveloper.assistantcore.repository.TopEquipmentRepository;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private TopEquipmentRepository topEquipmentRepository;

    @GetMapping("/top-breakdowns-week")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerWeek() {
        return monitoringRepository.getTopBreakdownsPerWeek();
    }

    @GetMapping("/top-breakdowns-week-key-lines")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerWeekKeyLines() {
        return monitoringRepository.getTopBreakdownsPerWeekKeyLines();
    }

    @GetMapping("/top-breakdowns-month")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerMonth(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return monitoringRepository.getTopBreakdownsPerMonth(year, month);
    }

    @GetMapping("/top-breakdowns-month-key-lines")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerMonthKeyLines(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return monitoringRepository.getTopBreakdownsPerMonthKeyLines(year, month);
    }

    /**
     * Детализация нарядов для ячейки времени простоя в таблицах «Топ поломок» / «Ключевые линии».
     * @param period {@code week} — текущая неделя (как в топе за неделю), {@code month} — календарный месяц (year/month).
     */
    @GetMapping("/top-breakdowns-drilldown")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsDrilldown(
            @RequestParam("machineName") String machineName,
            @RequestParam("period") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if ("week".equalsIgnoreCase(period)) {
            return monitoringRepository.getTopBreakdownsDrilldownForWeek(machineName);
        }
        if ("month".equalsIgnoreCase(period) && year != null && month != null) {
            // Как детализация из графика: месяц по production_day, иначе по start_bd_t1 (TopEquipmentRepository)
            return topEquipmentRepository.getBreakdownDetailsForMachineAndProductionMonth(year, month, machineName);
        }
        return Collections.emptyList();
    }

    @GetMapping("/top-breakdowns")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerDay() {
        return monitoringRepository.getTopBreakdownsPerDay();
    }

    @GetMapping("/top-breakdowns-key-lines")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsPerDayKeyLines() {
        return monitoringRepository.getTopBreakdownsPerDayKeyLines();
    }

    @GetMapping("/top-breakdowns-current")
    @ResponseBody
    public List<Map<String, Object>> getTopBreakdownsCurrent() {
        return monitoringRepository.getTopBreakdownsCurrentStatusOnline();
    }

    @Autowired
    private MonitoringRepository monitoringRepository;
    
    @GetMapping("/breakDown")
    @ResponseBody
    public List<Map<String, Object>> searchBreakDown(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return monitoringRepository.searchBreakDown(year, month);
    }

    @GetMapping("/availability")
    @ResponseBody
    public List<Map<String, Object>> searchAvailability(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return monitoringRepository.searchAvailability(year, month);
    }

    @GetMapping("/pm-plan-fact-tag")
    @ResponseBody
    public List<Map<String, Object>> getPmPlanFactTagPerMonth(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return monitoringRepository.getPmPlanFactTagPerMonth(year, month);
    }

    @GetMapping("/pm-plan-fact-tag-all")
    @ResponseBody
    public List<Map<String, Object>> getPmPlanFactTagAll(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String machineName) {
        if ((area != null && !area.isEmpty() && !area.equals("all")) || 
            (machineName != null && !machineName.isEmpty() && !machineName.equals("all"))) {
            return monitoringRepository.getPmPlanFactTagFiltered(area, machineName);
        }
        return monitoringRepository.getPmPlanFactTagAll();
    }

    /**
     * Детализация графика ППР на главной: записи за производственный день (логика как в {@code getPmPlanFactTagFiltered}).
     *
     * @param kind {@code plan} — по {@code scheduled_proposed_date}; {@code fact} — закрытые по {@code scheduled_date};
     *             {@code tag} — {@code tag_maintenance_records.production_day}
     */
    @GetMapping("/pm-drilldown")
    @ResponseBody
    public List<Map<String, Object>> getPmDrilldown(
            @RequestParam("productionDay") String productionDay,
            @RequestParam("kind") String kind) {
        if ("plan".equalsIgnoreCase(kind)) {
            return monitoringRepository.getPmPlannedForProductionDay(productionDay);
        }
        if ("fact".equalsIgnoreCase(kind)) {
            return monitoringRepository.getPmCompletedForProductionDay(productionDay);
        }
        if ("tag".equalsIgnoreCase(kind)) {
            return monitoringRepository.getTagMaintenanceForProductionDay(productionDay);
        }
        return Collections.emptyList();
    }

    @GetMapping("/pm/areas")
    @ResponseBody
    public List<Map<String, Object>> getPmAreas() {
        return monitoringRepository.getPmAreas();
    }

    @GetMapping("/pm/equipment")
    @ResponseBody
    public List<Map<String, Object>> getPmEquipment(@RequestParam(required = false) String area) {
        return monitoringRepository.getPmEquipmentByArea(area);
    }

    @GetMapping("/tag/areas")
    @ResponseBody
    public List<Map<String, Object>> getTagAreas() {
        return monitoringRepository.getTagAreas();
    }

    @GetMapping("/tag/equipment")
    @ResponseBody
    public List<Map<String, Object>> getTagEquipment(@RequestParam(required = false) String area) {
        return monitoringRepository.getTagEquipmentByArea(area);
    }

    @GetMapping("/bd/areas")
    @ResponseBody
    public List<Map<String, Object>> getBdAreas() {
        return monitoringRepository.getBdAreas();
    }

    @GetMapping("/bd/equipment")
    @ResponseBody
    public List<Map<String, Object>> getBdEquipment(@RequestParam(required = false) String area) {
        return monitoringRepository.getBdEquipmentByArea(area);
    }

    @GetMapping("/pm-tag-bd-completed")
    @ResponseBody
    public List<Map<String, Object>> getPmTagBdCompleted(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String machineName) {
        return monitoringRepository.getPmTagBdCompleted(area, machineName);
    }


    @GetMapping("/current-metrics")
    @ResponseBody
    public Map<String, Object> getCurrentMetrics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return monitoringRepository.getCurrentMetrics(year, month);
    }

    @GetMapping("/metrics-for-date")
    @ResponseBody
    public Map<String, Object> getMetricsForDate(@RequestParam("date") String date) {
        return monitoringRepository.getMetricsForDate(date);
    }

    @GetMapping("/equipment-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getEquipmentMaintenanceRecords(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String area) {
        if ((dateFrom != null && !dateFrom.isEmpty()) || 
            (dateTo != null && !dateTo.isEmpty()) || 
            (area != null && !area.isEmpty() && !area.equals("all"))) {
            return monitoringRepository.getEquipmentMaintenanceRecords(dateFrom, dateTo, area);
        }
        return monitoringRepository.getEquipmentMaintenanceRecords();
    }

    @GetMapping("/pm-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getPmMaintenanceRecords() {
        return monitoringRepository.getPmMaintenanceRecords();
    }

    @GetMapping("/tag-maintenance-records")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getTagMaintenanceRecords() {
        return monitoringRepository.getTagMaintenanceRecords();
    }

    @GetMapping("/diagnostics-reports")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getDiagnosticsReports() {
        return monitoringRepository.getDiagnosticsReports();
    }

    @GetMapping("/diagnostics/areas")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getDiagnosticsAreas() {
        return monitoringRepository.getDiagnosticsAreas();
    }

    @GetMapping("/diagnostics/equipment")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> getDiagnosticsEquipment(@RequestParam(required = false) String area) {
        return monitoringRepository.getEquipmentListByArea(area);
    }
}
