package com.kurashnation.config;

import com.kurashnation.util.LogUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Единый префикс {@code /api} для REST без {@code server.servlet.context-path}.
 * Так фронт по-прежнему ходит на {@code http://host:port/api/...}, а Spring Security и
 * маппинги совпадают с фактическим путём (меньше 404 из-за несовпадения context-path и прокси).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @PostConstruct
    void logStartup() {
        LogUtil.info("WebMvc: префикс REST /api для пакета com.kurashnation.controller");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forBasePackage("com.kurashnation.controller"));
    }
}
