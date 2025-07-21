package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@TestConfiguration
@Profile("test")
public class TestRedisConfiguration {
    
    @Bean
    @Primary
    public RefreshTokenService mockRefreshTokenService() {
        return Mockito.mock(RefreshTokenService.class);
    }
}