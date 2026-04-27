package com.poshhouse.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    private final String secret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtUtils(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
        @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secret = secret;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UserPrincipal principal) {
        return generateToken(principal, accessTokenExpirationMs, "access");
    }

    public String generateRefreshToken(UserPrincipal principal) {
        return generateToken(principal, refreshTokenExpirationMs, "refresh");
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public LocalDateTime getExpirationDateTime(String token) {
        return LocalDateTime.ofInstant(extractClaim(token, Claims::getExpiration).toInstant(), ZoneOffset.UTC);
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, "access");
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, "refresh");
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }

    private String generateToken(UserPrincipal principal, long expirationMs, String tokenType) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
            .subject(principal.getUsername())
            .claim("userId", principal.getId())
            .claim("email", principal.getEmail())
            .claim("role", principal.getRole())
            .claim("type", tokenType)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(getSigningKey())
            .compact();
    }

    private boolean isTokenValid(String token, UserDetails userDetails, String expectedType) {
        String username = extractUsername(token);
        String type = extractClaim(token, claims -> claims.get("type", String.class));
        return username.equals(userDetails.getUsername())
            && expectedType.equals(type)
            && extractClaim(token, Claims::getExpiration).after(new Date());
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
