package com.grad.book.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：签发与解析。令牌里带角色与数据范围，避免每个请求都查一次权限表。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt-secret}") String secret,
                   @Value("${app.jwt-expire-hours:12}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 60 * 60 * 1000;
    }

    public String create(UserContext.CurrentUser u) {
        Date now = new Date();
        return Jwts.builder()
                .subject(u.username())
                .claim("uid", u.userId())
                .claim("name", u.realName())
                .claim("role", u.roleCode())
                .claim("orgId", u.orgId())
                .claim("scope", u.dataScope())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public UserContext.CurrentUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new UserContext.CurrentUser(
                c.get("uid", Number.class).longValue(),
                c.getSubject(),
                c.get("name", String.class),
                c.get("role", String.class),
                c.get("orgId", Number.class) == null ? null : c.get("orgId", Number.class).longValue(),
                c.get("scope", String.class));
    }
}
