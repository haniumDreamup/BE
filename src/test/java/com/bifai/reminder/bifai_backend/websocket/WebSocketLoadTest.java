package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * WebSocket 부하 테스트
 * 100+ 동시 사용자 접속 및 메시지 처리 성능 측정
 * 
 * 실행 방법: -Dperformance.test=true 옵션 추가
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.bifai.reminder=WARN"  // 부하 테스트 중 로그 레벨 낮춤
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "performance.test", matches = "true")
@DisplayName("WebSocket 부하 테스트")
class WebSocketLoadTest {
  
  private static final Logger log = LoggerFactory.getLogger(WebSocketLoadTest.class);

  @LocalServerPort
  private int port;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private String wsUrl;
  private List<User> testUsers;
  private Map<Long, String> userTokens;
  
  // 성능 메트릭
  private final AtomicInteger successfulConnections = new AtomicInteger(0);
  private final AtomicInteger failedConnections = new AtomicInteger(0);
  private final AtomicLong totalMessagesSent = new AtomicLong(0);
  private final AtomicLong totalMessagesReceived = new AtomicLong(0);
  private final AtomicLong totalLatency = new AtomicLong(0);
  private final AtomicInteger messageCount = new AtomicInteger(0);

  @BeforeEach
  void setUp() {
    // WebSocket 클라이언트 설정
    List<Transport> transports = new ArrayList<>();
    transports.add(new WebSocketTransport(new StandardWebSocketClient()));
    SockJsClient sockJsClient = new SockJsClient(transports);
    
    stompClient = new WebSocketStompClient(sockJsClient);
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    
    // 대용량 메시지 처리를 위한 버퍼 크기 증가
    stompClient.setInboundMessageSizeLimit(64 * 1024);
    
    wsUrl = "ws://localhost:" + port + "/ws-bif";
    
    // 테스트 사용자 및 토큰 생성
    testUsers = new ArrayList<>();
    userTokens = new HashMap<>();
    
    for (int i = 0; i < 150; i++) {
      User user = userRepository.save(User.builder()
          .username("부하테스트사용자" + i)
          .email("loadtest" + i + "@example.com")
          .phoneNumber("010-" + String.format("%04d", i) + "-" + String.format("%04d", i))
          .isActive(true)
          .build());
      
      testUsers.add(user);
      
      // JWT 토큰 생성
      UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
          user.getEmail(),
          null,
          Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
      );
      String token = jwtTokenProvider.generateAccessToken(auth);
      userTokens.put(user.getUserId(), token);
    }
    
