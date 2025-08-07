package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.controller.WebSocketController;
import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * WebSocket 간단한 통합 테스트
 * 컨트롤러 레벨에서의 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("WebSocket 간단한 통합 테스트")
class SimpleWebSocketTest {

  @Autowired
  private WebSocketController webSocketController;

  @MockitoBean
  private WebSocketService webSocketService;

  @MockitoBean
  private SimpMessagingTemplate messagingTemplate;

  private Principal mockPrincipal;

  @BeforeEach
  void setUp() {
    mockPrincipal = () -> "test@example.com";
  }

  @Test
  @DisplayName("위치 업데이트 메시지 처리")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testLocationUpdateMessage() {
    // given
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .accuracy(10.0f)
        .speed(1.5f)
        .activityType("WALKING")
        .build();

    LocationUpdateMessage expectedMessage = LocationUpdateMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(LocalDateTime.now())
        .message("테스트사용자님이 걷고 있어요")
        .build();

    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenReturn(expectedMessage);

    // when
    LocationUpdateMessage result = webSocketController.updateLocation(request, mockPrincipal, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getLatitude()).isEqualTo(37.5665);
    assertThat(result.getLongitude()).isEqualTo(126.9780);
    assertThat(result.getMessage()).contains("걷고 있어요");
    
    verify(webSocketService).processLocationUpdate("test@example.com", request);
  }

  @Test
  @DisplayName("긴급 알림 전송")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testEmergencyAlertBroadcast() {
    // given
    EmergencyAlertRequest request = EmergencyAlertRequest.builder()
        .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
        .message("낙상이 감지되었습니다")
        .latitude(37.5665)
        .longitude(126.9780)
        .severityLevel(5)
        .requiresImmediateAction(true)
        .build();

    doNothing().when(webSocketService).broadcastEmergencyAlert(anyString(), any(EmergencyAlertRequest.class));

    // when
    webSocketController.sendEmergencyAlert(request, mockPrincipal);

    // then
    verify(webSocketService).broadcastEmergencyAlert("test@example.com", request);
  }

  @Test
  @DisplayName("활동 상태 업데이트")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testActivityStatusUpdate() {
    // given
    ActivityStatusRequest request = ActivityStatusRequest.builder()
        .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
        .batteryLevel(75)
        .stepCount(5000)
        .heartRate(75)
        .build();

    ActivityStatusMessage expectedMessage = ActivityStatusMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .status("ACTIVE")
        .statusDescription("활동 중")
        .batteryLevel(75)
        .timestamp(LocalDateTime.now())
        .friendlyMessage("테스트사용자님이 활동 중이에요. 배터리: 75%")
        .build();

    when(webSocketService.processActivityStatus(anyString(), any(ActivityStatusRequest.class)))
        .thenReturn(expectedMessage);

    // when
    ActivityStatusMessage result = webSocketController.updateActivityStatus(request, mockPrincipal);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getBatteryLevel()).isEqualTo(75);
    assertThat(result.getFriendlyMessage()).contains("활동 중");
    
