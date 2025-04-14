package org.arc.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import org.arc.auth.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secretKeyString;

  // 실제 서명에 사용할 Key 객체
  private Key secretKey;

  private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1시간

  // secretKeyString → Key 객체로 변환 (최초 1회)
  @PostConstruct
  public void init() {
    this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
  }

  public String generateAccessToken(String username, Role role) {
    return Jwts.builder()
        .setSubject(username)
        .claim("role", role.name())
        .setIssuedAt(new Date())  // 발급 시간
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 만료 시간
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public String generateRefreshToken(String username) {
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7)) // 7일
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public String extractUsername(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  // role 추출
  public Role extractRole(String token) {
    return Role.valueOf(getClaims(token).get("role", String.class));
  }

  // Claims 추출
  private Claims getClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  // 토큰 유효성 검증
  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(secretKey)
          .build()
          .parseClaimsJws(token); // 유효성 검사 + 만료 확인
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }


}
