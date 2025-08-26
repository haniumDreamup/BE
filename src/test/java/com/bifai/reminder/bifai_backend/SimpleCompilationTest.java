package com.bifai.reminder.bifai_backend;

import com.bifai.reminder.bifai_backend.config.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 간단한 컴파일 테스트
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
public class SimpleCompilationTest {

    @Test
    void contextLoads() {
        // 스프링 컨텍스트가 로드되는지 확인
        assertTrue(true, "컨텍스트 로드 성공");
    }
    
    @Test
    void simpleTest() {
        // 간단한 테스트
        int result = 2 + 2;
        assertTrue(result == 4, "기본 연산 테스트");
    }
}