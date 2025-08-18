package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.InteractionPattern;
import com.bifai.reminder.bifai_backend.entity.InteractionPattern.PatternType;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import com.bifai.reminder.bifai_backend.repository.InteractionPatternRepository;
import com.bifai.reminder.bifai_backend.repository.UserBehaviorLogRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 인터랙션 패턴 분석 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("인터랙션 패턴 분석 서비스 테스트")
class InteractionPatternAnalysisServiceTest {
  
  @Mock
  private InteractionPatternRepository patternRepository;
  
  @Mock
  private UserBehaviorLogRepository behaviorLogRepository;
  
  @Mock
  private UserRepository userRepository;
  
  @InjectMocks
  private InteractionPatternAnalysisService analysisService;
  
  private User testUser;
  private List<UserBehaviorLog> testLogs;
  private InteractionPattern testPattern;
  
  @BeforeEach
  void setUp() {
    testUser = User.builder()
      .userId(1L)
      .username("testuser")
      .email("test@example.com")
      .name("테스트 사용자")
      .build();
    
    testLogs = createTestLogs();
    
    testPattern = InteractionPattern.builder()
      .id(1L)
      .user(testUser)
      .patternType(PatternType.DAILY)
      .analysisDate(LocalDateTime.now())
      .timeWindowStart(LocalDateTime.now().minusHours(24))
      .timeWindowEnd(LocalDateTime.now())
      .clickFrequency(5.0)
      .avgSessionDuration(1800.0)
      .pageViewCount(10)
      .errorRate(2.5)
      .isAnomaly(false)
      .sampleSize(100)
      .build();
  }
  
  @Test
  @DisplayName("일일 패턴 분석 성공")
  void analyzeDailyPattern_Success() {
    // given
    LocalDateTime testDate = LocalDateTime.now();
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(behaviorLogRepository.findByUserAndCreatedAtBetween(any(), any(), any()))
      .thenReturn(testLogs);
    when(patternRepository.save(any(InteractionPattern.class))).thenReturn(testPattern);
    when(patternRepository.calculateBaselineMetrics(any(), any(), any(), any()))
      .thenReturn(new Object[]{5.0, 1800.0, 2.0, 1.0, 200.0, 0.5});
    
    // when
    InteractionPattern result = analysisService.analyzeDailyPattern(1L, testDate);
    
    // then
    assertNotNull(result);
    assertEquals(PatternType.DAILY, result.getPatternType());
    verify(patternRepository).save(any(InteractionPattern.class));
  }
  
  @Test
  @DisplayName("실시간 패턴 분석 성공")
  void analyzeRealtimePattern_Success() throws Exception {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(behaviorLogRepository.findByUserAndCreatedAtBetween(any(), any(), any()))
      .thenReturn(testLogs);
    when(patternRepository.save(any(InteractionPattern.class))).thenReturn(testPattern);
    when(patternRepository.calculateBaselineMetrics(any(), any(), any(), any()))
      .thenReturn(new Object[]{5.0, 1800.0, 2.0, 1.0, 200.0, 0.5});
    
    // when
    CompletableFuture<InteractionPattern> future = analysisService.analyzeRealtimePattern(1L);
    InteractionPattern result = future.get();
    
    // then
    assertNotNull(result);
    assertEquals(PatternType.DAILY, result.getPatternType()); // 테스트 패턴이 DAILY로 설정됨
    verify(patternRepository).save(any(InteractionPattern.class));
  }
  
