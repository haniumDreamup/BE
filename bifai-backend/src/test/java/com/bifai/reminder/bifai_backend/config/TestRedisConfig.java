package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 Redis 설정
 * 테스트 환경에서는 실제 Redis 대신 메모리 기반 캐시 사용
 */
@TestConfiguration
@EnableCaching
@Profile("test")
public class TestRedisConfig {

  @Bean
  @Primary
  public CacheManager testCacheManager() {
    return new ConcurrentMapCacheManager("testCache", "userCache", "tokenCache");
  }
}