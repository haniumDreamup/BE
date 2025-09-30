package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseResponseDto;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import com.bifai.reminder.bifai_backend.service.pose.PoseDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PoseController 단순 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PoseControllerSimpleTest {

  @Mock
  private PoseDataService poseDataService;

  @Mock
  private JwtAuthUtils jwtAuthUtils;

  @InjectMocks
  private PoseController poseController;

  private PoseDataDto validPoseData;
  private List<PoseDataDto.LandmarkDto> landmarks;

  @BeforeEach
  void setUp() {
    // Mock JWT 인증
    when(jwtAuthUtils.getCurrentUserId()).thenReturn(1L);

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

    validPoseData = PoseDataDto.builder()
        .userId(1L)
        .sessionId("test-session-123")
        .timestamp(LocalDateTime.now())
        .landmarks(landmarks)
        .build();
  }

  @Test
  @DisplayName("Pose 데이터 수신 - 낙상 없음")
  void receivePoseData_NoFall() {
    // given
    PoseResponseDto response = PoseResponseDto.builder()
        .sessionId("test-session-123")
        .frameCount(1)
        .fallDetected(false)
        .message("포즈 데이터가 성공적으로 처리되었습니다")
        .build();

    when(poseDataService.processPoseData(any(PoseDataDto.class)))
        .thenReturn(response);

    // when
    ResponseEntity<ApiResponse<PoseResponseDto>> result = 
        poseController.receivePoseData(validPoseData);

    // then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().isSuccess()).isTrue();
    assertThat(result.getBody().getData().getFallDetected()).isFalse();
    
    verify(poseDataService, times(1)).processPoseData(any(PoseDataDto.class));
  }

  @Test
  @DisplayName("Pose 데이터 수신 - 낙상 감지")
  void receivePoseData_WithFall() {
    // given
    PoseResponseDto response = PoseResponseDto.builder()
        .sessionId("test-session-123")
        .frameCount(30)
        .fallDetected(true)
        .confidenceScore(0.85f)
        .severity("HIGH")
        .fallEventId(1L)
        .message("낙상이 감지되었습니다! 보호자에게 알림을 전송했습니다.")
        .build();

    when(poseDataService.processPoseData(any(PoseDataDto.class)))
        .thenReturn(response);

    // when
    ResponseEntity<ApiResponse<PoseResponseDto>> result = 
        poseController.receivePoseData(validPoseData);

    // then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getData().getFallDetected()).isTrue();
    assertThat(result.getBody().getData().getConfidenceScore()).isEqualTo(0.85f);
    assertThat(result.getBody().getData().getSeverity()).isEqualTo("HIGH");
  }

  @Test
  @DisplayName("Pose 데이터 일괄 수신")
  void receivePoseDataBatch() {
    // given
    List<PoseDataDto> batchData = Arrays.asList(validPoseData, validPoseData);
    List<PoseResponseDto> responses = Arrays.asList(
        PoseResponseDto.builder()
            .sessionId("test-session-123")
            .frameCount(1)
            .fallDetected(false)
            .build(),
        PoseResponseDto.builder()
            .sessionId("test-session-123")
            .frameCount(2)
            .fallDetected(false)
            .build()
    );

    when(poseDataService.processPoseDataBatch(anyList()))
        .thenReturn(responses);

    // when
    ResponseEntity<ApiResponse<List<PoseResponseDto>>> result = 
        poseController.receivePoseDataBatch(batchData);

    // then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getData()).hasSize(2);
    assertThat(result.getBody().getMessage()).contains("2개의 포즈 데이터");
  }

  @Test
  @DisplayName("낙상 상태 조회")
  void getFallStatus() {
    // given
    Long userId = 1L;
    FallStatusDto fallStatus = FallStatusDto.builder()
        .userId(userId)
        .lastChecked(LocalDateTime.now())
        .recentFallEvents(new ArrayList<>())
        .isMonitoring(true)
        .sessionActive(true)
        .currentSessionId("test-session-123")
        .build();

    when(poseDataService.getFallStatus(userId))
        .thenReturn(fallStatus);

    // when
    ResponseEntity<ApiResponse<FallStatusDto>> result = 
        poseController.getFallStatus(userId);

    // then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getData().getUserId()).isEqualTo(userId);
    assertThat(result.getBody().getData().getIsMonitoring()).isTrue();
  }

  @Test
  @DisplayName("낙상 피드백 제출")
  void submitFallFeedback() {
    // given
    Long eventId = 1L;
    Map<String, Object> feedback = new HashMap<>();
    feedback.put("isFalsePositive", true);
    feedback.put("userComment", "의자에 앉는 동작이었습니다");

    doNothing().when(poseDataService)
        .submitFallFeedback(eq(eventId), eq(true), anyString());

    // when
    ResponseEntity<ApiResponse<Void>> result = 
        poseController.submitFallFeedback(eventId, feedback);

    // then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getMessage()).contains("피드백이 성공적으로 제출");
    
    verify(poseDataService, times(1))
        .submitFallFeedback(eq(eventId), eq(true), anyString());
  }

  @Test
  @DisplayName("Pose 데이터 처리 중 예외 발생")
  void receivePoseData_Exception() {
    // given
    when(poseDataService.processPoseData(any(PoseDataDto.class)))
        .thenThrow(new RuntimeException("처리 중 오류"));

    // when
    ResponseEntity<ApiResponse<PoseResponseDto>> result = 
        poseController.receivePoseData(validPoseData);

    // then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().isSuccess()).isFalse();
    assertThat(result.getBody().getError()).isNotNull();
    assertThat(result.getBody().getError().getMessage()).contains("처리에 실패");
  }
}