package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 가장 단순한 애플리케이션 테스트
 */
@SpringBootTest(properties = {
  "spring.batch.job.enabled=false",
  "spring.http.client.factory=simple"
})
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
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.chat.enabled=false",
    "spring.cache.type=none",
    "google.cloud.vision.enabled=false",
    "firebase.enabled=false"
})
class SimpleApplicationTest {
  

  @Test
  void contextLoads() {
    // 애플리케이션 컨텍스트만 로드
  }
}