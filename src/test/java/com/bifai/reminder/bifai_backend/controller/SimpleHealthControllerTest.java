package com.bifai.reminder.bifai_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HealthController 단위 테스트
 * 헬스체크 엔드포인트 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController 단위 테스트")
class SimpleHealthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    @Test
    @DisplayName("GET /api/health - 헬스체크 성공")
    void health_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Application is running"));
    }

    @Test
    @DisplayName("GET /api/v1/health - 헬스체크 V1 성공")
    void healthV1_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Application is running"));
    }

    @Test
    @DisplayName("GET /health - 기본 헬스체크 성공")
    void basicHealth_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Application is running"));
    }
}