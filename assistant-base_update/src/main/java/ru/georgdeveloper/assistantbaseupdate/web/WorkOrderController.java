package ru.georgdeveloper.assistantbaseupdate.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.georgdeveloper.assistantbaseupdate.service.WorkOrderService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-orders")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "workorder.enabled", havingValue = "true", matchIfMissing = false)
public class WorkOrderController {
    
    @Autowired
    private WorkOrderService workOrderService;
    
    /**
     * Получение последних 15 нарядов на работы для отображения в таблице
     */
    @GetMapping("/last-15")
    @ResponseBody
    public List<Map<String, Object>> getLast15WorkOrders() {
        return workOrderService.getLast15WorkOrders();
    }
    
    /**
     * Получение активных нарядов (не закрытых)
     */
    @GetMapping("/active")
    @ResponseBody
    public List<Map<String, Object>> getActiveWorkOrders() {
        return workOrderService.getActiveWorkOrders();
    }
    
    /**
     * Поиск нарядов по ключевому слову
     */
    @GetMapping("/search")
    @ResponseBody
    public List<Map<String, Object>> searchWorkOrders(@RequestParam("keyword") String keyword) {
        return workOrderService.searchWorkOrders(keyword);
    }
    
    /**
     * Получение нарядов для дашборда (совместимость с существующим API)
     */
    @GetMapping("/dashboard")
    @ResponseBody
    public List<Map<String, Object>> getWorkOrdersForDashboard() {
        return workOrderService.getLast15WorkOrders();
    }
}
