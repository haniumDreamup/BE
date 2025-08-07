package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 캐시 설정
 * 
 * <p>테스트 환경에서 Redis 캐시 서비스를 모킹합니다.</p>
 */
@TestConfiguration
@Profile("test")
public class TestCacheConfig {

  @Bean
  @Primary
  public RedisCacheService mockRedisCacheService() {
    return Mockito.mock(RedisCacheService.class);
  }
}