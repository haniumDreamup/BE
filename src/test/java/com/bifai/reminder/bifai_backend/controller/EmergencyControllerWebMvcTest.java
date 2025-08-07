package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.common.BaseController;
import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.EmergencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.bifai.reminder.bifai_backend.config.TestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

/**
 * 긴급 상황 컨트롤러 WebMvc 테스트
 */
@WebMvcTest(controllers = EmergencyController.class)
@ActiveProfiles("test")
@DisplayName("EmergencyController WebMvc 테스트")
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
@Import(TestMvcConfiguration.class)
class EmergencyControllerWebMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private EmergencyService emergencyService;

  @MockBean
  private UserRepository userRepository;
  
  @MockBean
  private com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider jwtTokenProvider;
  
  @MockBean
  private com.bifai.reminder.bifai_backend.security.jwt.JwtAuthenticationFilter jwtAuthenticationFilter;
  
  @MockBean
  private com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService userDetailsService;

  private EmergencyRequest validRequest;
  private EmergencyResponse mockResponse;

  @BeforeEach
  void setUp() {
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
  @WithMockUser(roles = "USER")
  void createEmergency_Success() throws Exception {
    // given
    given(emergencyService.createEmergency(any(EmergencyRequest.class)))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/emergency/alert")
            .with(csrf())
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
  @DisplayName("긴급 상황 발생 신고 - 인증 없음 실패")
  void createEmergency_Unauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/emergency/alert")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("낙상 감지 처리 - 성공")
  @WithMockUser(roles = "USER")
  void handleFallDetection_Success() throws Exception {
    // given
    FallDetectionRequest fallRequest = FallDetectionRequest.builder()
        .userId(1L)
        .latitude(37.5665)
        .longitude(126.9780)
        .confidence(0.95)
        .bodyAngle(75.0f)
        .velocity(2.5f)
        .timestamp(LocalDateTime.now())
        .build();

    given(emergencyService.handleFallDetection(any(FallDetectionRequest.class)))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/emergency/fall-detection")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(fallRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.type").value("MANUAL_ALERT"));

    verify(emergencyService).handleFallDetection(any(FallDetectionRequest.class));
  }

  @Test
  @DisplayName("긴급 상황 상태 조회 - 성공")
  @WithMockUser(roles = "USER")
  void getEmergencyStatus_Success() throws Exception {
    // given
    Long emergencyId = 1L;
    given(emergencyService.getEmergencyStatus(emergencyId))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(get("/api/v1/emergency/status/{id}", emergencyId)
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(emergencyId));
  }

  @Test
  @DisplayName("활성 긴급 상황 목록 조회 - 성공")
  @WithMockUser(roles = "ADMIN")
  void getActiveEmergencies_Success() throws Exception {
    // given
    List<EmergencyResponse> activeEmergencies = Arrays.asList(mockResponse);
    given(emergencyService.getActiveEmergencies())
        .willReturn(activeEmergencies);

    // when & then
    mockMvc.perform(get("/api/v1/emergency/active")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

    verify(emergencyService).getActiveEmergencies();
  }

  @Test
  @DisplayName("긴급 상황 해결 - 성공")
  @WithMockUser(roles = "ADMIN")
  void resolveEmergency_Success() throws Exception {
    // given
    Long emergencyId = 1L;
    ResolveEmergencyRequest resolveRequest = ResolveEmergencyRequest.builder()
        .resolutionNotes("상황이 해결되었습니다")
        .build();

    EmergencyResponse resolvedResponse = EmergencyResponse.builder()
        .id(emergencyId)
        .status(EmergencyStatus.RESOLVED)
        .resolvedAt(LocalDateTime.now())
        .resolutionNotes("상황이 해결되었습니다")
        .build();

    given(emergencyService.resolveEmergency(eq(emergencyId), any(ResolveEmergencyRequest.class)))
        .willReturn(resolvedResponse);

    // when & then
    mockMvc.perform(put("/api/v1/emergency/{id}/resolve", emergencyId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resolveRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("RESOLVED"));

    verify(emergencyService).resolveEmergency(eq(emergencyId), any(ResolveEmergencyRequest.class));
  }

  @Test
  @DisplayName("긴급 상황 해결 - 권한 없음")
  @WithMockUser(roles = "USER")
  void resolveEmergency_Forbidden() throws Exception {
    // given
    Long emergencyId = 1L;
    ResolveEmergencyRequest resolveRequest = ResolveEmergencyRequest.builder()
        .resolutionNotes("상황이 해결되었습니다")
        .build();

    // when & then
    mockMvc.perform(put("/api/v1/emergency/{id}/resolve", emergencyId)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(resolveRequest)))
        .andExpect(status().isForbidden());
  }
}