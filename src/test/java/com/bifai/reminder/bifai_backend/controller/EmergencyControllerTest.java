package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.service.EmergencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

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
 * 긴급 상황 컨트롤러 테스트
 * 
 * <p>EmergencyController의 모든 엔드포인트를 테스트합니다.
 * 인증, 권한, 입력 검증, 응답 형식 등을 검증합니다.</p>
 */
@DisplayName("EmergencyController 테스트")
class EmergencyControllerTest extends BaseControllerTest {

  @MockitoBean
  private EmergencyService emergencyService;

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
        .userName("홍길동")
        .type(EmergencyType.MANUAL_ALERT)
        .typeDescription("수동 호출")
        .status(EmergencyStatus.ACTIVE)
        .statusDescription("진행 중")
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울특별시 중구 태평로1가")
        .description("도움이 필요해요")
        .severity(EmergencySeverity.HIGH)
        .severityDescription("높음")
        .triggeredBy(TriggerSource.USER)
        .createdAt(LocalDateTime.now())
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
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("긴급 상황이 신고되었습니다"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.type").value("MANUAL_ALERT"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

    verify(emergencyService).createEmergency(any(EmergencyRequest.class));
  }

  @Test
  @DisplayName("긴급 상황 발생 신고 - 인증 없음 실패")
  void createEmergency_Unauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/emergency/alert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("긴급 상황 발생 신고 - 잘못된 입력")
  @WithMockUser(roles = "USER")
  void createEmergency_InvalidInput() throws Exception {
    // given - 필수 필드 누락
    EmergencyRequest invalidRequest = EmergencyRequest.builder()
        .description("도움이 필요해요")
        .build();

    // when & then
    mockMvc.perform(post("/api/v1/emergency/alert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  @DisplayName("낙상 감지 처리 - 성공")
  @WithMockUser(roles = "DEVICE")
  void handleFallDetection_Success() throws Exception {
    // given
    FallDetectionRequest fallRequest = FallDetectionRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .confidence(85.5)
        .imageUrl("https://example.com/fall-image.jpg")
        .build();

    given(emergencyService.handleFallDetection(any(FallDetectionRequest.class)))
        .willReturn(mockResponse);

    // when & then
    mockMvc.perform(post("/api/v1/emergency/fall-detection")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(fallRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("낙상 감지 긴급 상황이 생성되었습니다"));
  }

  @Test
  @DisplayName("긴급 상황 상태 조회 - 성공")
  @WithMockUser(roles = "USER")
  void getEmergencyStatus_Success() throws Exception {
    // given
    Long emergencyId = 1L;
    given(emergencyService.getEmergencyStatus(emergencyId))
        .willReturn(mockResponse);
    given(emergencyService.isOwnEmergency(emergencyId))
        .willReturn(true);

    // when & then
    mockMvc.perform(get("/api/v1/emergency/status/{id}", emergencyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value(emergencyId));
  }

  @Test
  @DisplayName("활성 긴급 상황 목록 조회 - 성공")
  @WithMockUser(roles = "ADMIN")
  void getActiveEmergencies_Success() throws Exception {
    // given
    List<EmergencyResponse> activeList = Arrays.asList(mockResponse);
    given(emergencyService.getActiveEmergencies())
        .willReturn(activeList);

    // when & then
    mockMvc.perform(get("/api/v1/emergency/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
  }

  @Test
  @DisplayName("긴급 상황 해결 - 성공")
  @WithMockUser(roles = "GUARDIAN")
  void resolveEmergency_Success() throws Exception {
    // given
    Long emergencyId = 1L;
    String resolvedBy = "보호자";
    String notes = "안전하게 해결됨";

    EmergencyResponse resolvedResponse = EmergencyResponse.builder()
        .id(emergencyId)
        .status(EmergencyStatus.RESOLVED)
        .statusDescription("해결됨")
        .resolvedAt(LocalDateTime.now())
        .resolvedBy(resolvedBy)
        .resolutionNotes(notes)
        .responseTimeSeconds(300)
        .build();

    given(emergencyService.resolveEmergency(emergencyId, resolvedBy, notes))
        .willReturn(resolvedResponse);
    given(emergencyService.isGuardianOfEmergency(emergencyId))
        .willReturn(true);

    // when & then
    mockMvc.perform(patch("/api/v1/emergency/{id}/resolve", emergencyId)
            .param("resolvedBy", resolvedBy)
            .param("notes", notes))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("긴급 상황이 해결되었습니다"))
        .andExpect(jsonPath("$.data.status").value("RESOLVED"));
  }

  @Test
  @DisplayName("긴급 상황 해결 - 권한 없음")
  @WithMockUser(roles = "USER")
  void resolveEmergency_Forbidden() throws Exception {
    // given
    Long emergencyId = 1L;
    given(emergencyService.isOwnEmergency(emergencyId)).willReturn(false);
    given(emergencyService.isGuardianOfEmergency(emergencyId)).willReturn(false);

    // when & then
    mockMvc.perform(patch("/api/v1/emergency/{id}/resolve", emergencyId)
            .param("resolvedBy", "사용자")
            .param("notes", "해결"))
        .andExpect(status().isForbidden());
  }
}