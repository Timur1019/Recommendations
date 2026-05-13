package com.kurashnation.repository;

import com.kurashnation.model.entity.FitnessTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FitnessTestRepository extends JpaRepository<FitnessTest, Long> {
    List<FitnessTest> findAllByAthleteIdOrderByTestDateDesc(Long athleteId);

    @Query("""
            select ft
            from FitnessTest ft
            where ft.athlete.id = :athleteId
              and ft.testDate >= :fromDate
            order by ft.testDate asc
            """)
    List<FitnessTest> findAllByAthleteIdSinceOrderByDateAsc(
            @Param("athleteId") Long athleteId,
            @Param("fromDate") LocalDate fromDate
    );
}

