package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
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
 * FallDetectionService 기본 동작 검증 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("낙상 감지 서비스 기본 테스트")
class FallDetectionServiceSimpleTest {

  @Mock
  private PoseDataRepository poseDataRepository;

  @Mock
  private FallEventRepository fallEventRepository;

  @Mock
  private NotificationService notificationService;
  
  @Mock
  private WebSocketService webSocketService;

  private FallDetectionService fallDetectionService;
  private ObjectMapper objectMapper;

  private User testUser;
  private PoseSession testSession;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    
    fallDetectionService = new FallDetectionService(
        poseDataRepository,
        fallEventRepository,
        notificationService,
        webSocketService,
        objectMapper
    );

    testUser = User.builder()
        .userId(1L)
        .username("테스트유저")
        .email("test@example.com")
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
  @DisplayName("단순 낙상 시나리오 - 바닥에 누워있는 상태")
  void simpleFallDetection() throws Exception {
    // given
    LocalDateTime now = LocalDateTime.now();
    
    // 현재 프레임 - 바닥에 누워있음
    PoseData currentFrame = PoseData.builder()
        .id(100L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(now)
        .centerY(0.85f)  // 매우 낮은 위치
        .overallConfidence(0.9f)
        .landmarksJson(createSimpleHorizontalLandmarks())
        .build();
    
    // 이전 프레임들 - 정상 → 낙상 (시간순으로 정렬)
    List<PoseData> recentFrames = new ArrayList<>();
    
    // 30프레임 전 (1초 전) - 서있던 상태
    for (int i = 0; i < 10; i++) {
      PoseData frame = PoseData.builder()
          .id((long)(29 - i))
          .user(testUser)
          .timestamp(now.minusNanos((29L - i) * 33_333_333L))
          .centerY(0.3f)  // 정상 높이
          .overallConfidence(0.9f)
          .landmarksJson(createSimpleVerticalLandmarks())
          .build();
      recentFrames.add(frame);
    }
    
    // 20프레임 전부터 낙상 시작
    for (int i = 10; i < 30; i++) {
      float progress = (i - 10) / 20.0f;
      PoseData frame = PoseData.builder()
          .id((long)(29 - i))
          .user(testUser)
          .timestamp(now.minusNanos((29L - i) * 33_333_333L))
          .centerY(0.3f + (0.55f * progress))  // 점진적으로 낮아짐
          .overallConfidence(0.9f)
          .landmarksJson(createSimpleVerticalLandmarks())
          .build();
      recentFrames.add(frame);
    }
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(recentFrames);
    
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(anyLong(), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    
    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> {
          FallEvent event = invocation.getArgument(0);
          event.setId(1L);
          return event;
        });
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isPresent();
    verify(webSocketService).broadcastFallAlert(anyLong(), anyString(), anyString(), anyDouble());
    verify(notificationService).sendFallAlert(any(FallEvent.class));
  }

  @Test
  @DisplayName("디버그용 - 낙상 감지 조건 체크")
  void debugFallConditions() throws Exception {
    // given
    LocalDateTime now = LocalDateTime.now();
    
    // 낙상 상태 프레임
    PoseData fallenFrame = PoseData.builder()
        .id(100L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(now)
        .centerY(0.8f)  // 낮은 위치
        .overallConfidence(0.85f)
        .landmarksJson(createSimpleHorizontalLandmarks())
        .build();
    
    // 간단한 이전 프레임들
    List<PoseData> frames = new ArrayList<>();
    for (int i = 35; i >= 0; i--) {
      PoseData frame = PoseData.builder()
          .id((long) i)
          .user(testUser)
          .timestamp(now.minusNanos((36L - i) * 33_333_333L))
          .centerY(i < 20 ? 0.8f : 0.3f)  // 20프레임 전부터 낮은 위치
          .overallConfidence(0.9f)
          .motionScore(i < 10 ? 0.001f : 0.1f)  // 10프레임 전부터 움직임 없음
          .landmarksJson(i < 20 ? createSimpleHorizontalLandmarks() : createSimpleVerticalLandmarks())
          .build();
      frames.add(frame);
    }
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(frames);
    
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(anyLong(), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    
    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> {
          FallEvent event = invocation.getArgument(0);
          event.setId(1L);
          // 디버그 정보 출력
          System.out.println("=== 낙상 감지됨 ===");
          System.out.println("심각도: " + event.getSeverity());
          System.out.println("신뢰도: " + event.getConfidenceScore());
          System.out.println("신체 각도: " + event.getBodyAngle());
          return event;
        });
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(fallenFrame);
    
    // then
    if (result.isEmpty()) {
      System.out.println("=== 낙상 감지 실패 ===");
      System.out.println("현재 Y 위치: " + fallenFrame.getCenterY());
      System.out.println("전체 신뢰도: " + fallenFrame.getOverallConfidence());
    }
    
    // 적어도 이 조건에서는 낙상이 감지되어야 함
    assertThat(result).isPresent();
  }

  @Test
  @DisplayName("오탐지 필터 검증 - 카메라 움직임")
  void falsePositive_CameraMovement() throws Exception {
    // given
    LocalDateTime now = LocalDateTime.now();
    
    PoseData currentFrame = PoseData.builder()
        .id(10L)
        .user(testUser)
        .timestamp(now)
        .centerY(0.5f)
        .overallConfidence(0.3f)  // 낮은 신뢰도 (카메라 움직임)
        .landmarksJson(createSimpleVerticalLandmarks())
        .build();
    
    // 신뢰도가 급격히 떨어지는 프레임들
    List<PoseData> frames = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
      PoseData frame = PoseData.builder()
          .id((long)(5 - i))
          .user(testUser)
          .timestamp(now.minusNanos((5L - i) * 33_333_333L))
          .centerY(0.5f)
          .overallConfidence(0.9f - (0.1f * i))  // 점진적으로 신뢰도 하락
          .landmarksJson(createSimpleVerticalLandmarks())
          .build();
      frames.add(frame);
    }
    
    // 충분한 프레임 추가
    for (int i = 6; i < 35; i++) {
      frames.add(PoseData.builder()
          .id((long) i)
          .user(testUser)
          .timestamp(now.minusNanos((i + 1) * 33_333_333L))
          .centerY(0.5f)
          .overallConfidence(0.9f)
          .landmarksJson(createSimpleVerticalLandmarks())
          .build());
    }
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(frames);
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isEmpty();  // 오탐지로 필터링되어야 함
    verify(notificationService, never()).sendFallAlert(any());
  }

  // 간단한 랜드마크 생성 헬퍼 메서드들
  
  private String createSimpleVerticalLandmarks() throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    // 서있는 자세의 주요 랜드마크만
    // 머리
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.NOSE, 0.5f, 0.1f));
    
    // 나머지 랜드마크들도 간단하게 추가
    for (int i = 1; i < 33; i++) {
      float y = 0.1f + (i / 33.0f) * 0.8f;  // 위에서 아래로
      landmarks.add(createLandmark(PoseDataDto.LandmarkType.values()[i], 0.5f, y));
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }
  
  private String createSimpleHorizontalLandmarks() throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    // 누워있는 자세 - 모든 랜드마크가 비슷한 높이
    for (int i = 0; i < 33; i++) {
      float x = 0.2f + (i / 33.0f) * 0.6f;  // 왼쪽에서 오른쪽으로
      float y = 0.85f + ((float)Math.random() * 0.05f - 0.025f);  // 약간의 변화
      landmarks.add(createLandmark(PoseDataDto.LandmarkType.values()[i], x, y));
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }
  
  private PoseDataDto.LandmarkDto createLandmark(PoseDataDto.LandmarkType type, float x, float y) {
    return PoseDataDto.LandmarkDto.builder()
        .type(type)
        .x(x)
        .y(y)
        .z(0.0f)
        .visibility(0.9f)
        .build();
  }
}