package com.bifai.reminder.bifai_backend.service.websocket;

import com.bifai.reminder.bifai_backend.dto.websocket.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket 메시지 처리 서비스
 * 실시간 위치 공유, 긴급 알림, 활동 상태 등 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

  private final SimpMessagingTemplate messagingTemplate;
  private final UserRepository userRepository;
  private final GuardianRepository guardianRepository;
  private final NotificationService notificationService;
  
  // 메시지 ID 생성용
  private final AtomicLong messageIdGenerator = new AtomicLong(0);
  
  // 채널별 구독자 추적
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> channelSubscribers = new ConcurrentHashMap<>();

  /**
   * 위치 업데이트 처리
   */
  @Transactional(readOnly = true)
  public LocationUpdateMessage processLocationUpdate(String username, LocationUpdateRequest request) {
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 위치 업데이트 메시지 생성
    LocationUpdateMessage message = LocationUpdateMessage.builder()
        .userId(user.getUserId())
        .username(user.getUsername())
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .accuracy(request.getAccuracy())
        .speed(request.getSpeed())
        .heading(request.getHeading())
        .activityType(request.getActivityType())
        .timestamp(LocalDateTime.now())
        .message(createFriendlyLocationMessage(user.getUsername(), request))
        .build();
    
    // 보호자들에게 위치 업데이트 전송
    List<Guardian> guardians = guardianRepository.findByUserIdAndCanViewLocationAndIsActive(
        user.getUserId(), true, true);
    
    guardians.forEach(guardian -> {
      String destination = "/user/" + guardian.getGuardianUser().getEmail() + "/queue/location";
      messagingTemplate.convertAndSend(destination, message);
    });
    
    return message;
  }

  /**
   * 긴급 알림 브로드캐스트
   */
  @Transactional
  public void broadcastEmergencyAlert(String username, EmergencyAlertRequest request) {
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 보호자들에게 긴급 알림 전송
    List<Guardian> guardians = guardianRepository.findByUserIdAndCanReceiveAlertsAndIsActive(
        user.getUserId(), true, true);
    
    guardians.forEach(guardian -> {
      // WebSocket으로 즉시 전송
      EmergencyAlertMessage alertMessage = EmergencyAlertMessage.builder()
          .alertId(messageIdGenerator.incrementAndGet())
          .patientId(user.getUserId())
          .patientName(user.getUsername())
          .alertType(request.getAlertType().name())
          .alertDescription(request.getAlertType().getDescription())
          .message(request.getMessage())
          .latitude(request.getLatitude())
          .longitude(request.getLongitude())
          .locationDescription(request.getLocationDescription())
          .severityLevel(request.getSeverityLevel())
          .requiresImmediateAction(request.getRequiresImmediateAction())
          .timestamp(LocalDateTime.now())
          .build();
      
      String destination = "/user/" + guardian.getGuardianUser().getEmail() + "/queue/emergency";
      messagingTemplate.convertAndSend(destination, alertMessage);
      
      // 추가로 푸시 알림도 전송
      notificationService.sendEmergencyAlert(guardian.getGuardianUser(), user, request.getMessage());
    });
    
    log.info("긴급 알림 전송 완료 - patient: {}, type: {}, guardians: {}", 
        user.getUsername(), request.getAlertType(), guardians.size());
  }

  /**
   * 활동 상태 처리
   */
  @Transactional(readOnly = true)
  public ActivityStatusMessage processActivityStatus(String username, ActivityStatusRequest request) {
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    ActivityStatusMessage message = ActivityStatusMessage.builder()
        .userId(user.getUserId())
        .username(user.getUsername())
        .status(request.getStatus().name())
        .statusDescription(request.getStatus().getDescription())
        .batteryLevel(request.getBatteryLevel())
        .heartRate(request.getHeartRate())
        .stepCount(request.getStepCount())
        .latitude(request.getCurrentLatitude())
        .longitude(request.getCurrentLongitude())
        .timestamp(LocalDateTime.now())
        .friendlyMessage(createFriendlyActivityMessage(user.getUsername(), request))
        .build();
    
    // 보호자들에게 활동 상태 업데이트 전송
    List<Guardian> guardians = guardianRepository.findByUserIdAndIsActive(
        user.getUserId(), true);
    
    guardians.forEach(guardian -> {
      String destination = "/user/" + guardian.getGuardianUser().getEmail() + "/queue/activity";
      messagingTemplate.convertAndSend(destination, message);
    });
    
    return message;
  }

  /**
   * 포즈 데이터 스트리밍 처리
   */
  public PoseStreamMessage processPoseStream(String username, PoseStreamRequest request) {
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 간단한 분석 결과 생성 (실제로는 FallDetectionService와 연동)
    PoseStreamMessage message = PoseStreamMessage.builder()
        .userId(user.getUserId())
        .frameId(request.getFrameId())
        .sessionId(request.getSessionId())
        .fallDetected(false)
        .confidenceScore(request.getConfidenceScore())
        .timestamp(LocalDateTime.now())
        .analysisResult("정상 자세")
        .build();
    
    return message;
  }

  /**
   * 개인 메시지 전송
   */
  @Transactional
  public PersonalMessage sendPersonalMessage(String fromUsername, PersonalMessageRequest request) {
    User fromUser = userRepository.findByEmail(fromUsername)
        .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다"));
    
    User toUser = userRepository.findById(request.getTargetUserId())
        .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다"));
    
    PersonalMessage message = PersonalMessage.builder()
        .messageId(messageIdGenerator.incrementAndGet())
        .fromUserId(fromUser.getUserId())
        .fromUsername(fromUser.getUsername())
        .toUserId(toUser.getUserId())
        .content(request.getContent())
        .messageType(request.getMessageType() != null ? 
            request.getMessageType().name() : PersonalMessageRequest.MessageType.TEXT.name())
        .priority(request.getPriority() != null ? request.getPriority() : 3)
        .timestamp(LocalDateTime.now())
        .delivered(false)
        .read(false)
        .formattedMessage(formatMessageForBifUser(request))
        .build();
    
    // 수신자에게 메시지 전송
    String destination = "/user/" + toUser.getEmail() + "/queue/messages";
    messagingTemplate.convertAndSend(destination, message);
    
    return message;
  }

  /**
   * 채널 구독 처리
   */
  public void handleChannelSubscription(String username, String channel, boolean subscribe) {
    channelSubscribers.putIfAbsent(channel, new ConcurrentHashMap<>());
    
    if (subscribe) {
      channelSubscribers.get(channel).put(username, true);
      log.info("채널 구독 추가 - user: {}, channel: {}", username, channel);
    } else {
      channelSubscribers.get(channel).remove(username);
      log.info("채널 구독 해제 - user: {}, channel: {}", username, channel);
    }
  }

  /**
   * 사용자 친화적 위치 메시지 생성
   */
  private String createFriendlyLocationMessage(String username, LocationUpdateRequest request) {
    StringBuilder message = new StringBuilder();
    message.append(username).append("님이 ");
    
    if (request.getActivityType() != null) {
      switch (request.getActivityType()) {
        case "WALKING":
          message.append("걷고 있어요");
          break;
        case "DRIVING":
          message.append("차로 이동 중이에요");
          break;
        case "STATIONARY":
          message.append("한 곳에 머물러 있어요");
          break;
        default:
          message.append("이동 중이에요");
      }
    } else {
      message.append("위치를 공유했어요");
    }
    
    if (request.getSpeed() != null && request.getSpeed() > 0) {
      message.append(" (속도: ").append(String.format("%.1f", request.getSpeed())).append("m/s)");
    }
    
    return message.toString();
  }

  /**
   * 사용자 친화적 활동 메시지 생성
   */
  private String createFriendlyActivityMessage(String username, ActivityStatusRequest request) {
    StringBuilder message = new StringBuilder();
    message.append(username).append("님이 ");
    message.append(request.getStatus().getDescription());
    
    if (request.getBatteryLevel() != null && request.getBatteryLevel() < 20) {
      message.append(" (배터리 부족: ").append(request.getBatteryLevel()).append("%)");
    }
    
    if (request.getStepCount() != null && request.getStepCount() > 0) {
      message.append(" - 오늘 ").append(request.getStepCount()).append("걸음");
    }
    
    return message.toString();
  }

  /**
   * BIF 사용자를 위한 메시지 포맷팅
   */
  private String formatMessageForBifUser(PersonalMessageRequest request) {
    StringBuilder formatted = new StringBuilder();
    
    if (request.getMessageType() != null) {
      switch (request.getMessageType()) {
        case REMINDER:
          formatted.append("🔔 알림: ");
          break;
        case INSTRUCTION:
          formatted.append("📋 할 일: ");
          break;
        case ENCOURAGEMENT:
          formatted.append("💪 응원: ");
          break;
        case CHECK_IN:
          formatted.append("👋 안부: ");
          break;
        default:
          formatted.append("💬 메시지: ");
      }
    }
    
    formatted.append(request.getContent());
    
    return formatted.toString();
  }
  
  /**
   * 낙상 알림 브로드캐스트
   */
  public void broadcastFallAlert(Long userId, String fallType, String severity, double confidence) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 낙상 알림 메시지 생성
    FallAlertMessage fallAlert = FallAlertMessage.builder()
        .userId(userId)
        .username(user.getUsername())
        .fallType(fallType)
        .severity(severity)
        .confidence(confidence)
        .message(createFallAlertMessage(user.getUsername(), fallType, severity))
        .timestamp(LocalDateTime.now())
        .build();
    
    // 본인에게 알림
    messagingTemplate.convertAndSendToUser(
        user.getEmail(),
        "/queue/fall-alert",
        fallAlert
    );
    
    // 보호자들에게 알림
    List<Guardian> guardians = guardianRepository.findByUserAndIsActiveTrue(user);
    for (Guardian guardian : guardians) {
      // 보호자용 상세 알림
      GuardianFallAlert guardianAlert = GuardianFallAlert.builder()
          .patientId(userId)
          .patientName(user.getUsername())
          .fallType(fallType)
          .severity(severity)
          .confidence(confidence)
          .message(createGuardianFallAlertMessage(user.getUsername(), fallType, severity))
          .requiresImmediateAction(isImmediateActionRequired(severity))
          .timestamp(LocalDateTime.now())
          .build();
      
      messagingTemplate.convertAndSend(
          "/user/" + guardian.getGuardianUser().getEmail() + "/queue/emergency",
          guardianAlert
      );
      
      // 심각한 경우 추가 알림
      if (isImmediateActionRequired(severity)) {
        // TODO: NotificationService에 낙상 알림 메서드 추가 필요
        // notificationService.sendFallEmergencyNotification(guardian, user, fallType, severity);
        log.error("심각한 낙상 감지 - 추가 알림 필요: guardian={}, patient={}, severity={}", 
            guardian.getGuardianUser().getEmail(), user.getUsername(), severity);
      }
    }
    
    log.warn("낙상 알림 전송 - userId: {}, type: {}, severity: {}, guardians: {}", 
        userId, fallType, severity, guardians.size());
  }
  
  /**
   * 낙상 알림 메시지 생성
   */
  private String createFallAlertMessage(String username, String fallType, String severity) {
    return String.format("%s님, 넘어진 것 같아요. %s 정도예요. 괜찮으신가요?", 
        username, severity);
  }
  
  /**
   * 보호자용 낙상 알림 메시지 생성
   */
  private String createGuardianFallAlertMessage(String patientName, String fallType, String severity) {
    return String.format("[긴급] %s님이 넘어졌습니다. 상태: %s (%s). 즉시 확인이 필요합니다.", 
        patientName, severity, fallType);
  }
  
  /**
   * 즉각적인 조치 필요 여부 판단
   */
  private boolean isImmediateActionRequired(String severity) {
    return "심각".equals(severity) || "위급".equals(severity);
  }
  
  /**
   * 긴급 알림 메시지 DTO (내부 클래스)
   */
  @lombok.Data
  @lombok.Builder
  private static class EmergencyAlertMessage {
    private Long alertId;
    private Long patientId;
    private String patientName;
    private String alertType;
    private String alertDescription;
    private String message;
    private Double latitude;
    private Double longitude;
    private String locationDescription;
    private Integer severityLevel;
    private Boolean requiresImmediateAction;
    private LocalDateTime timestamp;
  }
  
  /**
   * 낙상 알림 메시지 DTO
   */
  @lombok.Data
  @lombok.Builder
  private static class FallAlertMessage {
    private Long userId;
    private String username;
    private String fallType;
    private String severity;
    private double confidence;
    private String message;
    private LocalDateTime timestamp;
  }
  
  /**
   * 보호자용 낙상 알림 메시지 DTO
   */
  @lombok.Data
  @lombok.Builder
  private static class GuardianFallAlert {
    private Long patientId;
    private String patientName;
    private String fallType;
    private String severity;
    private double confidence;
    private String message;
    private boolean requiresImmediateAction;
    private LocalDateTime timestamp;
  }
}