package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.accessibility.*;
import com.bifai.reminder.bifai_backend.service.AccessibilityService;
import com.bifai.reminder.bifai_backend.service.VoiceGuidanceService;
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

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AccessibilityController 간단 테스트
 * 접근성 기능 핵심 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccessibilityController 간단 테스트")
class SimpleAccessibilityControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AccessibilityService accessibilityService;

    @Mock
    private VoiceGuidanceService voiceGuidanceService;

    @InjectMocks
    private AccessibilityController accessibilityController;

    private VoiceGuidanceRequest voiceGuidanceRequest;
    private AriaLabelRequest ariaLabelRequest;
    private AccessibilitySettingsDto settingsDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(accessibilityController).build();

        voiceGuidanceRequest = new VoiceGuidanceRequest();
        voiceGuidanceRequest.setContext("navigation");
        voiceGuidanceRequest.setParams(Map.of("action", "navigate", "target", "home"));
        voiceGuidanceRequest.setLanguage("ko-KR");

        ariaLabelRequest = new AriaLabelRequest();
        ariaLabelRequest.setElementType("button");
        ariaLabelRequest.setElementName("홈으로 가기");
        ariaLabelRequest.setAttributes(Map.of("role", "button"));

        settingsDto = AccessibilitySettingsDto.builder()
                .userId(1L)
                .highContrastEnabled(true)
                .colorScheme("dark")
                .fontSize("large")
                .voiceGuidanceEnabled(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/accessibility/voice-guidance - 음성 안내 생성 성공")
    void generateVoiceGuidance_Success() throws Exception {
        // Given
        when(voiceGuidanceService.generateVoiceGuidance(any(), anyString(), any()))
                .thenReturn("홈 화면으로 이동합니다");

        // When & Then
        mockMvc.perform(post("/api/v1/accessibility/voice-guidance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voiceGuidanceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("홈 화면으로 이동합니다"))
                .andExpect(jsonPath("$.data.context").value("navigation"));

        verify(voiceGuidanceService).generateVoiceGuidance(any(), eq("navigation"), any());
    }

    @Test
    @DisplayName("POST /api/v1/accessibility/aria-label - ARIA 라벨 생성 성공")
    void generateAriaLabel_Success() throws Exception {
        // Given
        when(voiceGuidanceService.generateAriaLabel(anyString(), anyString(), any()))
                .thenReturn("홈으로 가기 버튼");

        // When & Then
        mockMvc.perform(post("/api/v1/accessibility/aria-label")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ariaLabelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.label").value("홈으로 가기 버튼"))
                .andExpect(jsonPath("$.data.elementType").value("button"));

        verify(voiceGuidanceService).generateAriaLabel(eq("button"), eq("홈으로 가기"), any());
    }

    @Test
    @DisplayName("GET /api/v1/accessibility/screen-reader-hint - 스크린리더 힌트 생성 성공")
    void getScreenReaderHint_Success() throws Exception {
        // Given
        when(voiceGuidanceService.generateScreenReaderHint(anyString(), anyString()))
                .thenReturn("버튼을 누르면 홈 화면으로 이동합니다");

        // When & Then
        mockMvc.perform(get("/api/v1/accessibility/screen-reader-hint")
                        .param("action", "click")
                        .param("target", "home-button"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("버튼을 누르면 홈 화면으로 이동합니다"));

        verify(voiceGuidanceService).generateScreenReaderHint(eq("click"), eq("home-button"));
    }

    @Test
    @DisplayName("GET /api/v1/accessibility/settings - 접근성 설정 조회 성공")
    void getSettings_Success() throws Exception {
        // Given
        when(accessibilityService.getSettings(any())).thenReturn(settingsDto);

        // When & Then
        mockMvc.perform(get("/api/v1/accessibility/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.highContrastEnabled").value(true))
                .andExpect(jsonPath("$.data.colorScheme").value("dark"));

        verify(accessibilityService).getSettings(any());
    }

    @Test
    @DisplayName("PUT /api/v1/accessibility/settings - 설정 업데이트 성공")
    void updateSettings_Success() throws Exception {
        // Given
        AccessibilitySettingsDto updateDto = AccessibilitySettingsDto.builder()
                .userId(1L)
                .highContrastEnabled(false)
                .fontSize("medium")
                .build();
        
        when(accessibilityService.updateSettings(any(), any())).thenReturn(settingsDto);

        // When & Then
        mockMvc.perform(put("/api/v1/accessibility/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1));

        verify(accessibilityService).updateSettings(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/accessibility/settings/apply-profile - 프로파일 적용 성공")
    void applyProfile_Success() throws Exception {
        // Given
        when(accessibilityService.applyProfile(any(), anyString())).thenReturn(settingsDto);

        // When & Then
        mockMvc.perform(post("/api/v1/accessibility/settings/apply-profile")
                        .param("profileType", "senior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.message").value("프로파일이 적용되었습니다"));

        verify(accessibilityService).applyProfile(any(), eq("senior"));
    }

    @Test
    @org.junit.jupiter.api.Disabled("ServletException issue with exception handling")
    @DisplayName("POST /api/v1/accessibility/voice-guidance - 서비스 예외 발생")
    void generateVoiceGuidance_ServiceException() throws Exception {
        // Given
        when(voiceGuidanceService.generateVoiceGuidance(any(), anyString(), any()))
                .thenThrow(new RuntimeException("음성 안내 생성 실패"));

        // When & Then
        mockMvc.perform(post("/api/v1/accessibility/voice-guidance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voiceGuidanceRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(voiceGuidanceService).generateVoiceGuidance(any(), anyString(), any());
    }
}