package com.kurashnation.service.impl;

import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Athlete;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.AthleteRepository;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Если у пользователя роль ATHLETE, но строки в athletes нет (часто после смены роли в админке),
 * создаём минимальный профиль, чтобы API не отвечали 404.
 */
@Component
public class AthleteProfileProvisioning {

    private final AthleteRepository athleteRepository;

    public AthleteProfileProvisioning(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
    }

    /**
     * Отдельная транзакция: иначе при вызове из {@code @Transactional(readOnly = true)} INSERT в athletes падает
     * с «cannot execute INSERT in a read-only transaction».
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public Athlete ensureAthleteProfile(User user) {
        if (user.getRole() != UserRole.ATHLETE) {
            throw new ValidationException("Expected ATHLETE role");
        }
        return athleteRepository.findByUserId(user.getId()).orElseGet(() -> {
            LogUtil.warn("Auto-creating minimal athlete profile for userId=%s email=%s", user.getId(), user.getEmail());
            Athlete a = new Athlete();
            a.setUser(user);
            a.setRegion("TASHKENT");
            a.setWeightCategory("-81");
            a.setDateOfBirth(LocalDate.of(2000, 1, 1));
            a.setSportType("KURASH");
            return athleteRepository.save(a);
        });
    }
}
