package com.bifai.reminder.bifai_backend.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 캐시 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(name = "redisTemplate")
public class RedisCacheService {
  
  private final RedisTemplate<String, Object> redisTemplate;
  
  /**
   * 값 저장
   */
  public void set(String key, Object value, long timeout, TimeUnit unit) {
    try {
      redisTemplate.opsForValue().set(key, value, timeout, unit);
      log.debug("Redis에 값 저장 완료 - key: {}", key);
    } catch (Exception e) {
      log.error("Redis 저장 실패 - key: {}", key, e);
    }
  }
  
  /**
   * 값 조회
   */
  public Object get(String key) {
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("Redis 조회 실패 - key: {}", key, e);
      return null;
    }
  }
  
  /**
   * 값 삭제
   */
  public void delete(String key) {
    try {
      redisTemplate.delete(key);
      log.debug("Redis에서 값 삭제 완료 - key: {}", key);
    } catch (Exception e) {
      log.error("Redis 삭제 실패 - key: {}", key, e);
    }
  }
  
  /**
   * 키 존재 여부 확인
   */
  public boolean hasKey(String key) {
    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    } catch (Exception e) {
      log.error("Redis 키 확인 실패 - key: {}", key, e);
      return false;
    }
  }
}