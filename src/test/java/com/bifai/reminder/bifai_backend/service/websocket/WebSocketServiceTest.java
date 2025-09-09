package com.bifai.reminder.bifai_backend.service.websocket;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private UserRepository userRepository;

  @Mock
  private GuardianRepository guardianRepository;

  @Mock
  private NotificationService notificationService;

  private WebSocketService webSocketService;
  private User testUser;
  private User guardianUser;
  private Guardian testGuardian;

  @BeforeEach
  void setUp() {
    webSocketService = new WebSocketService(
        messagingTemplate,
        userRepository,
        guardianRepository,
        notificationService
    );

    // 테스트 사용자 설정
    testUser = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .email("test@example.com")
        .build();

    guardianUser = User.builder()
        .userId(2L)
        .username("보호자")
        .email("guardian@example.com")
        .build();

    testGuardian = Guardian.builder()
        .id(1L)
        .user(testUser)
        .guardianUser(guardianUser)
        .name("보호자")
        .canViewLocation(true)
        .canReceiveAlerts(true)
        .isActive(true)
        .build();
  }

  @Test
  @DisplayName("위치 업데이트 처리 - 보호자에게 전송")
  void processLocationUpdate_Success() {
    // given
    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .accuracy(10.0f)
        .speed(1.5f)
        .activityType("WALKING")
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(testUser));
    when(guardianRepository.findByUserIdAndCanViewLocationAndIsActive(1L, true, true))
        .thenReturn(Arrays.asList(testGuardian));

    // when
    LocationUpdateMessage result = webSocketService.processLocationUpdate("test@example.com", request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getLatitude()).isEqualTo(37.5665);
    assertThat(result.getMessage()).contains("걷고 있어요");

    // 보호자에게 메시지 전송 확인
    verify(messagingTemplate).convertAndSend(
        eq("/user/guardian@example.com/queue/location"),
        any(LocationUpdateMessage.class)
    );
  }

  @Test
  @DisplayName("긴급 알림 브로드캐스트 - 모든 보호자에게 전송")
  void broadcastEmergencyAlert_Success() {
    // given
    EmergencyAlertRequest request = EmergencyAlertRequest.builder()
        .alertType(EmergencyAlertRequest.AlertType.FALL_DETECTED)
        .message("낙상이 감지되었습니다")
        .latitude(37.5665)
        .longitude(126.9780)
        .severityLevel(5)
        .requiresImmediateAction(true)
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(testUser));
    when(guardianRepository.findByUserIdAndCanReceiveAlertsAndIsActive(1L, true, true))
        .thenReturn(Arrays.asList(testGuardian));

    // when
    webSocketService.broadcastEmergencyAlert("test@example.com", request);

    // then
    // WebSocket 메시지 전송 확인
    ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
    verify(messagingTemplate).convertAndSend(
        eq("/user/guardian@example.com/queue/emergency"),
        messageCaptor.capture()
    );

    // 추가 푸시 알림 전송 확인
    verify(notificationService).sendEmergencyAlert(
        eq(guardianUser),
        eq(testUser),
        eq("낙상이 감지되었습니다")
    );
  }

  @Test
  @DisplayName("활동 상태 업데이트 - 배터리 부족 경고 포함")
  void processActivityStatus_WithLowBattery() {
    // given
    ActivityStatusRequest request = ActivityStatusRequest.builder()
        .status(ActivityStatusRequest.ActivityStatus.ACTIVE)
        .batteryLevel(15) // 낮은 배터리
        .stepCount(5000)
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(testUser));
    when(guardianRepository.findByUserIdAndIsActive(1L, true))
        .thenReturn(Arrays.asList(testGuardian));

    // when
    ActivityStatusMessage result = webSocketService.processActivityStatus("test@example.com", request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getBatteryLevel()).isEqualTo(15);
    assertThat(result.getFriendlyMessage()).contains("배터리 부족");
    assertThat(result.getFriendlyMessage()).contains("5000걸음");
  }

  @Test
  @DisplayName("개인 메시지 전송 - 포맷팅 확인")
  void sendPersonalMessage_WithFormatting() {
    // given
    PersonalMessageRequest request = PersonalMessageRequest.builder()
        .targetUserId(2L)
        .content("약 드실 시간입니다")
        .messageType(PersonalMessageRequest.MessageType.REMINDER)
        .priority(4)
        .build();

    User targetUser = User.builder()
        .userId(2L)
        .username("수신자")
        .email("receiver@example.com")
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(testUser));
    when(userRepository.findById(2L))
        .thenReturn(Optional.of(targetUser));

    // when
    PersonalMessage result = webSocketService.sendPersonalMessage("test@example.com", request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getFromUserId()).isEqualTo(1L);
    assertThat(result.getToUserId()).isEqualTo(2L);
    assertThat(result.getFormattedMessage()).startsWith("🔔 알림:");
    assertThat(result.getFormattedMessage()).contains("약 드실 시간입니다");

    verify(messagingTemplate).convertAndSend(
        eq("/user/receiver@example.com/queue/messages"),
        any(PersonalMessage.class)
    );
  }

  @Test
  @DisplayName("사용자를 찾을 수 없는 경우 예외 처리")
  void processLocationUpdate_UserNotFound() {
    // given
    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.empty());

    LocationUpdateRequest request = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .build();

    // when & then
    assertThatThrownBy(() -> 
        webSocketService.processLocationUpdate("test@example.com", request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("사용자를 찾을 수 없습니다");
  }

  @Test
  @DisplayName("채널 구독 관리 테스트")
  void handleChannelSubscription() {
    // when - 구독
    webSocketService.handleChannelSubscription("test@example.com", "location", true);
    
    // then - 로그만 확인 (실제 동작은 내부 맵에 저장)
    // 구독 해제
    webSocketService.handleChannelSubscription("test@example.com", "location", false);
    
    // 메소드 호출이 예외 없이 완료되었는지만 확인
    assertThat(true).isTrue();
  }

  @Test
  @DisplayName("다양한 활동 유형의 친화적 메시지 생성")
  void createFriendlyLocationMessage_VariousActivityTypes() {
    // given
    User user = User.builder()
        .userId(1L)
        .username("테스트사용자")
        .email("test@example.com")
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(user));
    when(guardianRepository.findByUserIdAndCanViewLocationAndIsActive(anyLong(), anyBoolean(), anyBoolean()))
        .thenReturn(List.of());

    // when & then - DRIVING
    LocationUpdateRequest drivingRequest = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .activityType("DRIVING")
        .build();
    
    LocationUpdateMessage drivingResult = webSocketService.processLocationUpdate("test@example.com", drivingRequest);
    assertThat(drivingResult.getMessage()).contains("차로 이동 중이에요");

    // when & then - STATIONARY
    LocationUpdateRequest stationaryRequest = LocationUpdateRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .activityType("STATIONARY")
        .build();
    
    LocationUpdateMessage stationaryResult = webSocketService.processLocationUpdate("test@example.com", stationaryRequest);
    assertThat(stationaryResult.getMessage()).contains("한 곳에 머물러 있어요");
  }

  @Test
  @DisplayName("낙상 알림 브로드캐스트 - 보호자에게 전송")
  void broadcastFallAlert_Success() {
    // given
    when(userRepository.findById(1L))
        .thenReturn(Optional.of(testUser));
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));

    // when
    webSocketService.broadcastFallAlert(1L, "낙상이 감지되었습니다", "심각", 0.95);

    // then
    verify(messagingTemplate).convertAndSendToUser(
        eq("test@example.com"),
        eq("/queue/fall-alert"),
        any(Object.class)
    );
    
    verify(messagingTemplate).convertAndSend(
        eq("/user/guardian@example.com/queue/emergency"),
        any(Object.class)
    );
  }

  @Test
  @DisplayName("포즈 스트림 처리")
  void processPoseStream_Success() {
    // given
    PoseStreamRequest.PoseLandmark landmark = PoseStreamRequest.PoseLandmark.builder()
        .id(1)
        .x(0.5f)
        .y(0.3f)
        .z(0.1f)
        .visibility(0.95f)
        .build();

    PoseStreamRequest request = PoseStreamRequest.builder()
        .frameId(12345L)
        .landmarks(Arrays.asList(landmark))
        .confidenceScore(0.95f)
        .timestamp(System.currentTimeMillis())
        .sessionId("test-session")
        .build();

    when(userRepository.findByEmail("test@example.com"))
        .thenReturn(Optional.of(testUser));

    // when
    PoseStreamMessage result = webSocketService.processPoseStream("test@example.com", request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getFrameId()).isEqualTo(12345L);
    assertThat(result.getConfidenceScore()).isEqualTo(0.95f);
  }
}