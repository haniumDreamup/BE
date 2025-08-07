package com.bifai.reminder.bifai_backend;

import com.bifai.reminder.bifai_backend.config.TestRedisConfiguration;
import com.bifai.reminder.bifai_backend.config.TestVisionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 애플리케이션 부트스트랩 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestVisionConfiguration.class})
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.ai.openai.api.OpenAiApiAutoConfiguration",
    "spring.data.redis.timeout=2000",
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.speech.api-key=test-key",
    "app.jwt.secret=test-jwt-secret-key-for-test-1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "app.jwt.access-token-expiration-ms=3600000",
    "app.jwt.refresh-token-expiration-ms=604800000",
    "server.port=0",
    "logging.level.org.springframework.web=ERROR"
})
class BifaiBackendApplicationTests {

  @Test
  void contextLoads() {
    // 애플리케이션 컨텍스트 로드 테스트
  }
}