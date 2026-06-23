package ru.georgdeveloper.assistantcore.model;

/**
 * Один диапазон производственных дней в месяце (с first по last включительно).
 */
public class ProductionDaysCorrectionRange {

    private Integer id;
    private int firstProductionDay;
    private int lastProductionDay;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getFirstProductionDay() {
        return firstProductionDay;
    }

    public void setFirstProductionDay(int firstProductionDay) {
        this.firstProductionDay = firstProductionDay;
    }

    public int getLastProductionDay() {
        return lastProductionDay;
    }

    public void setLastProductionDay(int lastProductionDay) {
        this.lastProductionDay = lastProductionDay;
    }
}
