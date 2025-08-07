package com.bifai.reminder.bifai_backend;

import com.bifai.reminder.bifai_backend.config.TestRedisConfiguration;
import com.bifai.reminder.bifai_backend.config.TestVisionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 간단한 헬스체크 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestVisionConfiguration.class})
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.ai.openai.api.OpenAiApiAutoConfiguration",
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.speech.api-key=test-key",
    "app.jwt.secret=test-jwt-secret-key-for-test-1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "app.jwt.access-token-expiration-ms=3600000",
    "app.jwt.refresh-token-expiration-ms=604800000"
})
class SimpleHealthCheckTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @Test
  void healthCheck() throws Exception {
    mockMvc.perform(get("/api/v1/test/health"))
      .andExpect(status().isOk());
  }
}