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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 개선된 FallDetectionService 통합 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("낙상 감지 서비스 개선 기능 테스트")
class FallDetectionServiceEnhancedTest {

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
        .username("홍길동")
        .email("test@example.com")
        .build();

    testSession = PoseSession.builder()
        .id(1L)
        .sessionId("test-session-123")
        .user(testUser)
        .startTime(LocalDateTime.now())
        .status(PoseSession.SessionStatus.ACTIVE)
        .build();
  }

  @Test
  @DisplayName("실제 낙상 시나리오 - 급격한 하강 + 수평 자세 + 움직임 없음")
  void detectFall_RealFallScenario() throws Exception {
    // given
    List<PoseData> recentFrames = createFallSequence();
    PoseData currentFrame = createFallenFrame();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(recentFrames);
    
    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> {
          FallEvent event = invocation.getArgument(0);
          event.setId(1L);
          return event;
        });
    
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(anyLong(), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isPresent();
    FallEvent fallEvent = result.get();
    assertThat(fallEvent.getSeverity()).isIn(FallEvent.FallSeverity.HIGH, FallEvent.FallSeverity.CRITICAL);
    assertThat(fallEvent.getConfidenceScore()).isGreaterThanOrEqualTo(0.7f);
    
    // WebSocket 알림 검증
    verify(webSocketService).broadcastFallAlert(
        eq(testUser.getUserId()),
        anyString(),
        anyString(),
        anyDouble()
    );
    
    // 알림 서비스 호출 검증
    verify(notificationService).sendFallAlert(any(FallEvent.class));
  }

  @Test
  @DisplayName("오탐지 필터링 - 의자에 앉기 동작")
  void detectFall_FalsePositive_SittingPattern() throws Exception {
    // given
    List<PoseData> sittingSequence = createSittingSequence();
    PoseData currentFrame = createSittingFrame();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(sittingSequence);
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isEmpty();
    verify(webSocketService, never()).broadcastFallAlert(anyLong(), anyString(), anyString(), anyDouble());
    verify(notificationService, never()).sendFallAlert(any());
  }

  @Test
  @DisplayName("오탐지 필터링 - 운동(스쿼트) 동작")
  void detectFall_FalsePositive_ExercisePattern() throws Exception {
    // given
    List<PoseData> exerciseSequence = createExerciseSequence();
    PoseData currentFrame = exerciseSequence.get(0);
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(exerciseSequence);
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isEmpty();
    verify(notificationService, never()).sendFallAlert(any());
  }

  @Test
  @DisplayName("중복 낙상 감지 방지")
  void detectFall_PreventDuplicate() throws Exception {
    // given
    List<PoseData> fallSequence = createFallSequence();
    PoseData currentFrame = createFallenFrame();
    
    // 최근 낙상 이벤트가 이미 존재
    FallEvent recentFall = FallEvent.builder()
        .id(1L)
        .user(testUser)
        .detectedAt(LocalDateTime.now().minusSeconds(10))
        .build();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(fallSequence);
    
    // 중복 감지에서 사용되는 메서드만 stubbing
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(
        anyLong(), any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(recentFall));
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isEmpty();
    verify(fallEventRepository, never()).save(any());
    verify(webSocketService, never()).broadcastFallAlert(anyLong(), anyString(), anyString(), anyDouble());
  }

  @Test
  @DisplayName("낮은 포즈 신뢰도 - 낙상 감지 안함")
  void detectFall_LowPoseConfidence() throws Exception {
    // given
    List<PoseData> frames = createNormalSequence();
    PoseData lowConfidenceFrame = createLowConfidenceFrame();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(frames);
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(lowConfidenceFrame);
    
    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("WebSocket 알림 내용 검증 - 전방 낙상")
  void verifyWebSocketBroadcast_ForwardFall() throws Exception {
    // given
    List<PoseData> fallSequence = createFallSequence();
    PoseData currentFrame = createForwardFallFrame();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(fallSequence);
    
    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> {
          FallEvent event = invocation.getArgument(0);
          event.setId(1L);
          event.setBodyAngle(85.0f); // 전방 낙상 각도
          return event;
        });
    
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(anyLong(), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isPresent();
    
    ArgumentCaptor<String> fallTypeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> severityCaptor = ArgumentCaptor.forClass(String.class);
    
    verify(webSocketService).broadcastFallAlert(
        eq(testUser.getUserId()),
        fallTypeCaptor.capture(),
        severityCaptor.capture(),
        anyDouble()
    );
    
    assertThat(fallTypeCaptor.getValue()).isEqualTo("전방 낙상");
    assertThat(severityCaptor.getValue()).isIn("심각", "위급");
  }

  @Test
  @DisplayName("패턴 기반 낙상 감지 - 높이 감소 → 급격한 하강 → 정지")
  void detectFall_PatternBased() throws Exception {
    // given
    List<PoseData> patternSequence = createPatternBasedFallSequence();
    // 현재 프레임은 바닥에 있는 상태 (가장 최신)
    PoseData currentFrame = PoseData.builder()
        .id(300L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.85f)
        .overallConfidence(0.9f)
        .landmarksJson(createLandmarksJson(0.85f))
        .build();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(patternSequence);
    
    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> {
          FallEvent event = invocation.getArgument(0);
          event.setId(1L);
          return event;
        });
    
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(anyLong(), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isPresent();
    assertThat(result.get().getConfidenceScore()).isGreaterThan(0.75f);
  }

  @Test
  @DisplayName("급격한 각도 변화 감지")
  void detectFall_SuddenAngleChange() throws Exception {
    // given
    List<PoseData> angleChangeSequence = createAngleChangeSequence();
    // 현재 프레임은 수평 자세 (가장 최신)
    PoseData currentFrame = PoseData.builder()
        .id(301L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.8f)
        .overallConfidence(0.9f)
        .landmarksJson(createHorizontalLandmarksJson())
        .build();
    
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(angleChangeSequence);
    
    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> {
          FallEvent event = invocation.getArgument(0);
          event.setId(1L);
          return event;
        });
    
    when(fallEventRepository.findByUserIdAndDetectedAtAfter(anyLong(), any(LocalDateTime.class)))
        .thenReturn(Collections.emptyList());
    
    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);
    
    // then
    assertThat(result).isPresent();
  }

  // 테스트 데이터 생성 헬퍼 메서드들
  
  private List<PoseData> createFallSequence() throws Exception {
    List<PoseData> frames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(5);
    
    // 정상 상태 → 하강 → 바닥
    // 시간순으로 정렬 (최신이 앞에)
    for (int i = 0; i < 150; i++) {
      float centerY;
      
      if (i < 50) {
        centerY = 0.3f; // 정상 높이
      } else if (i < 100) {
        // 점진적 하강
        float progress = (i - 50) / 50.0f;
        centerY = 0.3f + (0.55f * progress); 
      } else {
        centerY = 0.85f; // 바닥
      }
      
      PoseData frame = PoseData.builder()
          .id((long)(149 - i))
          .user(testUser)
          .poseSession(testSession)
          .timestamp(baseTime.minusNanos((149L - i) * 33_333_333L)) // 최신이 baseTime에 가까움
          .centerY(centerY)
          .overallConfidence(0.9f)
          .landmarksJson(createLandmarksJson(centerY))
          .build();
      
      frames.add(frame);
    }
    
    return frames;
  }

  private PoseData createFallenFrame() throws Exception {
    return PoseData.builder()
        .id(200L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.85f)
        .overallConfidence(0.85f)
        .landmarksJson(createHorizontalLandmarksJson())
        .build();
  }

  private PoseData createForwardFallFrame() throws Exception {
    String landmarksJson = createForwardFallLandmarksJson();
    return PoseData.builder()
        .id(201L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.85f)
        .overallConfidence(0.9f)
        .landmarksJson(landmarksJson)
        .build();
  }

  private List<PoseData> createSittingSequence() throws Exception {
    List<PoseData> frames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(2);
    
    // 천천히 앉기 - 시간순으로 정렬
    for (int i = 0; i < 60; i++) {
      // 천천히 하강: 0.3f → 0.55f
      float centerY = 0.3f + (i * 0.25f / 60); 
      
      PoseData frame = PoseData.builder()
          .id((long)(59 - i))
          .user(testUser)
          .poseSession(testSession)
          .timestamp(baseTime.minusNanos((59L - i) * 33_333_333L))
          .centerY(centerY)
          .overallConfidence(0.9f)
          .landmarksJson(createLandmarksJson(centerY))
          .build();
      
      frames.add(frame);
    }
    
    return frames;
  }

  private PoseData createSittingFrame() throws Exception {
    return PoseData.builder()
        .id(100L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.55f) // 중간 높이
        .overallConfidence(0.9f)
        .landmarksJson(createSittingLandmarksJson())
        .build();
  }

  private List<PoseData> createExerciseSequence() throws Exception {
    List<PoseData> frames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(3);
    
    // 스쿼트 동작 - 상하 반복 - 시간순으로 정렬
    for (int i = 0; i < 90; i++) {
      float centerY;
      int cycle = i / 15; // 15프레임마다 사이클
      if (cycle % 2 == 0) {
        centerY = 0.3f + (i % 15) * 0.02f; // 하강
      } else {
        centerY = 0.6f - (i % 15) * 0.02f; // 상승
      }
      
      PoseData frame = PoseData.builder()
          .id((long)(89 - i))
          .user(testUser)
          .poseSession(testSession)
          .timestamp(baseTime.minusNanos((89L - i) * 33_333_333L))
          .centerY(centerY)
          .overallConfidence(0.9f)
          .landmarksJson(createLandmarksJson(centerY))
          .build();
      
      frames.add(frame);
    }
    
    return frames;
  }

  private List<PoseData> createNormalSequence() throws Exception {
    List<PoseData> frames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(2);
    
    for (int i = 59; i >= 0; i--) {
      PoseData frame = PoseData.builder()
          .id((long) i)
          .user(testUser)
          .poseSession(testSession)
          .timestamp(baseTime.plusNanos(i * 33_333_333L)) // 30fps
          .centerY(0.3f)
          .overallConfidence(0.9f)
          .landmarksJson(createLandmarksJson(0.3f))
          .build();
      
      frames.add(frame);
    }
    
    return frames;
  }

  private PoseData createLowConfidenceFrame() throws Exception {
    return PoseData.builder()
        .id(300L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(LocalDateTime.now())
        .centerY(0.5f)
        .overallConfidence(0.3f) // 낮은 신뢰도
        .landmarksJson(createLandmarksJson(0.5f))
        .build();
  }

  private List<PoseData> createPatternBasedFallSequence() throws Exception {
    List<PoseData> frames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(1);
    
    // 패턴: 천천히 하강 → 급격한 하강 → 정지
    for (int i = 0; i < 30; i++) {
      float centerY;
      if (i < 10) {
        centerY = 0.3f + i * 0.02f; // 천천히 하강
      } else if (i < 20) {
        centerY = 0.5f + (i - 10) * 0.035f; // 급격한 하강
      } else {
        centerY = 0.85f; // 바닥에 정지
      }
      
      PoseData frame = PoseData.builder()
          .id((long)(29 - i))
          .user(testUser)
          .poseSession(testSession)
          .timestamp(baseTime.minusNanos((29L - i) * 33_333_333L))
          .centerY(centerY)
          .overallConfidence(0.9f)
          .landmarksJson(createLandmarksJson(centerY))
          .build();
      
      frames.add(frame);
    }
    
    return frames;
  }

  private List<PoseData> createAngleChangeSequence() throws Exception {
    List<PoseData> frames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(1);
    
    // 정상 자세에서 급격한 각도 변화
    for (int i = 0; i < 30; i++) {
      String landmarksJson;
      
      if (i < 10) {
        landmarksJson = createVerticalLandmarksJson(); // 수직 자세
      } else if (i < 15) {
        landmarksJson = createVerticalLandmarksJson(); // 전환 구간
      } else {
        landmarksJson = createHorizontalLandmarksJson(); // 수평 자세
      }
      
      PoseData frame = PoseData.builder()
          .id((long)(29 - i))
          .user(testUser)
          .poseSession(testSession)
          .timestamp(baseTime.minusNanos((29L - i) * 33_333_333L))
          .centerY(i < 15 ? 0.3f : 0.8f)
          .overallConfidence(0.9f)
          .landmarksJson(landmarksJson)
          .build();
      
      frames.add(frame);
    }
    
    return frames;
  }

  private String createLandmarksJson(float centerY) throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    for (int i = 0; i < 33; i++) {
      PoseDataDto.LandmarkType type = PoseDataDto.LandmarkType.values()[i];
      float y = centerY + (i % 2 == 0 ? 0.1f : -0.1f);
      
      landmarks.add(PoseDataDto.LandmarkDto.builder()
          .type(type)
          .x(0.5f)
          .y(y)
          .z(0.0f)
          .visibility(0.95f)
          .build());
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }

  private String createHorizontalLandmarksJson() throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    // 수평 자세 시뮬레이션
    for (int i = 0; i < 33; i++) {
      PoseDataDto.LandmarkType type = PoseDataDto.LandmarkType.values()[i];
      float y = (float)(0.85f + (Math.random() * 0.1 - 0.05));
      float x = (float)(0.2f + (i / 33.0) * 0.6);
      
      landmarks.add(PoseDataDto.LandmarkDto.builder()
          .type(type)
          .x(x)
          .y(y)
          .z(0.0f)
          .visibility(0.9f)
          .build());
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }

  private String createForwardFallLandmarksJson() throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    // 전방 낙상 자세 - 머리가 앞쪽
    landmarks.add(PoseDataDto.LandmarkDto.builder() // NOSE
        .type(PoseDataDto.LandmarkType.NOSE)
        .x(0.7f)
        .y(0.85f)
        .z(0.0f)
        .visibility(0.9f)
        .build());
    
    // 어깨
    landmarks.add(PoseDataDto.LandmarkDto.builder() // LEFT_EYE_INNER
        .type(PoseDataDto.LandmarkType.LEFT_EYE_INNER)
        .x(0.68f)
        .y(0.84f)
        .z(0.0f)
        .visibility(0.9f)
        .build());
    
    // 나머지 랜드마크들 추가
    for (int i = 2; i < 33; i++) {
      PoseDataDto.LandmarkType type = PoseDataDto.LandmarkType.values()[i];
      float x = 0.5f - (i / 66.0f);
      float y = 0.85f;
      
      if (i == 11 || i == 12) { // 어깨
        x = 0.6f;
        y = 0.83f;
      } else if (i == 23 || i == 24) { // 엉덩이
        x = 0.4f;
        y = 0.82f;
      } else if (i == 27 || i == 28) { // 발목
        x = 0.2f;
        y = 0.85f;
      }
      
      landmarks.add(PoseDataDto.LandmarkDto.builder()
          .type(type)
          .x(x)
          .y(y)
          .z(0.0f)
          .visibility(0.85f)
          .build());
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }

  private String createSittingLandmarksJson() throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    // 앉은 자세 시뮬레이션
    for (int i = 0; i < 33; i++) {
      PoseDataDto.LandmarkType type = PoseDataDto.LandmarkType.values()[i];
      float x = 0.5f;
      float y;
      
      if (i == 0) { // 머리
        y = 0.35f;
      } else if (i >= 11 && i <= 12) { // 어깨
        y = 0.45f;
      } else if (i >= 23 && i <= 24) { // 엉덩이
        y = 0.55f;
      } else if (i >= 27 && i <= 28) { // 발목
        y = 0.8f;
      } else {
        y = 0.5f;
      }
      
      landmarks.add(PoseDataDto.LandmarkDto.builder()
          .type(type)
          .x(x)
          .y(y)
          .z(0.0f)
          .visibility(0.9f)
          .build());
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }

  private String createVerticalLandmarksJson() throws Exception {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    
    // 수직 자세 (서있는 자세)
    for (int i = 0; i < 33; i++) {
      PoseDataDto.LandmarkType type = PoseDataDto.LandmarkType.values()[i];
      float x = 0.5f;
      float y;
      
      if (i == 0) { // 머리
        y = 0.1f;
      } else if (i >= 11 && i <= 12) { // 어깨
        y = 0.25f;
      } else if (i >= 23 && i <= 24) { // 엉덩이
        y = 0.5f;
      } else if (i >= 27 && i <= 28) { // 발목
        y = 0.9f;
      } else {
        y = 0.3f + (i / 33.0f) * 0.5f;
      }
      
      landmarks.add(PoseDataDto.LandmarkDto.builder()
          .type(type)
          .x(x)
          .y(y)
          .z(0.0f)
          .visibility(0.95f)
          .build());
    }
    
    return objectMapper.writeValueAsString(landmarks);
  }
}