    verify(webSocketService).processActivityStatus("test@example.com", request);
  }

  @Test
  @DisplayName("개인 메시지 전송")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testPersonalMessageSend() {
    // given
    PersonalMessageRequest request = PersonalMessageRequest.builder()
        .targetUserId(2L)
        .content("안녕하세요! 오늘 기분은 어떠세요?")
        .messageType(PersonalMessageRequest.MessageType.TEXT)
        .priority(3)
        .build();

    PersonalMessage expectedMessage = PersonalMessage.builder()
        .messageId(1L)
        .fromUserId(1L)
        .fromUsername("테스트사용자")
        .toUserId(2L)
        .content("안녕하세요! 오늘 기분은 어떠세요?")
        .timestamp(LocalDateTime.now())
        .delivered(false)
        .formattedMessage("💬 테스트사용자: 안녕하세요! 오늘 기분은 어떠세요?")
        .build();

    when(webSocketService.sendPersonalMessage(anyString(), any(PersonalMessageRequest.class)))
        .thenReturn(expectedMessage);

    // when
    PersonalMessage result = webSocketController.sendPersonalMessage(request, mockPrincipal);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEqualTo("안녕하세요! 오늘 기분은 어떠세요?");
    assertThat(result.getToUserId()).isEqualTo(2L);
    assertThat(result.getFormattedMessage()).contains("💬");
    
    verify(webSocketService).sendPersonalMessage("test@example.com", request);
  }

  @Test
  @DisplayName("채널 구독 및 해제")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testChannelSubscriptionLifecycle() {
    // given
    String locationChannel = "location";
    String activityChannel = "activity";
    
    doNothing().when(webSocketService).handleChannelSubscription(anyString(), anyString(), anyBoolean());

    // when - 구독
    webSocketController.handleSubscribe(locationChannel, mockPrincipal);
    webSocketController.handleSubscribe(activityChannel, mockPrincipal);

    // then - 구독 확인
    verify(webSocketService).handleChannelSubscription("test@example.com", locationChannel, true);
    verify(webSocketService).handleChannelSubscription("test@example.com", activityChannel, true);

    // when - 구독 해제
    webSocketController.handleUnsubscribe(locationChannel, mockPrincipal);

    // then - 구독 해제 확인
    verify(webSocketService).handleChannelSubscription("test@example.com", locationChannel, false);
  }

  @Test
  @DisplayName("위치 업데이트 후 브로드캐스트")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testLocationUpdateBroadcast() {
    // given
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .accuracy(5.0f)
        .speed(0.0f)
        .activityType("STATIONARY")
        .build();

    LocationUpdateMessage message = LocationUpdateMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(LocalDateTime.now())
        .message("테스트사용자님이 멈춰있어요")
        .accuracy(5.0f)
        .speed(0.0f)
        .activityType("STATIONARY")
        .build();

    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenReturn(message);

    // when
    LocationUpdateMessage result = webSocketController.updateLocation(request, mockPrincipal, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getActivityType()).isEqualTo("STATIONARY");
    assertThat(result.getMessage()).contains("멈춰있어요");
    
    verify(webSocketService).processLocationUpdate("test@example.com", request);
  }

  @Test
  @DisplayName("배터리 부족 알림")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testLowBatteryAlert() {
    // given
    ActivityStatusRequest request = ActivityStatusRequest.builder()
        .status(ActivityStatusRequest.ActivityStatus.RESTING)
        .batteryLevel(15)
        .build();

    ActivityStatusMessage message = ActivityStatusMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .status("RESTING")
        .statusDescription("배터리 부족")
        .batteryLevel(15)
        .timestamp(LocalDateTime.now())
        .friendlyMessage("⚠️ 테스트사용자님의 배터리가 15%입니다. 충전이 필요해요!")
        .build();

    when(webSocketService.processActivityStatus(anyString(), any(ActivityStatusRequest.class)))
        .thenReturn(message);

    // when
    ActivityStatusMessage result = webSocketController.updateActivityStatus(request, mockPrincipal);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("RESTING");
    assertThat(result.getBatteryLevel()).isEqualTo(15);
    assertThat(result.getFriendlyMessage()).contains("충전이 필요해요");
    
    verify(webSocketService).processActivityStatus("test@example.com", request);
  }

  @Test
  @DisplayName("복약 알림 메시지")
  @WithMockUser(username = "test@example.com", roles = "USER")
  void testMedicationReminder() {
    // given
    PersonalMessageRequest request = PersonalMessageRequest.builder()
        .targetUserId(1L)
        .content("약 드실 시간이에요! 점심약을 잊지 마세요.")
        .messageType(PersonalMessageRequest.MessageType.REMINDER)
        .priority(5)
        .build();

    PersonalMessage message = PersonalMessage.builder()
        .messageId(100L)
        .fromUserId(0L) // 시스템 메시지
        .fromUsername("BIF 알림")
        .toUserId(1L)
        .content("약 드실 시간이에요! 점심약을 잊지 마세요.")
        .timestamp(LocalDateTime.now())
        .delivered(false)
        .formattedMessage("💊 약 드실 시간이에요! 점심약을 잊지 마세요.")
        .messageType("REMINDER")
        .priority(5)
        .build();

    when(webSocketService.sendPersonalMessage(anyString(), any(PersonalMessageRequest.class)))
        .thenReturn(message);

    // when
    PersonalMessage result = webSocketController.sendPersonalMessage(request, mockPrincipal);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).contains("약 드실 시간");
    assertThat(result.getFormattedMessage()).contains("💊");
    assertThat(result.getPriority()).isEqualTo(5);
    assertThat(result.getMessageType()).isEqualTo("REMINDER");
    
    verify(webSocketService).sendPersonalMessage("test@example.com", request);
  }
}