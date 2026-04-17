package ru.georgdeveloper.assistantcore.energy;

/**
 * Вид энергоресурса; соответствует файлу {@code energy/{name}-column-map-2026.yaml} в classpath.
 */
public enum EnergyResource {
    WATER("water"),
    ELECTRICITY("electricity"),
    GAS("gas"),
    NITROGEN("nitrogen"),
    STEAM("steam"),
    TOTAL("total");

    private final String mapFilePrefix;

    EnergyResource(String mapFilePrefix) {
        this.mapFilePrefix = mapFilePrefix;
    }

    public String classpathYamlPath() {
        return "energy/" + mapFilePrefix + "-column-map-2026.yaml";
    }
}
