package tn.esprit.smartforma.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.smartforma.auth.entity.Compte;
import tn.esprit.smartforma.exception.UnauthorizedException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${smartforma.jwt.secret}") String secret,
            @Value("${smartforma.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Compte compte) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(String.valueOf(compte.getId()))
                .claim("email", compte.getEmail())
                .claim("role", compte.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key);

        if (compte.getApprenant() != null && compte.getApprenant().getId() != null) {
            builder.claim("apprenantId", compte.getApprenant().getId());
        }

        return builder.compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Jeton d'authentification invalide ou expiré.");
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
