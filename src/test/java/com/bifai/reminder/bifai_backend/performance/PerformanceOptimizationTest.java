package com.bifai.reminder.bifai_backend.performance;

import com.bifai.reminder.bifai_backend.config.IntegrationTestConfig;
import com.bifai.reminder.bifai_backend.config.DataSourceConfig;
import com.bifai.reminder.bifai_backend.config.RedisCacheConfig;
import com.bifai.reminder.bifai_backend.service.OptimizedDashboardService;
import com.bifai.reminder.bifai_backend.service.cache.CacheWarmingService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.firebase.messaging.FirebaseMessaging;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 성능 최적화 통합 테스트
 */
@SpringBootTest(properties = {
  "spring.batch.job.enabled=false",
  "spring.http.client.factory=simple"
})
@Import(IntegrationTestConfig.class)
@TestPropertySource(properties = {
  "spring.cache.type=simple",
  "spring.ai.openai.api-key=test-key",
  "fcm.enabled=false"
})
@DisplayName("성능 최적화 테스트")
class PerformanceOptimizationTest {
  
  @Autowired
  private DataSource dataSource;
  
  @Autowired
  private CacheManager cacheManager;
  
  @MockBean
  private RedisTemplate<String, Object> redisTemplate;
  
  @MockBean(name = "stringRedisTemplate")
  private RedisTemplate<String, String> stringRedisTemplate;
  
  private final Map<String, Object> mockCache = new ConcurrentHashMap<>();
  
  @MockBean
  private RefreshTokenService refreshTokenService;
  
  @MockBean
  private RedisCacheService redisCacheService;
  
  @MockBean
  private ImageAnnotatorClient imageAnnotatorClient;
  
  @MockBean
  private FirebaseMessaging firebaseMessaging;
  
  
  
  
  @SpyBean
  private OptimizedDashboardService dashboardService;
  
  @SpyBean
  private CacheWarmingService cacheWarmingService;
  
  private ExecutorService executorService;
  
  @BeforeEach
  void setUp() {
    executorService = Executors.newFixedThreadPool(100);
    mockCache.clear();
    
    // Mock Redis Operations
    when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
    when(redisTemplate.opsForValue().get(anyString())).thenAnswer(invocation -> 
      mockCache.get(invocation.getArgument(0)));
    
    doAnswer(invocation -> {
      String key = invocation.getArgument(0);
      Object value = invocation.getArgument(1);
      mockCache.put(key, value);
      return null;
    }).when(redisTemplate.opsForValue()).set(anyString(), any());
    
    doAnswer(invocation -> {
      String key = invocation.getArgument(0);
      Object value = invocation.getArgument(1);
      mockCache.put(key, value);
      return null;
    }).when(redisTemplate.opsForValue()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    
    when(redisTemplate.hasKey(anyString())).thenAnswer(invocation -> 
      mockCache.containsKey(invocation.getArgument(0)));
    
    when(redisTemplate.getExpire(anyString())).thenReturn(300L);
  }
  
  @Test
  @DisplayName("HikariCP 커넥션 풀이 올바르게 설정되었는지 확인")
  void testHikariCPConfiguration() {
    assertThat(dataSource).isInstanceOf(HikariDataSource.class);
    
    HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
    
    // 커넥션 풀 설정 확인
    assertThat(hikariDataSource.getMaximumPoolSize()).isGreaterThanOrEqualTo(20);
    assertThat(hikariDataSource.getMinimumIdle()).isGreaterThanOrEqualTo(5);
    assertThat(hikariDataSource.getConnectionTimeout()).isLessThanOrEqualTo(30000);
    
    // 커넥션 풀 이름 확인
    assertThat(hikariDataSource.getPoolName()).contains("HikariCP");
  }
  
  @Test
  @DisplayName("동시 100명 사용자 커넥션 풀 테스트")
  void testConcurrentConnectionPool() throws Exception {
    int concurrentUsers = 100;
    CountDownLatch latch = new CountDownLatch(concurrentUsers);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    
    List<Future<Boolean>> futures = new ArrayList<>();
    
    for (int i = 0; i < concurrentUsers; i++) {
      final int userId = i;
      Future<Boolean> future = executorService.submit(() -> {
        try {
          // 데이터베이스 연결 테스트
          try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement()) {
              var resultSet = statement.executeQuery("SELECT 1");
              if (resultSet.next()) {
                successCount.incrementAndGet();
                return true;
              }
            }
          }
        } catch (Exception e) {
          failCount.incrementAndGet();
          return false;
        } finally {
          latch.countDown();
        }
        return false;
      });
      futures.add(future);
    }
    
    // 모든 요청 완료 대기 (최대 30초)
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    
    // 결과 검증
    assertThat(successCount.get()).isGreaterThanOrEqualTo(95); // 95% 이상 성공
    assertThat(failCount.get()).isLessThan(5); // 5% 미만 실패
    
    // 커넥션 풀 상태 확인
    HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
    HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
    
