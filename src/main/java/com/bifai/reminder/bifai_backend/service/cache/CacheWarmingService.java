package com.bifai.reminder.bifai_backend.service.cache;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.DailyStatusSummaryService;
import com.bifai.reminder.bifai_backend.service.OptimizedDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 캐시 워밍 서비스
 * 자주 접근하는 데이터를 미리 로드하여 성능 향상
 */
@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test && !prod")  // Temporarily disable in production
public class CacheWarmingService {
  
  private final UserRepository userRepository;
  private final OptimizedDashboardService dashboardService;
  private final DailyStatusSummaryService dailySummaryService;
  private final RedisTemplate<String, Object> redisTemplate;
  
  private final ExecutorService executorService = Executors.newFixedThreadPool(4);
  
  /**
   * 애플리케이션 시작 시 캐시 워밍
   */
  @EventListener(ApplicationReadyEvent.class)
  @Async
  public void warmUpCacheOnStartup() {
    log.info("캐시 워밍 시작");
    
    try {
      // 활성 사용자 목록 조회
      List<User> activeUsers = userRepository.findActiveUsersForCaching();
      
      // 병렬로 캐시 워밍 실행
      List<CompletableFuture<Void>> futures = activeUsers.stream()
        .limit(100) // 최대 100명까지만 초기 워밍
        .map(user -> CompletableFuture.runAsync(() -> 
          warmUpUserCache(user.getUserId()), executorService))
        .toList();
      
      // 모든 작업 완료 대기 (최대 30초)
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .get(30, TimeUnit.SECONDS);
      
      log.info("캐시 워밍 완료 - 사용자 수: {}", futures.size());
      
    } catch (Exception e) {
      log.error("캐시 워밍 실패", e);
    }
  }
  
  /**
   * 매일 새벽 4시에 캐시 재워밍
   */
  @Scheduled(cron = "0 0 4 * * *")
  public void scheduledCacheWarming() {
    log.info("정기 캐시 워밍 시작");
    
    try {
      // 오늘 활동이 예상되는 사용자들 캐시 워밍
      List<User> todayActiveUsers = userRepository.findUsersWithTodaySchedule();
      
      for (User user : todayActiveUsers) {
        warmUpUserCache(user.getUserId());
      }
      
      log.info("정기 캐시 워밍 완료 - 사용자 수: {}", todayActiveUsers.size());
      
    } catch (Exception e) {
      log.error("정기 캐시 워밍 실패", e);
    }
  }
  
  /**
   * 특정 사용자의 캐시 워밍
   */
  public void warmUpUserCache(Long userId) {
    try {
      LocalDate today = LocalDate.now();
      
      // 대시보드 데이터 미리 로드
      dashboardService.getComprehensiveDashboard(userId, today);
      
      // 주간 트렌드 미리 로드
      dashboardService.getWeeklyTrend(userId, today);
      
      // 일일 요약 미리 로드 (보호자별로는 실시간 조회)
      // dailySummaryService.getDailySummary(userId, null);
      
      // 자주 사용하는 키 미리 설정
      String userActiveKey = "user:active:" + userId;
      redisTemplate.opsForValue().set(userActiveKey, true, 1, TimeUnit.HOURS);
      
      log.debug("사용자 {} 캐시 워밍 완료", userId);
      
    } catch (Exception e) {
      log.warn("사용자 {} 캐시 워밍 실패: {}", userId, e.getMessage());
    }
  }
  
  /**
   * 캐시 무효화 (특정 사용자)
   */
  public void invalidateUserCache(Long userId) {
    try {
      // 사용자 관련 캐시 키 패턴
      String pattern = String.format("*:%d:*", userId);
      
      // 패턴에 맞는 모든 키 삭제
      var keys = redisTemplate.keys(pattern);
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("사용자 {} 캐시 무효화 - 삭제된 키: {}", userId, keys.size());
      }
      
    } catch (Exception e) {
      log.error("캐시 무효화 실패 - 사용자: {}", userId, e);
    }
  }
  
  /**
   * 캐시 상태 모니터링
   */
  @Scheduled(fixedDelay = 300000) // 5분마다
  public void monitorCacheStatus() {
    try {
      // Redis 연결 상태 확인
      String pong = redisTemplate.getConnectionFactory()
        .getConnection()
        .ping();
      
      if ("PONG".equals(pong)) {
        log.debug("Redis 연결 정상");
      } else {
        log.warn("Redis 연결 이상: {}", pong);
      }
      
      // 캐시 통계 (필요시 구현)
      logCacheStatistics();
      
    } catch (Exception e) {
      log.error("캐시 모니터링 실패", e);
    }
  }
  
  /**
   * 캐시 통계 로깅
   */
  private void logCacheStatistics() {
    try {
      // 전체 키 개수
      var keys = redisTemplate.keys("*");
      int totalKeys = keys != null ? keys.size() : 0;
      
      // 타입별 키 개수
      int userKeys = countKeys("user:*");
      int medicationKeys = countKeys("med:*");
      int dashboardKeys = countKeys("dashboard:*");
      
      log.info("캐시 통계 - 전체: {}, 사용자: {}, 복약: {}, 대시보드: {}", 
        totalKeys, userKeys, medicationKeys, dashboardKeys);
      
    } catch (Exception e) {
      log.debug("캐시 통계 수집 실패: {}", e.getMessage());
    }
  }
  
  /**
   * 특정 패턴의 키 개수 계산
   */
  private int countKeys(String pattern) {
    var keys = redisTemplate.keys(pattern);
    return keys != null ? keys.size() : 0;
  }
  
  /**
   * 종료 시 리소스 정리
   */
  @jakarta.annotation.PreDestroy
  public void cleanup() {
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}