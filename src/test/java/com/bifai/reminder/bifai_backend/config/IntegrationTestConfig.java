package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * 통합 테스트용 설정
 * @SpringBootTest와 함께 사용
 */
@TestConfiguration
@Profile("test")
@Import({
  TestInfrastructureConfig.class,
  TestExternalServicesConfig.class,
  TestSecurityConfig.class,
  TestWebSocketConfig.class
})
public class IntegrationTestConfig {
  // 모든 테스트 설정을 통합
}