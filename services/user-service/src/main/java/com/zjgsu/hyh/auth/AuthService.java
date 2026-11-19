package com.zjgsu.hyh.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordHasher passwordHasher;
    private final JwtEncoder jwtEncoder;
    private final long tokenTtlMinutes;

    public AuthService(JdbcTemplate jdbcTemplate, PasswordHasher passwordHasher, JwtEncoder jwtEncoder,
            @Value("${JWT_TTL_MINUTES:60}") long tokenTtlMinutes) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordHasher = passwordHasher;
        this.jwtEncoder = jwtEncoder;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public LoginResult login(String userId, String password) {
        List<Credential> credentials = jdbcTemplate.query(
                "SELECT password_hash, role FROM user_credentials WHERE user_id = ?",
                (rs, row) -> new Credential(rs.getString("password_hash"), rs.getString("role")), userId);
        if (credentials.isEmpty() || !passwordHasher.matches(password, credentials.getFirst().passwordHash())) {
            throw new InvalidCredentialsException();
        }
        String role = credentials.getFirst().role();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tokenTtlMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("user-service")
                .subject(userId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", List.of(role))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResult(token, role, tokenTtlMinutes * 60);
    }

    private record Credential(String passwordHash, String role) { }

    public record LoginResult(String accessToken, String role, long expiresIn) { }

    public static class InvalidCredentialsException extends RuntimeException { }
}
