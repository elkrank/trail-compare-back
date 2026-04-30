package com.trailmatch.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTtl;
    private final long refreshTtl;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-ttl-minutes}") long accessTtl,
                      @Value("${app.jwt.refresh-ttl-minutes}") long refreshTtl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.accessTtl = accessTtl; this.refreshTtl = refreshTtl;
    }
    public String generateAccess(String subject, String role) { return generate(subject, accessTtl, Map.of("role", role, "type", "access")); }
    public String generateRefresh(String subject) { return generate(subject, refreshTtl, Map.of("type", "refresh")); }
    private String generate(String subject, long ttlMinutes, Map<String, Object> claims){
        Instant now = Instant.now();
        return Jwts.builder().claims(claims).subject(subject).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlMinutes * 60))).signWith(key).compact();
    }
    public Claims parse(String token){ return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
