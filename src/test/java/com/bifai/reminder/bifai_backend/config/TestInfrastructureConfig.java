package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * 테스트 인프라 설정
 * Spring Boot 테스트 베스트 프랙티스에 따라 인프라 관련 Mock 제공
 */
@Configuration
@Profile("test")
@EnableCaching
public class TestInfrastructureConfig {
  
  // ===== Redis Mock Configuration =====
  @Bean
  @Primary
  public RedisConnectionFactory redisConnectionFactory() {
    return mock(RedisConnectionFactory.class);
  }
  
  @Bean
  @Primary 
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    return template;
  }
  
  @Bean
  @Primary
  public StringRedisTemplate stringRedisTemplate() {
    return mock(StringRedisTemplate.class);
  }
  
  // ===== Cache Configuration =====
  @Bean
  @Primary
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager(
      "users",
      "medications", 
      "dailySummary",
      "weeklySummary",
      "dashboard",
      "notification",
      "activityLogs",
      "guardianData",
      "interactionPatterns"
    );
  }
}