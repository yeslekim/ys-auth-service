package org.arc.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.arc.auth.dto.AuthResponse;
import org.arc.auth.dto.LoginRequest;
import org.arc.auth.dto.RefreshRequest;
import org.arc.auth.dto.RegisterRequest;
import org.arc.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인, 로그아웃, 토큰 재발급")
public class AuthController {

  @Autowired
  private AuthService authService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @GetMapping("/test-redis")
  public String testRedis() {
    try {
      redisTemplate.opsForValue().set("ping", "pong", Duration.ofSeconds(5));
      return redisTemplate.opsForValue().get("ping");
    } catch (Exception e) {
      e.printStackTrace();
      return "ERROR: " + e.getMessage();
    }
  }

  @PostMapping("/register")
  @Operation(summary = "회원가입", description = "유저 정보를 DB에 등록합니다.")
  public boolean register(@RequestBody RegisterRequest request) {  // 회원가입
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(summary = "로그인", description = "ID/PW로 로그인 후 JWT 토큰을 발급받습니다.")
  public AuthResponse login(@RequestBody LoginRequest request) { // 로그인
    return authService.login(request);
  }

  @PostMapping("/logout")
  @Operation(summary = "로그아웃", description = "JWT 토큰을 Redis에서 제거합니다.")
  public String logout(@RequestHeader("Authorization") String header) { // 로그아웃
    return authService.logout(header);
  }

  @PostMapping("/refresh")
  @Operation(summary = "토큰 재발급", description = "Refresh Token을 사용해 Access Token을 재발급합니다.")
  public AuthResponse refresh(@RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }
}
