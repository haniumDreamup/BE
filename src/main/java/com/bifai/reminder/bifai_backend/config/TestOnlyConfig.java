package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

/**
 * 테스트 프로파일 전용 최소 설정
 * Spring Boot Auto Configuration을 방해하지 않으면서 필수 Bean만 제공
 */
@Configuration
@Profile("test")
@EnableCaching
public class TestOnlyConfig {

  /**
   * 테스트용 PasswordEncoder - Spring Security 자동 설정 지원
   */
  @Bean
  @Primary
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * 테스트용 CacheManager - Redis 대신 메모리 캐시
   */
  @Bean
  @Primary
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager();
  }

  /**
   * 테스트용 AuthenticationManager
   */
  @Bean
  @Primary
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * 테스트용 RedisTemplate - Redis 없이 동작하는 Mock 구현체
   */
  @Bean
  @ConditionalOnMissingBean
  @Primary
  public RedisTemplate<String, Object> redisTemplate() {
    return new RedisTemplate<String, Object>() {
      @Override
      public void afterPropertiesSet() {
        // Redis 연결 없이 초기화
      }
    };
  }

  /**
   * 테스트용 StringRedisTemplate - Redis 없이 동작하는 Mock 구현체
   */
  @Bean
  @ConditionalOnMissingBean(name = "stringRedisTemplate")
  @Primary
  public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
    return new org.springframework.data.redis.core.StringRedisTemplate() {
      @Override
      public void afterPropertiesSet() {
        // Redis 연결 없이 초기화
      }
    };
  }

  /**
   * 테스트용 RedisCacheService - 메모리 기반 Mock 구현체
   */
  @Bean  
  @ConditionalOnMissingBean(RedisCacheService.class)
  @Primary
  public RedisCacheService mockRedisCacheService() {
    return new RedisCacheService(null) {
      private final java.util.concurrent.ConcurrentHashMap<String, Object> cache = 
          new java.util.concurrent.ConcurrentHashMap<>();

      @Override
      public void set(String key, Object value, long timeout, TimeUnit unit) {
        cache.put(key, value);
      }

      @Override
      public Object get(String key) {
        return cache.get(key);
      }

      @Override
      public void delete(String key) {
        cache.remove(key);
      }

      @Override
      public boolean hasKey(String key) {
        return cache.containsKey(key);
      }
    };
  }

}