  @Test
  @DisplayName("세션별 패턴 분석 성공")
  void analyzeSessionPattern_Success() {
    // given
    String sessionId = "test-session-123";
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(behaviorLogRepository.findByUserAndSessionId(testUser, sessionId))
      .thenReturn(testLogs);
    when(patternRepository.save(any(InteractionPattern.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    
    // when
    InteractionPattern result = analysisService.analyzeSessionPattern(1L, sessionId);
    
    // then
    assertNotNull(result);
    assertEquals(PatternType.SESSION, result.getPatternType());
    assertNotNull(result.getAvgSessionDuration());
    verify(patternRepository).save(any(InteractionPattern.class));
  }
  
  @Test
  @DisplayName("로그가 없을 때 null 반환")
  void analyzeDailyPattern_NoLogs_ReturnsNull() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(behaviorLogRepository.findByUserAndCreatedAtBetween(any(), any(), any()))
      .thenReturn(new ArrayList<>());
    
    // when
    InteractionPattern result = analysisService.analyzeDailyPattern(1L, LocalDateTime.now());
    
    // then
    assertNull(result);
    verify(patternRepository, never()).save(any());
  }
  
  @Test
  @DisplayName("이상 패턴 감지 - 높은 클릭 빈도")
  void detectAnomaly_HighClickFrequency() {
    // given
    InteractionPattern pattern = InteractionPattern.builder()
      .user(testUser)
      .patternType(PatternType.REALTIME)
      .clickFrequency(50.0) // 매우 높은 클릭 빈도
      .errorRate(2.0)
      .sampleSize(100)
      .build();
    
    // 평균 5, 표준편차 2로 설정 (50은 3σ를 크게 벗어남)
    Object[] baselineData = new Object[]{5.0, 1800.0, 2.0, 2.0, 200.0, 0.5};
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(behaviorLogRepository.findByUserAndCreatedAtBetween(any(), any(), any()))
      .thenReturn(createHighActivityLogs());
    when(patternRepository.calculateBaselineMetrics(any(), any(), any(), any()))
      .thenReturn(baselineData);
    when(patternRepository.save(any(InteractionPattern.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    
    // when
    InteractionPattern result = analysisService.analyzeDailyPattern(1L, LocalDateTime.now());
    
    // then
    assertNotNull(result);
    assertTrue(result.getIsAnomaly());
    assertThat(result.getAnomalyScore()).isGreaterThan(0);
  }
  
  @Test
  @DisplayName("최근 패턴 조회")
  void getRecentPatterns_Success() {
    // given
    List<InteractionPattern> patterns = Arrays.asList(testPattern);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(patternRepository.findByUserAndAnalysisDateAfterOrderByAnalysisDateDesc(any(), any()))
      .thenReturn(patterns);
    
    // when
    List<InteractionPattern> result = analysisService.getRecentPatterns(1L, 7);
    
    // then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testPattern, result.get(0));
  }
  
  @Test
  @DisplayName("이상 패턴 조회")
  void getAnomalousPatterns_Success() {
    // given
    InteractionPattern anomalousPattern = InteractionPattern.builder()
      .id(2L)
      .user(testUser)
      .patternType(PatternType.REALTIME)
      .isAnomaly(true)
      .anomalyScore(85.0)
      .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(patternRepository.findAnomalousPatterns(any(), any(), any()))
      .thenReturn(Arrays.asList(anomalousPattern));
    
    // when
    List<InteractionPattern> result = analysisService.getAnomalousPatterns(1L, 7);
    
    // then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getIsAnomaly());
    assertEquals(85.0, result.get(0).getAnomalyScore());
  }
  
  @Test
  @DisplayName("사용자를 찾을 수 없는 경우 예외 발생")
  void analyzeDailyPattern_UserNotFound_ThrowsException() {
    // given
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    
    // when & then
    assertThrows(IllegalArgumentException.class, () -> {
      analysisService.analyzeDailyPattern(999L, LocalDateTime.now());
    });
  }
  
  private List<UserBehaviorLog> createTestLogs() {
    List<UserBehaviorLog> logs = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    
    for (int i = 0; i < 10; i++) {
      UserBehaviorLog log = UserBehaviorLog.builder()
        .id((long) i)
        .user(testUser)
        .sessionId("session-123")
        .actionType(i % 3 == 0 ? ActionType.BUTTON_CLICK : ActionType.PAGE_VIEW)
        .pageUrl("/page" + i)
        .responseTimeMs(100 + i * 10)
        .timestamp(now.minusMinutes(i * 5))
        .build();
      logs.add(log);
    }
    
    // 에러 로그 추가
    logs.add(UserBehaviorLog.builder()
      .id(11L)
      .user(testUser)
      .sessionId("session-123")
      .actionType(ActionType.ERROR)
      .timestamp(now.minusMinutes(30))
      .build());
    
    return logs;
  }
  
  private List<UserBehaviorLog> createHighActivityLogs() {
    List<UserBehaviorLog> logs = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    
    // 높은 클릭 빈도 시뮬레이션
    for (int i = 0; i < 100; i++) {
      logs.add(UserBehaviorLog.builder()
        .id((long) i)
        .user(testUser)
        .sessionId("high-activity-session")
        .actionType(ActionType.BUTTON_CLICK)
        .timestamp(now.minusSeconds(i))
        .build());
    }
    
    return logs;
  }
}