package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 메시지 처리 컨트롤러
 * STOMP 프로토콜을 통한 실시간 메시지 라우팅
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

  private final WebSocketService webSocketService;

  /**
   * 실시간 위치 업데이트
   * /app/location/update로 메시지 전송 -> /topic/location/{userId}로 브로드캐스트
   */
  @MessageMapping("/location/update")
  @SendTo("/topic/location/{userId}")
  public LocationUpdateMessage updateLocation(
      @Payload LocationUpdateRequest request,
      Principal principal,
      SimpMessageHeaderAccessor headerAccessor) {
    
    log.info("위치 업데이트 - user: {}, lat: {}, lng: {}", 
        principal.getName(), request.getLatitude(), request.getLongitude());
    
    return webSocketService.processLocationUpdate(principal.getName(), request);
  }

  /**
   * 긴급 알림 전송
   * /app/emergency/alert로 메시지 전송 -> 관련 보호자들에게 개별 전송
   */
  @MessageMapping("/emergency/alert")
  public void sendEmergencyAlert(
      @Payload EmergencyAlertRequest request,
      Principal principal) {
    
    log.info("긴급 알림 발송 - user: {}, type: {}", principal.getName(), request.getAlertType());
    
    webSocketService.broadcastEmergencyAlert(principal.getName(), request);
  }

  /**
   * 활동 상태 업데이트
   * /app/activity/status로 메시지 전송 -> /topic/activity/{userId}로 브로드캐스트
   */
  @MessageMapping("/activity/status")
  @SendTo("/topic/activity/{userId}")
  public ActivityStatusMessage updateActivityStatus(
      @Payload ActivityStatusRequest request,
      Principal principal) {
    
    log.info("활동 상태 업데이트 - user: {}, status: {}", 
        principal.getName(), request.getStatus());
    
    return webSocketService.processActivityStatus(principal.getName(), request);
  }

  /**
   * 포즈 데이터 스트리밍
   * /app/pose/stream으로 메시지 전송 -> /topic/pose/{userId}로 브로드캐스트
   */
  @MessageMapping("/pose/stream")
  @SendTo("/topic/pose/{userId}")
  public PoseStreamMessage streamPoseData(
      @Payload PoseStreamRequest request,
      Principal principal) {
    
    log.debug("포즈 데이터 스트리밍 - user: {}", principal.getName());
    
    return webSocketService.processPoseStream(principal.getName(), request);
  }

  /**
   * 개인 메시지 전송
   * /app/message/send로 메시지 전송 -> 특정 사용자의 /queue/messages로 전송
   */
  @MessageMapping("/message/send")
  @SendToUser("/queue/messages")
  public PersonalMessage sendPersonalMessage(
      @Payload PersonalMessageRequest request,
      Principal principal) {
    
    log.info("개인 메시지 전송 - from: {}, to: {}", 
        principal.getName(), request.getTargetUserId());
    
    return webSocketService.sendPersonalMessage(principal.getName(), request);
  }

  /**
   * 특정 채널 구독 시작 알림
   */
  @MessageMapping("/subscribe/{channel}")
  public void handleSubscribe(
      @DestinationVariable String channel,
      Principal principal) {
    
    log.info("채널 구독 시작 - user: {}, channel: {}", principal.getName(), channel);
    
    webSocketService.handleChannelSubscription(principal.getName(), channel, true);
  }

  /**
   * 특정 채널 구독 해제 알림
   */
  @MessageMapping("/unsubscribe/{channel}")
  public void handleUnsubscribe(
      @DestinationVariable String channel,
      Principal principal) {
    
    log.info("채널 구독 해제 - user: {}, channel: {}", principal.getName(), channel);
    
    webSocketService.handleChannelSubscription(principal.getName(), channel, false);
  }
}