package com.gccoffee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;
    private final long adminExpireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-days:7}") int expireDays,
                   @Value("${app.jwt.admin-expire-days:1}") int adminExpireDays) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expireMillis = expireDays * 24L * 3600_000L;
        this.adminExpireMillis = adminExpireDays * 24L * 3600_000L;
    }

    public String createToken(Long uid, String role, String nickname, boolean admin) {
        long ttl = admin ? adminExpireMillis : expireMillis;
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(uid))
                .claim("role", role)
                .claim("nickname", nickname == null ? "" : nickname)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token，失败返回 null
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
