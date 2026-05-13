package com.kurashnation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Answers browser CORS preflight with 200 and concrete headers. {@link org.springframework.web.filter.CorsFilter}
 * can still let OPTIONS through to the dispatcher (405) when processing does not attach
 * {@code Access-Control-Allow-Origin} (e.g. subtle {@code CorsProcessor} / path edge cases).
 */
final class CorsPreflightFilter extends OncePerRequestFilter {

    private final CorsConfiguration corsConfiguration;

    CorsPreflightFilter(CorsConfiguration corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!CorsUtils.isPreFlightRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String allowOrigin = corsConfiguration.checkOrigin(origin);
        if (allowOrigin == null) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);
        response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders != null) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestedHeaders);
        } else {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        }

        String requestedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        response.setHeader(
                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                requestedMethod != null ? requestedMethod : "GET,POST,PUT,PATCH,DELETE,OPTIONS"
        );

        Long maxAge = corsConfiguration.getMaxAge();
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge != null ? maxAge : 3600L));
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
