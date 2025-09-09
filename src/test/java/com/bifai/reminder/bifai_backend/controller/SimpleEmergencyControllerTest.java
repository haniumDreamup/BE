package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.emergency.EmergencyRequest;
import com.bifai.reminder.bifai_backend.dto.emergency.EmergencyResponse;
import com.bifai.reminder.bifai_backend.dto.emergency.FallDetectionRequest;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.service.EmergencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmergencyController 간단 테스트
 * 긴급 상황 관리 핵심 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergencyController 간단 테스트")
class SimpleEmergencyControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private EmergencyService emergencyService;
    
    @Mock
    private com.bifai.reminder.bifai_backend.repository.UserRepository userRepository;

    @InjectMocks
    private EmergencyController emergencyController;

    private EmergencyRequest emergencyRequest;
    private EmergencyResponse emergencyResponse;
    private FallDetectionRequest fallDetectionRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(emergencyController).build();

        emergencyRequest = EmergencyRequest.builder()
                .type(Emergency.EmergencyType.PANIC_BUTTON)
                .description("도움이 필요합니다")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        emergencyResponse = EmergencyResponse.builder()
                .id(1L)
                .type(Emergency.EmergencyType.PANIC_BUTTON)
                .description("도움이 필요합니다")
                .status(Emergency.EmergencyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        fallDetectionRequest = FallDetectionRequest.builder()
                .confidence(0.95d)
                .latitude(37.5665)
                .longitude(126.9780)
                .imageUrl("https://example.com/fall-image.jpg")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/emergency/alert - 긴급 상황 신고 성공")
    void createEmergencyAlert_Success() throws Exception {
        // Given
        when(emergencyService.createEmergency(any(EmergencyRequest.class))).thenReturn(emergencyResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/emergency/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emergencyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.type").value("PANIC_BUTTON"))
                .andExpect(jsonPath("$.message").value("긴급 상황이 신고되었습니다. 보호자에게 알림을 전송했습니다"));

        verify(emergencyService).createEmergency(any(EmergencyRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/emergency/alert - 서비스 예외 발생")
    void createEmergencyAlert_ServiceException() throws Exception {
        // Given
        when(emergencyService.createEmergency(any(EmergencyRequest.class)))
                .thenThrow(new RuntimeException("긴급 상황 생성 실패"));

        // When & Then
        mockMvc.perform(post("/api/v1/emergency/alert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emergencyRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("긴급 상황 신고 중 오류가 발생했습니다"));

        verify(emergencyService).createEmergency(any(EmergencyRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/emergency/fall-detection - 낙상 감지 알림 성공")
    void reportFallDetection_Success() throws Exception {
        // Given
        EmergencyResponse fallResponse = EmergencyResponse.builder()
                .id(2L)
                .type(Emergency.EmergencyType.FALL_DETECTED)
                .description("낙상이 감지되었습니다")
                .status(Emergency.EmergencyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(emergencyService.handleFallDetection(any(FallDetectionRequest.class))).thenReturn(fallResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/emergency/fall-detection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fallDetectionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.type").value("FALL_DETECTED"))
                .andExpect(jsonPath("$.message").value("낙상이 감지되어 긴급 상황이 등록되었습니다"));

        verify(emergencyService).handleFallDetection(any(FallDetectionRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/emergency/status/{emergencyId} - 긴급 상황 상태 조회 성공")
    void getEmergencyStatus_Success() throws Exception {
        // Given
        when(emergencyService.getEmergencyStatus(1L)).thenReturn(emergencyResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/emergency/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("긴급 상황 정보를 가져왔습니다"));

        verify(emergencyService).getEmergencyStatus(1L);
    }

    @Test
    @DisplayName("GET /api/v1/emergency/status/{emergencyId} - 긴급 상황 없음")
    void getEmergencyStatus_NotFound() throws Exception {
        // Given
        when(emergencyService.getEmergencyStatus(999L)).thenThrow(new RuntimeException("긴급 상황을 찾을 수 없음"));

        // When & Then
        mockMvc.perform(get("/api/v1/emergency/status/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("긴급 상황을 찾을 수 없습니다"));

        verify(emergencyService).getEmergencyStatus(999L);
    }

    @Test
    @org.junit.jupiter.api.Disabled("ServletException issue - requires full Spring context")
    @DisplayName("GET /api/v1/emergency/history/{userId} - 긴급 상황 이력 조회 성공")
    void getUserEmergencyHistory_Success() throws Exception {
        // Given
        List<EmergencyResponse> emergencyList = Arrays.asList(emergencyResponse);
        Page<EmergencyResponse> emergencyPage = new PageImpl<>(emergencyList, PageRequest.of(0, 20), 1);
        when(emergencyService.getUserEmergencyHistory(eq(1L), any())).thenReturn(emergencyPage);

        // When & Then
        mockMvc.perform(get("/api/v1/emergency/history/1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.message").value("긴급 상황 이력을 가져왔습니다"));

        verify(emergencyService).getUserEmergencyHistory(eq(1L), any());
    }

    @Test
    @DisplayName("GET /api/v1/emergency/active - 활성 긴급 상황 목록 조회 성공")
    void getActiveEmergencies_Success() throws Exception {
        // Given
        List<EmergencyResponse> activeEmergencies = Arrays.asList(emergencyResponse);
        when(emergencyService.getActiveEmergencies()).thenReturn(activeEmergencies);

        // When & Then
        mockMvc.perform(get("/api/v1/emergency/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.message").value("활성 긴급 상황 목록을 가져왔습니다"));

        verify(emergencyService).getActiveEmergencies();
    }

    @Test
    @DisplayName("PUT /api/v1/emergency/{emergencyId}/resolve - 긴급 상황 해결 성공")
    void resolveEmergency_Success() throws Exception {
        // Given
        EmergencyResponse resolvedResponse = EmergencyResponse.builder()
                .id(1L)
                .type(Emergency.EmergencyType.PANIC_BUTTON)
                .description("도움이 필요합니다")
                .status(Emergency.EmergencyStatus.RESOLVED)
                .resolvedBy("보호자1")
                .resolvedAt(LocalDateTime.now())
                .build();

        when(emergencyService.resolveEmergency(eq(1L), eq("보호자1"), eq("안전 확인됨")))
                .thenReturn(resolvedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/emergency/1/resolve")
                        .param("resolvedBy", "보호자1")
                        .param("notes", "안전 확인됨"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedBy").value("보호자1"))
                .andExpect(jsonPath("$.message").value("긴급 상황이 해결 처리되었습니다"));

        verify(emergencyService).resolveEmergency(eq(1L), eq("보호자1"), eq("안전 확인됨"));
    }

    @Test
    @DisplayName("PUT /api/v1/emergency/{emergencyId}/resolve - 필수 파라미터 없이 해결")
    void resolveEmergency_WithoutNotes() throws Exception {
        // Given
        EmergencyResponse resolvedResponse = EmergencyResponse.builder()
                .id(1L)
                .type(Emergency.EmergencyType.PANIC_BUTTON)
                .description("도움이 필요합니다")
                .status(Emergency.EmergencyStatus.RESOLVED)
                .resolvedBy("보호자1")
                .resolvedAt(LocalDateTime.now())
                .build();

        when(emergencyService.resolveEmergency(eq(1L), eq("보호자1"), isNull()))
                .thenReturn(resolvedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/emergency/1/resolve")
                        .param("resolvedBy", "보호자1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.message").value("긴급 상황이 해결 처리되었습니다"));

        verify(emergencyService).resolveEmergency(eq(1L), eq("보호자1"), isNull());
    }
}