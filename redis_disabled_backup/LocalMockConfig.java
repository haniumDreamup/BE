package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 로컬 환경용 Mock 설정
 * 
 * <p>Redis가 없는 로컬 환경에서 사용할 Dummy 구현체들을 제공합니다.</p>
 */
@Configuration
@Profile("local")
public class LocalMockConfig {

  @Bean
  @Primary
  public RedisTemplate<String, Object> mockRedisTemplate() {
    return new MockRedisTemplate();
  }

  @Bean
  @Primary
  public RedisCacheService dummyRedisCacheService() {
    return new DummyRedisCacheService(mockRedisTemplate());
  }
  
  /**
   * Mock RedisTemplate for local environment
   */
  private static class MockRedisTemplate extends RedisTemplate<String, Object> {
    // Minimal implementation to satisfy dependencies
  }
  
  /**
   * Redis 없이 동작하는 더미 캐시 서비스
   */
  public static class DummyRedisCacheService extends RedisCacheService {
    
    public DummyRedisCacheService(RedisTemplate<String, Object> redisTemplate) {
      super(redisTemplate);
    }
    
    @Override
    public void put(String key, Object value) {
      // 로컬 환경에서는 캐싱하지 않음
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
      // 로컬 환경에서는 캐싱하지 않음
    }
    
    @Override
    public <T> T get(String key, Class<T> type) {
      return null;
    }
    
    @Override
    public boolean exists(String key) {
      return false;
    }
    
    @Override
    public boolean delete(String key) {
      return true;
    }
    
    @Override
    public long delete(Collection<String> keys) {
      return keys.size();
    }
    
    @Override
    public long deleteByPattern(String pattern) {
      return 0;
    }
    
    @Override
    public long getExpire(String key) {
      return -1;
    }
    
    @Override
    public boolean expire(String key, Duration duration) {
      return true;
    }
    
    @Override
    public long size(String pattern) {
      return 0;
    }
    
    @Override
    public void flushAll() {
      // 로컬 환경에서는 아무것도 하지 않음
    }
    
    @Override
    public void warmCache(String key, Object value, Duration ttl) {
      // 로컬 환경에서는 아무것도 하지 않음
    }
    
    @Override
    public Set<String> getActiveTokens() {
      return Collections.emptySet();
    }
    
    @Override
    public void revokeUserTokens(Long userId) {
      // 로컬 환경에서는 아무것도 하지 않음
    }
  }
}