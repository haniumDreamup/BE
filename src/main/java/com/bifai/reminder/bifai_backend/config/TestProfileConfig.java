package com.bifai.reminder.bifai_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test 프로파일 전용 설정
 * 메인 애플리케이션을 test 프로파일로 실행할 때 사용되는 기본 설정
 */
@Configuration
@Profile("test")
@EnableCaching
public class TestProfileConfig {
  
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
      "interactionPatterns",
      "voiceGuidance",
      "activityPatterns",
      "locationHistory",
      "schedules",
      "guardianPermissions"
    );
  }
  
  // ===== Security Configuration =====
  @Bean
  @Primary
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
  
  @Bean
  @Primary
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }
  
  // ===== Simple Redis Mock Configuration for Test Profile =====
  @Bean
  @Primary
  public RedisTemplate<String, Object> redisTemplate() {
    // Create a simple mock that provides basic functionality for testing
    RedisTemplate<String, Object> mockTemplate = new RedisTemplate<String, Object>() {
      private final Map<String, Object> inMemoryStore = new java.util.concurrent.ConcurrentHashMap<>();
      
      @Override
      public org.springframework.data.redis.core.ValueOperations<String, Object> opsForValue() {
        return new SimpleValueOperations();
      }
      
      @Override
      public org.springframework.data.redis.core.ListOperations<String, Object> opsForList() {
        return new SimpleListOperations();
      }
      
      @Override
      public Boolean hasKey(String key) {
        return inMemoryStore.containsKey(key);
      }
      
      @Override
      public Boolean expire(String key, long timeout, java.util.concurrent.TimeUnit unit) {
        return true; // Always return success for test
      }
      
      @Override
      public Boolean expire(String key, java.time.Duration timeout) {
        return true; // Always return success for test
      }
      
      private class SimpleValueOperations implements org.springframework.data.redis.core.ValueOperations<String, Object> {
        @Override
        public Object get(String key) {
          return inMemoryStore.get(key);
        }
        
        @Override
        public void set(String key, Object value) {
          inMemoryStore.put(key, value);
        }
        
        @Override
        public void set(String key, Object value, long timeout, java.util.concurrent.TimeUnit unit) {
          inMemoryStore.put(key, value); // Ignore timeout in test
        }
        
        @Override
        public void set(String key, Object value, java.time.Duration timeout) {
          inMemoryStore.put(key, value); // Ignore timeout in test
        }
        
        // All other methods return sensible defaults
        @Override public Boolean setIfAbsent(String key, Object value) { return true; }
        @Override public Boolean setIfAbsent(String key, Object value, long timeout, java.util.concurrent.TimeUnit unit) { return true; }
        @Override public Boolean setIfAbsent(String key, Object value, java.time.Duration timeout) { return true; }
        @Override public Boolean setIfPresent(String key, Object value) { return true; }
        @Override public Boolean setIfPresent(String key, Object value, long timeout, java.util.concurrent.TimeUnit unit) { return true; }
        @Override public Boolean setIfPresent(String key, Object value, java.time.Duration timeout) { return true; }
        @Override public void multiSet(Map<? extends String, ? extends Object> map) {}
        @Override public Boolean multiSetIfAbsent(Map<? extends String, ? extends Object> map) { return true; }
        @Override public String get(String key, long start, long end) { return null; }
        @Override public Object getAndDelete(String key) { return inMemoryStore.remove(key); }
        @Override public Object getAndExpire(String key, long timeout, java.util.concurrent.TimeUnit unit) { return get(key); }
        @Override public Object getAndExpire(String key, java.time.Duration timeout) { return get(key); }
        @Override public Object getAndPersist(String key) { return get(key); }
        @Override public Object getAndSet(String key, Object value) { return inMemoryStore.put(key, value); }
        @Override public java.util.List<Object> multiGet(java.util.Collection<String> keys) { return new java.util.ArrayList<>(); }
        @Override public Long increment(String key) { return 1L; }
        @Override public Long increment(String key, long delta) { return delta; }
        @Override public Double increment(String key, double delta) { return delta; }
        @Override public Long decrement(String key) { return 1L; }
        @Override public Long decrement(String key, long delta) { return delta; }
        @Override public Integer append(String key, String value) { return 0; }
        @Override public String get(String key, long start, long end, long timeout, java.util.concurrent.TimeUnit unit) { return null; }
        @Override public Long size(String key) { return 0L; }
        @Override public org.springframework.data.redis.core.RedisOperations<String, Object> getOperations() { return null; }
      }
      
      private class SimpleListOperations implements org.springframework.data.redis.core.ListOperations<String, Object> {
        @Override
        public java.util.List<Object> range(String key, long start, long end) {
          return new java.util.ArrayList<>();
        }
        
        @Override
        public Long rightPush(String key, Object value) {
          return 1L;
        }
        
        @Override
        public Long size(String key) {
          return 0L;
        }
        
        @Override
        public Object leftPop(String key) {
          return null;
        }
        
        // All other methods return sensible defaults
        @Override public Long leftPush(String key, Object value) { return 1L; }
        @Override public Long leftPushAll(String key, Object... values) { return (long)values.length; }
        @Override public Long leftPushAll(String key, java.util.Collection<Object> values) { return (long)values.size(); }
        @Override public Long leftPushIfPresent(String key, Object value) { return 1L; }
        @Override public Long rightPushAll(String key, Object... values) { return (long)values.length; }
        @Override public Long rightPushAll(String key, java.util.Collection<Object> values) { return (long)values.size(); }
        @Override public Long rightPushIfPresent(String key, Object value) { return 1L; }
        @Override public Object rightPop(String key) { return null; }
        @Override public Object leftPop(String key, java.time.Duration timeout) { return null; }
        @Override public Object rightPop(String key, java.time.Duration timeout) { return null; }
        @Override public Object rightPopAndLeftPush(String sourceKey, String destinationKey) { return null; }
        @Override public Object rightPopAndLeftPush(String sourceKey, String destinationKey, java.time.Duration timeout) { return null; }
        @Override public Object rightPopAndLeftPush(String sourceKey, String destinationKey, long timeout, java.util.concurrent.TimeUnit unit) { return null; }
        @Override public void set(String key, long index, Object value) {}
        @Override public Long remove(String key, long count, Object value) { return 0L; }
        @Override public Object index(String key, long index) { return null; }
        @Override public void trim(String key, long start, long end) {}
        @Override public org.springframework.data.redis.core.RedisOperations<String, Object> getOperations() { return null; }
      }
    };
    
    return mockTemplate;
  }
}