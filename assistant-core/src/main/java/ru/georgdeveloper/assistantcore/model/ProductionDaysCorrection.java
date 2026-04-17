package ru.georgdeveloper.assistantcore.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Корректировка производственных дней для месяца.
 * Содержит один или несколько диапазонов (например 1–14 и 21–31 при простое в середине месяца).
 * Показатели BD и доступность за месяц считаются только по этим дням; ППР (PM) не пересчитываются.
 */
public class ProductionDaysCorrection {

    private Integer id;
    private int year;
    private int month;
    private String comment;
    private List<ProductionDaysCorrectionRange> ranges = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ProductionDaysCorrectionRange> getRanges() {
        return ranges;
    }

    public void setRanges(List<ProductionDaysCorrectionRange> ranges) {
        this.ranges = ranges != null ? ranges : new ArrayList<>();
    }
}
