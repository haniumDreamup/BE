package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.PoseDataRepository;
import com.bifai.reminder.bifai_backend.repository.PoseSessionRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 간단한 PoseDataService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class SimplePoseDataServiceTest {

  @Mock
  private PoseDataRepository poseDataRepository;

  @Mock
  private PoseSessionRepository poseSessionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FallDetectionService fallDetectionService;

  private PoseDataService poseDataService;

  private User testUser;
  private PoseSession testSession;

  @BeforeEach
  void setUp() {
    // 실제 ObjectMapper 사용 (Redis는 제외)
    ObjectMapper objectMapper = new ObjectMapper();
    
    // Redis 없이 테스트용 서비스 생성
    poseDataService = new PoseDataService(
        poseDataRepository,
        poseSessionRepository,
        userRepository,
        fallDetectionService,
        objectMapper
    );

    testUser = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .build();

    testSession = PoseSession.builder()
        .id(1L)
        .sessionId("test-session-123")
        .user(testUser)
        .startTime(LocalDateTime.now())
        .status(PoseSession.SessionStatus.ACTIVE)
        .totalFrames(10)
        .build();
  }

  @Test
  @DisplayName("낙상 상태 조회")
  void getFallStatus() {
    // given
    Long userId = 1L;

    when(poseSessionRepository.findByUserIdAndStatus(userId, PoseSession.SessionStatus.ACTIVE))
        .thenReturn(Optional.of(testSession));

    List<FallEvent> recentEvents = Arrays.asList(
        FallEvent.builder()
            .id(1L)
            .user(testUser)
            .detectedAt(LocalDateTime.now().minusHours(2))
            .severity(FallEvent.FallSeverity.MEDIUM)
            .confidenceScore(0.75f)
            .status(FallEvent.EventStatus.NOTIFIED)
            .falsePositive(false)
            .build()
    );

    when(fallDetectionService.getRecentFallEvents(userId, 24))
        .thenReturn(recentEvents);

    // when
    FallStatusDto status = poseDataService.getFallStatus(userId);

    // then
    assertThat(status).isNotNull();
    assertThat(status.getUserId()).isEqualTo(userId);
    assertThat(status.getIsMonitoring()).isTrue();
    assertThat(status.getSessionActive()).isTrue();
    assertThat(status.getCurrentSessionId()).isEqualTo("test-session-123");
    assertThat(status.getRecentFallEvents()).hasSize(1);
    assertThat(status.getRecentFallEvents().get(0).getSeverity())
        .isEqualTo(FallEvent.FallSeverity.MEDIUM);
  }

  @Test
  @DisplayName("낙상 피드백 제출")
  void submitFallFeedback() {
    // given
    Long eventId = 1L;
    Boolean isFalsePositive = true;
    String userComment = "의자에 앉는 동작이었습니다";

    doNothing().when(fallDetectionService)
        .updateFallEventFeedback(eventId, isFalsePositive, userComment);

    // when
    assertThatCode(() -> 
        poseDataService.submitFallFeedback(eventId, isFalsePositive, userComment))
        .doesNotThrowAnyException();

    // then
    verify(fallDetectionService, times(1))
        .updateFallEventFeedback(eventId, isFalsePositive, userComment);
  }

  @Test
  @DisplayName("활성 세션이 없을 때 낙상 상태 조회")
  void getFallStatus_NoActiveSession() {
    // given
    Long userId = 1L;

    when(poseSessionRepository.findByUserIdAndStatus(userId, PoseSession.SessionStatus.ACTIVE))
        .thenReturn(Optional.empty());

    when(fallDetectionService.getRecentFallEvents(userId, 24))
        .thenReturn(new ArrayList<>());

    // when
    FallStatusDto status = poseDataService.getFallStatus(userId);

    // then
    assertThat(status).isNotNull();
    assertThat(status.getUserId()).isEqualTo(userId);
    assertThat(status.getIsMonitoring()).isFalse();
    assertThat(status.getSessionActive()).isFalse();
    assertThat(status.getCurrentSessionId()).isNull();
    assertThat(status.getRecentFallEvents()).isEmpty();
  }
}