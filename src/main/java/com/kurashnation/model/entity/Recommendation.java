package com.kurashnation.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recommendations")
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private Athlete athlete;

    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    @Type(JsonType.class)
    @Column(name = "week_plan", columnDefinition = "jsonb")
    private JsonNode weekPlan;

    @Type(JsonType.class)
    @Column(name = "deficits", columnDefinition = "jsonb")
    private JsonNode deficits;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    /** Если задано — показывается спортсмену как «совет дня» вместо эвристики по deficits (например план из справочника). */
    @Column(name = "custom_tip", columnDefinition = "text")
    private String customTip;

    @Column(name = "is_viewed", nullable = false)
    private boolean viewed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

