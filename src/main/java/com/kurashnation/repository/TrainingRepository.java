package com.kurashnation.repository;

import com.kurashnation.model.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    List<Training> findAllByAthleteIdOrderByTrainingDateDesc(Long athleteId);

    /** Все тренировки спортсменов, закреплённых за тренером */
    List<Training> findAllByAthlete_Coach_IdOrderByTrainingDateDesc(Long coachId);

    @Query("""
            select t
            from Training t
            where t.athlete.id = :athleteId
              and t.trainingDate >= :fromDate
            """)
    List<Training> findAllByAthleteIdSince(
            @Param("athleteId") Long athleteId,
            @Param("fromDate") LocalDate fromDate
    );
}

