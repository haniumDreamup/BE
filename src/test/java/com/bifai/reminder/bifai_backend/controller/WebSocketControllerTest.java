package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * WebSocket 컨트롤러 단위 테스트
 * 실제 WebSocket 연결 없이 컨트롤러 로직만 테스트
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

  @Mock
  private WebSocketService webSocketService;
  
  private WebSocketController controller;

  private LocationUpdateRequest locationRequest;
  private LocationUpdateMessage locationMessage;
  private EmergencyAlertRequest emergencyRequest;
  private ActivityStatusRequest activityRequest;
  private ActivityStatusMessage activityMessage;

  @BeforeEach
  void setUp() {
    controller = new WebSocketController(webSocketService);
    
    // 위치 업데이트 테스트 데이터
    locationRequest = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .accuracy(10.0f)
        .speed(1.5f)
        .activityType("WALKING")
        .build();

    locationMessage = LocationUpdateMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(LocalDateTime.now())
        .message("테스트사용자님이 걷고 있어요")
        .build();

    // 긴급 알림 테스트 데이터
    emergencyRequest = EmergencyAlertRequest.builder()
        .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
        .message("낙상이 감지되었습니다")
        .latitude(37.5665)
        .longitude(126.9780)
        .severityLevel(5)
        .requiresImmediateAction(true)
        .build();

    // 활동 상태 테스트 데이터
    activityRequest = ActivityStatusRequest.builder()
        .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
        .batteryLevel(75)
        .stepCount(5000)
        .build();

    activityMessage = ActivityStatusMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .status("ACTIVE")
        .statusDescription("활동 중")
        .batteryLevel(75)
        .timestamp(LocalDateTime.now())
        .friendlyMessage("테스트사용자님이 활동 중")
        .build();
  }

  @Test
  @DisplayName("위치 업데이트 메시지 처리 테스트")
  void testUpdateLocation() {
    // given
    when(webSocketService.processLocationUpdate(eq("test@example.com"), any(LocationUpdateRequest.class)))
        .thenReturn(locationMessage);

    // when
    LocationUpdateMessage result = controller.updateLocation(
        locationRequest, 
        () -> "test@example.com",
        null
    );

    // then
    verify(webSocketService).processLocationUpdate(eq("test@example.com"), any(LocationUpdateRequest.class));
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("긴급 알림 전송 테스트")
  void testSendEmergencyAlert() {
    // given
    doNothing().when(webSocketService).broadcastEmergencyAlert(
        eq("test@example.com"), any(EmergencyAlertRequest.class));

    // when
    controller.sendEmergencyAlert(emergencyRequest, () -> "test@example.com");

    // then
    verify(webSocketService).broadcastEmergencyAlert(
        eq("test@example.com"), any(EmergencyAlertRequest.class));
  }

  @Test
  @DisplayName("활동 상태 업데이트 테스트")
  void testUpdateActivityStatus() {
    // given
    when(webSocketService.processActivityStatus(eq("test@example.com"), any(ActivityStatusRequest.class)))
        .thenReturn(activityMessage);

    // when
    ActivityStatusMessage result = controller.updateActivityStatus(
        activityRequest,
        () -> "test@example.com"
    );

    // then
    verify(webSocketService).processActivityStatus(eq("test@example.com"), any(ActivityStatusRequest.class));
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("개인 메시지 전송 테스트")
  void testSendPersonalMessage() {
    // given
    PersonalMessageRequest request = PersonalMessageRequest.builder()
        .targetUserId(2L)
        .content("안녕하세요")
        .messageType(PersonalMessageRequest.MessageType.TEXT)
        .priority(3)
        .build();

    PersonalMessage response = PersonalMessage.builder()
        .messageId(1L)
        .fromUserId(1L)
        .fromUsername("테스트사용자")
        .toUserId(2L)
        .content("안녕하세요")
        .timestamp(LocalDateTime.now())
        .delivered(false)
        .formattedMessage("💬 메시지: 안녕하세요")
        .build();

    when(webSocketService.sendPersonalMessage(eq("test@example.com"), any(PersonalMessageRequest.class)))
        .thenReturn(response);

    // when
    PersonalMessage result = controller.sendPersonalMessage(
        request,
        () -> "test@example.com"
    );

    // then
    verify(webSocketService).sendPersonalMessage(eq("test@example.com"), any(PersonalMessageRequest.class));
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEqualTo("안녕하세요");
  }

  @Test
  @DisplayName("채널 구독 처리 테스트")
  void testHandleSubscribe() {
    // given
    doNothing().when(webSocketService).handleChannelSubscription(
        anyString(), anyString(), anyBoolean());

    // when
    controller.handleSubscribe("location", () -> "test@example.com");

    // then
    verify(webSocketService).handleChannelSubscription(
        eq("test@example.com"), eq("location"), eq(true));
  }

  @Test
  @DisplayName("채널 구독 해제 처리 테스트")
  void testHandleUnsubscribe() {
    // given
    doNothing().when(webSocketService).handleChannelSubscription(
        anyString(), anyString(), anyBoolean());

    // when
    controller.handleUnsubscribe("location", () -> "test@example.com");

    // then
    verify(webSocketService).handleChannelSubscription(
        eq("test@example.com"), eq("location"), eq(false));
  }
}