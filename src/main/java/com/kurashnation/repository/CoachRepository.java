package com.kurashnation.repository;

import com.kurashnation.model.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Long> {
    Optional<Coach> findByUserId(Long userId);

    /** После {@link com.kurashnation.service.impl.CoachProfileProvisioning} (REQUIRES_NEW) нужен user в текущей сессии для маппера. */
    @Query("SELECT DISTINCT c FROM Coach c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Coach> findByIdFetchUser(@Param("id") Long id);
}

