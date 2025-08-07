package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.FallEventRepository;
import com.bifai.reminder.bifai_backend.repository.PoseDataRepository;
import com.bifai.reminder.bifai_backend.service.NotificationService;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
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
 * 간단한 FallDetectionService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class SimpleFallDetectionServiceTest {

  @Mock
  private PoseDataRepository poseDataRepository;

  @Mock
  private FallEventRepository fallEventRepository;

  @Mock
  private NotificationService notificationService;
  
  @Mock
  private WebSocketService webSocketService;

  private FallDetectionService fallDetectionService;

  private User testUser;
  private PoseSession testSession;

  @BeforeEach
  void setUp() {
    // ObjectMapper는 실제 인스턴스 사용
    ObjectMapper objectMapper = new ObjectMapper();
    
    fallDetectionService = new FallDetectionService(
        poseDataRepository,
        fallEventRepository,
        notificationService,
        webSocketService,
        objectMapper
    );

    testUser = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .build();

    testSession = PoseSession.builder()
        .id(1L)
        .sessionId("test-session")
        .user(testUser)
        .startTime(LocalDateTime.now())
        .status(PoseSession.SessionStatus.ACTIVE)
        .build();
  }

  @Test
  @DisplayName("낙상 감지 테스트 - 데이터 부족으로 감지 안됨")
  void detectFall_InsufficientData() {
    // given
    PoseData currentFrame = PoseData.builder()
        .id(1L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.85f)
        .isHorizontal(true)
        .velocityY(0.2f)
        .motionScore(0.01f)
        .landmarksJson("[]")
        .build();

    // 29개의 프레임만 반환 (30개 미만)
    List<PoseData> insufficientFrames = new ArrayList<>();
    for (int i = 0; i < 29; i++) {
      insufficientFrames.add(PoseData.builder().build());
    }

    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(insufficientFrames);

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isEmpty();
    verify(fallEventRepository, never()).save(any());
    verify(notificationService, never()).sendFallAlert(any());
  }

  @Test
  @DisplayName("최근 낙상 이벤트 조회 테스트")
  void getRecentFallEvents() {
    // given
    Long userId = 1L;
    List<FallEvent> expectedEvents = Arrays.asList(
        FallEvent.builder()
            .id(1L)
            .user(testUser)
            .detectedAt(LocalDateTime.now().minusHours(1))
            .severity(FallEvent.FallSeverity.HIGH)
            .build(),
        FallEvent.builder()
            .id(2L)
            .user(testUser)
            .detectedAt(LocalDateTime.now().minusHours(2))
            .severity(FallEvent.FallSeverity.MEDIUM)
            .build()
    );

    when(fallEventRepository.findByUserIdAndDetectedAtBetween(
        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(expectedEvents);

    // when
    List<FallEvent> result = fallDetectionService.getRecentFallEvents(userId, 24);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getSeverity()).isEqualTo(FallEvent.FallSeverity.HIGH);
    assertThat(result.get(1).getSeverity()).isEqualTo(FallEvent.FallSeverity.MEDIUM);
  }

  @Test
  @DisplayName("낙상 이벤트 피드백 업데이트")
  void updateFallEventFeedback() {
    // given
    Long eventId = 1L;
    FallEvent existingEvent = FallEvent.builder()
        .id(eventId)
        .user(testUser)
        .status(FallEvent.EventStatus.NOTIFIED)
        .falsePositive(false)
        .build();

    when(fallEventRepository.findById(eventId))
        .thenReturn(Optional.of(existingEvent));

    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    fallDetectionService.updateFallEventFeedback(eventId, true, "의자에 앉는 동작");

    // then
    verify(fallEventRepository).save(argThat(event ->
        event.getFalsePositive() == true &&
        event.getStatus() == FallEvent.EventStatus.FALSE_POSITIVE &&
        "의자에 앉는 동작".equals(event.getUserFeedback())
    ));
  }

  @Test
  @DisplayName("예외 발생 시 빈 결과 반환")
  void detectFall_ExceptionHandling() {
    // given
    PoseData currentFrame = PoseData.builder()
        .id(1L)
        .user(testUser)
        .timestamp(LocalDateTime.now())
        .build();

    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenThrow(new RuntimeException("DB 오류"));

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isEmpty();
    verify(notificationService, never()).sendFallAlert(any());
  }
}