package com.bifai.reminder.bifai_backend.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.time.Instant;

/**
 * Refresh Token 관리 서비스
 * 메모리 기반으로 Refresh Token을 저장하고 관리
 * 추후 Redis 도입 시 쉽게 전환 가능하도록 설계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  
  // 임시로 메모리에 저장 (추후 DB 또는 Redis로 변경 가능)
  private final Map<String, TokenData> tokenStore = new ConcurrentHashMap<>();
  private final Map<Long, String> userTokenMap = new ConcurrentHashMap<>();
  // 디바이스별 토큰 관리를 위한 추가 맵
  private final Map<String, String> deviceTokenMap = new ConcurrentHashMap<>(); // deviceId -> token
  private final Map<Long, Map<String, String>> userDeviceTokenMap = new ConcurrentHashMap<>(); // userId -> (deviceId -> token)
  
  /**
   * Refresh Token 저장
   * @param userId 사용자 ID
   * @param refreshToken Refresh Token
   * @param expirationMs 만료 시간 (밀리초)
   */
  public void saveRefreshToken(Long userId, String refreshToken, long expirationMs) {
    // 기존 토큰이 있으면 삭제
    String oldToken = userTokenMap.get(userId);
    if (oldToken != null) {
      tokenStore.remove(oldToken);
    }
    
    // 새 토큰 저장
    Instant expiresAt = Instant.now().plusMillis(expirationMs);
    tokenStore.put(refreshToken, new TokenData(userId, expiresAt));
    userTokenMap.put(userId, refreshToken);
    
    log.debug("Refresh Token 저장 완료: userId={}", userId);
  }
  
  /**
   * Refresh Token 검증
   * @param refreshToken 검증할 Refresh Token
   * @return 유효한 경우 사용자 ID, 유효하지 않으면 null
   */
  public Long validateRefreshToken(String refreshToken) {
    TokenData tokenData = tokenStore.get(refreshToken);
    
    if (tokenData != null && !tokenData.isExpired()) {
      log.debug("Refresh Token 검증 성공: userId={}", tokenData.userId);
      return tokenData.userId;
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
    tokenStore.remove(oldToken);
    
    // 새 토큰 저장
    saveRefreshToken(userId, newToken, expirationMs);
    
    log.info("Refresh Token 교체 완료: userId={}", userId);
  }
  
  /**
   * 사용자의 Refresh Token 삭제
   * @param userId 사용자 ID
   */
  public void deleteRefreshToken(Long userId) {
    String token = userTokenMap.remove(userId);
    if (token != null) {
      tokenStore.remove(token);
      log.debug("Refresh Token 삭제 완료: userId={}", userId);
    }
  }
  
  /**
   * 사용자의 현재 Refresh Token 조회
   * @param userId 사용자 ID
   * @return Refresh Token (없으면 null)
   */
  public String getRefreshToken(Long userId) {
    String token = userTokenMap.get(userId);
    if (token != null) {
      TokenData tokenData = tokenStore.get(token);
      if (tokenData != null && !tokenData.isExpired()) {
        return token;
      }
    }
    return null;
  }
  
  /**
   * Refresh Token 존재 여부 확인
   * @param userId 사용자 ID
   * @return 존재하면 true, 없으면 false
   */
  public boolean hasRefreshToken(Long userId) {
    return getRefreshToken(userId) != null;
  }
  
  /**
   * 디바이스와 함께 Refresh Token 저장
   * @param userId 사용자 ID
   * @param refreshToken Refresh Token
   * @param deviceId 디바이스 ID
   * @param expirationMs 만료 시간 (밀리초)
   */
  public void saveRefreshTokenWithDevice(Long userId, String refreshToken, String deviceId, long expirationMs) {
    // 기존 토큰 정리
    Map<String, String> userDevices = userDeviceTokenMap.get(userId);
    if (userDevices != null) {
      String oldToken = userDevices.get(deviceId);
      if (oldToken != null) {
        tokenStore.remove(oldToken);
      }
    } else {
      userDevices = new ConcurrentHashMap<>();
      userDeviceTokenMap.put(userId, userDevices);
    }
    
    // 새 토큰 저장
    Instant expiresAt = Instant.now().plusMillis(expirationMs);
    tokenStore.put(refreshToken, new TokenData(userId, deviceId, expiresAt));
    userDevices.put(deviceId, refreshToken);
    deviceTokenMap.put(deviceId, refreshToken);
    
    log.debug("Refresh Token 저장 완료: userId={}, deviceId={}", userId, deviceId);
  }
  
  /**
   * 디바이스 ID와 함께 Refresh Token 검증
   * @param refreshToken 검증할 Refresh Token
   * @param deviceId 디바이스 ID
   * @return 유효한 경우 사용자 ID, 유효하지 않으면 null
   */
  public Long validateRefreshTokenWithDevice(String refreshToken, String deviceId) {
    TokenData tokenData = tokenStore.get(refreshToken);
    
    if (tokenData != null && !tokenData.isExpired() && 
        (deviceId == null || deviceId.equals(tokenData.deviceId))) {
      log.debug("Refresh Token 검증 성공: userId={}, deviceId={}", tokenData.userId, deviceId);
      return tokenData.userId;
    }
    
    log.warn("Refresh Token 검증 실패: 유효하지 않은 토큰 또는 디바이스 불일치");
    return null;
  }
  
  /**
   * 디바이스별 Refresh Token 교체
   * @param oldToken 기존 Refresh Token
   * @param newToken 새로운 Refresh Token
   * @param userId 사용자 ID
   * @param deviceId 디바이스 ID
   * @param expirationMs 만료 시간
   */
  public void rotateRefreshTokenWithDevice(String oldToken, String newToken, Long userId, 
                                          String deviceId, long expirationMs) {
    // 기존 토큰 삭제
    tokenStore.remove(oldToken);
    
    // 새 토큰 저장
    saveRefreshTokenWithDevice(userId, newToken, deviceId, expirationMs);
    
    log.info("Refresh Token 교체 완료: userId={}, deviceId={}", userId, deviceId);
  }
  
  /**
   * 특정 디바이스의 Refresh Token 삭제
   * @param userId 사용자 ID
   * @param deviceId 디바이스 ID
   */
  public void deleteRefreshTokenByDevice(Long userId, String deviceId) {
    Map<String, String> userDevices = userDeviceTokenMap.get(userId);
    if (userDevices != null) {
      String token = userDevices.remove(deviceId);
      if (token != null) {
        tokenStore.remove(token);
        deviceTokenMap.remove(deviceId);
        log.debug("Refresh Token 삭제 완료: userId={}, deviceId={}", userId, deviceId);
      }
    }
  }
  
  /**
   * 디바이스에 유효한 Refresh Token이 있는지 확인
   * @param userId 사용자 ID
   * @param deviceId 디바이스 ID
   * @return 유효한 토큰이 있으면 true
   */
  public boolean hasValidRefreshToken(Long userId, String deviceId) {
    Map<String, String> userDevices = userDeviceTokenMap.get(userId);
    if (userDevices != null) {
      String token = userDevices.get(deviceId);
      if (token != null) {
        TokenData tokenData = tokenStore.get(token);
        return tokenData != null && !tokenData.isExpired();
      }
    }
    return false;
  }
  
  /**
   * 토큰 데이터 클래스
   */
  private static class TokenData {
    private final Long userId;
    private final String deviceId;
    private final Instant expiresAt;
    
    TokenData(Long userId, Instant expiresAt) {
      this(userId, null, expiresAt);
    }
    
    TokenData(Long userId, String deviceId, Instant expiresAt) {
      this.userId = userId;
      this.deviceId = deviceId;
      this.expiresAt = expiresAt;
    }
    
    boolean isExpired() {
      return Instant.now().isAfter(expiresAt);
    }
  }
}