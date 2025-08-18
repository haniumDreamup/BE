package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis 테스트 설정
 * - test 프로파일: Mock 사용 (빠른 단위 테스트)
 * - test-integration 프로파일: 실제 Redis 또는 TestContainers 사용
 */
@TestConfiguration
public class RedisTestConfig {

  /**
   * 단위 테스트용 Mock Redis
   */
  @Bean
  @Primary
  @Profile("test")
  @ConditionalOnProperty(name = "redis.mock.enabled", havingValue = "true", matchIfMissing = true)
  public RedisTemplate<String, Object> mockRedisTemplate() {
    RedisTemplate<String, Object> template = mock(RedisTemplate.class);
    
    // 기본 동작 설정
    when(template.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
    when(template.opsForHash()).thenReturn(mock(org.springframework.data.redis.core.HashOperations.class));
    when(template.opsForList()).thenReturn(mock(org.springframework.data.redis.core.ListOperations.class));
    when(template.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(template.hasKey(anyString())).thenReturn(false);
    
    return template;
  }

  @Bean
  @Primary
  @Profile("test")
  @ConditionalOnProperty(name = "redis.mock.enabled", havingValue = "true", matchIfMissing = true)
  public StringRedisTemplate mockStringRedisTemplate() {
    StringRedisTemplate template = mock(StringRedisTemplate.class);
    
    when(template.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
    when(template.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    when(template.hasKey(anyString())).thenReturn(false);
    
    return template;
  }

  @Bean
  @Primary
  @Profile("test")
  @ConditionalOnProperty(name = "redis.mock.enabled", havingValue = "true", matchIfMissing = true)
  public RedisConnectionFactory mockRedisConnectionFactory() {
    return mock(LettuceConnectionFactory.class);
  }

  /**
   * 통합 테스트용 설정 (추후 TestContainers 추가 가능)
   */
  @Bean
  @Profile("test-integration")
  @ConditionalOnProperty(name = "redis.testcontainers.enabled", havingValue = "true")
  public RedisConnectionFactory testContainersRedisConnectionFactory() {
    // TODO: TestContainers Redis 설정
    // RedisContainer redis = new RedisContainer("redis:7-alpine");
    // redis.start();
    // return new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
    
    // 현재는 Mock 반환
    return mock(LettuceConnectionFactory.class);
  }
}