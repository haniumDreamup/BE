package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseResponseDto;
import com.bifai.reminder.bifai_backend.service.pose.PoseDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

/**
 * PoseController 단독 테스트
 * MockMvc를 standalone 모드로 설정하여 의존성 문제 해결
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PoseController 단독 테스트")
class PoseControllerStandaloneTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private PoseDataService poseDataService;

  @InjectMocks
  private PoseController poseController;

  private PoseDataDto validPoseData;
  private List<PoseDataDto.LandmarkDto> landmarks;

  @BeforeEach
  void setUp() {
    // MockMvc를 standalone 모드로 설정
    mockMvc = MockMvcBuilders.standaloneSetup(poseController)
        .build();
    
    // Jackson ObjectMapper 설정
    objectMapper.findAndRegisterModules();

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
  @DisplayName("POST /api/v1/pose/data - 유효한 Pose 데이터 수신 성공")
  void receivePoseData_Success() throws Exception {
    // given
    PoseResponseDto response = PoseResponseDto.builder()
        .sessionId("test-session-123")
        .frameCount(1)
        .fallDetected(false)
        .confidenceScore(0.0f)
        .message("포즈 데이터가 성공적으로 처리되었습니다")
        .build();

    given(poseDataService.processPoseData(any(PoseDataDto.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/v1/pose/data")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPoseData)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value("test-session-123"))
        .andExpect(jsonPath("$.data.frameCount").value(1))
        .andExpect(jsonPath("$.data.fallDetected").value(false));

    verify(poseDataService, times(1)).processPoseData(any(PoseDataDto.class));
  }

  @Test
  @DisplayName("POST /api/v1/pose/data - 랜드마크 개수 부족으로 실패")
  void receivePoseData_InvalidLandmarkCount() throws Exception {
    // given - 32개만 설정 (33개 필요)
    validPoseData.getLandmarks().remove(0);

    // when & then
    mockMvc.perform(post("/api/v1/pose/data")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPoseData)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/v1/pose/data/batch - 배치 Pose 데이터 수신 성공")
  void receivePoseDataBatch_Success() throws Exception {
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

    given(poseDataService.processPoseDataBatch(anyList()))
        .willReturn(responses);

    // when & then
    mockMvc.perform(post("/api/v1/pose/data/batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(batchData)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(2)))
        .andExpect(jsonPath("$.data[0].frameCount").value(1))
        .andExpect(jsonPath("$.data[1].frameCount").value(2));

    verify(poseDataService, times(1)).processPoseDataBatch(anyList());
  }

  @Test
  @DisplayName("GET /api/v1/pose/fall-status/{userId} - 낙상 상태 조회 성공")
  void getFallStatus_Success() throws Exception {
    // given
    Long userId = 1L;
    FallStatusDto fallStatus = FallStatusDto.builder()
        .userId(userId)
        .lastChecked(LocalDateTime.now())
        .recentFallEvents(new ArrayList<>())
        .isMonitoring(true)
        .sessionActive(true)
        .build();

    given(poseDataService.getFallStatus(userId))
        .willReturn(fallStatus);

    // when & then
    mockMvc.perform(get("/api/v1/pose/fall-status/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(userId))
        .andExpect(jsonPath("$.data.isMonitoring").value(true))
        .andExpect(jsonPath("$.data.sessionActive").value(true));

    verify(poseDataService, times(1)).getFallStatus(userId);
  }

  @Test
  @DisplayName("POST /api/v1/pose/data - 낙상 감지 시 알림 포함")
  void receivePoseData_WithFallDetection() throws Exception {
    // given
    PoseResponseDto response = PoseResponseDto.builder()
        .sessionId("test-session-123")
        .frameCount(30)
        .fallDetected(true)
        .confidenceScore(0.85f)
        .severity("HIGH")
        .message("낙상이 감지되었습니다! 보호자에게 알림을 전송했습니다.")
        .build();

    given(poseDataService.processPoseData(any(PoseDataDto.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/v1/pose/data")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPoseData)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.fallDetected").value(true))
        .andExpect(jsonPath("$.data.confidenceScore").value(0.85))
        .andExpect(jsonPath("$.data.severity").value("HIGH"))
        .andExpect(jsonPath("$.data.message").value("낙상이 감지되었습니다! 보호자에게 알림을 전송했습니다."));
  }

  @Test
  @DisplayName("POST /api/v1/pose/data - 잘못된 좌표 값으로 실패")
  void receivePoseData_InvalidCoordinates() throws Exception {
    // given - x 좌표가 범위를 벗어남
    validPoseData.getLandmarks().get(0).setX(1.5f); // 0-1 범위를 벗어남

    // when & then
    mockMvc.perform(post("/api/v1/pose/data")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validPoseData)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/v1/pose/fall-event/{eventId}/feedback - 낙상 피드백 제출")
  void submitFallFeedback_Success() throws Exception {
    // given
    Long eventId = 1L;
    Map<String, Object> feedbackData = new HashMap<>();
    feedbackData.put("isFalsePositive", true);
    feedbackData.put("userComment", "의자에 앉는 동작이었습니다");

    doNothing().when(poseDataService).submitFallFeedback(eq(eventId), eq(true), anyString());

    // when & then
    mockMvc.perform(post("/api/v1/pose/fall-event/{eventId}/feedback", eventId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(feedbackData)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("피드백이 성공적으로 제출되었습니다"));

    verify(poseDataService, times(1)).submitFallFeedback(eq(eventId), eq(true), anyString());
  }
}