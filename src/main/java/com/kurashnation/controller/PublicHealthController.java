package com.kurashnation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Лёгкий endpoint для Docker healthcheck (без Actuator / Security-нюансов).
 * Итоговый путь: {@code GET /api/health/live} (см. {@link com.kurashnation.config.WebMvcConfig}).
 */
@RestController
public class PublicHealthController {

    @GetMapping("/health/live")
    public String live() {
        return "OK";
    }
}
