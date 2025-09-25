package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.sos.SosRequest;
import com.bifai.reminder.bifai_backend.dto.sos.SosResponse;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.EmergencyStatus;
import com.bifai.reminder.bifai_backend.entity.Emergency.EmergencyType;
import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.EmergencyContactRepository;
import com.bifai.reminder.bifai_backend.repository.EmergencyRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SOS 서비스
 * 원터치 긴급 도움 요청 처리
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SosService {

  private final EmergencyRepository emergencyRepository;
  private final EmergencyContactRepository emergencyContactRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  /**
   * SOS 발동
   */
  public SosResponse triggerSos(Long userId, SosRequest request) {
    log.warn("SOS 발동! 사용자: {}, 위치: {},{}", 
        userId, request.getLatitude(), request.getLongitude());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 긴급 상황 생성
    Emergency emergency = createEmergency(user, request);

    // 긴급 연락처에 알림
    List<String> notifiedContacts = notifyEmergencyContacts(user, emergency, request);

    // 응답 생성
    return buildSosResponse(emergency, notifiedContacts, request);
  }

  /**
   * 긴급 상황 생성
   */
  private Emergency createEmergency(User user, SosRequest request) {
    EmergencyType type = mapEmergencyType(request.getEmergencyType());
    
    Emergency emergency = Emergency.builder()
        .user(user)
        .type(type)
        .status(EmergencyStatus.TRIGGERED)
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .address(request.getAddress())
        .description(request.getMessage())
        .triggeredAt(LocalDateTime.now())
        .notificationSent(false)
        .notificationCount(0)
        .build();

    emergency = emergencyRepository.save(emergency);
    log.info("긴급 상황 생성: ID {}, 타입 {}", emergency.getId(), type);
    
    return emergency;
  }

  /**
   * 긴급 연락처 알림
   */
  private List<String> notifyEmergencyContacts(
      User user, 
      Emergency emergency, 
      SosRequest request) {
    
    List<String> notifiedNames = new ArrayList<>();
    
    // 활성 긴급 연락처 조회
    List<EmergencyContact> contacts = emergencyContactRepository
        .findByUserAndIsActiveTrueOrderByPriorityAsc(user);

    if (contacts.isEmpty()) {
      log.warn("등록된 긴급 연락처가 없습니다: 사용자 {}", user.getUserId());
      return notifiedNames;
    }

    // 모든 연락처 또는 우선순위 연락처에만 알림
    List<EmergencyContact> targetContacts = request.getNotifyAllContacts() 
        ? contacts 
        : contacts.stream().limit(3).collect(Collectors.toList()); // 상위 3명만

    for (EmergencyContact contact : targetContacts) {
      try {
        sendEmergencyNotification(contact, emergency, request);
        notifiedNames.add(contact.getName());
      } catch (Exception e) {
        log.error("알림 발송 실패: {} - {}", contact.getName(), e.getMessage());
      }
    }

    // 긴급 상황 업데이트
    emergency.setNotificationSent(true);
    emergency.setNotificationCount(notifiedNames.size());
    emergency.setStatus(EmergencyStatus.NOTIFIED);
    emergencyRepository.save(emergency);

    log.info("{}\uba85에게 긴급 알림 발송 완료", notifiedNames.size());
    return notifiedNames;
  }

  /**
   * 긴급 알림 발송
   */
  private void sendEmergencyNotification(
      EmergencyContact contact, 
      Emergency emergency, 
      SosRequest request) {
    
    String message = buildEmergencyMessage(emergency, request);
    
    // SMS 발송
    if (contact.getPhoneNumber() != null) {
      notificationService.sendSmsNotification(
          contact.getPhoneNumber(), 
          message
      );
    }
    
    // 이메일 발송
    if (contact.getEmail() != null) {
      notificationService.sendEmailNotification(
          contact.getEmail(),
          "긴급! " + emergency.getUser().getName() + "님의 SOS 요청",
          message
      );
    }
  }

  /**
   * 긴급 메시지 생성
   */
  private String buildEmergencyMessage(Emergency emergency, SosRequest request) {
    StringBuilder message = new StringBuilder();
    
    message.append("긴급! ")
           .append(emergency.getUser().getFullName() != null ?
                   emergency.getUser().getFullName() : emergency.getUser().getName())
           .append("님이 도움을 요청했습니다.\n\n");
    
    if (request.getMessage() != null) {
      message.append("메시지: ").append(request.getMessage()).append("\n");
    }
    
    if (request.getShareLocation()) {
      message.append("\n현재 위치:\n");
      if (request.getAddress() != null) {
        message.append(request.getAddress()).append("\n");
      }
      message.append("지도: https://maps.google.com/?q=")
             .append(request.getLatitude())
             .append(",")
             .append(request.getLongitude())
             .append("\n");
    }
    
    message.append("\n시간: ").append(LocalDateTime.now().toString());
    
    return message.toString();
  }

  /**
   * SOS 응답 생성
   */
  private SosResponse buildSosResponse(
      Emergency emergency, 
      List<String> notifiedContacts,
      SosRequest request) {
    
    String instruction = getSimpleInstruction(emergency.getType());
    
    return SosResponse.builder()
        .emergencyId(emergency.getId())
        .status(emergency.getStatus().name())
        .message("도움 요청이 전달되었습니다")
        .triggeredAt(emergency.getTriggeredAt())
        .notifiedContacts(notifiedContacts.size())
        .notifiedContactNames(notifiedContacts)
        .locationShared(request.getShareLocation())
        .estimatedResponseTime("약 10-15분")
        .emergencyNumber(getEmergencyNumber(emergency.getType()))
        .simpleInstruction(instruction)
        .success(true)
        .build();
  }

  /**
   * 긴급 상황 타입 매핑
   */
  private EmergencyType mapEmergencyType(String type) {
    if (type == null) {
      return EmergencyType.PANIC_BUTTON;
    }
    
    return switch (type.toUpperCase()) {
      case "FALL" -> EmergencyType.FALL_DETECTED;
      case "PANIC" -> EmergencyType.PANIC_BUTTON;
      case "LOST" -> EmergencyType.LOST;
      case "MEDICAL" -> EmergencyType.MEDICAL;
      default -> EmergencyType.OTHER;
    };
  }

  /**
   * 간단한 행동 지침
   */
  private String getSimpleInstruction(EmergencyType type) {
    return switch (type) {
      case FALL_DETECTED -> "움직이지 마세요. 도움이 공 겁니다.";
      case MEDICAL -> "편안히 쉬세요. 의료진이 곹 도착합니다.";
      case LOST -> "그 자리에 계세요. 보호자가 찾으러 갑니다.";
      default -> "침착하세요. 도움이 공 도착합니다.";
    };
  }

  /**
   * 긴급 전화번호 반환
   */
  private String getEmergencyNumber(EmergencyType type) {
    return switch (type) {
      case MEDICAL -> "119";
      case FALL_DETECTED -> "119";
      default -> "112";
    };
  }

  /**
   * SOS 취소
   */
  public void cancelSos(Long userId, Long emergencyId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    Emergency emergency = emergencyRepository.findByIdAndUser(emergencyId, user)
        .orElseThrow(() -> new IllegalArgumentException("긴급 상황을 찾을 수 없습니다."));
    
    emergency.setStatus(EmergencyStatus.CANCELLED);
    emergency.setCancelledAt(LocalDateTime.now());
    emergencyRepository.save(emergency);
    
    log.info("SOS 취소: 사용자 {}, 긴급ID {}", userId, emergencyId);
  }

  /**
   * 최근 SOS 이력 조회
   */
  public List<Emergency> getRecentSosHistory(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    return emergencyRepository.findByUserOrderByTriggeredAtDesc(user)
        .stream()
        .limit(10)
        .collect(Collectors.toList());
  }
}