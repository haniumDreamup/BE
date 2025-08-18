package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.firebase.messaging.FirebaseMessaging;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 단순 컨텍스트 로드 테스트
 */
@SpringBootTest
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
class SimpleContextTest {
  
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
  void contextLoads() {
    // 컨텍스트가 성공적으로 로드되는지 확인
    assertThat(true).isTrue();
  }
}