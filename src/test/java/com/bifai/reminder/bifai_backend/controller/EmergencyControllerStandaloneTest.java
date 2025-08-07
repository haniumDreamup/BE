package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.service.EmergencyService;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

/**
 * 긴급 상황 컨트롤러 단독 테스트
 * MockMvc를 standalone 모드로 설정하여 의존성 문제 해결
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergencyController 단독 테스트")
class EmergencyControllerStandaloneTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private EmergencyService emergencyService;

  @InjectMocks
  private EmergencyController emergencyController;

  private EmergencyRequest validRequest;
  private EmergencyResponse mockResponse;

  @BeforeEach
  void setUp() {
    // MockMvc를 standalone 모드로 설정
    mockMvc = MockMvcBuilders.standaloneSetup(emergencyController)
        .build();
    
    // Jackson ObjectMapper 설정
    objectMapper.findAndRegisterModules();

    // 테스트용 요청 데이터 설정
    validRequest = EmergencyRequest.builder()
        .type(EmergencyType.MANUAL_ALERT)
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울특별시 중구 태평로1가")
        .description("도움이 필요해요")
        .severity(EmergencySeverity.HIGH)
        .build();

    // 테스트용 응답 데이터 설정
    mockResponse = EmergencyResponse.builder()
        .id(1L)
        .userId(1L)
        .type(EmergencyType.MANUAL_ALERT)
        .status(EmergencyStatus.ACTIVE)
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울특별시 중구 태평로1가")
        .description("도움이 필요해요")
        .severity(EmergencySeverity.HIGH)
        .createdAt(LocalDateTime.now())
        .notificationSent(true)
        .responderCount(2)
        .build();
  }

  @Test
  @DisplayName("긴급 상황 발생 신고 - 성공")
  void createEmergency_Success() throws Exception {
    // given
    given(emergencyService.createEmergency(any(EmergencyRequest.class)))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/emergency/alert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.type").value("MANUAL_ALERT"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    verify(emergencyService).createEmergency(any(EmergencyRequest.class));
  }

  @Test
  @DisplayName("낙상 감지 처리 - 성공")
  void handleFallDetection_Success() throws Exception {
    // given
    FallDetectionRequest fallRequest = FallDetectionRequest.builder()
        .userId(1L)
        .latitude(37.5665)
        .longitude(126.9780)
        .confidence(95.0)
        .bodyAngle(75.0f)
        .velocity(2.5f)
        .timestamp(LocalDateTime.now())
        .build();

    given(emergencyService.handleFallDetection(any(FallDetectionRequest.class)))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/emergency/fall-detection")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(fallRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.type").value("MANUAL_ALERT"));

    verify(emergencyService).handleFallDetection(any(FallDetectionRequest.class));
  }

  @Test
  @DisplayName("긴급 상황 상태 조회 - 성공")
  void getEmergencyStatus_Success() throws Exception {
    // given
    Long emergencyId = 1L;
    given(emergencyService.getEmergencyStatus(emergencyId))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(get("/api/v1/emergency/status/{id}", emergencyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(emergencyId));
  }

  @Test
  @DisplayName("활성 긴급 상황 목록 조회 - 성공")
  void getActiveEmergencies_Success() throws Exception {
    // given
    List<EmergencyResponse> activeEmergencies = Arrays.asList(mockResponse);
    given(emergencyService.getActiveEmergencies())
        .willReturn(activeEmergencies);

    // when & then
    mockMvc.perform(get("/api/v1/emergency/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

    verify(emergencyService).getActiveEmergencies();
  }

  @Test
  @DisplayName("긴급 상황 해결 - 성공")
  void resolveEmergency_Success() throws Exception {
    // given
    Long emergencyId = 1L;
    String resolvedBy = "보호자";
    String notes = "상황이 해결되었습니다";

    EmergencyResponse resolvedResponse = EmergencyResponse.builder()
        .id(emergencyId)
        .status(EmergencyStatus.RESOLVED)
        .resolvedAt(LocalDateTime.now())
        .resolutionNotes(notes)
        .build();

    given(emergencyService.resolveEmergency(eq(emergencyId), eq(resolvedBy), eq(notes)))
        .willReturn(resolvedResponse);

    // when & then
    mockMvc.perform(put("/api/v1/emergency/{emergencyId}/resolve", emergencyId)
            .param("resolvedBy", resolvedBy)
            .param("notes", notes))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("RESOLVED"));

    verify(emergencyService).resolveEmergency(eq(emergencyId), eq(resolvedBy), eq(notes));
  }
}