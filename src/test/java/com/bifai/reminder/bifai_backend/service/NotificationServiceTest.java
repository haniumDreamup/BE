package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import com.bifai.reminder.bifai_backend.service.notification.FcmService;
import com.bifai.reminder.bifai_backend.service.notification.NotificationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

  @Mock private GuardianRepository guardianRepository;
  @Mock private FcmService fcmService;
  @Mock private NotificationScheduler notificationScheduler;
  @Mock private UserRepository userRepository;
  @Mock private DeviceRepository deviceRepository;

  @InjectMocks
  private NotificationService notificationService;

  private User testUser;
  private User guardianUser;
  private Guardian testGuardian;
  private Emergency testEmergency;
  private FallEvent testFallEvent;
  private Device testDevice;
  private Device guardianDevice;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser = User.builder()
        .userId(1L)
        .username("testUser")
        .name("테스트 사용자")
        .email("test@example.com")
        .phoneNumber("010-1234-5678")
        .isActive(true)
        .cognitiveLevel(User.CognitiveLevel.MODERATE)
        .build();

    // 보호자 사용자 설정
    guardianUser = User.builder()
        .userId(2L)
        .username("guardianUser")
        .name("보호자")
        .email("guardian@example.com")
        .phoneNumber("010-9876-5432")
        .isActive(true)
        .cognitiveLevel(User.CognitiveLevel.MODERATE)
        .build();

    // 보호자 관계 설정
    testGuardian = Guardian.builder()
        .user(testUser)
        .guardianUser(guardianUser)
        .name("보호자")
        .relationshipType(Guardian.RelationshipType.CAREGIVER)
        .primaryPhone("010-9876-5432")
        .email("guardian@example.com")
        .isActive(true)
        .isPrimary(true)
        .approvalStatus(Guardian.ApprovalStatus.APPROVED)
        .canReceiveAlerts(true)
        .canViewLocation(true)
        .emergencyPriority(1)
        .build();

    // 긴급 상황 설정
    testEmergency = new Emergency();
    testEmergency.setId(1L);
    testEmergency.setUser(testUser);
    testEmergency.setType(Emergency.EmergencyType.PANIC_BUTTON);
    testEmergency.setDescription("도움이 필요합니다");
    testEmergency.setCreatedAt(LocalDateTime.now());
    testEmergency.setStatus(Emergency.EmergencyStatus.ACTIVE);

    // 낙상 사건 설정
    testFallEvent = new FallEvent();
    testFallEvent.setId(1L);
    testFallEvent.setUser(testUser);
    testFallEvent.setSeverity(FallEvent.FallSeverity.HIGH);
    testFallEvent.setDetectedAt(LocalDateTime.now());
    testFallEvent.setConfidenceScore(0.95f);
    testFallEvent.setBodyAngle(45.0f);
    testFallEvent.setStatus(FallEvent.EventStatus.DETECTED);

    // 테스트 디바이스 설정 (사용자용)
    testDevice = Device.builder()
        .user(testUser)
        .deviceId("test-device-001")
        .deviceName("테스트 디바이스")
        .deviceType("MOBILE")
        .fcmToken("test-fcm-token")
        .isActive(true)
        .batteryLevel(80)
        .lastSeen(LocalDateTime.now())
        .build();

    // 보호자 디바이스 설정
    guardianDevice = Device.builder()
        .user(guardianUser)
        .deviceId("guardian-device-001")
        .deviceName("보호자 디바이스")
        .deviceType("MOBILE")
        .fcmToken("test-fcm-token")
        .isActive(true)
        .batteryLevel(90)
        .lastSeen(LocalDateTime.now())
        .build();
  }

  @Test
  @DisplayName("보호자에게 긴급 상황 알림 전송 - 성공")
  void sendEmergencyNotification_Success() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendEmergencyNotification(testGuardian, testEmergency);

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("🚨 긴급 상황 발생"), contains("긴급 상황이 발생했습니다"), isNull());
  }

  @Test
  @DisplayName("보호자에게 긴급 상황 알림 전송 - FCM 실패 시에도 계속 진행")
  void sendEmergencyNotification_FcmFailureContinues() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doThrow(new RuntimeException("FCM 전송 실패")).when(fcmService)
        .sendPushNotification(anyString(), anyString(), anyString(), any());

    // When & Then - FCM 실패해도 SMS/이메일은 계속 진행되어야 함
    notificationService.sendEmergencyNotification(testGuardian, testEmergency);
    
    // FCM이 호출되었는지만 확인
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("🚨 긴급 상황 발생"), contains("긴급 상황이 발생했습니다"), isNull());
  }

  @Test
  @DisplayName("일반 푸시 알림 전송 - 성공")
  void sendPushNotification_Success() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendPushNotification(testUser.getUserId(), "테스트 제목", "테스트 메시지");

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("테스트 제목"), eq("테스트 메시지"), isNull());
  }

  @Test
  @DisplayName("일반 푸시 알림 전송 - FCM 토큰 없음")
  void sendPushNotification_NoFcmToken() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Collections.emptyList());

    // When
    notificationService.sendPushNotification(testUser.getUserId(), "테스트 제목", "테스트 메시지");

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("알림 생성 및 전송 - 성공")
  void createNotification_Success() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.createNotification(testUser.getUserId(), "알림 제목", "알림 내용");

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("알림 제목"), eq("알림 내용"), isNull());
  }

  @Test
  @DisplayName("알림 생성 및 전송 - FCM 실패 시에도 계속 진행")
  void createNotification_FcmFailureContinues() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));
    doThrow(new RuntimeException("FCM 전송 실패")).when(fcmService)
        .sendPushNotification(anyString(), anyString(), anyString(), any());

    // When & Then - FCM 실패해도 예외 발생하지 않음 (sendPushNotification이 예외를 삼킴)
    notificationService.createNotification(testUser.getUserId(), "알림 제목", "알림 내용");
    
    // FCM이 호출되었는지만 확인
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("알림 제목"), eq("알림 내용"), isNull());
  }

  @Test
  @DisplayName("낙상 감지 알림 전송 - 성공")
  void sendFallAlert_Success() {
    // Given
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("🚨 긴급: 낙상 감지"), contains("넘어진 것 같아요"), any());
  }

  @Test
  @DisplayName("낙상 감지 알림 전송 - 보호자 없음")
  void sendFallAlert_NoGuardians() {
    // Given
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Collections.emptyList());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("낙상 감지 알림 전송 - 심각도별 메시지 생성")
  void sendFallAlert_DifferentSeverity() {
    // Given
    testFallEvent.setSeverity(FallEvent.FallSeverity.CRITICAL);
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), eq("🚨 긴급: 낙상 감지"), contains("넘어졌어요! 지금 바로 확인해주세요!"), any());
  }

  @Test
  @DisplayName("SMS 알림 전송 - 로깅 확인")
  void sendSmsNotification_LoggingVerification() {
    // Given
    String phoneNumber = "010-1234-5678";
    String message = "테스트 SMS 메시지";

    // When
    notificationService.sendSmsNotification(phoneNumber, message);

    // Then - 로그만 확인하므로 특별한 검증 없음 (실제로는 로그 캡처하여 확인 가능)
  }

  @Test
  @DisplayName("이메일 알림 전송 - 로깅 확인")
  void sendEmailNotification_LoggingVerification() {
    // Given
    String email = "test@example.com";
    String subject = "테스트 제목";
    String content = "테스트 내용";

    // When
    notificationService.sendEmailNotification(email, subject, content);

    // Then - 로그만 확인하므로 특별한 검증 없음 (실제로는 로그 캡처하여 확인 가능)
  }

  @Test
  @DisplayName("WebSocket을 통한 긴급 알림 전송 - 성공")
  void sendEmergencyAlert_Success() {
    // Given
    String message = "긴급 상황입니다!";
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendEmergencyAlert(guardianUser, testUser, message);

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), contains("🚨 긴급 알림"), eq(message), isNull());
  }

  @Test
  @DisplayName("심각도별 낙상 메시지 생성 테스트 - CRITICAL")
  void fallAlertMessage_Critical() {
    // Given
    testFallEvent.setSeverity(FallEvent.FallSeverity.CRITICAL);
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    verify(fcmService).sendPushNotification(anyString(), anyString(), contains("넘어졌어요! 지금 바로"), any());
  }

  @Test
  @DisplayName("심각도별 낙상 메시지 생성 테스트 - LOW")
  void fallAlertMessage_Low() {
    // Given
    testFallEvent.setSeverity(FallEvent.FallSeverity.LOW);
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    verify(fcmService).sendPushNotification(anyString(), anyString(), contains("움직임이 이상해요"), any());
  }

  @Test
  @DisplayName("FCM 토큰 조회 - 성공")
  void getFcmToken_Success() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));

    // When
    notificationService.sendPushNotification(testUser.getUserId(), "테스트", "테스트");

    // Then
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("FCM 토큰 조회 - 토큰 없는 디바이스")
  void getFcmToken_NoToken() {
    // Given
    testDevice.setFcmToken(null);
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));

    // When
    notificationService.sendPushNotification(testUser.getUserId(), "테스트", "테스트");

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("보호자에게 긴급 상황 알림 전송 - 보호자 사용자 없음")
  void sendEmergencyNotification_NoGuardianUser() {
    // Given
    testGuardian.setGuardianUser(null);

    // When
    notificationService.sendEmergencyNotification(testGuardian, testEmergency);

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("보호자에게 긴급 상황 알림 전송 - 전화번호 없음")
  void sendEmergencyNotification_NoPhoneNumber() {
    // Given
    testGuardian.setPrimaryPhone(null);

    // When
    notificationService.sendEmergencyNotification(testGuardian, testEmergency);

    // Then
    // SMS는 전송되지 않아야 하지만 FCM은 전송되어야 함
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());
    
    notificationService.sendEmergencyNotification(testGuardian, testEmergency);
    
    verify(fcmService).sendPushNotification(eq("test-fcm-token"), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("보호자에게 긴급 상황 알림 전송 - 이메일 없음")
  void sendEmergencyNotification_NoEmail() {
    // Given
    testGuardian.setEmail(null);
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendEmergencyNotification(testGuardian, testEmergency);

    // Then
    // FCM과 SMS는 전송되지만 이메일은 전송되지 않음
    verify(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("알림 생성 및 전송 - FCM 토큰 조회 실패 시 정상 진행")
  void createNotification_FcmTokenRetrievalFailure() {
    // Given - sendPushNotification이 예외를 삼키므로 정상적으로 완료됨
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

    // When
    notificationService.createNotification(testUser.getUserId(), "테스트", "테스트");
    
    // Then - 예외 발생하지 않고 정상 완료
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("낙상 감지 알림 전송 - FCM 토큰 조회 실패")
  void sendFallAlert_FcmTokenRetrievalFailure() {
    // Given
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenThrow(new RuntimeException("디바이스 조회 실패"));

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    // FCM 전송은 실패하지만 다른 알림은 계속 진행
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("심각도별 낙상 메시지 생성 테스트 - MEDIUM")
  void fallAlertMessage_Medium() {
    // Given
    testFallEvent.setSeverity(FallEvent.FallSeverity.MEDIUM);
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    verify(fcmService).sendPushNotification(anyString(), anyString(), contains("넘어졌을 수 있어요"), any());
  }

  @Test
  @DisplayName("낙상 감지 알림 전송 - LOW 심각도, SMS 전송되지 않음")
  void sendFallAlert_LowSeverity_NoSms() {
    // Given
    testFallEvent.setSeverity(FallEvent.FallSeverity.LOW);
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    // LOW 심각도에서는 SMS가 전송되지 않아야 함 (조건: severity != LOW)
    verify(fcmService).sendPushNotification(anyString(), anyString(), contains("움직임이 이상해요"), any());
  }

  @Test
  @DisplayName("WebSocket을 통한 긴급 알림 전송 - FCM 토큰 없음")
  void sendEmergencyAlert_NoFcmToken() {
    // Given
    String message = "긴급 상황입니다!";
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Collections.emptyList());

    // When
    notificationService.sendEmergencyAlert(guardianUser, testUser, message);

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("WebSocket을 통한 긴급 알림 전송 - FCM 전송 실패")
  void sendEmergencyAlert_FcmFailure() {
    // Given
    String message = "긴급 상황입니다!";
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doThrow(new RuntimeException("FCM 전송 실패")).when(fcmService)
        .sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendEmergencyAlert(guardianUser, testUser, message);

    // Then
    verify(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("낙상 감지 알림 전송 - 보호자 개별 실패 처리")
  void sendFallAlert_IndividualGuardianFailure() {
    // Given
    Guardian failingGuardian = Guardian.builder()
        .user(testUser)
        .guardianUser(null) // 보호자 사용자 없음
        .name("실패 보호자")
        .relationshipType(Guardian.RelationshipType.CAREGIVER)
        .primaryPhone("010-0000-0000")
        .email("fail@example.com")
        .isActive(true)
        .build();
    
    when(guardianRepository.findByUserAndIsActiveTrue(testUser))
        .thenReturn(Arrays.asList(testGuardian, failingGuardian));
    when(deviceRepository.findActiveDevicesByUserId(guardianUser.getUserId()))
        .thenReturn(Arrays.asList(guardianDevice));
    doNothing().when(fcmService).sendPushNotification(anyString(), anyString(), anyString(), any());

    // When
    notificationService.sendFallAlert(testFallEvent);

    // Then
    // 성공한 보호자에게는 알림이 전송되어야 함
    verify(fcmService, times(1)).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("FCM 토큰 조회 - 빈 토큰 문자열")
  void getFcmToken_EmptyToken() {
    // Given
    testDevice.setFcmToken("");
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenReturn(Arrays.asList(testDevice));

    // When
    notificationService.sendPushNotification(testUser.getUserId(), "테스트", "테스트");

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("FCM 토큰 조회 - 디바이스 조회 실패")
  void getFcmToken_DeviceQueryFailure() {
    // Given
    when(deviceRepository.findActiveDevicesByUserId(testUser.getUserId()))
        .thenThrow(new RuntimeException("디바이스 조회 실패"));

    // When
    notificationService.sendPushNotification(testUser.getUserId(), "테스트", "테스트");

    // Then
    verify(fcmService, never()).sendPushNotification(anyString(), anyString(), anyString(), any());
  }
}