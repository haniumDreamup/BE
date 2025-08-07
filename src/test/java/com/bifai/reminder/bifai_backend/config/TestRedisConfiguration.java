package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestRedisConfiguration {
    
    @Bean
    @Primary
    public RefreshTokenService mockRefreshTokenService() {
        return Mockito.mock(RefreshTokenService.class);
    }

    @Bean
    @Primary
    public RedisCacheService mockRedisCacheService() {
        return Mockito.mock(RedisCacheService.class);
    }

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> testRedisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public StringRedisTemplate testStringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> testStringStringRedisTemplate() {
        return mock(RedisTemplate.class);
    }
}