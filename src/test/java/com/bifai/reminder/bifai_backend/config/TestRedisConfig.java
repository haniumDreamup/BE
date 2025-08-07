package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 테스트용 Redis 설정
 * 
 * <p>테스트 환경에서 Redis 연결을 모킹합니다.</p>
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfig {

  @Bean
  @Primary
  public RedisConnectionFactory testRedisConnectionFactory() {
    // 테스트용 임베디드 Redis나 모킹된 커넥션 팩토리 사용
    LettuceConnectionFactory factory = new LettuceConnectionFactory();
    factory.setValidateConnection(false);
    return factory;
  }

  @Bean
  @Primary  
  public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    return template;
  }
}