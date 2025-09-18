package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Repository 슬라이스 테스트용 설정
 * @DataJpaTest와 함께 사용
 */
@TestConfiguration
@Profile("test")
@EnableJpaAuditing
public class RepositoryTestConfig {
  // @DataJpaTest가 자동으로 H2 DB와 TestEntityManager를 제공
  // JPA Auditing을 활성화하여 BaseEntity의 auditing 필드들이 정상 동작하도록 함
}