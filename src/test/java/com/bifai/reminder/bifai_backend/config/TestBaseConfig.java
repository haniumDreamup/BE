package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * 통합 테스트 기본 설정
 * Spring Boot 3.5 베스트 프랙티스에 따라 모든 테스트 설정 통합
 */
@TestConfiguration
@Profile("test")
@Import({
  TestInfrastructureConfig.class,
  TestExternalServicesConfig.class, 
  TestSecurityConfig.class,
  TestHttpClientConfig.class,
  TestWebSocketConfig.class
})
public class TestBaseConfig {
  // 모든 테스트 설정을 통합하여 일관된 테스트 환경 제공
}