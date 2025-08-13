package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import com.bifai.reminder.bifai_backend.repository.UserBehaviorLogRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 사용자 행동 로깅 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 행동 로깅 서비스 테스트")
class UserBehaviorLoggingServiceTest {
  
  @Mock
  private UserBehaviorLogRepository userBehaviorLogRepository;
  
  @Mock
  private UserRepository userRepository;
  
  @Mock
  private HttpServletRequest request;
  
  @InjectMocks
  private UserBehaviorLoggingService loggingService;
  
  private User testUser;
  private UserBehaviorLog testLog;
  
  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .userId(1L)
        .username("testuser")
        .email("test@example.com")
        .name("테스트 사용자")
        .build();
    
    testLog = UserBehaviorLog.builder()
        .id(1L)
        .user(testUser)
        .sessionId("test-session-123")
        .actionType(ActionType.PAGE_VIEW)
        .build();
  }
  
  @Test
  @DisplayName("비동기 로그 저장 성공")
  void logUserBehaviorAsync_Success() throws Exception {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userBehaviorLogRepository.save(any(UserBehaviorLog.class))).thenReturn(testLog);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
    when(request.getRemoteAddr()).thenReturn("192.168.1.1");
    
    Map<String, Object> actionDetail = new HashMap<>();
    actionDetail.put("test", "value");
    
    // when
    CompletableFuture<UserBehaviorLog> future = loggingService.logUserBehaviorAsync(
        1L, "test-session", ActionType.PAGE_VIEW, actionDetail, request
    );
    
    UserBehaviorLog result = future.get();
    
    // then
    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(userRepository).findById(1L);
    verify(userBehaviorLogRepository).save(any(UserBehaviorLog.class));
  }
  
  @Test
  @DisplayName("동기 로그 저장 성공")
  void logUserBehavior_Success() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userBehaviorLogRepository.save(any(UserBehaviorLog.class))).thenReturn(testLog);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getHeader("User-Agent")).thenReturn("Chrome/100.0");
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    
    // when
    UserBehaviorLog result = loggingService.logUserBehavior(
        1L, "test-session", ActionType.BUTTON_CLICK, null, request
    );
    
    // then
    assertNotNull(result);
    assertEquals(testLog.getId(), result.getId());
    verify(userBehaviorLogRepository).save(any(UserBehaviorLog.class));
  }
  
  @Test
  @DisplayName("간단한 이벤트 로깅")
  void logSimpleEvent_Success() throws Exception {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userBehaviorLogRepository.save(any(UserBehaviorLog.class))).thenReturn(testLog);
    
    // when
    CompletableFuture<Void> future = loggingService.logSimpleEvent(
        1L, "test-session", ActionType.FEATURE_USE, "테스트 기능 사용"
    );
    
    future.get();
    
    // then
    verify(userBehaviorLogRepository).save(any(UserBehaviorLog.class));
  }
  
  @Test
  @DisplayName("오류 로그 기록")
  void logError_Success() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userBehaviorLogRepository.save(any(UserBehaviorLog.class))).thenReturn(testLog);
    
    Exception testException = new RuntimeException("테스트 예외");
    
    // when
    loggingService.logError(1L, "test-session", "테스트 오류", testException);
    
    // then
    verify(userBehaviorLogRepository).save(argThat(log -> 
        log.getActionType() == ActionType.ERROR &&
        log.getLogLevel() == UserBehaviorLog.LogLevel.ERROR
    ));
  }
  
  @Test
  @DisplayName("사용자를 찾을 수 없는 경우")
  void logUserBehavior_UserNotFound() {
    // given
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    
    // when & then
    assertThrows(RuntimeException.class, () -> {
      loggingService.logUserBehavior(
          999L, "test-session", ActionType.PAGE_VIEW, null, request
      );
    });
    
    verify(userBehaviorLogRepository, never()).save(any());
  }
  
  @Test
  @DisplayName("IP 주소 마스킹 테스트")
  void maskSensitiveData_IpAddress() {
    // given
    UserBehaviorLog log = UserBehaviorLog.builder()
        .ipAddress("192.168.1.100")
        .build();
    
    // when
    log.maskSensitiveData();
    
    // then
    assertEquals("192.168.1.xxx", log.getIpAddress());
  }
  
  @Test
  @DisplayName("민감 정보 제거 테스트")
  void maskSensitiveData_ActionDetail() {
    // given
    Map<String, Object> detail = new HashMap<>();
    detail.put("password", "secret123");
    detail.put("email", "user@example.com");
    detail.put("normalData", "value");
    
    UserBehaviorLog log = UserBehaviorLog.builder()
        .actionDetail(detail)
        .build();
    
    // when
    log.maskSensitiveData();
    
    // then
    assertFalse(log.getActionDetail().containsKey("password"));
    assertFalse(log.getActionDetail().containsKey("email"));
    assertTrue(log.getActionDetail().containsKey("normalData"));
  }
  
  @Test
  @DisplayName("디바이스 정보 추출 - 모바일")
  void extractDeviceInfo_Mobile() {
    // given
    when(request.getHeader("User-Agent"))
        .thenReturn("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36");
    when(request.getRequestURI()).thenReturn("/api/test");
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userBehaviorLogRepository.save(any(UserBehaviorLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    
    // when
    UserBehaviorLog result = loggingService.logUserBehavior(
        1L, "test-session", ActionType.PAGE_VIEW, null, request
    );
    
    // then
    assertThat(result.getDeviceInfo()).containsEntry("deviceType", "MOBILE");
    assertThat(result.getDeviceInfo()).containsEntry("os", "Android");
  }
  
  @Test
  @DisplayName("로그 레벨 결정 - ERROR 타입")
  void determineLogLevel_Error() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userBehaviorLogRepository.save(any(UserBehaviorLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(request.getRequestURI()).thenReturn("/api/test");
    
    // when
    UserBehaviorLog result = loggingService.logUserBehavior(
        1L, "test-session", ActionType.ERROR, null, request
    );
    
    // then
    assertEquals(UserBehaviorLog.LogLevel.ERROR, result.getLogLevel());
  }
}