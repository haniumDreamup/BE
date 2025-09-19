package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Repository 슬라이스 테스트용 설정
 * @DataJpaTest와 함께 사용
 */
@TestConfiguration
@Profile("test")
@EnableJpaAuditing(auditorAwareRef = "testAuditorProvider")
public class RepositoryTestConfig {

  /**
   * 테스트용 AuditorAware 구현체
   * 테스트 환경에서는 고정된 사용자 ID(-1L)를 반환
   */
  @Bean
  public AuditorAware<Long> testAuditorProvider() {
    return () -> Optional.of(-1L); // 테스트 시스템 사용자 ID
  }
}