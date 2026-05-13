package com.kurashnation.repository;

import com.kurashnation.model.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findFirstByAthleteIdOrderByGeneratedDateDesc(Long athleteId);
}

