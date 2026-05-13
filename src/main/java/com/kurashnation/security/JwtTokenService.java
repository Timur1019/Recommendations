package com.kurashnation.security;

import com.kurashnation.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenService {

    private final Key key;
    private final long ttlMinutes;

    public JwtTokenService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-ttl-minutes}") long ttlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64OrRawToBase64(secret)));
        this.ttlMinutes = ttlMinutes;
    }

    public String generateToken(Long userId, String email, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static String base64OrRawToBase64(String secret) {
        // jjwt needs a sufficiently long key; allowing env to pass either raw or base64.
        // If it's already base64, Decoders.BASE64 will work; if not, we base64 it.
        // Heuristic: base64 secrets often end with '=' and contain only base64 chars.
        String s = secret == null ? "" : secret.trim();
        boolean looksBase64 = s.matches("^[A-Za-z0-9+/=]+$") && (s.length() % 4 == 0);
        if (looksBase64) return s;
        return java.util.Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

