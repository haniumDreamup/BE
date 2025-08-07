package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.NotificationService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 테스트 환경 설정
 * 
 * <p>테스트 환경에서 필요한 Mock Bean들을 설정합니다.</p>
 */
@TestConfiguration
public class TestMockConfiguration {

  @Bean
  @Primary
  public RedisCacheService mockRedisCacheService() {
    return Mockito.mock(RedisCacheService.class);
  }

  @Bean
  @Primary
  public RedisTemplate<String, Object> mockRedisTemplate() {
    RedisTemplate<String, Object> redisTemplate = Mockito.mock(RedisTemplate.class);
    ValueOperations<String, Object> valueOperations = Mockito.mock(ValueOperations.class);
    Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    return redisTemplate;
  }

  @Bean
  @Primary
  public NotificationService mockNotificationService() {
    return Mockito.mock(NotificationService.class);
  }
}