package com.kurashnation.service.impl;

import com.kurashnation.exception.ValidationException;
import com.kurashnation.model.entity.Coach;
import com.kurashnation.model.entity.User;
import com.kurashnation.model.enums.UserRole;
import com.kurashnation.repository.CoachRepository;
import com.kurashnation.util.LogUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Если у пользователя роль COACH, но строки в coaches нет (часто после смены роли в админке),
 * создаём минимальный профиль, чтобы API не отвечали 404.
 */
@Component
public class CoachProfileProvisioning {

    private final CoachRepository coachRepository;

    public CoachProfileProvisioning(CoachRepository coachRepository) {
        this.coachRepository = coachRepository;
    }

    /** См. {@link AthleteProfileProvisioning#ensureAthleteProfile(User)} — та же причина для INSERT в coaches. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
    public Coach ensureCoachProfile(User user) {
        if (user.getRole() != UserRole.COACH) {
            throw new ValidationException("Expected COACH role");
        }
        return coachRepository.findByUserId(user.getId()).orElseGet(() -> {
            LogUtil.warn("Auto-creating minimal coach profile for userId=%s email=%s", user.getId(), user.getEmail());
            Coach c = new Coach();
            c.setUser(user);
            c.setRegion("TASHKENT");
            c.setFederationMember(false);
            return coachRepository.save(c);
        });
    }
}
