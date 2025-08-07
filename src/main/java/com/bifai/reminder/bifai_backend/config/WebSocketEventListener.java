package com.bifai.reminder.bifai_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 이벤트 리스너
 * 연결, 구독, 해제 등의 이벤트 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

  private final SimpMessagingTemplate messagingTemplate;
  
  // 활성 세션 추적
  private final ConcurrentHashMap<String, String> activeUsers = new ConcurrentHashMap<>();
  private final AtomicInteger connectionCount = new AtomicInteger(0);

  /**
   * WebSocket 연결 성공 이벤트
   */
  @EventListener
  public void handleWebSocketConnectListener(SessionConnectedEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = headerAccessor.getSessionId();
    String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
    
    activeUsers.put(sessionId, username);
    int currentCount = connectionCount.incrementAndGet();
    
    log.info("새로운 WebSocket 연결 - sessionId: {}, user: {}, 현재 연결 수: {}", 
        sessionId, username, currentCount);
    
    // 연결 상태 브로드캐스트
    broadcastConnectionStatus(username, true);
  }

  /**
   * WebSocket 연결 해제 이벤트
   */
  @EventListener
  public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = headerAccessor.getSessionId();
    String username = activeUsers.remove(sessionId);
    int currentCount = connectionCount.decrementAndGet();
    
    log.info("WebSocket 연결 해제 - sessionId: {}, user: {}, 남은 연결 수: {}", 
        sessionId, username, currentCount);
    
    if (username != null) {
      // 연결 해제 상태 브로드캐스트
      broadcastConnectionStatus(username, false);
    }
  }

  /**
   * 채널 구독 이벤트
   */
  @EventListener
  public void handleSubscribeEvent(SessionSubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String destination = headerAccessor.getDestination();
    String sessionId = headerAccessor.getSessionId();
    String username = activeUsers.get(sessionId);
    
    log.info("채널 구독 - user: {}, destination: {}", username, destination);
  }

  /**
   * 채널 구독 해제 이벤트
   */
  @EventListener
  public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
    StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
    String sessionId = headerAccessor.getSessionId();
    String username = activeUsers.get(sessionId);
    
    log.info("채널 구독 해제 - user: {}", username);
  }

  /**
   * 연결 상태 변경 브로드캐스트
   */
  private void broadcastConnectionStatus(String username, boolean isConnected) {
    ConnectionStatusMessage message = ConnectionStatusMessage.builder()
        .username(username)
        .connected(isConnected)
        .timestamp(System.currentTimeMillis())
        .activeUserCount(connectionCount.get())
        .build();
    
    messagingTemplate.convertAndSend("/topic/connection-status", message);
  }

  /**
   * 연결 상태 메시지 DTO
   */
  @lombok.Data
  @lombok.Builder
  private static class ConnectionStatusMessage {
    private String username;
    private boolean connected;
    private long timestamp;
    private int activeUserCount;
  }
  
  /**
   * 현재 활성 연결 수 조회
   */
  public int getActiveConnectionCount() {
    return connectionCount.get();
  }
  
  /**
   * 특정 사용자의 연결 상태 확인
   */
  public boolean isUserConnected(String username) {
    return activeUsers.containsValue(username);
  }
}