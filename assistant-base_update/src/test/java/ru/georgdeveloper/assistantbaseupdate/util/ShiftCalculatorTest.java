package ru.georgdeveloper.assistantbaseupdate.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для ShiftCalculator
 */
public class ShiftCalculatorTest {

    @Test
    public void testDayShift() {
        // Тест дневной смены (08:00-20:00)
        LocalDateTime dayTime = LocalDateTime.of(2024, 1, 15, 10, 30); // 10:30
        assertTrue(ShiftCalculator.isDayShift(dayTime));
        
        LocalDateTime dayStart = LocalDateTime.of(2024, 1, 15, 8, 0); // 08:00
        assertTrue(ShiftCalculator.isDayShift(dayStart));
        
        LocalDateTime dayEnd = LocalDateTime.of(2024, 1, 15, 19, 59); // 19:59
        assertTrue(ShiftCalculator.isDayShift(dayEnd));
    }

    @Test
    public void testNightShift() {
        // Тест ночной смены (20:00-08:00)
        LocalDateTime nightTime = LocalDateTime.of(2024, 1, 15, 22, 30); // 22:30
        assertFalse(ShiftCalculator.isDayShift(nightTime));
        
        LocalDateTime nightStart = LocalDateTime.of(2024, 1, 15, 20, 0); // 20:00
        assertFalse(ShiftCalculator.isDayShift(nightStart));
        
        LocalDateTime earlyMorning = LocalDateTime.of(2024, 1, 16, 7, 30); // 07:30 следующего дня
        assertFalse(ShiftCalculator.isDayShift(earlyMorning));
    }

    @Test
    public void testShiftStart() {
        // Дневная смена
        LocalDateTime dayTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        LocalDateTime expectedDayStart = LocalDateTime.of(2024, 1, 15, 8, 0);
        assertEquals(expectedDayStart, ShiftCalculator.getCurrentShiftStart(dayTime));
        
        // Ночная смена (вечер)
        LocalDateTime nightTime = LocalDateTime.of(2024, 1, 15, 22, 30);
        LocalDateTime expectedNightStart = LocalDateTime.of(2024, 1, 15, 20, 0);
        assertEquals(expectedNightStart, ShiftCalculator.getCurrentShiftStart(nightTime));
        
        // Ночная смена (раннее утро)
        LocalDateTime earlyMorning = LocalDateTime.of(2024, 1, 16, 7, 30);
        LocalDateTime expectedEarlyMorningStart = LocalDateTime.of(2024, 1, 15, 20, 0);
        assertEquals(expectedEarlyMorningStart, ShiftCalculator.getCurrentShiftStart(earlyMorning));
    }

    @Test
    public void testMinutesFromShiftStart() {
        // Дневная смена - 2.5 часа с начала
        LocalDateTime dayTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        long minutes = ShiftCalculator.getMinutesFromShiftStart(dayTime);
        assertEquals(150, minutes); // 2.5 часа = 150 минут
        
        // Ночная смена - 2.5 часа с начала
        LocalDateTime nightTime = LocalDateTime.of(2024, 1, 15, 22, 30);
        long nightMinutes = ShiftCalculator.getMinutesFromShiftStart(nightTime);
        assertEquals(150, nightMinutes); // 2.5 часа = 150 минут
    }

    @Test
    public void testIncrementalWorkingTime() {
        // Тест с дневной сменой - 2.5 часа с начала (150 минут из 720 минут смены)
        LocalDateTime dayTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        double fullWorkingTime = 720.0; // 12 часов в минутах
        double incremental = ShiftCalculator.calculateIncrementalWorkingTime(fullWorkingTime, dayTime);
        
        // Ожидаем: 720 * (150/720) = 150 минут
        assertEquals(150.0, incremental, 0.01);
        
        // Тест с ночной сменой - 4 часа с начала (240 минут из 720 минут смены)
        LocalDateTime nightTime = LocalDateTime.of(2024, 1, 16, 0, 0);
        double nightIncremental = ShiftCalculator.calculateIncrementalWorkingTime(fullWorkingTime, nightTime);
        
        // Ожидаем: 720 * (240/720) = 240 минут
        assertEquals(240.0, nightIncremental, 0.01);
    }

    @Test
    public void testTotalShiftMinutes() {
        // Дневная смена: 08:00-20:00 = 12 часов = 720 минут
        LocalDateTime dayTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        long dayMinutes = ShiftCalculator.getTotalShiftMinutes(dayTime);
        assertEquals(720, dayMinutes);
        
        // Ночная смена: 20:00-08:00 = 12 часов = 720 минут
        LocalDateTime nightTime = LocalDateTime.of(2024, 1, 15, 22, 30);
        long nightMinutes = ShiftCalculator.getTotalShiftMinutes(nightTime);
        assertEquals(720, nightMinutes);
    }
}
