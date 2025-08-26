package com.bifai.reminder.bifai_backend;

import com.bifai.reminder.bifai_backend.config.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 애플리케이션 부트스트랩 테스트
 */
@SpringBootTest(properties = {
  "spring.batch.job.enabled=false",
  "spring.http.client.factory=simple"
})
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
class BifaiBackendApplicationTests {
  

  @Test
  void contextLoads() {
    // 애플리케이션 컨텍스트 로드 테스트
  }
}