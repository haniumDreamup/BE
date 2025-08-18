package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 기반 실시간 패턴 집계 서비스
 * 사용자 행동을 실시간으로 집계하고 분석
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimePatternAggregationService {
  
  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;
  
  private static final String KEY_PREFIX = "pattern:realtime:";
  private static final String CLICK_COUNT_KEY = KEY_PREFIX + "clicks:";
  private static final String PAGE_VIEW_KEY = KEY_PREFIX + "pageviews:";
  private static final String ERROR_COUNT_KEY = KEY_PREFIX + "errors:";
  private static final String FEATURE_USE_KEY = KEY_PREFIX + "features:";
  private static final String SESSION_KEY = KEY_PREFIX + "sessions:";
  private static final String NAVIGATION_KEY = KEY_PREFIX + "navigation:";
  private static final String HOURLY_STATS_KEY = KEY_PREFIX + "hourly:";
  
  /**
   * 클릭 이벤트 집계
   */
  @Async("taskExecutor")
  public CompletableFuture<Void> aggregateClick(Long userId, String sessionId, String element) {
    try {
      String userKey = CLICK_COUNT_KEY + userId;
      String hourKey = getCurrentHourKey();
      
      // 전체 클릭 카운트 증가
      redisTemplate.opsForValue().increment(userKey);
      
      // 시간별 클릭 카운트
      redisTemplate.opsForHash().increment(userKey + ":hourly", hourKey, 1);
      
      // 클릭 요소별 집계
      redisTemplate.opsForZSet().incrementScore(userKey + ":elements", element, 1);
      
      // TTL 설정 (24시간)
      redisTemplate.expire(userKey, 24, TimeUnit.HOURS);
      
      log.debug("Click aggregated for user: {}, element: {}", userId, element);
      
    } catch (Exception e) {
      log.error("Failed to aggregate click for user: {}", userId, e);
    }
    
    return CompletableFuture.completedFuture(null);
  }
  
  /**
   * 페이지 뷰 집계
   */
  @Async("taskExecutor")
  public CompletableFuture<Void> aggregatePageView(Long userId, String sessionId, String pageUrl) {
    try {
      String userKey = PAGE_VIEW_KEY + userId;
      String timestamp = String.valueOf(System.currentTimeMillis());
      
      // 페이지 뷰 카운트
      redisTemplate.opsForValue().increment(userKey + ":count");
      
      // 페이지별 방문 횟수
      redisTemplate.opsForZSet().incrementScore(userKey + ":pages", pageUrl, 1);
      
      // 네비게이션 경로 저장 (시간순)
      String navKey = NAVIGATION_KEY + userId + ":" + sessionId;
      redisTemplate.opsForZSet().add(navKey, pageUrl, Double.parseDouble(timestamp));
      
      // 세션 활동 시간 업데이트
      updateSessionActivity(userId, sessionId);
      
      // TTL 설정
      redisTemplate.expire(userKey + ":count", 24, TimeUnit.HOURS);
      redisTemplate.expire(userKey + ":pages", 24, TimeUnit.HOURS);
      redisTemplate.expire(navKey, 2, TimeUnit.HOURS);
      
    } catch (Exception e) {
      log.error("Failed to aggregate page view for user: {}", userId, e);
    }
    
    return CompletableFuture.completedFuture(null);
  }
  
  /**
   * 에러 집계
   */
  @Async("taskExecutor")
  public CompletableFuture<Void> aggregateError(Long userId, String errorType, String details) {
    try {
      String userKey = ERROR_COUNT_KEY + userId;
      String hourKey = getCurrentHourKey();
      
      // 전체 에러 카운트
      redisTemplate.opsForValue().increment(userKey + ":total");
      
      // 에러 타입별 카운트
      redisTemplate.opsForHash().increment(userKey + ":types", errorType, 1);
      
      // 시간별 에러 발생
      redisTemplate.opsForHash().increment(userKey + ":hourly", hourKey, 1);
      
      // 최근 에러 저장 (최대 100개)
      String errorJson = objectMapper.writeValueAsString(Map.of(
        "type", errorType,
        "details", details,
        "timestamp", System.currentTimeMillis()
      ));
      
      redisTemplate.opsForList().leftPush(userKey + ":recent", errorJson);
      redisTemplate.opsForList().trim(userKey + ":recent", 0, 99);
      
      // TTL 설정
      redisTemplate.expire(userKey + ":total", 24, TimeUnit.HOURS);
      redisTemplate.expire(userKey + ":types", 24, TimeUnit.HOURS);
      redisTemplate.expire(userKey + ":recent", 6, TimeUnit.HOURS);
      
    } catch (Exception e) {
      log.error("Failed to aggregate error for user: {}", userId, e);
    }
    
    return CompletableFuture.completedFuture(null);
  }
  
  /**
   * 기능 사용 집계
   */
  @Async("taskExecutor")
  public CompletableFuture<Void> aggregateFeatureUse(Long userId, String feature, Map<String, Object> metadata) {
    try {
      String userKey = FEATURE_USE_KEY + userId;
      
      // 기능별 사용 횟수
      redisTemplate.opsForZSet().incrementScore(userKey + ":usage", feature, 1);
      
      // 최근 사용 기능 (시간 기반)
      double timestamp = System.currentTimeMillis();
      redisTemplate.opsForZSet().add(userKey + ":recent", feature, timestamp);
      
      // 기능별 메타데이터 저장
      if (metadata != null && !metadata.isEmpty()) {
        String metaJson = objectMapper.writeValueAsString(metadata);
        redisTemplate.opsForHash().put(userKey + ":metadata", feature, metaJson);
      }
      
      // TTL 설정
      redisTemplate.expire(userKey + ":usage", 24, TimeUnit.HOURS);
      redisTemplate.expire(userKey + ":recent", 6, TimeUnit.HOURS);
      
    } catch (Exception e) {
      log.error("Failed to aggregate feature use for user: {}", userId, e);
    }
    
    return CompletableFuture.completedFuture(null);
  }
  
  /**
   * 세션 활동 업데이트
   */
  private void updateSessionActivity(Long userId, String sessionId) {
    String sessionKey = SESSION_KEY + userId + ":" + sessionId;
    long now = System.currentTimeMillis();
    
    // 세션 시작 시간
    redisTemplate.opsForHash().putIfAbsent(sessionKey, "start", String.valueOf(now));
    
    // 최근 활동 시간
    redisTemplate.opsForHash().put(sessionKey, "lastActivity", String.valueOf(now));
    
    // 활동 카운트
    redisTemplate.opsForHash().increment(sessionKey, "activityCount", 1);
    
    // TTL 설정 (2시간)
    redisTemplate.expire(sessionKey, 2, TimeUnit.HOURS);
  }
  
  /**
   * 실시간 통계 조회
   */
  public Map<String, Object> getRealtimeStats(Long userId) {
    Map<String, Object> stats = new HashMap<>();
    
    try {
      // 클릭 통계
      String clickCount = redisTemplate.opsForValue().get(CLICK_COUNT_KEY + userId);
      stats.put("totalClicks", clickCount != null ? Long.parseLong(clickCount) : 0);
      
      // 페이지 뷰 통계
      String pageViewCount = redisTemplate.opsForValue().get(PAGE_VIEW_KEY + userId + ":count");
      stats.put("totalPageViews", pageViewCount != null ? Long.parseLong(pageViewCount) : 0);
      
      // Top 페이지
      Set<ZSetOperations.TypedTuple<String>> topPages = 
        redisTemplate.opsForZSet().reverseRangeWithScores(PAGE_VIEW_KEY + userId + ":pages", 0, 4);
      
      List<Map<String, Object>> topPagesList = topPages.stream()
        .map(tuple -> {
          Map<String, Object> pageMap = new HashMap<>();
          pageMap.put("page", tuple.getValue());
          pageMap.put("visits", tuple.getScore().intValue());
          return pageMap;
        })
        .collect(Collectors.toList());
      stats.put("topPages", topPagesList);
      
      // 에러 통계
      String errorCount = redisTemplate.opsForValue().get(ERROR_COUNT_KEY + userId + ":total");
      stats.put("totalErrors", errorCount != null ? Long.parseLong(errorCount) : 0);
      
      // Top 기능
      Set<ZSetOperations.TypedTuple<String>> topFeatures = 
        redisTemplate.opsForZSet().reverseRangeWithScores(FEATURE_USE_KEY + userId + ":usage", 0, 4);
      
      List<Map<String, Object>> topFeaturesList = topFeatures.stream()
        .map(tuple -> {
          Map<String, Object> featureMap = new HashMap<>();
          featureMap.put("feature", tuple.getValue());
          featureMap.put("usage", tuple.getScore().intValue());
          return featureMap;
        })
        .collect(Collectors.toList());
      stats.put("topFeatures", topFeaturesList);
      
      // 시간별 활동
      Map<Object, Object> hourlyClicks = 
        redisTemplate.opsForHash().entries(CLICK_COUNT_KEY + userId + ":hourly");
      stats.put("hourlyActivity", hourlyClicks);
      
    } catch (Exception e) {
      log.error("Failed to get realtime stats for user: {}", userId, e);
    }
    
    return stats;
  }
  
  /**
   * 네비게이션 경로 조회
   */
  public List<String> getNavigationPath(Long userId, String sessionId) {
    try {
      String navKey = NAVIGATION_KEY + userId + ":" + sessionId;
      Set<String> pages = redisTemplate.opsForZSet().range(navKey, 0, -1);
      return new ArrayList<>(pages);
    } catch (Exception e) {
      log.error("Failed to get navigation path for user: {}, session: {}", userId, sessionId, e);
      return new ArrayList<>();
    }
  }
  
  /**
   * 세션 정보 조회
   */
  public Map<String, Object> getSessionInfo(Long userId, String sessionId) {
    try {
      String sessionKey = SESSION_KEY + userId + ":" + sessionId;
      Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
      
      if (sessionData.isEmpty()) {
        return null;
      }
      
      Map<String, Object> sessionInfo = new HashMap<>();
      sessionInfo.put("sessionId", sessionId);
      sessionInfo.put("startTime", sessionData.get("start"));
      sessionInfo.put("lastActivity", sessionData.get("lastActivity"));
      sessionInfo.put("activityCount", sessionData.get("activityCount"));
      
      // 세션 지속 시간 계산
      if (sessionData.get("start") != null && sessionData.get("lastActivity") != null) {
        long start = Long.parseLong(sessionData.get("start").toString());
        long last = Long.parseLong(sessionData.get("lastActivity").toString());
        sessionInfo.put("duration", (last - start) / 1000); // 초 단위
      }
      
      return sessionInfo;
      
    } catch (Exception e) {
      log.error("Failed to get session info for user: {}, session: {}", userId, sessionId, e);
      return null;
    }
  }
  
  /**
   * 실시간 이상 패턴 감지
   */
  public boolean detectRealtimeAnomaly(Long userId) {
    try {
      Map<String, Object> stats = getRealtimeStats(userId);
      
      // 에러율 체크
      Long errors = (Long) stats.get("totalErrors");
      Long pageViews = (Long) stats.get("totalPageViews");
      
      if (pageViews > 0 && errors > 0) {
        double errorRate = (double) errors / pageViews * 100;
        if (errorRate > 10) { // 10% 이상 에러율
          log.warn("High error rate detected for user: {}, rate: {}%", userId, errorRate);
          return true;
        }
      }
      
      // 클릭 빈도 체크 (1분당 100회 이상)
      Long clicks = (Long) stats.get("totalClicks");
      if (clicks > 100) {
        log.warn("Abnormal click frequency detected for user: {}, clicks: {}/min", userId, clicks);
        return true;
      }
      
      return false;
      
    } catch (Exception e) {
      log.error("Failed to detect realtime anomaly for user: {}", userId, e);
      return false;
    }
  }
  
  /**
   * 현재 시간 키 생성
   */
  private String getCurrentHourKey() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));
  }
  
  /**
   * 집계 데이터 정리 (오래된 데이터 삭제)
   */
  public void cleanupOldData(Long userId, int hoursToKeep) {
    try {
      // 패턴별 키 목록
      List<String> patterns = Arrays.asList(
        CLICK_COUNT_KEY + userId + "*",
        PAGE_VIEW_KEY + userId + "*",
        ERROR_COUNT_KEY + userId + "*",
        FEATURE_USE_KEY + userId + "*",
        SESSION_KEY + userId + "*",
        NAVIGATION_KEY + userId + "*"
      );
      
      for (String pattern : patterns) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
          for (String key : keys) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.HOURS);
            if (ttl == null || ttl > hoursToKeep) {
              redisTemplate.expire(key, hoursToKeep, TimeUnit.HOURS);
            }
          }
        }
      }
      
      log.info("Cleaned up old aggregation data for user: {}", userId);
      
    } catch (Exception e) {
      log.error("Failed to cleanup old data for user: {}", userId, e);
    }
  }
}