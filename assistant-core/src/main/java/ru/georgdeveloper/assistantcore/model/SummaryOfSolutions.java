package ru.georgdeveloper.assistantcore.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "Summary_of_solutions")
@Data
public class SummaryOfSolutions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private String date;

    @Column(name = "executor")
    private String executor;

    @Column(name = "region")
    private String region;

    @Column(name = "equipment")
    private String equipment;

    @Column(name = "node")
    private String node;

    @Column(name = "notes_on_the_operation_of_the_equipment")
    private String notes_on_the_operation_of_the_equipment;

    @Column(name = "measures_taken")
    private String measures_taken;

    @Column(name = "comments")
    private String comments;

}
