package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.emergencycontact.EmergencyContactRequest;
import com.bifai.reminder.bifai_backend.dto.emergencycontact.EmergencyContactResponse;
import com.bifai.reminder.bifai_backend.dto.emergencycontact.ContactAvailabilityResponse;
import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.exception.DuplicateResourceException;
import com.bifai.reminder.bifai_backend.repository.EmergencyContactRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 긴급 연락처 관리 서비스
 * 보호자 및 의료진 연락처 CRUD 및 관리 기능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmergencyContactService {

  private final EmergencyContactRepository emergencyContactRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  
  private static final int MAX_CONTACTS_PER_USER = 20;
  private static final String ENCRYPTION_PASSWORD = System.getenv("CONTACT_ENCRYPTION_KEY");
  private static final String ENCRYPTION_SALT = "5c0744940b5c369b";
  
  private final TextEncryptor encryptor = ENCRYPTION_PASSWORD != null ? 
      Encryptors.text(ENCRYPTION_PASSWORD, ENCRYPTION_SALT) : null;

  /**
   * 긴급 연락처 추가
   */
  public EmergencyContactResponse createEmergencyContact(Long userId, EmergencyContactRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    // 최대 개수 체크
    long currentCount = emergencyContactRepository.countByUser(user);
    if (currentCount >= MAX_CONTACTS_PER_USER) {
      throw new IllegalStateException(
          String.format("연락처는 최대 %d개까지만 등록할 수 있어요.", MAX_CONTACTS_PER_USER)
      );
    }
    
    // 중복 체크
    if (emergencyContactRepository.existsByUserAndPhoneNumber(user, request.getPhoneNumber())) {
      throw new DuplicateResourceException("이미 등록된 전화번호예요.");
    }
    
    if (request.getEmail() != null && 
        emergencyContactRepository.existsByUserAndEmail(user, request.getEmail())) {
      throw new DuplicateResourceException("이미 등록된 이메일이에요.");
    }
    
    // 주요 연락처 설정 시 기존 주요 연락처 해제
    if (Boolean.TRUE.equals(request.getIsPrimary())) {
      emergencyContactRepository.findByUserAndIsPrimaryTrue(user)
          .ifPresent(existing -> {
            existing.setIsPrimary(false);
            emergencyContactRepository.save(existing);
          });
    }
    
    // 우선순위 자동 설정
    if (request.getPriority() == null) {
      request.setPriority((int) currentCount + 1);
    } else {
      // 중간에 삽입하는 경우 기존 우선순위 조정
      emergencyContactRepository.incrementPriorities(user, request.getPriority());
    }
    
    EmergencyContact contact = EmergencyContact.builder()
        .user(user)
        .name(request.getName())
        .relationship(request.getRelationship())
        .phoneNumber(encryptPhoneNumber(request.getPhoneNumber()))
        .email(request.getEmail())
        .contactType(request.getContactType())
        .priority(request.getPriority())
        .isPrimary(request.getIsPrimary())
        .isActive(true)
        .canReceiveAlerts(request.getCanReceiveAlerts() != null ? request.getCanReceiveAlerts() : true)
        .canAccessLocation(request.getCanAccessLocation() != null ? request.getCanAccessLocation() : false)
        .canAccessHealthData(request.getCanAccessHealthData() != null ? request.getCanAccessHealthData() : false)
        .canMakeDecisions(request.getCanMakeDecisions() != null ? request.getCanMakeDecisions() : false)
        .availableStartTime(request.getAvailableStartTime() != null ? 
            LocalTime.parse(request.getAvailableStartTime()) : null)
        .availableEndTime(request.getAvailableEndTime() != null ? 
            LocalTime.parse(request.getAvailableEndTime()) : null)
        .availableDays(request.getAvailableDays())
        .preferredContactMethod(request.getPreferredContactMethod())
        .languagePreference(request.getLanguagePreference() != null ? 
            request.getLanguagePreference() : "ko")
        .notes(request.getNotes())
        .isMedicalProfessional(request.getIsMedicalProfessional())
        .specialization(request.getSpecialization())
        .hospitalName(request.getHospitalName())
        .licenseNumber(request.getLicenseNumber())
        .createdBy(user)
        .build();
    
    contact = emergencyContactRepository.save(contact);
    
    // 검증 코드 생성 및 전송
    if (Boolean.TRUE.equals(request.getSendVerification())) {
      sendVerificationCode(contact);
    }
    
    log.info("Emergency contact created: {} for user {}", contact.getName(), userId);
    
    return convertToResponse(contact);
  }

  /**
   * 긴급 연락처 수정
   */
  public EmergencyContactResponse updateEmergencyContact(Long userId, Long contactId, 
                                                        EmergencyContactRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    EmergencyContact contact = emergencyContactRepository.findByIdAndUser(contactId, user)
        .orElseThrow(() -> new ResourceNotFoundException("연락처를 찾을 수 없습니다."));
    
    // 전화번호 변경 시 중복 체크
    if (!contact.getPhoneNumber().equals(encryptPhoneNumber(request.getPhoneNumber())) &&
        emergencyContactRepository.existsByUserAndPhoneNumber(user, request.getPhoneNumber())) {
      throw new DuplicateResourceException("이미 등록된 전화번호예요.");
    }
    
    // 업데이트
    contact.setName(request.getName());
    contact.setRelationship(request.getRelationship());
    contact.setPhoneNumber(encryptPhoneNumber(request.getPhoneNumber()));
    contact.setEmail(request.getEmail());
    
    if (request.getContactType() != null) {
      contact.setContactType(request.getContactType());
    }
    if (request.getPriority() != null && !request.getPriority().equals(contact.getPriority())) {
      // 우선순위 재조정
      adjustPriorities(user, contact.getPriority(), request.getPriority());
      contact.setPriority(request.getPriority());
    }
    if (request.getIsPrimary() != null) {
      if (Boolean.TRUE.equals(request.getIsPrimary()) && !contact.getIsPrimary()) {
        // 새로운 주요 연락처 설정
        emergencyContactRepository.findByUserAndIsPrimaryTrue(user)
            .ifPresent(existing -> {
              existing.setIsPrimary(false);
              emergencyContactRepository.save(existing);
            });
      }
      contact.setIsPrimary(request.getIsPrimary());
    }
    
    // 권한 설정
    if (request.getCanReceiveAlerts() != null) {
      contact.setCanReceiveAlerts(request.getCanReceiveAlerts());
    }
    if (request.getCanAccessLocation() != null) {
      contact.setCanAccessLocation(request.getCanAccessLocation());
    }
    if (request.getCanAccessHealthData() != null) {
      contact.setCanAccessHealthData(request.getCanAccessHealthData());
    }
    if (request.getCanMakeDecisions() != null) {
      contact.setCanMakeDecisions(request.getCanMakeDecisions());
    }
    
    // 가용성 설정
    if (request.getAvailableStartTime() != null) {
      contact.setAvailableStartTime(LocalTime.parse(request.getAvailableStartTime()));
    }
    if (request.getAvailableEndTime() != null) {
      contact.setAvailableEndTime(LocalTime.parse(request.getAvailableEndTime()));
    }
    if (request.getAvailableDays() != null) {
      contact.setAvailableDays(request.getAvailableDays());
    }
    
    // 기타 정보
    if (request.getPreferredContactMethod() != null) {
      contact.setPreferredContactMethod(request.getPreferredContactMethod());
    }
    if (request.getLanguagePreference() != null) {
      contact.setLanguagePreference(request.getLanguagePreference());
    }
    if (request.getNotes() != null) {
      contact.setNotes(request.getNotes());
    }
    
    // 의료진 정보
    if (request.getIsMedicalProfessional() != null) {
      contact.setIsMedicalProfessional(request.getIsMedicalProfessional());
    }
    if (request.getSpecialization() != null) {
      contact.setSpecialization(request.getSpecialization());
    }
    if (request.getHospitalName() != null) {
      contact.setHospitalName(request.getHospitalName());
    }
    if (request.getLicenseNumber() != null) {
      contact.setLicenseNumber(request.getLicenseNumber());
    }
    
    contact = emergencyContactRepository.save(contact);
    
    log.info("Emergency contact updated: {} for user {}", contact.getName(), userId);
    
    return convertToResponse(contact);
  }

  /**
   * 긴급 연락처 삭제
   */
  public void deleteEmergencyContact(Long userId, Long contactId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    EmergencyContact contact = emergencyContactRepository.findByIdAndUser(contactId, user)
        .orElseThrow(() -> new ResourceNotFoundException("연락처를 찾을 수 없습니다."));
    
    emergencyContactRepository.delete(contact);
    
    // 우선순위 재정렬
    reorderPriorities(user);
    
    log.info("Emergency contact deleted: {} for user {}", contact.getName(), userId);
  }

  /**
   * 긴급 연락처 단일 조회
   */
  @Transactional(readOnly = true)
  public EmergencyContactResponse getEmergencyContact(Long userId, Long contactId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    EmergencyContact contact = emergencyContactRepository.findByIdAndUser(contactId, user)
        .orElseThrow(() -> new ResourceNotFoundException("연락처를 찾을 수 없습니다."));
    
    return convertToResponse(contact);
  }

  /**
   * 사용자의 모든 긴급 연락처 조회
   */
  @Transactional(readOnly = true)
  public List<EmergencyContactResponse> getUserEmergencyContacts(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<EmergencyContact> contacts = emergencyContactRepository
        .findByUserOrderByPriorityAsc(user);
    
    return contacts.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * 활성화된 연락처만 조회
   */
  @Transactional(readOnly = true)
  public List<EmergencyContactResponse> getActiveContacts(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<EmergencyContact> contacts = emergencyContactRepository
        .findByUserAndIsActiveTrueOrderByPriorityAsc(user);
    
    return contacts.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * 현재 연락 가능한 연락처 조회
   */
  @Transactional(readOnly = true)
  public List<ContactAvailabilityResponse> getAvailableContacts(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<EmergencyContact> contacts = emergencyContactRepository
        .findAvailableContacts(user);
    
    return contacts.stream()
        .map(contact -> ContactAvailabilityResponse.builder()
            .contactId(contact.getId())
            .name(contact.getName())
            .relationship(contact.getRelationship())
            .phoneNumber(decryptPhoneNumber(contact.getPhoneNumber()))
            .preferredContactMethod(contact.getPreferredContactMethod())
            .isAvailable(contact.isAvailable())
            .responseRate(contact.getResponseRate())
            .averageResponseTimeMinutes(contact.getAverageResponseTimeMinutes())
            .lastContactedAt(contact.getLastContactedAt())
            .build())
        .collect(Collectors.toList());
  }

  /**
   * 의료진 연락처 조회
   */
  @Transactional(readOnly = true)
  public List<EmergencyContactResponse> getMedicalContacts(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<EmergencyContact> contacts = emergencyContactRepository
        .findMedicalContacts(user);
    
    return contacts.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * 연락처 검증
   */
  public EmergencyContactResponse verifyContact(Long userId, Long contactId, String verificationCode) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    EmergencyContact contact = emergencyContactRepository
        .findByUserAndVerificationCode(user, verificationCode)
        .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 인증 코드입니다."));
    
    if (!contact.getId().equals(contactId)) {
      throw new IllegalArgumentException("연락처 정보가 일치하지 않습니다.");
    }
    
    contact.setVerified(true);
    contact.setVerifiedAt(LocalDateTime.now());
    contact.setVerificationCode(null); // 인증 코드 제거
    
    contact = emergencyContactRepository.save(contact);
    
    log.info("Emergency contact verified: {} for user {}", contact.getName(), userId);
    
    return convertToResponse(contact);
  }

  /**
   * 연락처 활성화/비활성화 토글
   */
  public EmergencyContactResponse toggleContactActive(Long userId, Long contactId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    EmergencyContact contact = emergencyContactRepository.findByIdAndUser(contactId, user)
        .orElseThrow(() -> new ResourceNotFoundException("연락처를 찾을 수 없습니다."));
    
    contact.setIsActive(!contact.getIsActive());
    contact = emergencyContactRepository.save(contact);
    
    log.info("Emergency contact {} toggled to {}", contact.getName(), 
            contact.getIsActive() ? "active" : "inactive");
    
    return convertToResponse(contact);
  }

  /**
   * 우선순위 변경
   */
  public void updateContactPriorities(Long userId, List<Long> contactIds) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    int priority = 1;
    for (Long contactId : contactIds) {
      EmergencyContact contact = emergencyContactRepository.findByIdAndUser(contactId, user)
          .orElseThrow(() -> new ResourceNotFoundException("연락처를 찾을 수 없습니다: " + contactId));
      
      contact.setPriority(priority++);
      emergencyContactRepository.save(contact);
    }
    
    log.info("Updated priorities for {} contacts for user {}", contactIds.size(), userId);
  }

  /**
   * 연락 기록 업데이트
   */
  public void updateContactRecord(Long userId, Long contactId, boolean responded, long responseTimeMinutes) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    EmergencyContact contact = emergencyContactRepository.findByIdAndUser(contactId, user)
        .orElseThrow(() -> new ResourceNotFoundException("연락처를 찾을 수 없습니다."));
    
    contact.updateContactRecord(responded, responseTimeMinutes);
    emergencyContactRepository.save(contact);
    
    log.debug("Contact record updated for {}: responded={}, responseTime={}min", 
             contact.getName(), responded, responseTimeMinutes);
  }

  /**
   * 전화번호 암호화
   */
  private String encryptPhoneNumber(String phoneNumber) {
    if (encryptor != null && phoneNumber != null) {
      return encryptor.encrypt(phoneNumber);
    }
    return phoneNumber;
  }

  /**
   * 전화번호 복호화
   */
  private String decryptPhoneNumber(String encryptedPhoneNumber) {
    if (encryptor != null && encryptedPhoneNumber != null) {
      try {
        return encryptor.decrypt(encryptedPhoneNumber);
      } catch (Exception e) {
        log.warn("Failed to decrypt phone number: {}", e.getMessage());
        return encryptedPhoneNumber;
      }
    }
    return encryptedPhoneNumber;
  }

  /**
   * 검증 코드 전송
   */
  private void sendVerificationCode(EmergencyContact contact) {
    String code = generateVerificationCode();
    contact.setVerificationCode(code);
    emergencyContactRepository.save(contact);
    
    String message = String.format(
        "[BIF-AI] 긴급 연락처 등록 인증 코드: %s", code
    );
    
    // SMS 또는 이메일로 전송
    if (contact.getPreferredContactMethod() == EmergencyContact.ContactMethod.EMAIL && 
        contact.getEmail() != null) {
      notificationService.sendEmailNotification(contact.getEmail(), "긴급 연락처 인증", message);
    } else {
      notificationService.sendSmsNotification(
          decryptPhoneNumber(contact.getPhoneNumber()), message
      );
    }
  }

  /**
   * 6자리 검증 코드 생성
   */
  private String generateVerificationCode() {
    Random random = new Random();
    return String.format("%06d", random.nextInt(1000000));
  }

  /**
   * 우선순위 조정
   */
  private void adjustPriorities(User user, int oldPriority, int newPriority) {
    List<EmergencyContact> contacts = emergencyContactRepository.findByUserOrderByPriorityAsc(user);
    
    for (EmergencyContact contact : contacts) {
      int currentPriority = contact.getPriority();
      
      if (oldPriority < newPriority) {
        // 아래로 이동
        if (currentPriority > oldPriority && currentPriority <= newPriority) {
          contact.setPriority(currentPriority - 1);
          emergencyContactRepository.save(contact);
        }
      } else {
        // 위로 이동
        if (currentPriority >= newPriority && currentPriority < oldPriority) {
          contact.setPriority(currentPriority + 1);
          emergencyContactRepository.save(contact);
        }
      }
    }
  }

  /**
   * 우선순위 재정렬
   */
  private void reorderPriorities(User user) {
    List<EmergencyContact> contacts = emergencyContactRepository.findByUserOrderByPriorityAsc(user);
    
    int priority = 1;
    for (EmergencyContact contact : contacts) {
      if (contact.getPriority() != priority) {
        contact.setPriority(priority);
        emergencyContactRepository.save(contact);
      }
      priority++;
    }
  }

  /**
   * Entity를 Response DTO로 변환
   */
  private EmergencyContactResponse convertToResponse(EmergencyContact contact) {
    return EmergencyContactResponse.builder()
        .id(contact.getId())
        .name(contact.getName())
        .relationship(contact.getRelationship())
        .phoneNumber(decryptPhoneNumber(contact.getPhoneNumber()))
        .email(contact.getEmail())
        .contactType(contact.getContactType())
        .priority(contact.getPriority())
        .isPrimary(contact.getIsPrimary())
        .isActive(contact.getIsActive())
        .canReceiveAlerts(contact.getCanReceiveAlerts())
        .canAccessLocation(contact.getCanAccessLocation())
        .canAccessHealthData(contact.getCanAccessHealthData())
        .canMakeDecisions(contact.getCanMakeDecisions())
        .availableStartTime(contact.getAvailableStartTime() != null ? 
            contact.getAvailableStartTime().toString() : null)
        .availableEndTime(contact.getAvailableEndTime() != null ? 
            contact.getAvailableEndTime().toString() : null)
        .availableDays(contact.getAvailableDays())
        .preferredContactMethod(contact.getPreferredContactMethod())
        .languagePreference(contact.getLanguagePreference())
        .notes(contact.getNotes())
        .isMedicalProfessional(contact.getIsMedicalProfessional())
        .specialization(contact.getSpecialization())
        .hospitalName(contact.getHospitalName())
        .licenseNumber(contact.getLicenseNumber())
        .lastContactedAt(contact.getLastContactedAt())
        .contactCount(contact.getContactCount())
        .responseRate(contact.getResponseRate())
        .averageResponseTimeMinutes(contact.getAverageResponseTimeMinutes())
        .verified(contact.getVerified())
        .verifiedAt(contact.getVerifiedAt())
        .createdAt(contact.getCreatedAt())
        .updatedAt(contact.getUpdatedAt())
        .isAvailable(contact.isAvailable())
        .isMedicalStaff(contact.isMedicalStaff())
        .permissionSummary(contact.getPermissionSummary())
        .build();
  }
}