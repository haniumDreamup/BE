package com.bifai.reminder.bifai_backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

/**
 * Redis 캐시 설정
 * 성능 최적화를 위한 캐싱 전략 구현
 */
@Configuration
@EnableCaching
@Profile("!test")
@Slf4j
public class RedisCacheConfig {
  
  /**
   * Redis 캐시 매니저 설정
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
      .entryTtl(Duration.ofMinutes(30)) // 기본 TTL 30분
      .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
      .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
      .disableCachingNullValues(); // null 값 캐싱 비활성화
    
    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(defaultConfig)
      // 캐시별 개별 설정
      .withCacheConfiguration("users", 
        defaultConfig.entryTtl(Duration.ofHours(2))) // 사용자 정보: 2시간
      .withCacheConfiguration("medications", 
        defaultConfig.entryTtl(Duration.ofHours(1))) // 복약 정보: 1시간
      .withCacheConfiguration("dailySummary", 
        defaultConfig.entryTtl(Duration.ofMinutes(15))) // 일일 요약: 15분
      .withCacheConfiguration("weeklySummary", 
        defaultConfig.entryTtl(Duration.ofHours(3))) // 주간 요약: 3시간
      .withCacheConfiguration("dashboard", 
        defaultConfig.entryTtl(Duration.ofMinutes(5))) // 대시보드: 5분
      .withCacheConfiguration("voiceGuidance", 
        defaultConfig.entryTtl(Duration.ofHours(24))) // 음성 안내: 24시간
      .withCacheConfiguration("activityPatterns", 
        defaultConfig.entryTtl(Duration.ofHours(6))) // 활동 패턴: 6시간
      .withCacheConfiguration("locationHistory", 
        defaultConfig.entryTtl(Duration.ofMinutes(10))) // 위치 기록: 10분
      .withCacheConfiguration("schedules", 
        defaultConfig.entryTtl(Duration.ofMinutes(30))) // 일정: 30분
      .withCacheConfiguration("guardianPermissions", 
        defaultConfig.entryTtl(Duration.ofHours(1))) // 보호자 권한: 1시간
      .build();
  }
  
  /**
   * Redis Template 설정
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    // Key Serializer
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    
    // Value Serializer
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);
    
    template.afterPropertiesSet();
    
    log.info("Redis Template 설정 완료");
    return template;
  }
  
  /**
   * 캐시 매니저 커스터마이저
   */
  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return (builder) -> {
      log.info("Redis 캐시 매니저 커스터마이징 적용");
      
      // 트랜잭션 지원 활성화
      builder.transactionAware();
      
      // 캐시 통계 활성화 (필요시)
      builder.enableStatistics();
    };
  }
  
  /**
   * ObjectMapper 설정
   */
  private ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    
    // Java 8 시간 타입 지원
    mapper.registerModule(new JavaTimeModule());
    
    // 타입 정보 포함 (역직렬화를 위해)
    mapper.activateDefaultTyping(
      BasicPolymorphicTypeValidator.builder()
        .allowIfSubType(Object.class)
        .build(),
      ObjectMapper.DefaultTyping.NON_FINAL,
      JsonTypeInfo.As.PROPERTY
    );
    
    return mapper;
  }
  
  /**
   * 캐시 키 생성 전략
   */
  @Bean
  public CacheKeyGenerator cacheKeyGenerator() {
    return new CacheKeyGenerator();
  }
  
  /**
   * 커스텀 캐시 키 생성기
   */
  public static class CacheKeyGenerator {
    
    /**
     * 사용자별 캐시 키 생성
     */
    public String userCacheKey(Long userId, String suffix) {
      return String.format("user:%d:%s", userId, suffix);
    }
    
    /**
     * 날짜별 캐시 키 생성
     */
    public String dateCacheKey(Long userId, String date, String type) {
      return String.format("date:%d:%s:%s", userId, date, type);
    }
    
    /**
     * 복약 캐시 키 생성
     */
    public String medicationCacheKey(Long userId, Long medicationId) {
      return String.format("med:%d:%d", userId, medicationId);
    }
    
    /**
     * 활동 캐시 키 생성
     */
    public String activityCacheKey(Long userId, String period) {
      return String.format("activity:%d:%s", userId, period);
    }
  }
}