package com.kurashnation.repository;

import com.kurashnation.model.entity.GoldStandard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoldStandardRepository extends JpaRepository<GoldStandard, Long> {
    Optional<GoldStandard> findByWeightCategory(String weightCategory);
}

