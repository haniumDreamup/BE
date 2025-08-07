package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.controller.WebSocketController;
import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocket 간단한 부하 테스트
 * Mock을 사용한 성능 측정
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocket 간단한 부하 테스트")
class WebSocketSimpleLoadTest {
  
  private static final Logger log = LoggerFactory.getLogger(WebSocketSimpleLoadTest.class);

  @Mock
  private WebSocketService webSocketService;

  @InjectMocks
  private WebSocketController webSocketController;

  private final AtomicInteger processedMessages = new AtomicInteger(0);
  private final AtomicLong totalProcessingTime = new AtomicLong(0);
  private final AtomicInteger errors = new AtomicInteger(0);

  @BeforeEach
  void setUp() {
    // 테스트별로 개별 모킹 설정
    processedMessages.set(0);
    totalProcessingTime.set(0);
    errors.set(0);
  }

  @Test
  @DisplayName("동시 메시지 처리 성능 테스트")
  void testConcurrentMessageProcessing() throws Exception {
    // Mock 설정
    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenAnswer(invocation -> {
          Thread.sleep(10); // 실제 처리 시간 시뮬레이션
          processedMessages.incrementAndGet();
          
          return LocationUpdateMessage.builder()
              .userId(1L)
              .username("테스트사용자")
              .latitude(37.5665)
              .longitude(126.9780)
              .timestamp(LocalDateTime.now())
              .message("위치가 업데이트되었습니다")
              .build();
        });

    when(webSocketService.processActivityStatus(anyString(), any(ActivityStatusRequest.class)))
        .thenAnswer(invocation -> {
          Thread.sleep(5); // 실제 처리 시간 시뮬레이션
          processedMessages.incrementAndGet();
          
          return ActivityStatusMessage.builder()
              .userId(1L)
              .username("테스트사용자")
              .status("ACTIVE")
              .statusDescription("활동 중")
              .batteryLevel(75)
              .timestamp(LocalDateTime.now())
              .friendlyMessage("활동 중입니다")
              .build();
        });

    doAnswer(invocation -> {
      Thread.sleep(15); // 긴급 알림 처리 시간
      processedMessages.incrementAndGet();
      return null;
    }).when(webSocketService).broadcastEmergencyAlert(anyString(), any(EmergencyAlertRequest.class));
    
    int threadCount = 20;
    int messagesPerThread = 50;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount * messagesPerThread);
    
    Instant startTime = Instant.now();
    
    try {
      // 20개 스레드에서 각각 50개 메시지 전송
      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        executor.submit(() -> {
          for (int j = 0; j < messagesPerThread; j++) {
            try {
              long messageStartTime = System.currentTimeMillis();
              
              // 다양한 메시지 타입 전송
              if (j % 3 == 0) {
                // 위치 업데이트
                LocationUpdateRequest request = LocationUpdateRequest.builder()
                    .latitude(37.5665 + (threadIndex * 0.001))
                    .longitude(126.9780 + (j * 0.001))
                    .accuracy(10.0f)
                    .speed(1.5f)
                    .activityType("WALKING")
                    .build();
                
                Principal principal = () -> "user" + threadIndex + "@example.com";
                webSocketController.updateLocation(request, principal, null);
                
              } else if (j % 3 == 1) {
                // 활동 상태 업데이트
                ActivityStatusRequest request = ActivityStatusRequest.builder()
                    .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
                    .batteryLevel(75 - j)
                    .heartRate(70 + j % 30)
                    .build();
                
                Principal principal = () -> "user" + threadIndex + "@example.com";
                webSocketController.updateActivityStatus(request, principal);
                
              } else {
                // 긴급 알림
                EmergencyAlertRequest request = EmergencyAlertRequest.builder()
                    .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
                    .message("긴급 상황 발생")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .severityLevel(5)
                    .requiresImmediateAction(true)
                    .build();
                
                Principal principal = () -> "user" + threadIndex + "@example.com";
                webSocketController.sendEmergencyAlert(request, principal);
              }
              
              long messageEndTime = System.currentTimeMillis();
              totalProcessingTime.addAndGet(messageEndTime - messageStartTime);
              
            } catch (Exception e) {
              errors.incrementAndGet();
              log.error("메시지 처리 실패: {}", e.getMessage());
            } finally {
              latch.countDown();
            }
          }
        });
      }
      
      // 모든 메시지 처리 완료 대기
      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
      
      Duration totalTime = Duration.between(startTime, Instant.now());
      
      // 성능 분석
      int totalMessages = threadCount * messagesPerThread;
      log.info("=== 부하 테스트 결과 ===");
      log.info("총 메시지 수: {}", totalMessages);
      log.info("처리된 메시지: {}", processedMessages.get());
      log.info("에러 발생: {}", errors.get());
      log.info("전체 처리 시간: {}ms", totalTime.toMillis());
      log.info("평균 처리 시간: {}ms", totalProcessingTime.get() / processedMessages.get());
      log.info("초당 처리량: {} messages/sec", 
          processedMessages.get() / (totalTime.toMillis() / 1000.0));
      
