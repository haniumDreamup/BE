package com.bifai.reminder.bifai_backend.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

/**
 * Redis 상태 점검 인디케이터
 * Redis 연결 상태와 성능을 모니터링
 */
@Slf4j
@Component("redisHealth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = false)
public class RedisHealthIndicator implements HealthIndicator {
    
    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, String> stringRedisTemplate;
    
    private static final int WARNING_RESPONSE_TIME_MS = 50;
    private static final int CRITICAL_RESPONSE_TIME_MS = 500;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Redis 연결 테스트
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                // PING 명령 실행
                String pong = connection.ping();
                long pingTime = System.currentTimeMillis() - startTime;
                
                if (!"PONG".equals(pong)) {
                    return builder.down()
                            .withDetail("error", "Invalid PING response: " + pong)
                            .build();
                }
                
                builder.withDetail("ping", pong)
                       .withDetail("pingTimeMs", pingTime);
                
                // Redis 정보 수집
                Properties info = connection.info();
                if (info != null) {
                    addRedisInfo(builder, info);
                }
                
                // 읽기/쓰기 테스트
                performReadWriteTest(builder);
                
                // 메모리 사용량 확인
                checkMemoryUsage(connection, builder);
                
                // 상태 결정
                if (pingTime > CRITICAL_RESPONSE_TIME_MS) {
                    builder.status(new Health.Builder().down().build().getStatus())
                           .withDetail("error", "Critical response time: " + pingTime + "ms");
                } else if (pingTime > WARNING_RESPONSE_TIME_MS) {
                    builder.status(new Health.Builder().up().build().getStatus())
                           .withDetail("warning", "Slow response time: " + pingTime + "ms");
                } else {
                    builder.up();
                }
            }
            
        } catch (Exception e) {
            log.error("Redis 헬스 체크 실패", e);
            return builder.down()
                    .withException(e)
                    .build();
        }
        
        return builder.build();
    }
    
    /**
     * Redis 서버 정보 추가
     */
    private void addRedisInfo(Health.Builder builder, Properties info) {
        // Redis 버전
        String version = info.getProperty("redis_version");
        if (version != null) {
            builder.withDetail("version", version);
        }
        
        // 운영 모드
        String mode = info.getProperty("redis_mode");
        if (mode != null) {
            builder.withDetail("mode", mode);
        }
        
        // 연결된 클라이언트 수
        String connectedClients = info.getProperty("connected_clients");
        if (connectedClients != null) {
            builder.withDetail("connectedClients", connectedClients);
        }
        
        // 사용 메모리
        String usedMemory = info.getProperty("used_memory_human");
        if (usedMemory != null) {
            builder.withDetail("usedMemory", usedMemory);
        }
        
        // 최대 메모리
        String maxMemory = info.getProperty("maxmemory_human");
        if (maxMemory != null && !"0B".equals(maxMemory)) {
            builder.withDetail("maxMemory", maxMemory);
        }
        
        // 업타임
        String uptimeDays = info.getProperty("uptime_in_days");
        if (uptimeDays != null) {
            builder.withDetail("uptimeDays", uptimeDays);
        }
    }
    
    /**
     * 읽기/쓰기 성능 테스트
     */
    private void performReadWriteTest(Health.Builder builder) {
        String testKey = "health:test:" + UUID.randomUUID();
        String testValue = "BIF-HEALTH-CHECK-" + System.currentTimeMillis();
        
        try {
            // 쓰기 테스트
            long writeStart = System.currentTimeMillis();
            stringRedisTemplate.opsForValue().set(testKey, testValue);
            long writeTime = System.currentTimeMillis() - writeStart;
            
            // 읽기 테스트
            long readStart = System.currentTimeMillis();
            String readValue = stringRedisTemplate.opsForValue().get(testKey);
            long readTime = System.currentTimeMillis() - readStart;
            
            // 삭제
            stringRedisTemplate.delete(testKey);
            
            builder.withDetail("writeTimeMs", writeTime)
                   .withDetail("readTimeMs", readTime);
            
            if (!testValue.equals(readValue)) {
                builder.withDetail("dataIntegrityError", "Written and read values do not match");
            }
            
            // 성능 경고
            if (writeTime > 10 || readTime > 10) {
                builder.withDetail("performanceWarning", 
                    String.format("Slow operation detected - Write: %dms, Read: %dms", writeTime, readTime));
            }
            
        } catch (Exception e) {
            builder.withDetail("readWriteTestError", e.getMessage());
        }
    }
    
    /**
     * 메모리 사용량 확인
     */
    private void checkMemoryUsage(RedisConnection connection, Health.Builder builder) {
        try {
            Properties info = connection.info("memory");
            if (info == null) return;
            
            String usedMemoryStr = info.getProperty("used_memory");
            String maxMemoryStr = info.getProperty("maxmemory");
            
            if (usedMemoryStr != null && maxMemoryStr != null && !"0".equals(maxMemoryStr)) {
                long usedMemory = Long.parseLong(usedMemoryStr);
                long maxMemory = Long.parseLong(maxMemoryStr);
                
                double usagePercent = (double) usedMemory / maxMemory * 100;
                builder.withDetail("memoryUsagePercent", String.format("%.1f%%", usagePercent));
                
                if (usagePercent > 90) {
                    builder.withDetail("memoryWarning", "High memory usage: " + String.format("%.1f%%", usagePercent));
                } else if (usagePercent > 95) {
                    builder.withDetail("memoryCritical", "Critical memory usage: " + String.format("%.1f%%", usagePercent));
                }
            }
            
        } catch (Exception e) {
            log.debug("메모리 사용량 확인 실패", e);
        }
    }
}