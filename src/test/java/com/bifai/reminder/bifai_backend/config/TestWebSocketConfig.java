package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 테스트 설정
 * Spring Boot 3.5 테스트 환경에서 WebSocket/STOMP 지원
 */
@TestConfiguration
@Profile("test")
@EnableWebSocketMessageBroker
public class TestWebSocketConfig implements WebSocketMessageBrokerConfigurer {
  
  @MockBean
  private SimpMessagingTemplate messagingTemplate;
  
  @MockBean
  private WebSocketEventListener webSocketEventListener;
  
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic", "/queue", "/user");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
  }
  
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-bif")
        .setAllowedOriginPatterns("*")
        .withSockJS();
  }
}