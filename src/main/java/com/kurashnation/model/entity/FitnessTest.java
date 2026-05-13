package com.kurashnation.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fitness_tests")
public class FitnessTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "pullups_count")
    private Integer pullupsCount;

    @Column(name = "run_30m_seconds", precision = 5, scale = 2)
    private BigDecimal run30mSeconds;

    @Column(name = "run_2000m_seconds", precision = 8, scale = 2)
    private BigDecimal run2000mSeconds;

    @Column(name = "burpees_60sec")
    private Integer burpees60sec;

    @Column(name = "flexibility_cm")
    private Integer flexibilityCm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

