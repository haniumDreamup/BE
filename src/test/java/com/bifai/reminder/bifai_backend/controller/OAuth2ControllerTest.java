package com.bifai.reminder.bifai_backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.firebase.messaging.FirebaseMessaging;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
  
  @MockBean
  private RedisTemplate<String, Object> redisTemplate;
  
  @MockBean
  private RefreshTokenService refreshTokenService;
  
  @MockBean
  private RedisCacheService redisCacheService;
  
  @MockBean
  private ImageAnnotatorClient imageAnnotatorClient;
  
  @MockBean
  private FirebaseMessaging firebaseMessaging;
  
  @MockBean
  private S3Client s3Client;
  
  @MockBean
  private S3AsyncClient s3AsyncClient;
  
  @MockBean
  private S3Presigner s3Presigner;

  @Test
  @DisplayName("OAuth2 로그인 URL 조회 성공")
  void testGetOAuth2LoginUrls() throws Exception {
    mockMvc.perform(get("/api/v1/auth/oauth2/login-urls"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.kakao").value("/oauth2/authorization/kakao"))
      .andExpect(jsonPath("$.data.naver").value("/oauth2/authorization/naver"))
      .andExpect(jsonPath("$.data.google").value("/oauth2/authorization/google"))
      .andExpect(jsonPath("$.message").value("소셜 로그인 주소를 가져왔습니다."));
  }
}