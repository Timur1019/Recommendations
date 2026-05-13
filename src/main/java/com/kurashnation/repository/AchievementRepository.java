package com.kurashnation.repository;

import com.kurashnation.model.entity.Achievement;
import com.kurashnation.model.enums.MedalType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findAllByAthleteIdOrderByCompetitionDateDesc(Long athleteId);

    @EntityGraph(attributePaths = {"trainingWeeks", "athlete", "athlete.user"})
    @Query("select a from Achievement a where a.athlete.id = :athleteId order by a.competitionDate desc")
    List<Achievement> findAllByAthleteIdWithWeeksOrderByCompetitionDateDesc(@Param("athleteId") Long athleteId);

    @EntityGraph(attributePaths = {"trainingWeeks", "athlete", "athlete.user"})
    @Query("select a from Achievement a where a.athlete.id in :athleteIds")
    List<Achievement> findAllByAthleteIdInWithWeeks(@Param("athleteIds") List<Long> athleteIds);

    @Query("""
            select distinct a.athlete.id
            from Achievement a
            where a.verifiedByAdmin = true
              and a.medalType = :medalType
              and a.competitionDate >= :fromDate
            """)
    List<Long> findDistinctAthleteIdsWithMedalSince(
            @Param("medalType") MedalType medalType,
            @Param("fromDate") LocalDate fromDate
    );
}

