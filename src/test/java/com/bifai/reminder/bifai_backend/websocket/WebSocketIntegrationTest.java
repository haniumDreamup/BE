package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * WebSocket 통합 테스트
 * 실제 WebSocket 연결과 STOMP 프로토콜을 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("docker-test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  private WebSocketStompClient stompClient;
  private User testUser;
  private User guardianUser;
  private String authToken;
  private String wsUrl;

  @BeforeEach
  void setUp() {
    // WebSocket 클라이언트 설정
    List<Transport> transports = new ArrayList<>();
    transports.add(new WebSocketTransport(new StandardWebSocketClient()));
    SockJsClient sockJsClient = new SockJsClient(transports);
    
    stompClient = new WebSocketStompClient(sockJsClient);
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    
    // 테스트 URL 설정
    wsUrl = "ws://localhost:" + port + "/ws-bif";
    
    // 테스트 사용자 생성
    testUser = userRepository.save(User.builder()
        .username("테스트사용자")
        .email("test@example.com")
        .phoneNumber("010-1234-5678")
        .isActive(true)
        .build());
    
    guardianUser = userRepository.save(User.builder()
        .username("보호자")
        .email("guardian@example.com")
        .phoneNumber("010-9876-5432")
        .isActive(true)
        .build());
    
    // JWT 토큰 생성
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        testUser.getEmail(),
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
    authToken = jwtTokenProvider.generateAccessToken(auth);
  }

  @AfterEach
  void tearDown() {
    if (stompClient != null) {
      stompClient.stop();
    }
  }

  @Test
  @Order(1)
  @DisplayName("WebSocket 연결 성공 테스트")
  void testWebSocketConnection() throws Exception {
    // given
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + authToken);
    
    StompSessionHandler sessionHandler = new TestSessionHandler(failure) {
      @Override
      public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        latch.countDown();
      }
    };
    
    // when
    StompSession session = stompClient.connectAsync(wsUrl, headers, sessionHandler)
        .get(5, TimeUnit.SECONDS);
    
    // then
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(session.isConnected()).isTrue();
    assertThat(failure.get()).isNull();
    
    session.disconnect();
  }

  @Test
  @Order(2)
  @DisplayName("인증 없이 연결 시도 시 실패")
  void testConnectionWithoutAuth() {
    // given
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    // Authorization 헤더 없음
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new TestSessionHandler(new AtomicReference<>()))
          .get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Order(3)
  @DisplayName("위치 업데이트 메시지 전송 및 수신")
  void testLocationUpdate() throws Exception {
    // given
    CompletableFuture<LocationUpdateMessage> messageFuture = new CompletableFuture<>();
    
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + authToken);
    
    StompSession session = stompClient.connectAsync(wsUrl, headers, 
        new TestSessionHandler(new AtomicReference<>())).get(5, TimeUnit.SECONDS);
    
    // 위치 업데이트 구독
    session.subscribe("/topic/location/" + testUser.getUserId(), new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return LocationUpdateMessage.class;
      }
      
      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageFuture.complete((LocationUpdateMessage) payload);
      }
    });
    
    // when - 위치 업데이트 전송
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .accuracy(10.0f)
        .speed(1.5f)
        .activityType("WALKING")
        .build();
    
    session.send("/app/location/update", request);
    
    // then
    LocationUpdateMessage response = messageFuture.get(5, TimeUnit.SECONDS);
    assertThat(response).isNotNull();
    assertThat(response.getLatitude()).isEqualTo(37.5665);
    assertThat(response.getLongitude()).isEqualTo(126.9780);
    assertThat(response.getMessage()).contains("걷고 있어요");
    
    session.disconnect();
  }

  @Test
  @Order(4)
  @DisplayName("긴급 알림 브로드캐스트 테스트")
  void testEmergencyAlert() throws Exception {
    // given - 보호자 세션 생성
    String guardianToken = createTokenForUser(guardianUser);
    WebSocketHttpHeaders guardianHeaders = new WebSocketHttpHeaders();
    guardianHeaders.add("Authorization", "Bearer " + guardianToken);
    
    CompletableFuture<Object> alertFuture = new CompletableFuture<>();
    
    StompSession guardianSession = stompClient.connectAsync(wsUrl, guardianHeaders,
        new TestSessionHandler(new AtomicReference<>())).get(5, TimeUnit.SECONDS);
    
    // 보호자가 긴급 알림 구독
    guardianSession.subscribe("/user/queue/emergency", new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return Object.class; // EmergencyAlertMessage
      }
      
      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        alertFuture.complete(payload);
      }
    });
    
    // when - 환자가 긴급 알림 전송
    WebSocketHttpHeaders patientHeaders = new WebSocketHttpHeaders();
    patientHeaders.add("Authorization", "Bearer " + authToken);
    
    StompSession patientSession = stompClient.connectAsync(wsUrl, patientHeaders,
        new TestSessionHandler(new AtomicReference<>())).get(5, TimeUnit.SECONDS);
    
    EmergencyAlertRequest alertRequest = EmergencyAlertRequest.builder()
        .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
        .message("낙상이 감지되었습니다")
        .latitude(37.5665)
        .longitude(126.9780)
        .severityLevel(5)
        .requiresImmediateAction(true)
        .build();
    
    patientSession.send("/app/emergency/alert", alertRequest);
    
    // then
    Object alert = alertFuture.get(5, TimeUnit.SECONDS);
    assertThat(alert).isNotNull();
    
    guardianSession.disconnect();
    patientSession.disconnect();
  }

  @Test
  @Order(5)
  @DisplayName("채널 구독 및 구독 해제 테스트")
  void testSubscriptionLifecycle() throws Exception {
    // given
    CountDownLatch subscribeLatch = new CountDownLatch(1);
    CountDownLatch unsubscribeLatch = new CountDownLatch(1);
    
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + authToken);
    
    StompSession session = stompClient.connectAsync(wsUrl, headers,
        new TestSessionHandler(new AtomicReference<>())).get(5, TimeUnit.SECONDS);
    
    // when - 구독
    StompSession.Subscription subscription = session.subscribe("/topic/activity/" + testUser.getUserId(),
        new StompFrameHandler() {
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return ActivityStatusMessage.class;
          }
          
          @Override
          public void handleFrame(StompHeaders headers, Object payload) {
            subscribeLatch.countDown();
          }
        });
    
    // 구독 확인을 위한 메시지 전송
    ActivityStatusRequest statusRequest = ActivityStatusRequest.builder()
        .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
        .batteryLevel(75)
        .build();
    
    session.send("/app/activity/status", statusRequest);
    
    // then - 구독 확인
    assertThat(subscribeLatch.await(5, TimeUnit.SECONDS)).isTrue();
    
    // when - 구독 해제
    subscription.unsubscribe();
    Thread.sleep(500); // 구독 해제 처리 대기
    
    // 구독 해제 후 메시지 전송
    session.send("/app/activity/status", statusRequest);
    
    // then - 메시지를 받지 않아야 함
    assertThat(unsubscribeLatch.await(2, TimeUnit.SECONDS)).isFalse();
    
    session.disconnect();
  }

  @Test
  @Order(6)
  @DisplayName("동시 다중 연결 테스트")
  void testConcurrentConnections() throws Exception {
    int connectionCount = 10;
    CountDownLatch connectLatch = new CountDownLatch(connectionCount);
    CountDownLatch messageLatch = new CountDownLatch(connectionCount);
    List<StompSession> sessions = new CopyOnWriteArrayList<>();
    
    ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
    
    try {
      // 다중 연결 생성
      for (int i = 0; i < connectionCount; i++) {
        final int index = i;
        executor.submit(() -> {
          try {
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("Authorization", "Bearer " + authToken);
            
            StompSession session = stompClient.connectAsync(wsUrl, headers,
                new TestSessionHandler(new AtomicReference<>())).get(5, TimeUnit.SECONDS);
            
            sessions.add(session);
            connectLatch.countDown();
            
            // 각 세션에서 메시지 구독
            session.subscribe("/topic/test", new StompFrameHandler() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return String.class;
              }
              
              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                messageLatch.countDown();
              }
            });
            
          } catch (Exception e) {
            fail("Connection failed for client " + index + ": " + e.getMessage());
          }
        });
      }
      
      // 모든 연결 대기
      assertThat(connectLatch.await(10, TimeUnit.SECONDS)).isTrue();
      assertThat(sessions).hasSize(connectionCount);
      
      // 한 세션에서 브로드캐스트 메시지 전송
      sessions.get(0).send("/app/broadcast", "Test message");
      
      // 모든 클라이언트가 메시지 수신 확인
      assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
      
    } finally {
      // 정리
      sessions.forEach(StompSession::disconnect);
      executor.shutdown();
    }
  }

  @Test
  @Order(7)
  @DisplayName("메시지 전송 시간 측정 - 3초 이내 응답")
  void testMessageLatency() throws Exception {
    // given
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + authToken);
    
    StompSession session = stompClient.connectAsync(wsUrl, headers,
        new TestSessionHandler(new AtomicReference<>())).get(5, TimeUnit.SECONDS);
    
    CompletableFuture<Long> latencyFuture = new CompletableFuture<>();
    long startTime = System.currentTimeMillis();
    
    // 메시지 구독
    session.subscribe("/topic/location/" + testUser.getUserId(), new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return LocationUpdateMessage.class;
      }
      
      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        long endTime = System.currentTimeMillis();
        latencyFuture.complete(endTime - startTime);
      }
    });
    
    // when - 메시지 전송
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .build();
    
    session.send("/app/location/update", request);
    
    // then - 3초 이내 응답 확인
    Long latency = latencyFuture.get(5, TimeUnit.SECONDS);
    assertThat(latency).isLessThan(3000); // 3초 이내
    
    session.disconnect();
  }

  /**
   * JWT 토큰 생성 헬퍼 메서드
   */
  private String createTokenForUser(User user) {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        user.getEmail(),
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
    return jwtTokenProvider.generateAccessToken(auth);
  }

  /**
   * 테스트용 세션 핸들러
   */
  private static class TestSessionHandler extends StompSessionHandlerAdapter {
    private final AtomicReference<Throwable> failure;
    
    public TestSessionHandler(AtomicReference<Throwable> failure) {
      this.failure = failure;
    }
    
    @Override
    public void handleException(StompSession session, StompCommand command, 
                               StompHeaders headers, byte[] payload, Throwable exception) {
      failure.set(exception);
    }
    
    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
      failure.set(exception);
    }
  }
}