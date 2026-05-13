package com.kurashnation.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "achievement_training_weeks")
public class AchievementTrainingWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Type(JsonType.class)
    @Column(name = "schedule_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode scheduleJson;
}
