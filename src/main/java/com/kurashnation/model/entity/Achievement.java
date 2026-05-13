package com.kurashnation.model.entity;

import com.kurashnation.model.enums.CompetitionLevel;
import com.kurashnation.model.enums.MedalType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "achievements")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(name = "competition_name", nullable = false, length = 200)
    private String competitionName;

    @Column(name = "competition_date", nullable = false)
    private LocalDate competitionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_level", nullable = false, length = 30)
    private CompetitionLevel competitionLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "medal_type", nullable = false, length = 20)
    private MedalType medalType;

    @Column(name = "medal_photo_url", length = 500)
    private String medalPhotoUrl;

    @Column(name = "verified_by_admin", nullable = false)
    private boolean verifiedByAdmin = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "achievement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AchievementTrainingWeek> trainingWeeks = new ArrayList<>();
}

