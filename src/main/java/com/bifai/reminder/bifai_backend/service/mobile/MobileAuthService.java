package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileLoginRequest;
import com.bifai.reminder.bifai_backend.dto.mobile.MobileLoginResponse;
import com.bifai.reminder.bifai_backend.dto.mobile.MobileRefreshRequest;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 모바일 인증 서비스
 * 
 * 모바일 앱 전용 인증 로직을 처리합니다.
 * 디바이스 관리와 푸시 토큰 관리를 포함합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MobileAuthService {
  
  private final UserRepository userRepository;
  private final GuardianRepository guardianRepository;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenService refreshTokenService;
  private final MobileDeviceService deviceService;
  
  /**
   * 모바일 로그인 처리
   * 
   * @param request 로그인 요청
   * @return 로그인 응답 (토큰 포함)
   */
  public MobileLoginResponse login(MobileLoginRequest request) {
    log.info("모바일 로그인 처리: username={}, deviceId={}", 
        request.getUsername(), request.getDeviceId());
    
    try {
      // 인증 시도
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getPassword()
          )
      );
      
      // 사용자 정보 조회
      User user = userRepository.findByUsernameOrEmail(request.getUsername())
          .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요"));
      
      // 디바이스 정보 저장
      deviceService.registerDevice(user, request);
      
      // 푸시 토큰 업데이트 (있는 경우)
      if (request.getPushToken() != null) {
        deviceService.updatePushToken(request.getDeviceId(), request.getPushToken());
      }
      
      // JWT 토큰 생성
      String accessToken = jwtTokenProvider.generateAccessToken(authentication);
      String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
      
      // Refresh Token을 Redis에 저장 (디바이스 ID와 연결)
      refreshTokenService.saveRefreshTokenWithDevice(
          user.getUserId(),
          refreshToken,
          request.getDeviceId(),
          jwtTokenProvider.getRefreshTokenExpirationMs()
      );
      
      // 로그인 시간 업데이트
      user.updateLastLogin();
      userRepository.save(user);
      
      log.info("모바일 로그인 성공: userId={}, deviceId={}", 
          user.getUserId(), request.getDeviceId());
      
      return buildLoginResponse(user, accessToken, refreshToken);
      
    } catch (AuthenticationException e) {
      log.error("모바일 로그인 실패: {}", e.getMessage());
      throw new IllegalArgumentException("아이디나 비밀번호를 확인해주세요");
    }
  }
  
  /**
   * 토큰 갱신
   * 
   * @param request 리프레시 토큰 요청
   * @return 새로운 토큰 응답
   */
  public MobileLoginResponse refresh(MobileRefreshRequest request) {
    log.info("토큰 갱신 처리: deviceId={}", request.getDeviceId());
    
    String refreshToken = request.getRefreshToken();
    
    // Refresh Token 유효성 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new IllegalArgumentException("유효하지 않은 토큰이에요");
    }
    
    // 토큰 타입 확인
    String tokenType = jwtTokenProvider.getTokenType(refreshToken);
    if (!"refresh".equals(tokenType)) {
      throw new IllegalArgumentException("리프레시 토큰이 아니에요");
    }
    
    // Redis에서 Refresh Token 검증 (디바이스 ID 확인)
    Long userId = refreshTokenService.validateRefreshTokenWithDevice(
        refreshToken, 
        request.getDeviceId()
    );
    
    if (userId == null) {
      throw new IllegalArgumentException("다시 로그인해 주세요");
    }
    
    // 사용자 정보 조회
    User user = userRepository.findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요"));
    
    // 새로운 토큰 생성
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        user.getUsername(), null, 
        java.util.Collections.singletonList(() -> "ROLE_USER")
    );
    
    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);
    
    // Token Rotation - 기존 토큰 무효화 및 새 토큰 저장
    refreshTokenService.rotateRefreshTokenWithDevice(
        refreshToken,
        newRefreshToken,
        user.getUserId(),
        request.getDeviceId(),
        jwtTokenProvider.getRefreshTokenExpirationMs()
    );
    
    log.info("토큰 갱신 성공: userId={}", user.getUserId());
    
    return buildLoginResponse(user, newAccessToken, newRefreshToken);
  }
  
  /**
   * 로그아웃 처리
   * 
   * @param deviceId 디바이스 ID
   */
  public void logout(String deviceId) {
    log.info("로그아웃 처리: deviceId={}", deviceId);
    
    try {
      // 디바이스 정보로 사용자 찾기
      Long userId = deviceService.getUserIdByDeviceId(deviceId);
      
      if (userId != null) {
        // Redis에서 Refresh Token 삭제
        refreshTokenService.deleteRefreshTokenByDevice(userId, deviceId);
        
        // 푸시 토큰 제거
        deviceService.removePushToken(deviceId);
        
        log.info("로그아웃 완료: userId={}, deviceId={}", userId, deviceId);
      }
    } catch (Exception e) {
      log.error("로그아웃 처리 중 오류: {}", e.getMessage());
      // 로그아웃은 실패해도 진행
    }
  }
  
  /**
   * 자동 로그인 가능 여부 확인
   * 
   * @param deviceId 디바이스 ID
   * @return 자동 로그인 가능 여부
   */
  public boolean canAutoLogin(String deviceId) {
    try {
      Long userId = deviceService.getUserIdByDeviceId(deviceId);
      if (userId == null) {
        return false;
      }
      
      // Redis에서 해당 디바이스의 Refresh Token 존재 여부 확인
      return refreshTokenService.hasValidRefreshToken(userId, deviceId);
      
    } catch (Exception e) {
      log.error("자동 로그인 체크 실패: {}", e.getMessage());
      return false;
    }
  }
  
  /**
   * 로그인 응답 생성
   * 
   * @param user 사용자
   * @param accessToken 액세스 토큰
   * @param refreshToken 리프레시 토큰
   * @return 로그인 응답
   */
  private MobileLoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
    // 주 보호자 정보 조회
    Optional<Guardian> primaryGuardian = guardianRepository.findPrimaryGuardianByUserId(user.getUserId());
    String guardianContact = primaryGuardian
        .map(g -> maskPhoneNumber(g.getPrimaryPhone()))
        .orElse(null);
    
    return MobileLoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000) // 초 단위
        .user(MobileLoginResponse.UserInfo.builder()
            .id(user.getUserId())
            .name(user.getName())
            .profileImage(user.getProfileImageUrl())
            .cognitiveLevel(user.getCognitiveLevel().name())
            .language(user.getLanguagePreference())
            .emergencyMode(user.getEmergencyModeEnabled())
            .guardianContact(guardianContact)
            .build())
        .build();
  }
  
  /**
   * 전화번호 마스킹
   * 
   * @param phoneNumber 전화번호
   * @return 마스킹된 전화번호
   */
  private String maskPhoneNumber(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.length() < 8) {
      return phoneNumber;
    }
    
    // 010-1234-5678 -> 010-****-5678
    return phoneNumber.replaceAll("(\\d{3})-?(\\d{3,4})-?(\\d{4})", "$1-****-$3");
  }
}