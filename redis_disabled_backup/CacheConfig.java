package com.bifai.reminder.bifai_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 캐시 설정 - Redis 비활성화 시 사용
 * 메모리 기반 캐시 제공
 */
@Configuration
@EnableCaching
@Profile("!redis")
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "users",
            "sessions",
            "tokens",
            "permissions",
            "configs"
        );
    }
}