package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 전체 애플리케이션 설정
 * Redis, Security 등 모든 필수 Bean을 제공
 */
@TestConfiguration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
public class TestApplicationConfig {

  @Bean
  @Primary
  public RedisConnectionFactory testRedisConnectionFactory() {
    // Mock RedisConnectionFactory
    return mock(LettuceConnectionFactory.class);
  }

  @Bean
  @Primary
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(testRedisConnectionFactory());
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    
    // Mock이므로 afterPropertiesSet을 호출하지 않음
    return mock(RedisTemplate.class);
  }

  @Bean
  @Primary
  public StringRedisTemplate stringRedisTemplate() {
    return mock(StringRedisTemplate.class);
  }

  @Bean
  @Primary
  public RedisTemplate<String, String> stringStringRedisTemplate() {
    return mock(RedisTemplate.class);
  }
}