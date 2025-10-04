package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.FallEvent;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 알림 서비스
 * 긴급 상황 및 일반 알림 전송 담당
 */
@Slf4j
@Service
public class NotificationService {

  private final GuardianRepository guardianRepository;
  private final com.bifai.reminder.bifai_backend.service.mobile.FcmService fcmService;

  private static final com.bifai.reminder.bifai_backend.service.mobile.FcmService.NotificationCategory DEFAULT_CATEGORY =
      com.bifai.reminder.bifai_backend.service.mobile.FcmService.NotificationCategory.REMINDER;
  private static final com.bifai.reminder.bifai_backend.service.mobile.FcmService.Priority DEFAULT_PRIORITY =
      com.bifai.reminder.bifai_backend.service.mobile.FcmService.Priority.NORMAL;
  private final com.bifai.reminder.bifai_backend.service.notification.NotificationScheduler notificationScheduler;
  private final com.bifai.reminder.bifai_backend.repository.UserRepository userRepository;
  private final com.bifai.reminder.bifai_backend.repository.DeviceRepository deviceRepository;

  public NotificationService(
      GuardianRepository guardianRepository,
      @Autowired(required = false) com.bifai.reminder.bifai_backend.service.mobile.FcmService fcmService,
      com.bifai.reminder.bifai_backend.service.notification.NotificationScheduler notificationScheduler,
      com.bifai.reminder.bifai_backend.repository.UserRepository userRepository,
      com.bifai.reminder.bifai_backend.repository.DeviceRepository deviceRepository) {
    this.guardianRepository = guardianRepository;
    this.fcmService = fcmService;
    this.notificationScheduler = notificationScheduler;
    this.userRepository = userRepository;
    this.deviceRepository = deviceRepository;
  }

  /**
   * 보호자에게 긴급 상황 알림 전송
   */
  public void sendEmergencyNotification(Guardian guardian, Emergency emergency) {
    try {
      String emergencyTitle = "🚨 긴급 상황 발생";
      String emergencyMessage = String.format("%s님에게 긴급 상황이 발생했습니다: %s", 
          emergency.getUser().getUsername(), emergency.getType().name());
      
      // 1. Push 알림 (FCM) - 보호자의 사용자 계정으로 전송
      if (guardian.getGuardianUser() != null) {
        sendPushNotification(guardian.getGuardianUser().getUserId(), emergencyTitle, emergencyMessage);
      }
      
      // 2. SMS 알림 - 긴급 상황이므로 항상 전송
      if (guardian.getPrimaryPhone() != null) {
        sendSmsNotification(guardian.getPrimaryPhone(), emergencyTitle + " - " + emergencyMessage);
      }
      
      // 3. 이메일 알림 - 상세 정보 포함
      if (guardian.getEmail() != null) {
        String detailedMessage = String.format(
            "긴급 상황 상세 정보\\n\\n" +
            "대상자: %s\\n" +
            "발생 시간: %s\\n" +
            "긴급 상황 유형: %s\\n\\n" +
            "즉시 대상자의 상태를 확인하시고, 필요시 119에 신고해주세요.",
            emergency.getUser().getUsername(),
            emergency.getCreatedAt(),
            emergency.getType().name()
        );
        sendEmailNotification(guardian.getEmail(), emergencyTitle, detailedMessage);
      }
      
      log.info("긴급 상황 알림 전송 완료: guardianId={}, emergencyId={}, type={}", 
          guardian.getId(), emergency.getId(), emergency.getType());
          
    } catch (Exception e) {
      log.error("긴급 상황 알림 전송 실패: guardianId={}, emergencyId={}, error={}", 
          guardian.getId(), emergency.getId(), e.getMessage());
      throw new RuntimeException("긴급 알림 전송에 실패했습니다", e);
    }
  }

  /**
   * 일반 푸시 알림 전송
   */
  public void sendPushNotification(Long userId, String title, String message) {
    try {
      if (fcmService == null) {
        log.warn("FCM 서비스가 비활성화되어 푸시 알림을 전송할 수 없습니다: userId={}", userId);
        return;
      }

      String fcmToken = getFcmTokenForUser(userId);
      if (fcmToken != null) {
        fcmService.sendNotification(fcmToken, title, message, null, DEFAULT_CATEGORY, DEFAULT_PRIORITY);
        log.info("푸시 알림 전송 완료: userId={}, title={}", userId, title);
      } else {
        log.warn("FCM 토큰을 찾을 수 없음: userId={}", userId);
      }
    } catch (Exception e) {
      log.error("푸시 알림 전송 실패: userId={}, error={}", userId, e.getMessage());
    }
  }
  
