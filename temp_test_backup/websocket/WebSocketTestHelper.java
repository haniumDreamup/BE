package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 테스트를 위한 헬퍼 클래스
 * 실제 WebSocket 연결 없이 메시지 처리 로직을 테스트
 */
public class WebSocketTestHelper {

  /**
   * Mock STOMP 세션 생성
   */
  public static class MockStompSession {
    private final String sessionId;
    private final Principal principal;
    private final Map<String, BlockingQueue<Object>> subscriptions = new HashMap<>();
    private boolean connected = true;
    
    public MockStompSession(String sessionId, Principal principal) {
      this.sessionId = sessionId;
      this.principal = principal;
    }
    
    public void subscribe(String destination) {
      subscriptions.put(destination, new LinkedBlockingQueue<>());
    }
    
    public void send(String destination, Object payload) {
      if (!connected) {
        throw new IllegalStateException("Session is not connected");
      }
      // 메시지 전송 시뮬레이션
      subscriptions.computeIfPresent(destination, (k, queue) -> {
        queue.offer(payload);
        return queue;
      });
    }
    
    public <T> T receive(String destination, Class<T> type, long timeout, TimeUnit unit) 
        throws InterruptedException {
      BlockingQueue<Object> queue = subscriptions.get(destination);
      if (queue == null) {
        return null;
      }
      Object message = queue.poll(timeout, unit);
      return type.cast(message);
    }
    
    public void disconnect() {
      connected = false;
      subscriptions.clear();
    }
    
    public boolean isConnected() {
      return connected;
    }
    
    public String getSessionId() {
      return sessionId;
    }
    
    public Principal getPrincipal() {
      return principal;
    }
  }
  
  /**
   * Mock Principal 구현
   */
  public static class MockPrincipal implements Principal {
    private final String name;
    
    public MockPrincipal(String name) {
      this.name = name;
    }
    
    @Override
    public String getName() {
      return name;
    }
  }
  
  /**
   * STOMP 메시지 빌더
   */
  public static class StompMessageBuilder {
    
    public static Message<byte[]> createConnectMessage(String sessionId, Principal principal) {
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
      accessor.setSessionId(sessionId);
      accessor.setUser(principal);
      accessor.setAcceptVersion("1.0,1.1,1.2");
      accessor.setHeartbeat(10000, 10000);
      return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
    
    public static Message<byte[]> createSubscribeMessage(String sessionId, String destination, 
                                                         String subscriptionId) {
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
      accessor.setSessionId(sessionId);
      accessor.setDestination(destination);
      accessor.setSubscriptionId(subscriptionId);
      return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
    
    public static Message<Object> createSendMessage(String sessionId, String destination, 
                                                    Object payload) {
      SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
      accessor.setSessionId(sessionId);
      accessor.setDestination(destination);
      accessor.setLeaveMutable(true);
      return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
    
    public static Message<byte[]> createDisconnectMessage(String sessionId) {
      StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
      accessor.setSessionId(sessionId);
      return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
  }
  
  /**
   * 테스트용 메시지 수신 핸들러
   */
  public static class TestMessageHandler {
    private final Map<String, CompletableFuture<Object>> futures = new HashMap<>();
    private final Map<String, CountDownLatch> latches = new HashMap<>();
    private final Map<String, BlockingQueue<Object>> queues = new HashMap<>();
    
    public void expectMessage(String destination) {
      futures.put(destination, new CompletableFuture<>());
      latches.put(destination, new CountDownLatch(1));
      queues.put(destination, new LinkedBlockingQueue<>());
    }
    
    public void handleMessage(String destination, Object message) {
      CompletableFuture<Object> future = futures.get(destination);
      if (future != null) {
        future.complete(message);
      }
      
      CountDownLatch latch = latches.get(destination);
      if (latch != null) {
        latch.countDown();
      }
      
      BlockingQueue<Object> queue = queues.get(destination);
      if (queue != null) {
        queue.offer(message);
      }
    }
    
    public <T> T awaitMessage(String destination, Class<T> type, long timeout, TimeUnit unit) 
        throws Exception {
      CompletableFuture<Object> future = futures.get(destination);
      if (future != null) {
        Object result = future.get(timeout, unit);
        return type.cast(result);
      }
      return null;
    }
    
    public boolean awaitLatch(String destination, long timeout, TimeUnit unit) 
        throws InterruptedException {
      CountDownLatch latch = latches.get(destination);
      if (latch != null) {
        return latch.await(timeout, unit);
      }
      return false;
    }
    
    public <T> T pollMessage(String destination, Class<T> type, long timeout, TimeUnit unit) 
        throws InterruptedException {
      BlockingQueue<Object> queue = queues.get(destination);
      if (queue != null) {
        Object message = queue.poll(timeout, unit);
        return type.cast(message);
      }
      return null;
    }
    
    public void reset() {
      futures.clear();
      latches.clear();
      queues.clear();
    }
  }
  
  /**
   * 테스트 데이터 생성 유틸리티
   */
  public static class TestDataFactory {
    
    public static User createTestUser(Long id, String email) {
      return User.builder()
          .userId(id)
          .email(email)
          .username("testuser_" + id)
          .name("테스트 사용자 " + id)
          .phoneNumber("010-0000-" + String.format("%04d", id))
          .passwordHash("$2a$10$test")
          .isActive(true)
          .build();
    }
    
    public static LocationUpdateRequest createLocationUpdate(double lat, double lng) {
      return LocationUpdateRequest.builder()
          .latitude(lat)
          .longitude(lng)
          .accuracy(10.0f)
          .speed(1.5f)
          .activityType("WALKING")
          .build();
    }
    
    public static EmergencyAlertRequest createEmergencyAlert() {
      return EmergencyAlertRequest.builder()
          .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
          .message("낙상이 감지되었습니다")
          .latitude(37.5665)
          .longitude(126.9780)
          .severityLevel(5)
          .requiresImmediateAction(true)
          .build();
    }
    
    public static ActivityStatusRequest createActivityStatus(int batteryLevel) {
      return ActivityStatusRequest.builder()
          .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
          .batteryLevel(batteryLevel)
          .build();
    }
    
    public static LocationUpdateMessage createLocationMessage(LocationUpdateRequest request) {
      return LocationUpdateMessage.builder()
          .userId(1L)
          .latitude(request.getLatitude())
          .longitude(request.getLongitude())
          .accuracy(request.getAccuracy())
          .speed(request.getSpeed())
          .activityType(request.getActivityType())
          .message("위치가 업데이트되었습니다")
          .timestamp(LocalDateTime.now())
          .build();
    }
    
    public static ActivityStatusMessage createActivityMessage(ActivityStatusRequest request) {
      return ActivityStatusMessage.builder()
          .userId(1L)
          .status(request.getStatus().name())
          .batteryLevel(request.getBatteryLevel())
          .timestamp(LocalDateTime.now())
          .build();
    }
  }
  
  /**
   * Mock MessageChannel 구현
   */
  public static class MockMessageChannel implements MessageChannel {
    private final BlockingQueue<Message<?>> sentMessages = new LinkedBlockingQueue<>();
    
    @Override
    public boolean send(Message<?> message) {
      return sentMessages.offer(message);
    }
    
    @Override
    public boolean send(Message<?> message, long timeout) {
      try {
        return sentMessages.offer(message, timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    
    public Message<?> getLastSentMessage() {
      return sentMessages.poll();
    }
    
    public int getSentMessageCount() {
      return sentMessages.size();
    }
    
    public void clear() {
      sentMessages.clear();
    }
  }
}