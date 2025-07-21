package com.bifai.reminder.bifai_backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐시 설정
 * BIF 서비스를 위한 고성능 캐싱 시스템 구성
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${spring.data.redis.timeout:2000}")
    private long timeout;
    
    @Value("${spring.data.redis.database:0}")
    private int database;
    
    /**
     * Redis 연결 팩토리
     * Lettuce 클라이언트를 사용한 고성능 연결 관리
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(database);
        
        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        
        // Lettuce 클라이언트 설정
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .shutdownTimeout(Duration.ofMillis(timeout))
                .build();
        
        log.info("Redis 연결 설정 - Host: {}, Port: {}, Database: {}", redisHost, redisPort, database);
        
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
    
    /**
     * Redis 템플릿 설정
     * 객체 직렬화를 위한 Jackson 사용
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Jackson 직렬화 설정
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = 
                new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 키는 String으로, 값은 JSON으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 캐시 매니저 설정
     * 다양한 캐시 영역별 TTL 설정
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(createObjectMapper())));
        
        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 사용자 정보 캐시 (30분)
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 리마인더 캐시 (5분)
        cacheConfigurations.put("reminders", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 위치 정보 캐시 (1분)
        cacheConfigurations.put("locations", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        // 일정 캐시 (10분)
        cacheConfigurations.put("schedules", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 설정 캐시 (1시간)
        cacheConfigurations.put("settings", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 토큰 캐시 (액세스 토큰 만료 시간과 동일)
        cacheConfigurations.put("tokens", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        log.info("Redis 캐시 매니저 초기화 - 캐시 영역: {}", cacheConfigurations.keySet());
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
    
    /**
     * ObjectMapper 생성 및 설정
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 시간 모듈 등록
        objectMapper.registerModule(new JavaTimeModule());
        
        // 타입 정보 포함 설정 (다형성 지원)
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        return objectMapper;
    }
    
}