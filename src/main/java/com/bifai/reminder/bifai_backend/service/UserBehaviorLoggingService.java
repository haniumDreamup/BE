package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.LogLevel;
import com.bifai.reminder.bifai_backend.repository.UserBehaviorLogRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 사용자 행동 로깅 서비스
 * 비동기로 사용자 인터랙션을 수집하고 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorLoggingService {
  
  private final UserBehaviorLogRepository userBehaviorLogRepository;
  private final UserRepository userRepository;
  
  /**
   * 비동기로 사용자 행동 로그 저장
   */
  @Async("taskExecutor")
  @Transactional
  public CompletableFuture<UserBehaviorLog> logUserBehaviorAsync(
      Long userId,
      String sessionId, 
      ActionType actionType,
      Map<String, Object> actionDetail,
      HttpServletRequest request) {
    
    try {
      // 시작 시간 기록
      long startTime = System.currentTimeMillis();
      
      // 사용자 조회
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
      
      // 디바이스 정보 수집
      Map<String, Object> deviceInfo = extractDeviceInfo(request);
      
      // 로그 엔티티 생성
      UserBehaviorLog behaviorLog = UserBehaviorLog.builder()
          .user(user)
          .sessionId(sessionId != null ? sessionId : generateSessionId())
          .actionType(actionType)
          .actionDetail(actionDetail != null ? actionDetail : new HashMap<>())
          .deviceInfo(deviceInfo)
          .pageUrl(request.getRequestURI())
          .referrerUrl(request.getHeader("Referer"))
          .ipAddress(getClientIpAddress(request))
          .userAgent(request.getHeader("User-Agent"))
          .logLevel(determineLogLevel(actionType))
          .responseTimeMs((int)(System.currentTimeMillis() - startTime))
          .build();
      
      // 민감 정보 마스킹
      behaviorLog.maskSensitiveData();
      
      // 저장
      UserBehaviorLog savedLog = userBehaviorLogRepository.save(behaviorLog);
      
      log.debug("사용자 행동 로그 저장 완료 - userId: {}, actionType: {}, sessionId: {}", 
               userId, actionType, sessionId);
      
      return CompletableFuture.completedFuture(savedLog);
      
    } catch (Exception e) {
      log.error("사용자 행동 로그 저장 실패 - userId: {}, actionType: {}", userId, actionType, e);
      return CompletableFuture.failedFuture(e);
    }
  }
  
  /**
   * 동기 로그 저장 (중요한 이벤트용)
   */
  @Transactional
  public UserBehaviorLog logUserBehavior(
      Long userId,
      String sessionId,
      ActionType actionType,
      Map<String, Object> actionDetail,
      HttpServletRequest request) {
    
    try {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
      
      UserBehaviorLog behaviorLog = UserBehaviorLog.builder()
          .user(user)
          .sessionId(sessionId != null ? sessionId : generateSessionId())
          .actionType(actionType)
          .actionDetail(actionDetail != null ? actionDetail : new HashMap<>())
          .deviceInfo(extractDeviceInfo(request))
          .pageUrl(request.getRequestURI())
          .referrerUrl(request.getHeader("Referer"))
          .ipAddress(getClientIpAddress(request))
          .userAgent(request.getHeader("User-Agent"))
          .logLevel(determineLogLevel(actionType))
          .build();
      
      behaviorLog.maskSensitiveData();
      
      return userBehaviorLogRepository.save(behaviorLog);
      
    } catch (Exception e) {
      log.error("동기 로그 저장 실패 - userId: {}, actionType: {}", userId, actionType, e);
      throw new RuntimeException("로그 저장 실패", e);
    }
  }
  
  /**
   * 간단한 이벤트 로깅
   */
  @Async("taskExecutor")
  public CompletableFuture<Void> logSimpleEvent(
      Long userId,
      String sessionId,
      ActionType actionType,
      String message) {
    
    try {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
      
      Map<String, Object> detail = new HashMap<>();
      detail.put("message", message);
      detail.put("timestamp", LocalDateTime.now().toString());
      
      UserBehaviorLog behaviorLog = UserBehaviorLog.builder()
          .user(user)
          .sessionId(sessionId != null ? sessionId : generateSessionId())
          .actionType(actionType)
          .actionDetail(detail)
          .logLevel(LogLevel.INFO)
          .build();
      
      userBehaviorLogRepository.save(behaviorLog);
      
      return CompletableFuture.completedFuture(null);
      
    } catch (Exception e) {
      log.error("간단한 이벤트 로깅 실패 - userId: {}, actionType: {}", userId, actionType, e);
      return CompletableFuture.failedFuture(e);
    }
  }
  
  /**
   * 오류 로그 기록
   */
  @Async("taskExecutor")
  public void logError(Long userId, String sessionId, String errorMessage, Exception exception) {
    try {
      User user = userRepository.findById(userId).orElse(null);
      
      Map<String, Object> errorDetail = new HashMap<>();
      errorDetail.put("errorMessage", errorMessage);
      errorDetail.put("exceptionClass", exception != null ? exception.getClass().getName() : "Unknown");
      errorDetail.put("stackTrace", exception != null ? exception.getMessage() : "No stack trace");
      
      UserBehaviorLog errorLog = UserBehaviorLog.builder()
          .user(user)
          .sessionId(sessionId != null ? sessionId : generateSessionId())
          .actionType(ActionType.ERROR)
          .actionDetail(errorDetail)
          .logLevel(LogLevel.ERROR)
          .build();
      
      userBehaviorLogRepository.save(errorLog);
      
    } catch (Exception e) {
      log.error("오류 로그 저장 실패", e);
    }
  }
  
  /**
   * 디바이스 정보 추출
   */
  private Map<String, Object> extractDeviceInfo(HttpServletRequest request) {
    Map<String, Object> deviceInfo = new HashMap<>();
    
    String userAgent = request.getHeader("User-Agent");
    if (userAgent != null) {
      deviceInfo.put("userAgent", userAgent);
      
      // 간단한 디바이스 타입 판별
      if (userAgent.contains("Mobile")) {
        deviceInfo.put("deviceType", "MOBILE");
      } else if (userAgent.contains("Tablet")) {
        deviceInfo.put("deviceType", "TABLET");
      } else {
        deviceInfo.put("deviceType", "DESKTOP");
      }
      
      // 브라우저 판별
      if (userAgent.contains("Chrome")) {
        deviceInfo.put("browser", "Chrome");
      } else if (userAgent.contains("Firefox")) {
        deviceInfo.put("browser", "Firefox");
      } else if (userAgent.contains("Safari")) {
        deviceInfo.put("browser", "Safari");
      } else {
        deviceInfo.put("browser", "Other");
      }
      
      // OS 판별
      if (userAgent.contains("Windows")) {
        deviceInfo.put("os", "Windows");
      } else if (userAgent.contains("Mac")) {
        deviceInfo.put("os", "MacOS");
      } else if (userAgent.contains("Android")) {
        deviceInfo.put("os", "Android");
      } else if (userAgent.contains("iOS")) {
        deviceInfo.put("os", "iOS");
      } else {
        deviceInfo.put("os", "Other");
      }
    }
    
    // 화면 크기 (클라이언트에서 전송된 경우)
    String screenSize = request.getHeader("X-Screen-Size");
    if (screenSize != null) {
      deviceInfo.put("screenSize", screenSize);
    }
    
    return deviceInfo;
  }
  
  /**
   * 클라이언트 IP 주소 추출
   */
  private String getClientIpAddress(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    
    // 다중 프록시 경우 첫 번째 IP 사용
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    
    return ip;
  }
  
  /**
   * 세션 ID 생성
   */
  private String generateSessionId() {
    return UUID.randomUUID().toString();
  }
  
  /**
   * 로그 레벨 결정
   */
  private LogLevel determineLogLevel(ActionType actionType) {
    switch (actionType) {
      case ERROR:
      case APP_CRASH:
        return LogLevel.ERROR;
      case WARNING:
      case FORM_ERROR:
        return LogLevel.WARN;
      default:
        return LogLevel.INFO;
    }
  }
}