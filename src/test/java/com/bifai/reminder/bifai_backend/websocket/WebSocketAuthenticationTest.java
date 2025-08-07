package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * WebSocket 인증 테스트
 * JWT 토큰 검증, 권한 체크, 인증 실패 시나리오 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSocketAuthenticationTest {

  @LocalServerPort
  private int port;

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private WebSocketStompClient stompClient;
  private String wsUrl;
  private User testUser;

  @BeforeEach
  void setUp() {
    // WebSocket 클라이언트 설정
    stompClient = new WebSocketStompClient(new SockJsClient(
        List.of(new WebSocketTransport(new StandardWebSocketClient()))));
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    
    wsUrl = "ws://localhost:" + port + "/ws-bif";
    
    // 테스트 사용자 생성
    testUser = userRepository.save(User.builder()
        .username("인증테스트사용자")
        .email("auth-test@example.com")
        .phoneNumber("010-1111-2222")
        .isActive(true)
        .build());
  }

  @AfterEach
  void tearDown() {
    if (stompClient != null) {
      stompClient.stop();
    }
  }

  @Test
  @Order(1)
  @DisplayName("유효한 JWT 토큰으로 연결 성공")
  void testConnectionWithValidToken() throws Exception {
    // given
    String validToken = createValidToken(testUser.getEmail());
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + validToken);
    
    CountDownLatch connectedLatch = new CountDownLatch(1);
    AtomicReference<Throwable> failure = new AtomicReference<>();
    
    // when
    StompSession session = stompClient.connectAsync(wsUrl, headers, 
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            connectedLatch.countDown();
          }
          
          @Override
          public void handleException(StompSession session, StompCommand command,
                                     StompHeaders headers, byte[] payload, Throwable exception) {
            failure.set(exception);
          }
        }).get(5, TimeUnit.SECONDS);
    
    // then
    assertThat(connectedLatch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(session.isConnected()).isTrue();
    assertThat(failure.get()).isNull();
    
    session.disconnect();
  }

  @Test
  @Order(2)
  @DisplayName("만료된 JWT 토큰으로 연결 실패")
  void testConnectionWithExpiredToken() {
    // given
    String expiredToken = createExpiredToken(testUser.getEmail());
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + expiredToken);
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {})
          .get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(Exception.class);
  }

  @Test
  @Order(3)
  @DisplayName("잘못된 서명의 JWT 토큰으로 연결 실패")
  void testConnectionWithInvalidSignature() {
    // given
    String invalidToken = createTokenWithInvalidSignature(testUser.getEmail());
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + invalidToken);
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {})
          .get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(Exception.class);
  }

  @Test
  @Order(4)
  @DisplayName("Authorization 헤더 없이 연결 실패")
  void testConnectionWithoutAuthHeader() {
    // given
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    // Authorization 헤더 없음
    
    CountDownLatch errorLatch = new CountDownLatch(1);
    AtomicReference<String> errorMessage = new AtomicReference<>();
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {
        @Override
        public void handleException(StompSession session, StompCommand command,
                                   StompHeaders headers, byte[] payload, Throwable exception) {
          errorMessage.set(exception.getMessage());
          errorLatch.countDown();
        }
      }).get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(Exception.class);
  }

  @Test
  @Order(5)
  @DisplayName("Bearer 프리픽스 없는 토큰으로 연결 실패")
  void testConnectionWithoutBearerPrefix() {
    // given
    String validToken = createValidToken(testUser.getEmail());
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", validToken); // Bearer 없이
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {})
          .get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(Exception.class);
  }

  @Test
  @Order(6)
  @DisplayName("존재하지 않는 사용자의 토큰으로 연결 실패")
  void testConnectionWithNonExistentUser() {
    // given
    String tokenForNonExistentUser = createValidToken("nonexistent@example.com");
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + tokenForNonExistentUser);
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {})
          .get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(Exception.class);
  }

  @Test
  @Order(7)
  @DisplayName("비활성화된 사용자의 토큰으로 연결 시도")
  void testConnectionWithInactiveUser() throws Exception {
    // given - 사용자 비활성화
    testUser.setIsActive(false);
    userRepository.save(testUser);
    
    String validToken = createValidToken(testUser.getEmail());
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + validToken);
    
    // when & then
    assertThatThrownBy(() -> {
      stompClient.connectAsync(wsUrl, headers, new StompSessionHandlerAdapter() {})
          .get(5, TimeUnit.SECONDS);
    }).hasCauseInstanceOf(Exception.class);
  }

  @Test
  @Order(8)
  @DisplayName("토큰 갱신 후 재연결 성공")
  void testReconnectionWithRefreshedToken() throws Exception {
    // given - 첫 번째 연결
    String firstToken = createValidToken(testUser.getEmail());
    WebSocketHttpHeaders firstHeaders = new WebSocketHttpHeaders();
    firstHeaders.add("Authorization", "Bearer " + firstToken);
    
    StompSession firstSession = stompClient.connectAsync(wsUrl, firstHeaders,
        new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    
    assertThat(firstSession.isConnected()).isTrue();
    firstSession.disconnect();
    
    // when - 새 토큰으로 재연결
    Thread.sleep(1000); // 토큰 발급 시간 차이를 위한 대기
    String newToken = createValidToken(testUser.getEmail());
    WebSocketHttpHeaders newHeaders = new WebSocketHttpHeaders();
    newHeaders.add("Authorization", "Bearer " + newToken);
    
    CountDownLatch reconnectLatch = new CountDownLatch(1);
    
    StompSession newSession = stompClient.connectAsync(wsUrl, newHeaders,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            reconnectLatch.countDown();
          }
        }).get(5, TimeUnit.SECONDS);
    
    // then
    assertThat(reconnectLatch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(newSession.isConnected()).isTrue();
    
    newSession.disconnect();
  }

  @Test
  @Order(9)
  @DisplayName("다른 권한의 토큰으로 연결 테스트")
  void testConnectionWithDifferentRoles() throws Exception {
    // given - GUARDIAN 권한 사용자
    User guardianUser = userRepository.save(User.builder()
        .username("보호자인증")
        .email("guardian-auth@example.com")
        .isActive(true)
        .build());
    
    String guardianToken = createTokenWithRole(guardianUser.getEmail(), "ROLE_GUARDIAN");
    WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    headers.add("Authorization", "Bearer " + guardianToken);
    
    CountDownLatch connectedLatch = new CountDownLatch(1);
    
    // when
    StompSession session = stompClient.connectAsync(wsUrl, headers,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            connectedLatch.countDown();
          }
        }).get(5, TimeUnit.SECONDS);
    
    // then
    assertThat(connectedLatch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThat(session.isConnected()).isTrue();
    
    session.disconnect();
  }

  /**
   * 유효한 JWT 토큰 생성
   */
  private String createValidToken(String email) {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        email,
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
    return jwtTokenProvider.generateAccessToken(auth);
  }

  /**
   * 특정 권한을 가진 JWT 토큰 생성
   */
  private String createTokenWithRole(String email, String role) {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        email,
        null,
        Collections.singletonList(new SimpleGrantedAuthority(role))
    );
    return jwtTokenProvider.generateAccessToken(auth);
  }

  /**
   * 만료된 JWT 토큰 생성
   */
  private String createExpiredToken(String email) {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    Date expiration = new Date(now.getTime() - 1000); // 1초 전 만료
    
    return Jwts.builder()
        .setSubject(email)
        .claim("type", "access")
        .setIssuedAt(now)
        .setExpiration(expiration)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  /**
   * 잘못된 서명의 JWT 토큰 생성
   */
  private String createTokenWithInvalidSignature(String email) {
    String wrongSecret = "wrong-secret-key-for-testing-invalid-signature-1234567890";
    SecretKey key = Keys.hmacShaKeyFor(wrongSecret.getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    Date expiration = new Date(now.getTime() + 3600000);
    
    return Jwts.builder()
        .setSubject(email)
        .claim("type", "access")
        .setIssuedAt(now)
        .setExpiration(expiration)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }
}