package com.bifai.reminder.bifai_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket 설정 클래스
 * STOMP 프로토콜을 사용한 실시간 양방향 통신 구성
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

  /**
   * 메시지 브로커 구성
   * - /topic: 1:N 브로드캐스트 (위치 공유, 상태 업데이트 등)
   * - /queue: 1:1 개인 메시지 (개인 알림 등)
   * - /app: 클라이언트에서 서버로 메시지 전송 시 prefix
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // 클라이언트가 구독할 수 있는 목적지 prefix
    config.enableSimpleBroker("/topic", "/queue");
    
    // 클라이언트가 서버로 메시지 전송 시 사용할 prefix
    config.setApplicationDestinationPrefixes("/app");
    
    // 사용자별 목적지 prefix (개인 메시지용)
    config.setUserDestinationPrefix("/user");
    
    log.info("WebSocket 메시지 브로커 구성 완료");
  }

  /**
   * STOMP 엔드포인트 등록
   * - /ws-bif: WebSocket 연결 엔드포인트
   * - SockJS 폴백 지원 (WebSocket을 지원하지 않는 환경용)
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-bif")
        .setAllowedOriginPatterns("*") // CORS 설정 (프로덕션에서는 구체적인 도메인 설정)
        .withSockJS() // SockJS 폴백 지원
        .setHeartbeatTime(25000) // 하트비트 간격 (25초)
        .setDisconnectDelay(10000); // 연결 해제 지연 시간 (10초)
    
    log.info("STOMP 엔드포인트 등록: /ws-bif");
  }

  /**
   * WebSocket 전송 설정
   * - 메시지 크기 제한
   * - 전송 시간 제한
   */
  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration
        .setMessageSizeLimit(128 * 1024) // 메시지 크기 제한: 128KB
        .setSendBufferSizeLimit(512 * 1024) // 전송 버퍼 크기: 512KB
        .setSendTimeLimit(20 * 1000); // 전송 시간 제한: 20초
    
    log.info("WebSocket 전송 설정 완료");
  }

  /**
   * 인바운드 채널 설정
   * - 핸드셰이크 인터셉터 추가 가능
   * - 인증/인가 처리
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration
        .interceptors(webSocketAuthChannelInterceptor)
        .taskExecutor()
        .corePoolSize(4) // 기본 스레드 풀 크기
        .maxPoolSize(8)  // 최대 스레드 풀 크기
        .queueCapacity(100); // 큐 용량
  }
}