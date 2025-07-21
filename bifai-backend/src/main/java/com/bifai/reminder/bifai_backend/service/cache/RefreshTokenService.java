package com.bifai.reminder.bifai_backend.service.cache;

import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스
 * Redis를 사용하여 Refresh Token을 안전하게 저장하고 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  
  private final RedisCacheService redisCacheService;
  
  private static final String REFRESH_TOKEN_PREFIX = "refresh_token:user:";
  private static final String TOKEN_TO_USER_PREFIX = "token_to_user:";
  
  /**
   * Refresh Token 저장
   * @param userId 사용자 ID
   * @param refreshToken Refresh Token
   * @param expirationMs 만료 시간 (밀리초)
   */
  public void saveRefreshToken(Long userId, String refreshToken, long expirationMs) {
    String userKey = REFRESH_TOKEN_PREFIX + userId;
    String tokenKey = TOKEN_TO_USER_PREFIX + refreshToken;
    
    // 사용자 ID → Refresh Token 매핑
    redisCacheService.put(userKey, refreshToken, Duration.ofMillis(expirationMs));
    
    // Refresh Token → 사용자 ID 매핑 (빠른 검증을 위해)
    redisCacheService.put(tokenKey, userId.toString(), Duration.ofMillis(expirationMs));
    
    log.debug("Refresh Token 저장 완료: userId={}", userId);
  }
  
  /**
   * Refresh Token 검증
   * @param refreshToken 검증할 Refresh Token
   * @return 유효한 경우 사용자 ID, 유효하지 않으면 null
   */
  public Long validateRefreshToken(String refreshToken) {
    String tokenKey = TOKEN_TO_USER_PREFIX + refreshToken;
    String userIdStr = redisCacheService.get(tokenKey, String.class);
    
    if (userIdStr != null) {
      Long userId = Long.parseLong(userIdStr);
      String userKey = REFRESH_TOKEN_PREFIX + userId;
      String storedToken = redisCacheService.get(userKey, String.class);
      
      // 저장된 토큰과 일치하는지 확인
      if (refreshToken.equals(storedToken)) {
        log.debug("Refresh Token 검증 성공: userId={}", userId);
        return userId;
      }
    }
    
    log.warn("Refresh Token 검증 실패: 유효하지 않은 토큰");
    return null;
  }
  
  /**
   * Refresh Token 교체 (Token Rotation)
   * @param oldToken 기존 Refresh Token
   * @param newToken 새로운 Refresh Token
   * @param userId 사용자 ID
   * @param expirationMs 만료 시간 (밀리초)
   */
  public void rotateRefreshToken(String oldToken, String newToken, Long userId, long expirationMs) {
    // 기존 토큰 삭제
    deleteToken(oldToken);
    
    // 새 토큰 저장
    saveRefreshToken(userId, newToken, expirationMs);
    
    log.info("Refresh Token 교체 완료: userId={}", userId);
  }
  
  /**
   * 사용자의 Refresh Token 삭제
   * @param userId 사용자 ID
   */
  public void deleteRefreshToken(Long userId) {
    String userKey = REFRESH_TOKEN_PREFIX + userId;
    String refreshToken = redisCacheService.get(userKey, String.class);
    
    if (refreshToken != null) {
      // 토큰 → 사용자 매핑 삭제
      String tokenKey = TOKEN_TO_USER_PREFIX + refreshToken;
      redisCacheService.delete(tokenKey);
    }
    
    // 사용자 → 토큰 매핑 삭제
    redisCacheService.delete(userKey);
    
    log.debug("Refresh Token 삭제 완료: userId={}", userId);
  }
  
  /**
   * 특정 토큰 삭제
   * @param token 삭제할 토큰
   */
  private void deleteToken(String token) {
    String tokenKey = TOKEN_TO_USER_PREFIX + token;
    String userIdStr = redisCacheService.get(tokenKey, String.class);
    
    if (userIdStr != null) {
      Long userId = Long.parseLong(userIdStr);
      String userKey = REFRESH_TOKEN_PREFIX + userId;
      
      // 사용자 → 토큰 매핑 삭제
      redisCacheService.delete(userKey);
    }
    
    // 토큰 → 사용자 매핑 삭제
    redisCacheService.delete(tokenKey);
  }
  
  /**
   * 사용자의 현재 Refresh Token 조회
   * @param userId 사용자 ID
   * @return Refresh Token (없으면 null)
   */
  public String getRefreshToken(Long userId) {
    String userKey = REFRESH_TOKEN_PREFIX + userId;
    return redisCacheService.get(userKey, String.class);
  }
  
  /**
   * Refresh Token 존재 여부 확인
   * @param userId 사용자 ID
   * @return 존재하면 true, 없으면 false
   */
  public boolean hasRefreshToken(Long userId) {
    String userKey = REFRESH_TOKEN_PREFIX + userId;
    return redisCacheService.exists(userKey);
  }
}