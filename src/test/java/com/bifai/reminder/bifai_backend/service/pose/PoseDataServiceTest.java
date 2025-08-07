package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseResponseDto;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.PoseDataRepository;
import com.bifai.reminder.bifai_backend.repository.PoseSessionRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PoseDataService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PoseDataServiceTest {

  @Mock
  private PoseDataRepository poseDataRepository;

  @Mock
  private PoseSessionRepository poseSessionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FallDetectionService fallDetectionService;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ListOperations<String, Object> listOperations;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private PoseDataService poseDataService;

  private User testUser;
  private PoseSession testSession;
  private PoseDataDto validPoseData;
  private List<PoseDataDto.LandmarkDto> landmarks;

  @BeforeEach
  void setUp() {
    // 테스트 사용자
    testUser = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .build();

    // 테스트 세션
    testSession = PoseSession.builder()
        .id(1L)
        .sessionId("test-session-123")
        .user(testUser)
        .startTime(LocalDateTime.now())
        .status(PoseSession.SessionStatus.ACTIVE)
        .totalFrames(10)
        .build();

    // 33개의 유효한 랜드마크 생성
    landmarks = new ArrayList<>();
    for (int i = 0; i < 33; i++) {
      landmarks.add(PoseDataDto.LandmarkDto.builder()
          .type(PoseDataDto.LandmarkType.values()[i])
          .x(0.5f)
          .y(0.5f)
          .z(0.0f)
          .visibility(0.9f)
          .build());
    }

    // 유효한 PoseDataDto
    validPoseData = PoseDataDto.builder()
        .userId(1L)
        .sessionId("test-session-123")
        .timestamp(LocalDateTime.now())
        .landmarks(landmarks)
        .build();

    // RedisTemplate 설정은 각 테스트에서 필요할 때만 설정
  }

  @Test
  @DisplayName("단일 Pose 데이터 처리 - 낙상 없음")
  void processPoseData_NoFall() throws Exception {
    // given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(poseSessionRepository.findBySessionId("test-session-123"))
        .thenReturn(Optional.of(testSession));

    PoseData savedPoseData = PoseData.builder()
        .id(1L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(validPoseData.getTimestamp())
        .centerY(0.5f)
        .build();

    when(poseDataRepository.save(any(PoseData.class)))
        .thenReturn(savedPoseData);

    when(objectMapper.writeValueAsString(any()))
        .thenReturn("[]");

    when(fallDetectionService.detectFall(any(PoseData.class)))
        .thenReturn(Optional.empty());

    // when
    PoseResponseDto response = poseDataService.processPoseData(validPoseData);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getSessionId()).isEqualTo("test-session-123");
    assertThat(response.getFallDetected()).isFalse();
    assertThat(response.getFrameCount()).isEqualTo(10); // 현재 프레임 수
    assertThat(response.getMessage()).contains("성공적으로 처리");

    verify(poseDataRepository, times(1)).save(any(PoseData.class));
    verify(fallDetectionService, times(1)).detectFall(any(PoseData.class));
  }

  @Test
  @DisplayName("단일 Pose 데이터 처리 - 낙상 감지")
  void processPoseData_WithFall() throws Exception {
    // given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(poseSessionRepository.findBySessionId("test-session-123"))
        .thenReturn(Optional.of(testSession));

    PoseData savedPoseData = PoseData.builder()
        .id(1L)
        .user(testUser)
        .poseSession(testSession)
        .timestamp(validPoseData.getTimestamp())
        .centerY(0.85f)
        .isHorizontal(true)
        .build();

    when(poseDataRepository.save(any(PoseData.class)))
        .thenReturn(savedPoseData);

    when(objectMapper.writeValueAsString(any()))
        .thenReturn("[]");

    FallEvent detectedFall = FallEvent.builder()
        .id(1L)
        .user(testUser)
        .severity(FallEvent.FallSeverity.HIGH)
        .confidenceScore(0.85f)
        .build();

    when(fallDetectionService.detectFall(any(PoseData.class)))
        .thenReturn(Optional.of(detectedFall));

    // when
    PoseResponseDto response = poseDataService.processPoseData(validPoseData);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getFallDetected()).isTrue();
    assertThat(response.getFallEventId()).isEqualTo(1L);
    assertThat(response.getConfidenceScore()).isEqualTo(0.85f);
    assertThat(response.getSeverity()).isEqualTo("HIGH");
    assertThat(response.getMessage()).contains("낙상이 감지");
  }

  @Test
  @DisplayName("새 세션 생성")
  void processPoseData_CreateNewSession() throws Exception {
    // given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    validPoseData.setSessionId(null); // 세션 ID 없음

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(testUser));

    PoseSession newSession = PoseSession.builder()
        .id(2L)
        .sessionId("generated-session-id") // 실제 값으로 변경
        .user(testUser)
        .startTime(LocalDateTime.now())
        .status(PoseSession.SessionStatus.ACTIVE)
        .totalFrames(0)
        .build();

    when(poseSessionRepository.findBySessionId(anyString()))
        .thenReturn(Optional.empty());

    when(poseSessionRepository.save(any(PoseSession.class)))
        .thenReturn(newSession);

    when(poseDataRepository.save(any(PoseData.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(objectMapper.writeValueAsString(any()))
        .thenReturn("[]");

    when(fallDetectionService.detectFall(any()))
        .thenReturn(Optional.empty());

    // when
    PoseResponseDto response = poseDataService.processPoseData(validPoseData);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getSessionId()).isNotNull();
    
    verify(userRepository, times(1)).findById(1L);
    verify(poseSessionRepository, times(1)).save(any(PoseSession.class));
  }

  @Test
  @DisplayName("배치 Pose 데이터 처리")
  void processPoseDataBatch_Success() throws Exception {
    // given
    List<PoseDataDto> batchData = Arrays.asList(validPoseData, validPoseData);

    when(poseSessionRepository.findBySessionId("test-session-123"))
        .thenReturn(Optional.of(testSession));

    when(poseDataRepository.save(any(PoseData.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(objectMapper.writeValueAsString(any()))
        .thenReturn("[]");

    // 마지막 프레임에서만 낙상 감지
    when(fallDetectionService.detectFall(any(PoseData.class)))
        .thenReturn(Optional.empty());

    // when
    List<PoseResponseDto> responses = poseDataService.processPoseDataBatch(batchData);

    // then
    assertThat(responses).hasSize(2);
    assertThat(responses.get(0).getFrameCount()).isEqualTo(11); // totalFrames(10) + i(0) + 1
    assertThat(responses.get(1).getFrameCount()).isEqualTo(12); // totalFrames(10) + i(1) + 1

    verify(poseDataRepository, times(2)).save(any(PoseData.class));
    verify(fallDetectionService, times(1)).detectFall(any(PoseData.class)); // 마지막 프레임만
    verify(poseSessionRepository, times(1)).save(any(PoseSession.class)); // 세션 업데이트
  }

  @Test
  @DisplayName("낙상 상태 조회")
  void getFallStatus_Success() {
    // given
    Long userId = 1L;

    when(poseSessionRepository.findByUserIdAndStatus(userId, PoseSession.SessionStatus.ACTIVE))
        .thenReturn(Optional.of(testSession));

    List<FallEvent> recentEvents = Arrays.asList(
        FallEvent.builder()
            .id(1L)
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
    assertThat(status.getRecentFallEvents().get(0).getEventId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("낙상 피드백 제출")
  void submitFallFeedback_Success() {
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
  @DisplayName("Redis 버퍼 추가 - 실패 시 로그만 기록")
  void addToBuffer_RedisFailure() throws Exception {
    // given
    when(poseSessionRepository.findBySessionId("test-session-123"))
        .thenReturn(Optional.of(testSession));

    when(poseDataRepository.save(any(PoseData.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    when(objectMapper.writeValueAsString(any()))
        .thenReturn("[]");

    // Redis 작업 설정
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(listOperations.rightPush(anyString(), any()))
        .thenThrow(new RuntimeException("Redis connection failed"));

    when(fallDetectionService.detectFall(any()))
        .thenReturn(Optional.empty());

    // when & then - Redis 실패해도 처리는 계속됨
    assertThatCode(() -> poseDataService.processPoseData(validPoseData))
        .doesNotThrowAnyException();

    verify(listOperations, times(1)).rightPush(anyString(), any());
  }

  @Test
  @DisplayName("빈 배치 데이터 처리")
  void processPoseDataBatch_Empty() {
    // given
    List<PoseDataDto> emptyList = new ArrayList<>();

    // when
    List<PoseResponseDto> responses = poseDataService.processPoseDataBatch(emptyList);

    // then
    assertThat(responses).isEmpty();
    
    verify(poseDataRepository, never()).save(any());
    verify(fallDetectionService, never()).detectFall(any());
  }
}