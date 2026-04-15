package ru.georgdeveloper.assistantcore.energy;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Загрузка YAML-карт колонок Excel для последующего импорта и отрисовки страниц по виду энергии.
 */
@Service
public class EnergyColumnMapService {

    private final Map<EnergyResource, Map<String, Object>> cache = new EnumMap<>(EnergyResource.class);

    /**
     * Содержимое YAML как вложенные {@code Map}/{@code List} (удобно для JSON API).
     */
    public Map<String, Object> getColumnMap(EnergyResource resource) {
        return cache.computeIfAbsent(resource, this::loadYaml);
    }

    private Map<String, Object> loadYaml(EnergyResource resource) {
        ClassPathResource cpr = new ClassPathResource(resource.classpathYamlPath());
        if (!cpr.exists()) {
            return Collections.emptyMap();
        }
        Yaml yaml = new Yaml();
        try (InputStream in = cpr.getInputStream()) {
            Map<String, Object> parsed = yaml.load(in);
            return parsed != null ? parsed : Collections.emptyMap();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource.classpathYamlPath(), e);
        }
    }
}
