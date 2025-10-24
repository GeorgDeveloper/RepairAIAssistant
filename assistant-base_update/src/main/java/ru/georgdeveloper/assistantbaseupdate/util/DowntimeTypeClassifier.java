package ru.georgdeveloper.assistantbaseupdate.util;

/**
 * Классификатор типов причин простоя для цветовой индикации.
 */
public final class DowntimeTypeClassifier {
    
    private static final String ELECTRICAL_PATTERN = "E/|EKTPuKA";
    private static final String ELECTRONIC_PATTERN = "E/|EKTPOHuKA";
    private static final String MECHANICAL_PATTERN = "MEXAHuKA";
    
    private static final String ELECTRICAL_TYPE = "electrical";
    private static final String ELECTRONIC_TYPE = "electronic";
    private static final String MECHANICAL_TYPE = "mechanical";
    private static final String UNKNOWN_TYPE = "unknown";
    
    private DowntimeTypeClassifier() {
        // Utility class
    }
    
    /**
     * Определяет тип причины простоя для цветовой индикации
     * @param pcsDftDesc описание причины простоя
     * @return тип простоя (electrical, electronic, mechanical, unknown)
     */
    public static String getDowntimeType(String pcsDftDesc) {
        if (pcsDftDesc == null || pcsDftDesc.trim().isEmpty()) {
            return UNKNOWN_TYPE;
        }
        
        String desc = pcsDftDesc.trim();
        String descLower = desc.toLowerCase();
        
        // Электрика - проверяем как в оригинальном виде, так и в нижнем регистре
        if (desc.contains(ELECTRICAL_PATTERN) || 
            descLower.contains("electrical") || 
            descLower.contains("электрика") || 
            descLower.contains("e/|електрuка")) {
            return ELECTRICAL_TYPE;
        }
        
        // Электроника - проверяем как в оригинальном виде, так и в нижнем регистре
        if (desc.contains(ELECTRONIC_PATTERN) || 
            descLower.contains("electronic") || 
            descLower.contains("электроника") || 
            descLower.contains("e/|електронuка")) {
            return ELECTRONIC_TYPE;
        }
        
        // Механика - проверяем как в оригинальном виде, так и в нижнем регистре
        if (desc.contains(MECHANICAL_PATTERN) || 
            descLower.contains("mechanical") || 
            descLower.contains("механuка")) {
            return MECHANICAL_TYPE;
        }
        
        return UNKNOWN_TYPE;
    }
}