    log.info("부하 테스트 준비 완료 - 사용자 수: {}", testUsers.size());
  }

  @AfterEach
  void tearDown() {
    if (stompClient != null) {
      stompClient.stop();
    }
    
    // 성능 메트릭 출력
    log.info("=== 부하 테스트 결과 ===");
    log.info("성공한 연결: {}", successfulConnections.get());
    log.info("실패한 연결: {}", failedConnections.get());
    log.info("전송된 메시지: {}", totalMessagesSent.get());
    log.info("수신된 메시지: {}", totalMessagesReceived.get());
    if (messageCount.get() > 0) {
      log.info("평균 응답 시간: {}ms", totalLatency.get() / messageCount.get());
    }
  }

  @Test
  @Order(1)
  @DisplayName("100명 동시 접속 테스트")
  void test100ConcurrentConnections() throws Exception {
    int connectionCount = 100;
    CountDownLatch connectionLatch = new CountDownLatch(connectionCount);
    List<StompSession> sessions = new CopyOnWriteArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(20);
    
    Instant startTime = Instant.now();
    
    try {
      // 100명 동시 접속 시도
      for (int i = 0; i < connectionCount; i++) {
        final int userIndex = i;
        executor.submit(() -> {
          try {
            User user = testUsers.get(userIndex);
            String token = userTokens.get(user.getUserId());
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            
            StompSession session = stompClient.connectAsync(wsUrl, headers,
                new LoadTestSessionHandler(user.getUserId())).get(5, TimeUnit.SECONDS);
            
            sessions.add(session);
            successfulConnections.incrementAndGet();
            connectionLatch.countDown();
            
          } catch (Exception e) {
            failedConnections.incrementAndGet();
            connectionLatch.countDown();
            log.error("사용자 {} 연결 실패: {}", userIndex, e.getMessage());
          }
        });
      }
      
      // 모든 연결 완료 대기
      boolean completed = connectionLatch.await(30, TimeUnit.SECONDS);
      assertThat(completed).isTrue();
      
      Duration connectionTime = Duration.between(startTime, Instant.now());
      log.info("100명 연결 완료 시간: {}ms", connectionTime.toMillis());
      
      // 연결 성공률 확인
      assertThat(successfulConnections.get()).isGreaterThanOrEqualTo(95); // 95% 이상 성공
      
      // 연결된 세션들 정리
      sessions.forEach(StompSession::disconnect);
      
    } finally {
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  @Order(2)
  @DisplayName("대량 메시지 처리 성능 테스트")
  void testHighVolumeMessageProcessing() throws Exception {
    int userCount = 50;
    int messagesPerUser = 20;
    CountDownLatch messageLatch = new CountDownLatch(userCount * messagesPerUser);
    List<StompSession> sessions = new CopyOnWriteArrayList<>();
    Map<Long, List<Long>> latencyMap = new ConcurrentHashMap<>();
    
    ExecutorService connectionExecutor = Executors.newFixedThreadPool(10);
    ExecutorService messageExecutor = Executors.newFixedThreadPool(20);
    
    try {
      // 50명 연결
      CountDownLatch connectionLatch = new CountDownLatch(userCount);
      for (int i = 0; i < userCount; i++) {
        final int userIndex = i;
        connectionExecutor.submit(() -> {
          try {
            User user = testUsers.get(userIndex);
            String token = userTokens.get(user.getUserId());
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            
            StompSession session = stompClient.connectAsync(wsUrl, headers,
                new LoadTestSessionHandler(user.getUserId())).get(5, TimeUnit.SECONDS);
            
            // 위치 업데이트 구독
            session.subscribe("/topic/location/" + user.getUserId(), new StompFrameHandler() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return LocationUpdateMessage.class;
              }
              
              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                totalMessagesReceived.incrementAndGet();
                messageLatch.countDown();
                
                LocationUpdateMessage message = (LocationUpdateMessage) payload;
                if (message.getTimestamp() != null) {
                  long latency = System.currentTimeMillis() - 
                      message.getTimestamp().toInstant(java.time.ZoneOffset.of("+09:00")).toEpochMilli();
                  totalLatency.addAndGet(latency);
                  messageCount.incrementAndGet();
                  
                  latencyMap.computeIfAbsent(message.getUserId(), k -> new ArrayList<>()).add(latency);
                }
              }
            });
            
            sessions.add(session);
            connectionLatch.countDown();
            
          } catch (Exception e) {
            connectionLatch.countDown();
            log.error("연결 실패: {}", e.getMessage());
          }
        });
      }
      
      connectionLatch.await(10, TimeUnit.SECONDS);
      log.info("{}명 연결 완료", sessions.size());
      
      Thread.sleep(1000); // 구독 안정화 대기
      
      // 각 사용자가 20개씩 메시지 전송
      Instant messageStartTime = Instant.now();
      
      for (int i = 0; i < sessions.size(); i++) {
        final StompSession session = sessions.get(i);
        final int userIndex = i;
        
        messageExecutor.submit(() -> {
          for (int j = 0; j < messagesPerUser; j++) {
            try {
              LocationUpdateRequest request = LocationUpdateRequest.builder()
                  .latitude(37.5665 + (userIndex * 0.001))
                  .longitude(126.9780 + (j * 0.001))
                  .accuracy(10.0f)
                  .speed(1.5f)
                  .activityType("WALKING")
                  .build();
              
              session.send("/app/location/update", request);
              totalMessagesSent.incrementAndGet();
              
              Thread.sleep(50); // 메시지 간격
              
            } catch (Exception e) {
              log.error("메시지 전송 실패: {}", e.getMessage());
            }
          }
        });
      }
      
      // 모든 메시지 수신 대기
      boolean completed = messageLatch.await(60, TimeUnit.SECONDS);
      
      Duration messageTime = Duration.between(messageStartTime, Instant.now());
      log.info("총 메시지 처리 시간: {}ms", messageTime.toMillis());
      
      // 성능 분석
      assertThat(completed).isTrue();
      assertThat(totalMessagesSent.get()).isEqualTo(userCount * messagesPerUser);
      assertThat(totalMessagesReceived.get()).isGreaterThanOrEqualTo((int)(userCount * messagesPerUser * 0.95)); // 95% 이상 수신
      
      // 지연 시간 분석
      long avgLatency = messageCount.get() > 0 ? totalLatency.get() / messageCount.get() : 0;
      log.info("평균 메시지 지연 시간: {}ms", avgLatency);
      assertThat(avgLatency).isLessThan(3000); // BIF 요구사항: 3초 이내
      
      // 세션 정리
      sessions.forEach(StompSession::disconnect);
      
    } finally {
      connectionExecutor.shutdown();
      messageExecutor.shutdown();
      connectionExecutor.awaitTermination(10, TimeUnit.SECONDS);
      messageExecutor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  @Order(3)
  @DisplayName("긴급 알림 브로드캐스트 성능 테스트")
  void testEmergencyBroadcastPerformance() throws Exception {
    int guardianCount = 30;
    int patientCount = 10;
    CountDownLatch broadcastLatch = new CountDownLatch(guardianCount * patientCount);
    List<StompSession> guardianSessions = new CopyOnWriteArrayList<>();
    List<StompSession> patientSessions = new CopyOnWriteArrayList<>();
    
    ExecutorService executor = Executors.newFixedThreadPool(10);
    
    try {
      // 보호자 30명 연결 및 긴급 알림 구독
      CountDownLatch guardianConnectionLatch = new CountDownLatch(guardianCount);
      for (int i = 0; i < guardianCount; i++) {
        final int guardianIndex = i;
        executor.submit(() -> {
          try {
            User guardian = testUsers.get(guardianIndex);
            String token = userTokens.get(guardian.getUserId());
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            
            StompSession session = stompClient.connectAsync(wsUrl, headers,
                new LoadTestSessionHandler(guardian.getUserId())).get(5, TimeUnit.SECONDS);
            
            // 긴급 알림 구독
            session.subscribe("/user/queue/emergency", new StompFrameHandler() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return Object.class;
              }
              
              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                totalMessagesReceived.incrementAndGet();
                broadcastLatch.countDown();
              }
            });
            
            guardianSessions.add(session);
            guardianConnectionLatch.countDown();
            
          } catch (Exception e) {
            guardianConnectionLatch.countDown();
            log.error("보호자 연결 실패: {}", e.getMessage());
          }
        });
      }
      
      guardianConnectionLatch.await(10, TimeUnit.SECONDS);
      log.info("보호자 {}명 연결 완료", guardianSessions.size());
      
      Thread.sleep(1000); // 구독 안정화 대기
      
      // 환자 10명 연결
      CountDownLatch patientConnectionLatch = new CountDownLatch(patientCount);
      for (int i = 0; i < patientCount; i++) {
        final int patientIndex = guardianCount + i;
        executor.submit(() -> {
          try {
            User patient = testUsers.get(patientIndex);
            String token = userTokens.get(patient.getUserId());
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            
            StompSession session = stompClient.connectAsync(wsUrl, headers,
                new LoadTestSessionHandler(patient.getUserId())).get(5, TimeUnit.SECONDS);
            
            patientSessions.add(session);
            patientConnectionLatch.countDown();
            
          } catch (Exception e) {
            patientConnectionLatch.countDown();
            log.error("환자 연결 실패: {}", e.getMessage());
          }
        });
      }
      
      patientConnectionLatch.await(10, TimeUnit.SECONDS);
      log.info("환자 {}명 연결 완료", patientSessions.size());
      
      // 각 환자가 긴급 알림 전송
      Instant broadcastStartTime = Instant.now();
      
      for (StompSession patientSession : patientSessions) {
        EmergencyAlertRequest request = EmergencyAlertRequest.builder()
            .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
            .message("낙상이 감지되었습니다")
            .latitude(37.5665)
            .longitude(126.9780)
            .severityLevel(5)
            .requiresImmediateAction(true)
            .build();
        
        patientSession.send("/app/emergency/alert", request);
        totalMessagesSent.incrementAndGet();
        
        Thread.sleep(100); // 메시지 간격
      }
      
      // 모든 브로드캐스트 수신 대기
      boolean completed = broadcastLatch.await(30, TimeUnit.SECONDS);
      
      Duration broadcastTime = Duration.between(broadcastStartTime, Instant.now());
      log.info("긴급 알림 브로드캐스트 완료 시간: {}ms", broadcastTime.toMillis());
      
      // 브로드캐스트 성능 확인
      assertThat(completed).isTrue();
      assertThat(totalMessagesReceived.get()).isGreaterThanOrEqualTo((int)(guardianCount * patientCount * 0.9)); // 90% 이상 수신
      
      // 세션 정리
      guardianSessions.forEach(StompSession::disconnect);
      patientSessions.forEach(StompSession::disconnect);
      
    } finally {
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  @Test
  @Order(4)
  @DisplayName("지속적인 부하 테스트 (1분)")
  void testSustainedLoad() throws Exception {
    int activeUsers = 50;
    Duration testDuration = Duration.ofMinutes(1);
    List<StompSession> sessions = new CopyOnWriteArrayList<>();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    AtomicInteger errors = new AtomicInteger(0);
    
    try {
      // 50명 연결
      CountDownLatch connectionLatch = new CountDownLatch(activeUsers);
      for (int i = 0; i < activeUsers; i++) {
        final int userIndex = i;
        scheduler.execute(() -> {
          try {
            User user = testUsers.get(userIndex);
            String token = userTokens.get(user.getUserId());
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + token);
            
            StompSession session = stompClient.connectAsync(wsUrl, headers,
                new LoadTestSessionHandler(user.getUserId())).get(5, TimeUnit.SECONDS);
            
            // 위치 업데이트 구독
            session.subscribe("/topic/location/" + user.getUserId(), new StompFrameHandler() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return LocationUpdateMessage.class;
              }
              
              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                totalMessagesReceived.incrementAndGet();
              }
            });
            
            sessions.add(session);
            connectionLatch.countDown();
            
          } catch (Exception e) {
            connectionLatch.countDown();
            errors.incrementAndGet();
            log.error("연결 실패: {}", e.getMessage());
          }
        });
      }
      
      connectionLatch.await(10, TimeUnit.SECONDS);
      log.info("{}명 연결 완료, 1분간 부하 테스트 시작", sessions.size());
      
      // 1분간 지속적으로 메시지 전송
      Instant endTime = Instant.now().plus(testDuration);
      AtomicInteger messageIndex = new AtomicInteger(0);
      
      for (int i = 0; i < sessions.size(); i++) {
        final StompSession session = sessions.get(i);
        final int userIndex = i;
        
        // 각 사용자는 2초마다 위치 업데이트
        scheduler.scheduleAtFixedRate(() -> {
          try {
            if (Instant.now().isBefore(endTime)) {
              LocationUpdateRequest request = LocationUpdateRequest.builder()
                  .latitude(37.5665 + (userIndex * 0.001))
                  .longitude(126.9780 + (messageIndex.getAndIncrement() * 0.0001))
                  .accuracy(10.0f)
                  .speed(1.5f)
                  .activityType("WALKING")
                  .build();
              
              session.send("/app/location/update", request);
              totalMessagesSent.incrementAndGet();
            }
          } catch (Exception e) {
            errors.incrementAndGet();
            log.error("메시지 전송 실패: {}", e.getMessage());
          }
        }, 0, 2, TimeUnit.SECONDS);
      }
      
      // 테스트 시간 대기
      Thread.sleep(testDuration.toMillis());
      
      // 결과 분석
      log.info("1분 부하 테스트 완료");
      log.info("전송된 메시지: {}", totalMessagesSent.get());
      log.info("수신된 메시지: {}", totalMessagesReceived.get());
      log.info("에러 발생: {}", errors.get());
      
      // 안정성 확인
      double successRate = (double) totalMessagesReceived.get() / totalMessagesSent.get();
      log.info("메시지 전달 성공률: {}%", String.format("%.2f", successRate * 100));
      assertThat(successRate).isGreaterThan(0.95); // 95% 이상 성공
      
      // 세션 정리
      sessions.forEach(StompSession::disconnect);
      
    } finally {
      scheduler.shutdown();
      scheduler.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  /**
   * 부하 테스트용 세션 핸들러
   */
  private class LoadTestSessionHandler extends StompSessionHandlerAdapter {
    private final Long userId;
    
    public LoadTestSessionHandler(Long userId) {
      this.userId = userId;
    }
    
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      log.debug("사용자 {} 연결 성공", userId);
    }
    
    @Override
    public void handleException(StompSession session, StompCommand command,
                               StompHeaders headers, byte[] payload, Throwable exception) {
      log.error("사용자 {} 세션 에러: {}", userId, exception.getMessage());
    }
    
    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
      log.error("사용자 {} 전송 에러: {}", userId, exception.getMessage());
    }
  }
}