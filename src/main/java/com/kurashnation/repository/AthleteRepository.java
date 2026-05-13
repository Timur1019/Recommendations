package com.kurashnation.repository;

import com.kurashnation.model.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    Optional<Athlete> findByUserId(Long userId);

    List<Athlete> findAllByCoachId(Long coachId);

    @Query("select a from Athlete a where a.id in :ids")
    List<Athlete> findAllByIdIn(@Param("ids") List<Long> ids);

    /** После {@link com.kurashnation.service.impl.AthleteProfileProvisioning} (REQUIRES_NEW) — user/coach для MapStruct в текущей сессии. */
    @Query("SELECT DISTINCT a FROM Athlete a JOIN FETCH a.user LEFT JOIN FETCH a.coach WHERE a.id = :id")
    Optional<Athlete> findByIdFetchUserAndCoach(@Param("id") Long id);
}

