package com.kurashnation.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gold_standards", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gold_standards_weight_category", columnNames = "weight_category")
})
public class GoldStandard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "weight_category", nullable = false, length = 20)
    private String weightCategory;

    @Column(name = "avg_trainings_per_week", precision = 4, scale = 2)
    private BigDecimal avgTrainingsPerWeek;

    @Column(name = "avg_intensity", precision = 3, scale = 2)
    private BigDecimal avgIntensity;

    @Column(name = "avg_pullups")
    private Integer avgPullups;

    @Column(name = "avg_run_30m", precision = 5, scale = 2)
    private BigDecimal avgRun30m;

    @Column(name = "avg_run_2000m", precision = 8, scale = 2)
    private BigDecimal avgRun2000m;

    @Column(name = "avg_burpees")
    private Integer avgBurpees;

    @Column(name = "avg_competitions_per_year", precision = 4, scale = 2)
    private BigDecimal avgCompetitionsPerYear;

    @CreationTimestamp
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;
}

