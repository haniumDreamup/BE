package com.bifai.reminder.bifai_backend.websocket;

import com.bifai.reminder.bifai_backend.controller.WebSocketController;
import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocket Mock 테스트
 * 실제 연결 없이 메시지 처리 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket Mock 테스트")
class MockWebSocketTest {

  @Mock
  private WebSocketService webSocketService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @InjectMocks
  private WebSocketController webSocketController;

  private Principal principal;
  private User testUser;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .email("test@example.com")
        .build();

    // Principal 설정
    principal = new UsernamePasswordAuthenticationToken(
        "test@example.com",
        null,
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
  }

  @Test
  @DisplayName("위치 업데이트 처리")
  void testLocationUpdate() {
    // given
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .accuracy(10.0f)
        .speed(1.5f)
        .activityType("WALKING")
        .build();

    LocationUpdateMessage expectedResponse = LocationUpdateMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .latitude(37.5665)
        .longitude(126.9780)
        .timestamp(LocalDateTime.now())
        .message("테스트사용자님이 걷고 있어요")
        .build();

    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenReturn(expectedResponse);

    // when
    LocationUpdateMessage result = webSocketController.updateLocation(request, principal, null);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getLatitude()).isEqualTo(37.5665);
    verify(webSocketService).processLocationUpdate("test@example.com", request);
  }

  @Test
  @DisplayName("긴급 알림 전송")
  void testEmergencyAlert() {
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
    webSocketController.sendEmergencyAlert(request, principal);

    // then
    verify(webSocketService).broadcastEmergencyAlert("test@example.com", request);
  }

  @Test
  @DisplayName("활동 상태 업데이트")
  void testActivityStatus() {
    // given
    ActivityStatusRequest request = ActivityStatusRequest.builder()
        .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
        .batteryLevel(75)
        .stepCount(5000)
        .build();

    ActivityStatusMessage expectedResponse = ActivityStatusMessage.builder()
        .userId(1L)
        .username("테스트사용자")
        .status("ACTIVE")
        .statusDescription("활동 중")
        .batteryLevel(75)
        .timestamp(LocalDateTime.now())
        .friendlyMessage("테스트사용자님이 활동 중이에요")
        .build();

    when(webSocketService.processActivityStatus(anyString(), any(ActivityStatusRequest.class)))
        .thenReturn(expectedResponse);

    // when
    ActivityStatusMessage result = webSocketController.updateActivityStatus(request, principal);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo("ACTIVE");
    assertThat(result.getBatteryLevel()).isEqualTo(75);
    verify(webSocketService).processActivityStatus("test@example.com", request);
  }

  @Test
  @DisplayName("개인 메시지 전송")
  void testPersonalMessage() {
    // given
    PersonalMessageRequest request = PersonalMessageRequest.builder()
        .targetUserId(2L)
        .content("안녕하세요")
        .messageType(PersonalMessageRequest.MessageType.TEXT)
        .priority(3)
        .build();

    PersonalMessage expectedResponse = PersonalMessage.builder()
        .messageId(1L)
        .fromUserId(1L)
        .fromUsername("테스트사용자")
        .toUserId(2L)
        .content("안녕하세요")
        .timestamp(LocalDateTime.now())
        .delivered(false)
        .formattedMessage("💬 메시지: 안녕하세요")
        .build();

    when(webSocketService.sendPersonalMessage(anyString(), any(PersonalMessageRequest.class)))
        .thenReturn(expectedResponse);

    // when
    PersonalMessage result = webSocketController.sendPersonalMessage(request, principal);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEqualTo("안녕하세요");
    assertThat(result.getToUserId()).isEqualTo(2L);
    verify(webSocketService).sendPersonalMessage("test@example.com", request);
  }

  @Test
  @DisplayName("채널 구독 처리")
  void testChannelSubscription() {
    // given
    String channel = "location";
    doNothing().when(webSocketService).handleChannelSubscription(anyString(), anyString(), anyBoolean());

    // when
    webSocketController.handleSubscribe(channel, principal);

    // then
    verify(webSocketService).handleChannelSubscription("test@example.com", channel, true);
  }

  @Test
  @DisplayName("채널 구독 해제 처리")
  void testChannelUnsubscription() {
    // given
    String channel = "activity";
    doNothing().when(webSocketService).handleChannelSubscription(anyString(), anyString(), anyBoolean());

    // when
    webSocketController.handleUnsubscribe(channel, principal);

    // then
    verify(webSocketService).handleChannelSubscription("test@example.com", channel, false);
  }


  @Test
  @DisplayName("다중 사용자 메시지 라우팅")
  void testMultiUserMessageRouting() {
    // given
    LocationUpdateRequest request1 = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .build();

    LocationUpdateRequest request2 = LocationUpdateRequest.builder()
        .latitude(37.5666)
        .longitude(126.9781)
        .build();

    Principal user1 = new UsernamePasswordAuthenticationToken("user1@example.com", null);
    Principal user2 = new UsernamePasswordAuthenticationToken("user2@example.com", null);

    LocationUpdateMessage response1 = LocationUpdateMessage.builder()
        .userId(1L)
        .latitude(37.5665)
        .longitude(126.9780)
        .build();

    LocationUpdateMessage response2 = LocationUpdateMessage.builder()
        .userId(2L)
        .latitude(37.5666)
        .longitude(126.9781)
        .build();

    when(webSocketService.processLocationUpdate("user1@example.com", request1)).thenReturn(response1);
    when(webSocketService.processLocationUpdate("user2@example.com", request2)).thenReturn(response2);

    // when
    LocationUpdateMessage result1 = webSocketController.updateLocation(request1, user1, null);
    LocationUpdateMessage result2 = webSocketController.updateLocation(request2, user2, null);

    // then
    assertThat(result1.getUserId()).isEqualTo(1L);
    assertThat(result2.getUserId()).isEqualTo(2L);
    verify(webSocketService, times(2)).processLocationUpdate(anyString(), any(LocationUpdateRequest.class));
  }

  @Test
  @DisplayName("에러 상황 처리")
  void testErrorHandling() {
    // given
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .build();

    when(webSocketService.processLocationUpdate(anyString(), any(LocationUpdateRequest.class)))
        .thenThrow(new RuntimeException("처리 중 오류 발생"));

    // when & then
    try {
      webSocketController.updateLocation(request, principal, null);
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).isEqualTo("처리 중 오류 발생");
    }

    verify(webSocketService).processLocationUpdate("test@example.com", request);
  }
}