      // 성능 기준 확인
      assertThat(processedMessages.get()).isEqualTo(totalMessages);
      assertThat(errors.get()).isEqualTo(0);
      assertThat(totalTime.toMillis()).isLessThan(30000); // 30초 이내 완료
      
      double avgProcessingTime = (double) totalProcessingTime.get() / processedMessages.get();
      assertThat(avgProcessingTime).isLessThan(100); // 평균 100ms 이내
      
    } finally {
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("메시지 버스트 처리 테스트")
  void testMessageBurstHandling() throws Exception {
    // Mock 설정
    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenAnswer(invocation -> {
          Thread.sleep(10); // 실제 처리 시간 시뮬레이션
          processedMessages.incrementAndGet();
          
          return LocationUpdateMessage.builder()
              .userId(1L)
              .username("테스트사용자")
              .latitude(37.5665)
              .longitude(126.9780)
              .timestamp(LocalDateTime.now())
              .message("위치가 업데이트되었습니다")
              .build();
        });
    
    int burstSize = 100;
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    Instant burstStart = Instant.now();
    
    // 100개 메시지를 동시에 전송
    for (int i = 0; i < burstSize; i++) {
      final int messageIndex = i;
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          LocationUpdateRequest request = LocationUpdateRequest.builder()
              .latitude(37.5665 + (messageIndex * 0.0001))
              .longitude(126.9780)
              .accuracy(10.0f)
              .speed(2.0f)
              .activityType("RUNNING")
              .build();
          
          Principal principal = () -> "burst-user@example.com";
          webSocketController.updateLocation(request, principal, null);
          
        } catch (Exception e) {
          errors.incrementAndGet();
          throw new CompletionException(e);
        }
      });
      
      futures.add(future);
    }
    
    // 모든 메시지 처리 완료 대기
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .get(10, TimeUnit.SECONDS);
    
    Duration burstDuration = Duration.between(burstStart, Instant.now());
    
    log.info("=== 버스트 테스트 결과 ===");
    log.info("버스트 크기: {}", burstSize);
    log.info("처리 시간: {}ms", burstDuration.toMillis());
    log.info("초당 처리량: {} messages/sec", 
        burstSize / (burstDuration.toMillis() / 1000.0));
    
    // 버스트 처리 성능 확인
    assertThat(processedMessages.get()).isGreaterThanOrEqualTo(burstSize);
    assertThat(errors.get()).isEqualTo(0);
    assertThat(burstDuration.toMillis()).isLessThan(5000); // 5초 이내 처리
  }

  @Test
  @DisplayName("다양한 메시지 타입 동시 처리")
  void testMixedMessageTypes() throws Exception {
    // Mock 설정
    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenAnswer(invocation -> {
          processedMessages.incrementAndGet();
          return LocationUpdateMessage.builder()
              .userId(1L)
              .username("테스트사용자")
              .latitude(37.5665)
              .longitude(126.9780)
              .timestamp(LocalDateTime.now())
              .message("위치가 업데이트되었습니다")
              .build();
        });

    when(webSocketService.processActivityStatus(anyString(), any(ActivityStatusRequest.class)))
        .thenAnswer(invocation -> {
          processedMessages.incrementAndGet();
          return ActivityStatusMessage.builder()
              .userId(1L)
              .username("테스트사용자")
              .status("ACTIVE")
              .statusDescription("활동 중")
              .batteryLevel(75)
              .timestamp(LocalDateTime.now())
              .friendlyMessage("활동 중입니다")
              .build();
        });

    doAnswer(invocation -> {
      processedMessages.incrementAndGet();
      return null;
    }).when(webSocketService).broadcastEmergencyAlert(anyString(), any(EmergencyAlertRequest.class));
    
    when(webSocketService.sendPersonalMessage(anyString(), any()))
        .thenAnswer(invocation -> {
          PersonalMessageRequest request = invocation.getArgument(1);
          return PersonalMessage.builder()
              .messageId(System.currentTimeMillis())
              .content(request.getContent())
              .messageType("TEXT")
              .fromUserId(1L)
              .toUserId(request.getTargetUserId())
              .timestamp(LocalDateTime.now())
              .build();
        });
    
    int iterations = 30;
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(iterations * 4); // 4가지 메시지 타입
    
    AtomicInteger locationMessages = new AtomicInteger(0);
    AtomicInteger activityMessages = new AtomicInteger(0);
    AtomicInteger emergencyMessages = new AtomicInteger(0);
    AtomicInteger personalMessages = new AtomicInteger(0);
    
    Instant startTime = Instant.now();
    
    try {
      for (int i = 0; i < iterations; i++) {
        final int iteration = i;
        
        // 위치 업데이트
        executor.submit(() -> {
          try {
            LocationUpdateRequest request = LocationUpdateRequest.builder()
                .latitude(37.5665)
                .longitude(126.9780 + (iteration * 0.001))
                .build();
            
            Principal principal = () -> "mixed-user@example.com";
            webSocketController.updateLocation(request, principal, null);
            locationMessages.incrementAndGet();
            
          } finally {
            latch.countDown();
          }
        });
        
        // 활동 상태
        executor.submit(() -> {
          try {
            ActivityStatusRequest request = ActivityStatusRequest.builder()
                .status(ActivityStatusRequest.ActivityStatus.values()[iteration % 8])
                .batteryLevel(100 - iteration)
                .build();
            
            Principal principal = () -> "mixed-user@example.com";
            webSocketController.updateActivityStatus(request, principal);
            activityMessages.incrementAndGet();
            
          } finally {
            latch.countDown();
          }
        });
        
        // 긴급 알림
        executor.submit(() -> {
          try {
            EmergencyAlertRequest request = EmergencyAlertRequest.builder()
                .alertType(EmergencyAlertRequest.AlertType.values()[iteration % 5])
                .message("긴급 테스트 " + iteration)
                .severityLevel(iteration % 5 + 1)
                .build();
            
            Principal principal = () -> "mixed-user@example.com";
            webSocketController.sendEmergencyAlert(request, principal);
            emergencyMessages.incrementAndGet();
            
          } finally {
            latch.countDown();
          }
        });
        
        // 개인 메시지
        executor.submit(() -> {
          try {
            PersonalMessageRequest request = PersonalMessageRequest.builder()
                .targetUserId((long) (iteration % 10))
                .content("테스트 메시지 " + iteration)
                .messageType(PersonalMessageRequest.MessageType.TEXT)
                .priority(iteration % 5 + 1)
                .build();
            
            // 개인 메시지 전송
            Principal principal = () -> "mixed-user@example.com";
            webSocketController.sendPersonalMessage(request, principal);
            personalMessages.incrementAndGet();
            
          } finally {
            latch.countDown();
          }
        });
      }
      
      // 모든 메시지 처리 완료 대기
      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
      
      Duration totalTime = Duration.between(startTime, Instant.now());
      
      log.info("=== 혼합 메시지 테스트 결과 ===");
      log.info("위치 업데이트: {}", locationMessages.get());
      log.info("활동 상태: {}", activityMessages.get());
      log.info("긴급 알림: {}", emergencyMessages.get());
      log.info("개인 메시지: {}", personalMessages.get());
      log.info("전체 처리 시간: {}ms", totalTime.toMillis());
      
      // 모든 메시지 타입이 처리되었는지 확인
      assertThat(locationMessages.get()).isEqualTo(iterations);
      assertThat(activityMessages.get()).isEqualTo(iterations);
      assertThat(emergencyMessages.get()).isEqualTo(iterations);
      assertThat(personalMessages.get()).isEqualTo(iterations);
      
    } finally {
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  @DisplayName("메모리 효율성 테스트")
  void testMemoryEfficiency() throws Exception {
    // Mock 설정
    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenAnswer(invocation -> {
          processedMessages.incrementAndGet();
          return LocationUpdateMessage.builder()
              .userId(1L)
              .username("테스트사용자")
              .latitude(37.5665)
              .longitude(126.9780)
              .timestamp(LocalDateTime.now())
              .message("위치가 업데이트되었습니다")
              .build();
        });
    
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();
    
    int messageCount = 1000;
    List<LocationUpdateMessage> responses = new ArrayList<>();
    
    // 1000개 메시지 처리
    for (int i = 0; i < messageCount; i++) {
      LocationUpdateRequest request = LocationUpdateRequest.builder()
          .latitude(37.5665 + (i * 0.00001))
          .longitude(126.9780)
          .accuracy(10.0f)
          .build();
      
      Principal principal = () -> "memory-test@example.com";
      LocationUpdateMessage response = webSocketController.updateLocation(request, principal, null);
      
      if (i % 100 == 0) {
        responses.add(response); // 일부만 보관
      }
    }
    
    System.gc(); // GC 실행 (테스트 목적)
    Thread.sleep(100);
    
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = (finalMemory - initialMemory) / 1024 / 1024; // MB 단위
    
    log.info("=== 메모리 효율성 테스트 결과 ===");
    log.info("처리된 메시지: {}", messageCount);
    log.info("메모리 사용량: {} MB", memoryUsed);
    log.info("메시지당 평균 메모리: {} KB", (memoryUsed * 1024) / messageCount);
    
    // 메모리 사용량이 합리적인지 확인
    assertThat(memoryUsed).isLessThan(100); // 100MB 이하
  }
}