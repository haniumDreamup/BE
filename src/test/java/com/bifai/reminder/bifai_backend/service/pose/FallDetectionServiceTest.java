package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.FallEventRepository;
import com.bifai.reminder.bifai_backend.repository.PoseDataRepository;
import com.bifai.reminder.bifai_backend.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FallDetectionService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class FallDetectionServiceTest {

  @Mock
  private PoseDataRepository poseDataRepository;

  @Mock
  private FallEventRepository fallEventRepository;

  @Mock
  private NotificationService notificationService;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private FallDetectionService fallDetectionService;

  private User testUser;
  private PoseSession testSession;
  private PoseData currentFrame;
  private List<PoseData> recentFrames;
  private List<PoseDataDto.LandmarkDto> normalLandmarks;
  private List<PoseDataDto.LandmarkDto> fallenLandmarks;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .build();

    // 테스트 세션 설정
    testSession = PoseSession.builder()
        .id(1L)
        .sessionId("test-session")
        .user(testUser)
        .startTime(LocalDateTime.now().minusMinutes(5))
        .status(PoseSession.SessionStatus.ACTIVE)
        .build();

    // 정상 자세 랜드마크 (서있는 자세)
    normalLandmarks = createNormalStandingPose();

    // 낙상 자세 랜드마크 (누운 자세)
    fallenLandmarks = createFallenPose();

    // 현재 프레임 설정
    currentFrame = createPoseData(testUser, testSession, LocalDateTime.now(), 0.5f, false);
    currentFrame.setId(100L); // ID 설정 추가

    // 최근 프레임들 설정 (5초간의 데이터)
    recentFrames = new ArrayList<>();
    LocalDateTime baseTime = LocalDateTime.now().minusSeconds(5);
    for (int i = 0; i < 150; i++) { // 30fps * 5초
      recentFrames.add(createPoseData(
          testUser, 
          testSession, 
          baseTime.plusNanos(i * 33000000L), // 33ms 간격 (나노초로 변환)
          0.3f, // 높은 위치
          false
      ));
    }
  }

  @Test
  @DisplayName("정상적인 낙상 감지 - 급격한 하강 + 수평 자세")
  void detectFall_Success() throws Exception {
    // given
    // 현재 프레임은 바닥에 누운 상태
    currentFrame.setCenterY(0.85f); // 낮은 위치
    currentFrame.setIsHorizontal(true);
    currentFrame.setVelocityY(0.2f); // 빠른 하강 속도 설정
    currentFrame.setMotionScore(0.005f); // 움직임 거의 없음

    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(recentFrames);

    when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(fallenLandmarks);

    FallEvent savedEvent = FallEvent.builder()
        .id(1L)
        .user(testUser)
        .poseSession(testSession)
        .detectedAt(LocalDateTime.now())
        .severity(FallEvent.FallSeverity.HIGH)
        .confidenceScore(0.8f)
        .status(FallEvent.EventStatus.DETECTED)
        .build();

    when(fallEventRepository.save(any(FallEvent.class)))
        .thenReturn(savedEvent);

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getSeverity()).isEqualTo(FallEvent.FallSeverity.HIGH);
    assertThat(result.get().getConfidenceScore()).isGreaterThanOrEqualTo(0.7f);

    verify(notificationService, times(1)).sendFallAlert(any(FallEvent.class));
    verify(fallEventRepository, times(1)).save(any(FallEvent.class)); // 생성만 (알림 실패로 업데이트 안됨)
  }

  @Test
  @DisplayName("낙상 미감지 - 정상적인 서있는 자세")
  void detectFall_NoFall_StandingPosition() throws Exception {
    // given
    currentFrame.setCenterY(0.4f); // 정상 높이
    currentFrame.setIsHorizontal(false);

    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(recentFrames);

    when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(normalLandmarks);

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isEmpty();
    verify(notificationService, never()).sendFallAlert(any());
    verify(fallEventRepository, never()).save(any());
  }

  @Test
  @DisplayName("낙상 미감지 - 데이터 부족")
  void detectFall_InsufficientData() {
    // given - 1초 미만의 데이터만 제공
    List<PoseData> insufficientFrames = recentFrames.subList(0, 20); // 20 프레임만

    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(insufficientFrames);

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isEmpty();
    verify(fallEventRepository, never()).save(any());
  }

  @Test
  @DisplayName("낙상 감지 후 움직임 없음 - 심각도 HIGH")
  void detectFall_NoMotionAfterFall() throws Exception {
    // given
    currentFrame.setCenterY(0.85f);
    currentFrame.setIsHorizontal(true);

    // 최근 3초간 움직임이 거의 없는 프레임들
    List<PoseData> noMotionFrames = new ArrayList<>();
    for (int i = 0; i < 90; i++) { // 3초간
      PoseData frame = createPoseData(
          testUser,
          testSession,
          LocalDateTime.now().minusNanos(i * 33000000L),
          0.85f, // 같은 낮은 위치 유지
          true
      );
      noMotionFrames.add(frame);
    }

    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(noMotionFrames);

    when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(fallenLandmarks);

    FallEvent savedEvent = FallEvent.builder()
        .id(1L)
        .user(testUser)
        .severity(FallEvent.FallSeverity.HIGH)
        .build();

    when(fallEventRepository.save(any(FallEvent.class)))
        .thenReturn(savedEvent);

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getSeverity()).isIn(
        FallEvent.FallSeverity.HIGH,
        FallEvent.FallSeverity.CRITICAL
    );
  }

  @Test
  @DisplayName("최근 낙상 이벤트 조회")
  void getRecentFallEvents() {
    // given
    Long userId = 1L;
    int hours = 24;
    List<FallEvent> expectedEvents = Arrays.asList(
        FallEvent.builder().id(1L).build(),
        FallEvent.builder().id(2L).build()
    );

    when(fallEventRepository.findByUserIdAndDetectedAtBetween(
        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(expectedEvents);

    // when
    List<FallEvent> result = fallDetectionService.getRecentFallEvents(userId, hours);

    // then
    assertThat(result).hasSize(2);
    verify(fallEventRepository, times(1)).findByUserIdAndDetectedAtBetween(
        eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("낙상 이벤트 피드백 업데이트 - 오탐지")
  void updateFallEventFeedback_FalsePositive() {
    // given
    Long eventId = 1L;
    FallEvent event = FallEvent.builder()
        .id(eventId)
        .status(FallEvent.EventStatus.NOTIFIED)
        .build();

    when(fallEventRepository.findById(eventId))
        .thenReturn(Optional.of(event));

    when(fallEventRepository.save(any(FallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    fallDetectionService.updateFallEventFeedback(eventId, true, "의자에 앉는 동작이었습니다");

    // then
    verify(fallEventRepository, times(1)).save(argThat(savedEvent -> 
        savedEvent.getFalsePositive() == true &&
        savedEvent.getStatus() == FallEvent.EventStatus.FALSE_POSITIVE &&
        savedEvent.getUserFeedback().equals("의자에 앉는 동작이었습니다")
    ));
  }

  @Test
  @DisplayName("낙상 감지 중 예외 발생 시 빈 결과 반환")
  void detectFall_ExceptionHandling() {
    // given
    when(poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenThrow(new RuntimeException("DB 연결 오류"));

    // when
    Optional<FallEvent> result = fallDetectionService.detectFall(currentFrame);

    // then
    assertThat(result).isEmpty();
    verify(notificationService, never()).sendFallAlert(any());
  }

  // 헬퍼 메서드들
  private PoseData createPoseData(User user, PoseSession session, 
                                  LocalDateTime timestamp, float centerY, boolean isHorizontal) {
    return PoseData.builder()
        .user(user)
        .poseSession(session)
        .timestamp(timestamp)
        .centerY(centerY)
        .isHorizontal(isHorizontal)
        .velocityY(0.0f)
        .motionScore(0.0f)
        .landmarksJson("[]")
        .overallConfidence(0.9f) // 신뢰도 추가
        .build();
  }

  private List<PoseDataDto.LandmarkDto> createNormalStandingPose() {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    // 서있는 자세: 머리가 위, 발이 아래
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.NOSE, 0.5f, 0.2f)); // 머리
    
    // 어깨
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.LEFT_SHOULDER, 0.4f, 0.35f));
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.RIGHT_SHOULDER, 0.6f, 0.35f));
    
    // 엉덩이
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.LEFT_HIP, 0.45f, 0.6f));
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.RIGHT_HIP, 0.55f, 0.6f));
    
    // 발목
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.LEFT_ANKLE, 0.45f, 0.9f));
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.RIGHT_ANKLE, 0.55f, 0.9f));
    
    // 나머지 랜드마크들 채우기
    for (int i = landmarks.size(); i < 33; i++) {
      landmarks.add(createLandmark(PoseDataDto.LandmarkType.values()[i], 0.5f, 0.5f));
    }
    
    return landmarks;
  }

  private List<PoseDataDto.LandmarkDto> createFallenPose() {
    List<PoseDataDto.LandmarkDto> landmarks = new ArrayList<>();
    // 누운 자세: 모든 점들이 비슷한 Y 좌표에 위치
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.NOSE, 0.2f, 0.8f)); // 머리
    
    // 어깨 (수평)
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.LEFT_SHOULDER, 0.3f, 0.82f));
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.RIGHT_SHOULDER, 0.3f, 0.78f));
    
    // 엉덩이 (수평)
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.LEFT_HIP, 0.5f, 0.82f));
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.RIGHT_HIP, 0.5f, 0.78f));
    
    // 발목 (수평)
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.LEFT_ANKLE, 0.7f, 0.82f));
    landmarks.add(createLandmark(PoseDataDto.LandmarkType.RIGHT_ANKLE, 0.7f, 0.78f));
    
    // 나머지 랜드마크들 채우기
    for (int i = landmarks.size(); i < 33; i++) {
      landmarks.add(createLandmark(PoseDataDto.LandmarkType.values()[i], 0.5f, 0.8f));
    }
    
    return landmarks;
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