package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import jakarta.persistence.EntityManagerFactory;
import static org.mockito.Mockito.mock;

/**
 * WebMvcTest를 위한 테스트 설정
 * JPA 관련 빈을 Mock으로 제공
 */
@TestConfiguration
public class TestMvcConfiguration {

  @Bean
  @Primary
  public EntityManagerFactory entityManagerFactory() {
    return mock(EntityManagerFactory.class);
  }
}