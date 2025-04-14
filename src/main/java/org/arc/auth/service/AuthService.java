package org.arc.auth.service;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.arc.auth.dto.LoginRequest;
import org.arc.auth.dto.RegisterRequest;
import org.arc.auth.dto.AuthResponse;
import org.arc.auth.dto.RefreshRequest;
import org.arc.auth.entity.Role;
import org.arc.auth.entity.User;
import org.arc.auth.repository.UserRepository;
import org.arc.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {


  @Autowired
  private AuthenticationManager authManager;
  @Autowired
  private JwtUtil jwtUtil;
  @Autowired
  private UserRepository userRepo;
  @Autowired
  private RedisTemplate<String, String> redisTemplate;
  @Autowired
  private BCryptPasswordEncoder encoder;


  public boolean register(RegisterRequest request) {
    try {
      String encoded = encoder.encode(request.getPassword());
      Role role = request.getRole() == null ? Role.ROLE_USER : request.getRole();
      User user = new User(request.getUsername(), encoded, role);
      userRepo.save(user);
      return true;
    } catch (Exception e) {
      log.error("[AuthService] register error: {}", e.getMessage());
      return false;
    }
  }

  public AuthResponse login(LoginRequest request) {
    Authentication auth = new UsernamePasswordAuthenticationToken(
        request.getUsername(), request.getPassword()
    );
    authManager.authenticate(auth);

    User user = userRepo.findByUsername(request.getUsername()).orElseThrow();

    // 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(request.getUsername(), user.getRole());
    String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

    // redis 저장
    redisTemplate.opsForValue().set("ACCESS_" + request.getUsername(), accessToken, 1, TimeUnit.HOURS);
    redisTemplate.opsForValue().set("REFRESH_" + request.getUsername(), refreshToken, 7, TimeUnit.DAYS);

    return new AuthResponse(accessToken, refreshToken);
  }

  public AuthResponse refresh(RefreshRequest request) {
    String redisRefreshToken = redisTemplate.opsForValue().get("REFRESH_" + request.getUsername());

    if (redisRefreshToken == null || !redisRefreshToken.equals(request.getRefreshToken())) {
      throw new RuntimeException("Invalid Refresh Token");
    }

    User user = userRepo.findByUsername(request.getUsername()).orElseThrow();
    String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());

    redisTemplate.opsForValue().set("ACCESS_" + user.getUsername(), newAccessToken);

    return new AuthResponse(newAccessToken, request.getRefreshToken()); // 기존 Refresh 유지
  }

  public String logout(String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      String username = jwtUtil.extractUsername(token);
      redisTemplate.delete(username);
    }
    return "Logged out";
  }
}
