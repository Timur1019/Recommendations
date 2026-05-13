package com.kurashnation.repository;

import com.kurashnation.model.entity.TrainingRequest;
import com.kurashnation.model.enums.TrainingRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRequestRepository extends JpaRepository<TrainingRequest, Long> {
    List<TrainingRequest> findAllByAthleteIdAndStatusOrderByCreatedAtDesc(Long athleteId, TrainingRequestStatus status);

    List<TrainingRequest> findAllByAthlete_IdOrderByCreatedAtDesc(Long athleteId);

    List<TrainingRequest> findAllByAthlete_Coach_IdOrderByCreatedAtDesc(Long coachId);

    Page<TrainingRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

