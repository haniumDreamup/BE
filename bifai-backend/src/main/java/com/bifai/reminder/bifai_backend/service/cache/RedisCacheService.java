package com.bifai.reminder.bifai_backend.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 캐시 서비스
 * BIF 서비스를 위한 중앙 집중식 캐시 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 캐시 저장
     */
    public void put(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("캐시 저장 - Key: {}", key);
        } catch (Exception e) {
            log.error("캐시 저장 실패 - Key: {}", key, e);
        }
    }
    
    /**
     * 캐시 저장 (TTL 포함)
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            log.debug("캐시 저장 - Key: {}, TTL: {}", key, ttl);
        } catch (Exception e) {
            log.error("캐시 저장 실패 - Key: {}", key, e);
        }
    }
    
    /**
     * 캐시 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && type.isInstance(value)) {
                log.debug("캐시 히트 - Key: {}", key);
                return (T) value;
            }
            log.debug("캐시 미스 - Key: {}", key);
            return null;
        } catch (Exception e) {
            log.error("캐시 조회 실패 - Key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("캐시 존재 확인 실패 - Key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 캐시 삭제
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("캐시 삭제 - Key: {}, Result: {}", key, deleted);
            return deleted != null && deleted;
        } catch (Exception e) {
            log.error("캐시 삭제 실패 - Key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 다중 캐시 삭제
     */
    public long delete(Collection<String> keys) {
        try {
            Long deleted = redisTemplate.delete(keys);
            log.debug("다중 캐시 삭제 - Keys: {}, Deleted: {}", keys.size(), deleted);
            return deleted != null ? deleted : 0;
        } catch (Exception e) {
            log.error("다중 캐시 삭제 실패", e);
            return 0;
        }
    }
    
    /**
     * 패턴으로 키 삭제
     */
    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.debug("패턴 캐시 삭제 - Pattern: {}, Deleted: {}", pattern, deleted);
                return deleted != null ? deleted : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("패턴 캐시 삭제 실패 - Pattern: {}", pattern, e);
            return 0;
        }
    }
    
    /**
     * TTL 조회
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("TTL 조회 실패 - Key: {}", key, e);
            return -1;
        }
    }
    
    /**
     * TTL 설정
     */
    public boolean expire(String key, Duration duration) {
        try {
            Boolean result = redisTemplate.expire(key, duration);
            log.debug("TTL 설정 - Key: {}, Duration: {}, Result: {}", key, duration, result);
            return result != null && result;
        } catch (Exception e) {
            log.error("TTL 설정 실패 - Key: {}", key, e);
            return false;
        }
    }
    
    /**
     * 캐시 크기 조회
     */
    public long size(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("캐시 크기 조회 실패 - Pattern: {}", pattern, e);
            return 0;
        }
    }
    
    /**
     * 전체 캐시 초기화 (주의: 운영 환경에서는 사용 금지)
     */
    public void flushAll() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.warn("전체 캐시 초기화 완료 - 삭제된 키: {}", keys.size());
            }
        } catch (Exception e) {
            log.error("전체 캐시 초기화 실패", e);
        }
    }
    
    /**
     * 캐시 워밍 - 미리 캐시를 채워넣는 기능
     */
    public void warmCache(String key, Object value, Duration ttl) {
        if (!exists(key)) {
            put(key, value, ttl);
            log.info("캐시 워밍 완료 - Key: {}", key);
        }
    }
}