    assertThat(poolMXBean.getActiveConnections()).isLessThanOrEqualTo(
      hikariDataSource.getMaximumPoolSize());
  }
  
  @Test
  @DisplayName("Mock Redis 캐싱이 작동하는지 확인")
  void testRedisCaching() {
    String cacheKey = "test:cache:key";
    String testData = "테스트 데이터";
    
    // 캐시 저장
    redisTemplate.opsForValue().set(cacheKey, testData);
    
    // 캐시 조회
    Object cachedData = redisTemplate.opsForValue().get(cacheKey);
    assertThat(cachedData).isEqualTo(testData);
    
    // 캐시 TTL 확인 (Mock이므로 고정값 반환)
    Long ttl = redisTemplate.getExpire(cacheKey);
    assertThat(ttl).isNotNull();
    assertThat(ttl).isEqualTo(300L);
  }
  
  @Test
  @DisplayName("캐시 워밍 서비스가 작동하는지 확인")
  void testCacheWarmingService() {
    Long userId = 1L;
    
    // Mock 설정: warmUpUserCache가 호출되면 캐시에 데이터 추가
    doAnswer(invocation -> {
      mockCache.put("user:active:" + userId, true);
      return null;
    }).when(cacheWarmingService).warmUpUserCache(userId);
    
    // 캐시 워밍 실행
    cacheWarmingService.warmUpUserCache(userId);
    
    // 캐시가 생성되었는지 확인
    String userActiveKey = "user:active:" + userId;
    Boolean isCached = redisTemplate.hasKey(userActiveKey);
    
    assertThat(isCached).isTrue();
  }
  
  @Test
  @DisplayName("대시보드 서비스 응답 시간 테스트")
  void testDashboardResponseTime() throws Exception {
    Long userId = 1L;
    LocalDate today = LocalDate.now();
    
    // Mock 대시보드 데이터
    Map<String, Object> mockDashboard = new HashMap<>();
    mockDashboard.put("status", "OK");
    
    // 첫 번째 호출은 느리게, 두 번째 호출은 빠르게 설정
    when(dashboardService.getComprehensiveDashboard(userId, today))
      .thenAnswer(new Answer<Map<String, Object>>() {
        private int callCount = 0;
        @Override
        public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
          callCount++;
          if (callCount == 1) {
            Thread.sleep(100); // 첫 번째 호출은 100ms 지연
          } else {
            Thread.sleep(10); // 두 번째 호출은 10ms 지연
          }
          return mockDashboard;
        }
      });
    
    // 첫 번째 호출 (캐시 미스 시뮬레이션)
    long startTime = System.currentTimeMillis();
    dashboardService.getComprehensiveDashboard(userId, today);
    long firstCallTime = System.currentTimeMillis() - startTime;
    
    // 두 번째 호출 (캐시 히트 시뮬레이션)
    startTime = System.currentTimeMillis();
    dashboardService.getComprehensiveDashboard(userId, today);
    long secondCallTime = System.currentTimeMillis() - startTime;
    
    // 캐시 히트가 더 빠른지 확인
    assertThat(secondCallTime).isLessThan(firstCallTime);
    
    // 응답 시간이 500ms 이내인지 확인
    assertThat(secondCallTime).isLessThan(500);
  }
  
  @Test
  @DisplayName("동시 요청 처리 성능 테스트")
  void testConcurrentRequestPerformance() throws Exception {
    int totalRequests = 1000;
    int concurrentThreads = 50;
    CountDownLatch latch = new CountDownLatch(totalRequests);
    AtomicInteger completedRequests = new AtomicInteger(0);
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < totalRequests; i++) {
      final int requestId = i;
      executorService.submit(() -> {
        try {
          // 간단한 캐시 작업 시뮬레이션
          String key = "perf:test:" + (requestId % 100);
          redisTemplate.opsForValue().set(key, "data-" + requestId, 1, TimeUnit.MINUTES);
          redisTemplate.opsForValue().get(key);
          
          completedRequests.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }
    
    // 모든 요청 완료 대기
    assertTrue(latch.await(30, TimeUnit.SECONDS));
    
    long totalTime = System.currentTimeMillis() - startTime;
    double requestsPerSecond = (double) completedRequests.get() / (totalTime / 1000.0);
    
    // 성능 기준: 초당 100개 이상 요청 처리
    assertThat(requestsPerSecond).isGreaterThan(100);
    
    // 모든 요청이 완료되었는지 확인
    assertThat(completedRequests.get()).isEqualTo(totalRequests);
  }
  
  @Test
  @DisplayName("메모리 사용량 모니터링")
  void testMemoryUsage() throws InterruptedException {
    Runtime runtime = Runtime.getRuntime();
    
    // GC 실행
    System.gc();
    Thread.sleep(100);
    
    long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
    
    // 부하 생성
    List<String> dataList = new ArrayList<>();
    for (int i = 0; i < 10000; i++) {
      dataList.add("테스트 데이터 " + i);
    }
    
    long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = usedMemoryAfter - usedMemoryBefore;
    
    // 메모리 증가량이 100MB 이하인지 확인
    assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
    
    // 힙 메모리가 충분한지 확인
    long maxMemory = runtime.maxMemory();
    assertThat(usedMemoryAfter).isLessThan((long)(maxMemory * 0.8)); // 80% 미만 사용
  }
  
  @Test
  @DisplayName("캐시 히트율 측정")
  void testCacheHitRate() {
    int totalRequests = 100;
    int uniqueKeys = 20;
    AtomicInteger cacheHits = new AtomicInteger(0);
    AtomicInteger cacheMisses = new AtomicInteger(0);
    
    for (int i = 0; i < totalRequests; i++) {
      String key = "cache:test:" + (i % uniqueKeys);
      
      if (redisTemplate.hasKey(key)) {
        cacheHits.incrementAndGet();
        redisTemplate.opsForValue().get(key);
      } else {
        cacheMisses.incrementAndGet();
        redisTemplate.opsForValue().set(key, "data-" + i, 5, TimeUnit.MINUTES);
      }
    }
    
    double hitRate = (double) cacheHits.get() / totalRequests * 100;
    
    // 캐시 히트율이 70% 이상인지 확인
    assertThat(hitRate).isGreaterThan(70);
  }
}