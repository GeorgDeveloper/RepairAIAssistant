package ru.georgdeveloper.assistantcore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.georgdeveloper.assistantcore.energy.EnergyColumnMapService;
import ru.georgdeveloper.assistantcore.energy.EnergyResource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/energy")
public class EnergyColumnMapController {

    private final EnergyColumnMapService energyColumnMapService;

    public EnergyColumnMapController(EnergyColumnMapService energyColumnMapService) {
        this.energyColumnMapService = energyColumnMapService;
    }

    /**
     * Список доступных видов энергии (имена enum для {@link #getColumnMap(String)}).
     */
    @GetMapping("/column-map")
    public List<String> listColumnMaps() {
        return Arrays.stream(EnergyResource.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    /**
     * Полная карта колонок для импорта Excel и построения таблицы/графиков на странице вида энергии.
     */
    @GetMapping("/column-map/{resource}")
    public ResponseEntity<Map<String, Object>> getColumnMap(@PathVariable String resource) {
        EnergyResource key;
        try {
            key = EnergyResource.valueOf(resource.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Map<String, Object> body = energyColumnMapService.getColumnMap(key);
        if (body.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Map<String, Object> withMeta = new LinkedHashMap<>();
        withMeta.put("resource", key.name());
        withMeta.put("map", body);
        return ResponseEntity.ok(withMeta);
    }
}
