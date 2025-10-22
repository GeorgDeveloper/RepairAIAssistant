package ru.georgdeveloper.assistantbaseupdate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.georgdeveloper.assistantbaseupdate.entity.sqlserver.WorkOrder;
import ru.georgdeveloper.assistantbaseupdate.repository.sqlserver.WorkOrderRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkOrderService {
    
    @Autowired
    private WorkOrderRepository workOrderRepository;
    
    /**
     * Получение последних 20 нарядов на работы для отображения в таблице
     */
    public List<Map<String, Object>> getLast15WorkOrders() {
        try {
            List<WorkOrder> workOrders = workOrderRepository.findLast15WorkOrders(PageRequest.of(0, 20));
            System.out.println("Found " + workOrders.size() + " work orders");
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (WorkOrder workOrder : workOrders) {
                Map<String, Object> workOrderMap = new HashMap<>();
                workOrderMap.put("idCode", workOrder.getIdCode());
                workOrderMap.put("woCodeName", workOrder.getWoCodeName());
                workOrderMap.put("machineName", workOrder.getMachineName());
                workOrderMap.put("assembly", workOrder.getAssembly());
                workOrderMap.put("subAssembly", workOrder.getSubAssembly());
                workOrderMap.put("type", workOrder.getTypeWo());
                workOrderMap.put("status", workOrder.getWoStatusLocalDescr());
                workOrderMap.put("duration", formatDuration(workOrder.getDuration()));
                workOrderMap.put("sDuration", workOrder.getSDuration());
                workOrderMap.put("dateT1", workOrder.getDateT1());
                workOrderMap.put("sDateT1", workOrder.getSDateT1());
                workOrderMap.put("dateT4", workOrder.getDateT4());
                workOrderMap.put("sDateT4", workOrder.getSDateT4());
                workOrderMap.put("maintainers", workOrder.getMaintainers());
                workOrderMap.put("comment", workOrder.getComment());
                workOrderMap.put("initialComment", workOrder.getInitialComment());
                workOrderMap.put("plantDepartmentGeographicalCodeName", workOrder.getPlantDepartmentGeographicalCodeName());
                workOrderMap.put("woBreakDownTime", workOrder.getWoBreakDownTime());
                workOrderMap.put("sWoBreakDownTime", workOrder.getSWoBreakDownTime());
                workOrderMap.put("logisticTime", workOrder.getLogisticTime());
                workOrderMap.put("sLogisticTime", workOrder.getSLogisticTime());
                workOrderMap.put("ttr", workOrder.getTtr());
                workOrderMap.put("sTtr", workOrder.getSTtr());
                workOrderMap.put("workCenter", workOrder.getWorkCenter());
                workOrderMap.put("customField01", workOrder.getCustomField01());
                
                result.add(workOrderMap);
                System.out.println("Added work order: " + workOrder.getMachineName() + " - " + workOrder.getWoStatusLocalDescr());
            }
            
            System.out.println("Returning " + result.size() + " work orders");
            return result;
        } catch (Exception e) {
            System.err.println("Error in getLast15WorkOrders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Получение активных нарядов (не закрытых)
     */
    public List<Map<String, Object>> getActiveWorkOrders() {
        try {
            List<WorkOrder> workOrders = workOrderRepository.findActiveWorkOrders(PageRequest.of(0, 50));
            System.out.println("Found " + workOrders.size() + " active work orders");
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (WorkOrder workOrder : workOrders) {
                Map<String, Object> workOrderMap = new HashMap<>();
                workOrderMap.put("idCode", workOrder.getIdCode());
                workOrderMap.put("woCodeName", workOrder.getWoCodeName());
                workOrderMap.put("machineName", workOrder.getMachineName());
                workOrderMap.put("assembly", workOrder.getAssembly());
                workOrderMap.put("subAssembly", workOrder.getSubAssembly());
                workOrderMap.put("type", workOrder.getTypeWo());
                workOrderMap.put("status", workOrder.getWoStatusLocalDescr());
                workOrderMap.put("duration", formatDuration(workOrder.getDuration()));
                workOrderMap.put("sDuration", workOrder.getSDuration());
                workOrderMap.put("dateT1", workOrder.getDateT1());
                workOrderMap.put("sDateT1", workOrder.getSDateT1());
                workOrderMap.put("maintainers", workOrder.getMaintainers());
                workOrderMap.put("comment", workOrder.getComment());
                workOrderMap.put("initialComment", workOrder.getInitialComment());
                workOrderMap.put("plantDepartmentGeographicalCodeName", workOrder.getPlantDepartmentGeographicalCodeName());
                workOrderMap.put("workCenter", workOrder.getWorkCenter());
                workOrderMap.put("customField01", workOrder.getCustomField01());
                
                result.add(workOrderMap);
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Error in getActiveWorkOrders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Поиск нарядов по ключевому слову
     */
    public List<Map<String, Object>> searchWorkOrders(String keyword) {
        try {
            List<WorkOrder> workOrders = workOrderRepository.findByKeyword(keyword, PageRequest.of(0, 20));
            System.out.println("Found " + workOrders.size() + " work orders for keyword: " + keyword);
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (WorkOrder workOrder : workOrders) {
                Map<String, Object> workOrderMap = new HashMap<>();
                workOrderMap.put("idCode", workOrder.getIdCode());
                workOrderMap.put("woCodeName", workOrder.getWoCodeName());
                workOrderMap.put("machineName", workOrder.getMachineName());
                workOrderMap.put("assembly", workOrder.getAssembly());
                workOrderMap.put("subAssembly", workOrder.getSubAssembly());
                workOrderMap.put("type", workOrder.getTypeWo());
                workOrderMap.put("status", workOrder.getWoStatusLocalDescr());
                workOrderMap.put("duration", formatDuration(workOrder.getDuration()));
                workOrderMap.put("sDuration", workOrder.getSDuration());
                workOrderMap.put("dateT1", workOrder.getDateT1());
                workOrderMap.put("sDateT1", workOrder.getSDateT1());
                workOrderMap.put("dateT4", workOrder.getDateT4());
                workOrderMap.put("sDateT4", workOrder.getSDateT4());
                workOrderMap.put("maintainers", workOrder.getMaintainers());
                workOrderMap.put("comment", workOrder.getComment());
                workOrderMap.put("initialComment", workOrder.getInitialComment());
                workOrderMap.put("plantDepartmentGeographicalCodeName", workOrder.getPlantDepartmentGeographicalCodeName());
                workOrderMap.put("workCenter", workOrder.getWorkCenter());
                workOrderMap.put("customField01", workOrder.getCustomField01());
                
                result.add(workOrderMap);
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Error in searchWorkOrders: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Форматирование длительности в читаемый вид
     */
    private String formatDuration(Integer duration) {
        if (duration == null) return "0.00:00";
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;
        return String.format("%d.%02d:%02d", hours, minutes, seconds);
    }
}
