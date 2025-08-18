package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.firebase.messaging.FirebaseMessaging;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.data.redis.core.RedisTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 간단한 헬스체크 테스트
 */
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
class SimpleHealthCheckTest {
  
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
  void healthCheck() throws Exception {
    mockMvc.perform(get("/api/v1/test/health"))
      .andExpect(status().isOk());
  }
}