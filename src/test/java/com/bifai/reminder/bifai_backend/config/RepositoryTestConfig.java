package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Repository 슬라이스 테스트용 설정
 * @DataJpaTest와 함께 사용
 */
@TestConfiguration
@Profile("test")
public class RepositoryTestConfig {
  // @DataJpaTest가 자동으로 H2 DB와 TestEntityManager를 제공
  // 추가 설정이 필요한 경우 여기에 추가
}