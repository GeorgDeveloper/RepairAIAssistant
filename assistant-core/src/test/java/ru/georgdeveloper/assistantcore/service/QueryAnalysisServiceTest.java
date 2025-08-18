package ru.georgdeveloper.assistantcore.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для улучшенного анализа запросов
 */
@SpringBootTest
class QueryAnalysisServiceTest {
    
    @Test
    void testDateParsing() {
        assertEquals("01.05.2024", DateParsingUtils.parseDate("01.05.2024"));
        assertEquals("2024-05-01", DateParsingUtils.parseDate("2024-05-01"));
        assertEquals("Май", DateParsingUtils.parseDate("май"));
        assertEquals("Январь", DateParsingUtils.parseDate("январь"));
    }
    
    @Test
    void testDateRangeExtraction() {
        DateParsingUtils.DateRange range1 = DateParsingUtils.extractDateRange("январь-март");
        assertNotNull(range1);
        assertTrue(range1.isRange());
        assertEquals("Январь", range1.getStartDate());
        assertEquals("Март", range1.getEndDate());
        
        DateParsingUtils.DateRange range2 = DateParsingUtils.extractDateRange("01.01.2024-15.01.2024");
        assertNotNull(range2);
        assertTrue(range2.isRange());
        assertEquals("01.01.2024", range2.getStartDate());
        assertEquals("15.01.2024", range2.getEndDate());
    }
}