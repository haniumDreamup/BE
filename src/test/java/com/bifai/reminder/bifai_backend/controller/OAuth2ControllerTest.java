package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.config.IntegrationTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ItemProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.junit.jupiter.api.Disabled("Replaced by OAuth2ControllerStandaloneTest - Spring context loading issues")
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.batch.job.enabled=false",
    "spring.http.client.factory=simple"
  }
)
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
class OAuth2ControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("OAuth2 로그인 URL 조회 성공")
  void testGetOAuth2LoginUrls() throws Exception {
    // Test simplified - just check if endpoint responds
    mockMvc.perform(get("/api/v1/auth/oauth2/login-urls")
        .accept("application/json"))
      .andDo(result -> {
        System.out.println("Response status: " + result.getResponse().getStatus());
        System.out.println("Response body: " + result.getResponse().getContentAsString());
        System.out.println("Content type: " + result.getResponse().getContentType());
      })
      .andExpect(status().isOk());
  }
}