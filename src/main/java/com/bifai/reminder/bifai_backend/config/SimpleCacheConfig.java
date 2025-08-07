package com.bifai.reminder.bifai_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 간단한 캐시 설정 - Redis 없이 동작
 */
@Configuration
@EnableCaching
public class SimpleCacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}