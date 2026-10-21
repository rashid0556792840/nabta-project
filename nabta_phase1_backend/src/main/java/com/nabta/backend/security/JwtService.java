package com.nabta.backend.security;
import com.nabta.backend.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;
    public JwtService(@Value("${nabta.jwt.secret}") String secret, @Value("${nabta.jwt.expiration-ms}") long expirationMs) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalStateException("NABTA_JWT_SECRET must be at least 32 bytes.");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMs = expirationMs;
    }
    public String generate(AppUser user) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(user.getId())).issuedAt(now).expiration(new Date(now.getTime()+expirationMs)).signWith(key).compact();
    }
    public String subject(String token) {
        try { Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); return claims.getSubject(); }
        catch (JwtException | IllegalArgumentException ex) { return null; }
    }
    public long getExpirationSeconds() { return expirationMs / 1000; }
}
