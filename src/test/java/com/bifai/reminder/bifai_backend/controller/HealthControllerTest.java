package com.bifai.reminder.bifai_backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HealthController 테스트 - Spring Context 통합 테스트 (비활성화됨)
 * SimpleHealthControllerTest로 대체됨
 */
@org.junit.jupiter.api.Disabled("Replaced by SimpleHealthControllerTest - Spring context loading issues")
@SpringBootTest(properties = {
  "spring.batch.job.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @Test
  @DisplayName("GET /api/health - 헬스체크 성공")
  void health_Success() throws Exception {
    mockMvc.perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.status").value("UP"))
      .andExpect(jsonPath("$.message").value("Application is running"));
  }
  
  @Test
  @DisplayName("GET /api/v1/health - V1 헬스체크 성공")
  void healthV1_Success() throws Exception {
    mockMvc.perform(get("/api/v1/health"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.status").value("UP"))
      .andExpect(jsonPath("$.message").value("Application is running"));
  }
  
  @Test
  @DisplayName("GET /health - 기본 헬스체크 성공")
  void basicHealth_Success() throws Exception {
    mockMvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.status").value("UP"))
      .andExpect(jsonPath("$.message").value("Application is running"));
  }
}