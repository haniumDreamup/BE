package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.FallEvent;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 알림 서비스
 * 긴급 상황 및 일반 알림 전송 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
  
  private final GuardianRepository guardianRepository;

  /**
   * 보호자에게 긴급 상황 알림 전송
   */
  public void sendEmergencyNotification(Guardian guardian, Emergency emergency) {
    // TODO: 실제 알림 전송 로직 구현
    // 1. Push 알림 (FCM)
    // 2. SMS 알림
    // 3. 이메일 알림
    
    log.info("긴급 상황 알림 전송: guardianId={}, emergencyId={}, type={}", 
        guardian.getId(), emergency.getId(), emergency.getType());
  }

  /**
   * 일반 푸시 알림 전송
   */
  public void sendPushNotification(Long userId, String title, String message) {
    // TODO: FCM을 통한 푸시 알림 구현
    log.info("푸시 알림 전송: userId={}, title={}", userId, title);
  }
  
  /**
   * 알림 생성 및 전송
   */
  public void createNotification(Long userId, String title, String message) {
    // TODO: 알림 저장 및 전송 로직 구현
    log.info("알림 생성: userId={}, title={}, message={}", userId, title, message);
    sendPushNotification(userId, title, message);
  }

  /**
   * SMS 알림 전송
   */
  public void sendSmsNotification(String phoneNumber, String message) {
    // TODO: SMS 전송 서비스 연동
    log.info("SMS 알림 전송: phoneNumber={}", phoneNumber);
  }

  /**
   * 이메일 알림 전송
   */
  public void sendEmailNotification(String email, String subject, String content) {
    // TODO: 이메일 전송 서비스 구현
    log.info("이메일 알림 전송: email={}, subject={}", email, subject);
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