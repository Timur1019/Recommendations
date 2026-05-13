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
@Table(name = "athletes")
public class Athlete {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id")
    private Coach coach;

    @Column(name = "region", nullable = false, length = 50)
    private String region;

    @Column(name = "weight_category", nullable = false, length = 20)
    private String weightCategory;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "rank", length = 50)
    private String rank;

    @Column(name = "sport_type", nullable = false, length = 80)
    private String sportType = "KURASH";

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "body_weight_kg", precision = 5, scale = 2)
    private BigDecimal bodyWeightKg;

    @Column(name = "goal_text", columnDefinition = "TEXT")
    private String goalText;

    @Column(name = "current_medal_count_gold", nullable = false)
    private int currentMedalCountGold = 0;

    @Column(name = "current_medal_count_silver", nullable = false)
    private int currentMedalCountSilver = 0;

    @Column(name = "current_medal_count_bronze", nullable = false)
    private int currentMedalCountBronze = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

