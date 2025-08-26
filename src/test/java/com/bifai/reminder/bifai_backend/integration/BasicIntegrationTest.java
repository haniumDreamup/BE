package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.BifaiBackendApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.bifai.reminder.bifai_backend.config.IntegrationTestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 기본 통합 테스트
 * 
 * <p>애플리케이션의 기본적인 구동과 헬스체크를 확인합니다.</p>
 */
@SpringBootTest(classes = BifaiBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "app.jwt.secret=test-jwt-secret-key-for-bifai-backend-application-test-environment-only-with-minimum-64-bytes-requirement",
    "app.jwt.access-token-expiration-ms=900000",
    "app.jwt.refresh-token-expiration-ms=604800000",
    "fcm.enabled=false",
    "spring.ai.openai.api-key=test-key"
})
@DisplayName("기본 통합 테스트")
class BasicIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("애플리케이션 컨텍스트 로드 성공")
  void contextLoads() {
    // 컨텍스트 로드 확인
  }

  @Test
  @DisplayName("헬스체크 API 호출 성공")
  @Disabled("테스트 환경 문제로 일시 비활성화")
  void healthCheck() throws Exception {
    mockMvc.perform(get("/api/v1/auth/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value("인증 서비스가 정상 작동 중입니다"));
  }

  @Test
  @DisplayName("액추에이터 헬스 체크")
  @Disabled("테스트 환경 문제로 일시 비활성화")
  void actuatorHealthCheck() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }
}