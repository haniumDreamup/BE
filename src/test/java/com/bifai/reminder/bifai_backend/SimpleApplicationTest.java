package com.bifai.reminder.bifai_backend;

import com.bifai.reminder.bifai_backend.config.TestRedisConfiguration;
import com.bifai.reminder.bifai_backend.config.TestVisionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 가장 단순한 애플리케이션 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
class SimpleApplicationTest {

  @Test
  void contextLoads() {
    // 애플리케이션 컨텍스트만 로드
  }
}