  /**
   * 알림 생성 및 전송
   */
  public void createNotification(Long userId, String title, String message) {
    try {
      log.info("알림 생성: userId={}, title={}, message={}", userId, title, message);
      
      // TODO: 향후 알림 히스토리 저장이 필요한 경우 데이터베이스 저장 로직 추가
      // notificationRepository.save(new Notification(userId, title, message));
      
      // FCM을 통한 푸시 알림 전송
      sendPushNotification(userId, title, message);
      
    } catch (Exception e) {
      log.error("알림 생성 및 전송 실패: userId={}, error={}", userId, e.getMessage());
      throw new RuntimeException("알림 전송에 실패했습니다", e);
    }
  }

  /**
   * SMS 알림 전송
   */
  public void sendSmsNotification(String phoneNumber, String message) {
    // SMS 전송은 별도의 SMS 서비스 (예: AWS SNS, 국내 SMS 서비스) 연동 필요
    // 현재는 로깅으로 대체하고 향후 필요시 구현
    log.info("SMS 알림 전송 요청: phoneNumber={}, message={}", 
        phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length()-4), message);
    
    // TODO: 실제 SMS 서비스 연동
    // 예시: smsService.sendSms(phoneNumber, message);
  }

  /**
   * 이메일 알림 전송
   */
  public void sendEmailNotification(String email, String subject, String content) {
    // 이메일 전송은 별도의 이메일 서비스 (예: Spring Mail, AWS SES) 연동 필요
    // 현재는 로깅으로 대체하고 향후 필요시 구현
    String maskedEmail = email.substring(0, 3) + "***@" + email.substring(email.indexOf("@") + 1);
    log.info("이메일 알림 전송 요청: email={}, subject={}", maskedEmail, subject);
    
    // TODO: 실제 이메일 서비스 연동
    // 예시: emailService.sendEmail(email, subject, content);
  }

  /**
   * 낙상 감지 알림 전송
   * 모든 활성 보호자에게 긴급 알림 전송
   */
  public void sendFallAlert(FallEvent fallEvent) {
    // 1. 사용자의 모든 활성 보호자 조회
    List<Guardian> activeGuardians = guardianRepository.findByUserAndIsActiveTrue(
        fallEvent.getUser()
    );
    
    if (activeGuardians.isEmpty()) {
      log.warn("낙상 감지되었으나 알림 받을 보호자가 없습니다. userId={}", 
          fallEvent.getUser().getUserId());
      return;
    }
    
    // 2. 심각도에 따른 메시지 생성
    String alertTitle = "🚨 긴급: 낙상 감지";
    String alertMessage = createFallAlertMessage(fallEvent);
    
    // 3. 모든 보호자에게 알림 전송
    for (Guardian guardian : activeGuardians) {
      try {
        // Push 알림 (최우선) - 보호자의 사용자 정보로 전송
        if (guardian.getGuardianUser() != null) {
          sendPushNotification(
              guardian.getGuardianUser().getUserId(), 
              alertTitle, 
              alertMessage
          );
        }
        
        // SMS 알림 (긴급) - 보호자의 주 연락처로 전송
        if (guardian.getPrimaryPhone() != null && 
            fallEvent.getSeverity() != FallEvent.FallSeverity.LOW) {
          sendSmsNotification(
              guardian.getPrimaryPhone(), 
              alertTitle + " - " + alertMessage
          );
        }
        
        // 이메일 알림 (기록용)
        if (guardian.getEmail() != null) {
          String detailedContent = createDetailedFallReport(fallEvent);
          sendEmailNotification(
              guardian.getEmail(),
              alertTitle + " - " + fallEvent.getUser().getUsername(),
              detailedContent
          );
        }
        
        log.info("낙상 알림 전송 완료: guardianId={}, fallEventId={}, severity={}", 
            guardian.getId(), fallEvent.getId(), fallEvent.getSeverity());
            
      } catch (Exception e) {
        log.error("보호자 알림 전송 실패: guardianId={}, error={}", 
            guardian.getId(), e.getMessage());
      }
    }
  }
  
  /**
   * 낙상 알림 메시지 생성
   */
  private String createFallAlertMessage(FallEvent fallEvent) {
    String userName = fallEvent.getUser().getUsername();
    String time = fallEvent.getDetectedAt().toLocalTime().toString().substring(0, 5);
    
    switch (fallEvent.getSeverity()) {
      case CRITICAL:
        return String.format("%s님이 넘어졌어요! 지금 바로 확인해주세요! (시간: %s)", 
            userName, time);
      case HIGH:
        return String.format("%s님이 넘어진 것 같아요. 빨리 확인해주세요. (시간: %s)", 
            userName, time);
      case MEDIUM:
        return String.format("%s님이 넘어졌을 수 있어요. 확인이 필요해요. (시간: %s)", 
            userName, time);
      case LOW:
        return String.format("%s님의 움직임이 이상해요. 확인해보세요. (시간: %s)", 
            userName, time);
      default:
        return String.format("%s님에게 문제가 생겼어요. 확인해주세요. (시간: %s)", 
            userName, time);
    }
  }
  
  /**
   * 상세 낙상 리포트 생성 (이메일용)
   */
  private String createDetailedFallReport(FallEvent fallEvent) {
    return String.format(
        "낙상 감지 상세 정보\n\n" +
        "대상자: %s\n" +
        "발생 시간: %s\n" +
        "심각도: %s\n" +
        "신뢰도: %.1f%%\n" +
        "신체 각도: %.1f도\n" +
        "상태: %s\n\n" +
        "즉시 대상자의 상태를 확인하시고, 필요시 119에 신고해주세요.\n" +
        "이 알림은 AI가 자동으로 감지한 것으로, 오탐지일 수 있습니다.",
        fallEvent.getUser().getUsername(),
        fallEvent.getDetectedAt(),
        translateSeverity(fallEvent.getSeverity()),
        fallEvent.getConfidenceScore() * 100,
        fallEvent.getBodyAngle(),
        translateStatus(fallEvent.getStatus())
    );
  }
  
  /**
   * 심각도 한글 번역
   */
  private String translateSeverity(FallEvent.FallSeverity severity) {
    switch (severity) {
      case CRITICAL: return "매우 위험";
      case HIGH: return "위험";
      case MEDIUM: return "주의";
      case LOW: return "경미";
      default: return "알 수 없음";
    }
  }
  
  /**
   * 상태 한글 번역
   */
  private String translateStatus(FallEvent.EventStatus status) {
    switch (status) {
      case DETECTED: return "감지됨";
      case NOTIFIED: return "알림 전송됨";
      case ACKNOWLEDGED: return "확인됨";
      case RESOLVED: return "해결됨";
      case FALSE_POSITIVE: return "오탐지";
      default: return "알 수 없음";
    }
  }
  
  /**
   * 사용자의 FCM 토큰 조회
   */
  private String getFcmTokenForUser(Long userId) {
    try {
      // 사용자의 활성 디바이스 찾기
      List<com.bifai.reminder.bifai_backend.entity.Device> activeDevices = deviceRepository.findActiveDevicesByUserId(userId);
      
      if (activeDevices.isEmpty()) {
        return null;
      }
      
      // 가장 최근에 사용된 디바이스의 FCM 토큰 반환
      com.bifai.reminder.bifai_backend.entity.Device primaryDevice = activeDevices.stream()
          .filter(d -> d.getFcmToken() != null && !d.getFcmToken().isEmpty())
          .findFirst()
          .orElse(null);
      
      return primaryDevice != null ? primaryDevice.getFcmToken() : null;
      
    } catch (Exception e) {
      log.error("FCM 토큰 조회 실패: userId={}, error={}", userId, e.getMessage());
      return null;
    }
  }
  
  /**
   * WebSocket을 통한 긴급 알림 전송
   * 보호자 사용자에게 환자의 긴급 상황 알림
   */
  public void sendEmergencyAlert(User guardianUser, User patient, String message) {
    try {
      // Push 알림
      String alertTitle = "🚨 긴급 알림: " + patient.getUsername();
      sendPushNotification(guardianUser.getUserId(), alertTitle, message);
      
      // SMS 알림 (보호자의 전화번호가 있는 경우)
      // 실제 구현에서는 Guardian 엔티티에서 전화번호를 가져와야 함
      
      log.info("긴급 알림 전송 완료: guardianUserId={}, patientUserId={}, message={}", 
          guardianUser.getUserId(), patient.getUserId(), message);
          
    } catch (Exception e) {
      log.error("긴급 알림 전송 실패: guardianUserId={}, patientUserId={}, error={}", 
          guardianUser.getUserId(), patient.getUserId(), e.getMessage());
    }
  }
}