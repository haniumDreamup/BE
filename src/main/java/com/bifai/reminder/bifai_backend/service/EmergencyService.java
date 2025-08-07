package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.common.BaseService;
import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.dto.emergency.ResolveEmergencyRequest;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.EmergencyRepository;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 긴급 상황 관리 서비스
 * 
 * <p>BIF 사용자의 긴급 상황 처리 및 보호자 알림을 담당합니다.
 * 낙상 감지, 안전 구역 이탈, 수동 호출 등 다양한 긴급 상황을 관리합니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmergencyService extends BaseService {

  private final EmergencyRepository emergencyRepository;
  private final GuardianRepository guardianRepository;
  private final NotificationService notificationService;

  /**
   * 긴급 상황 발생 신고
   * 
   * <p>사용자가 수동으로 긴급 상황을 신고할 때 호출됩니다.
   * 긴급 상황을 데이터베이스에 저장하고 보호자에게 알림을 전송합니다.</p>
   * 
   * @param request 긴급 상황 요청 정보
   * @return 생성된 긴급 상황 응답
   */
  @Transactional
  public EmergencyResponse createEmergency(EmergencyRequest request) {
    // 현재 사용자 조회
    User user = getCurrentUser();

    // 긴급 상황 생성
    Emergency emergency = Emergency.builder()
        .user(user)
        .type(request.getType())
        .status(EmergencyStatus.ACTIVE)
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .address(request.getAddress())
        .description(request.getDescription())
        .severity(request.getSeverity() != null ? request.getSeverity() : EmergencySeverity.HIGH)
        .triggeredBy(TriggerSource.USER)
        .imageUrl(request.getImageUrl())
        .build();

    emergency = emergencyRepository.save(emergency);
    
    // 보호자에게 알림 전송
    notifyGuardians(emergency);
    
    log.warn("긴급 상황 발생: userId={}, type={}, location=({},{})", 
        user.getId(), request.getType(), request.getLatitude(), request.getLongitude());

    return EmergencyResponse.from(emergency);
  }

  /**
   * 낙상 감지 처리
   * 
   * <p>AI 또는 디바이스에서 낙상을 감지했을 때 호출됩니다.
   * 신뢰도에 따라 심각도를 자동으로 결정하고 긴급 상황을 생성합니다.</p>
   * 
   * @param request 낙상 감지 요청 정보
   * @return 생성된 긴급 상황 응답
   */
  @Transactional
  public EmergencyResponse handleFallDetection(FallDetectionRequest request) {
    // 현재 사용자 조회
    User user = getCurrentUser();

    // 심각도 결정
    EmergencySeverity severity = determineSeverity(request.getConfidence());

    // 긴급 상황 생성
    Emergency emergency = Emergency.builder()
        .user(user)
        .type(EmergencyType.FALL_DETECTION)
        .status(EmergencyStatus.ACTIVE)
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .description("낙상이 감지되었습니다. 신뢰도: " + request.getConfidence() + "%")
        .severity(severity)
        .triggeredBy(TriggerSource.AI_DETECTION)
        .fallConfidence(request.getConfidence())
        .imageUrl(request.getImageUrl())
        .build();

    emergency = emergencyRepository.save(emergency);
    
    // 보호자에게 알림 전송
    notifyGuardians(emergency);
    
    log.error("낙상 감지 긴급 상황: userId={}, confidence={}, location=({},{})", 
        user.getId(), request.getConfidence(), request.getLatitude(), request.getLongitude());

    return EmergencyResponse.from(emergency);
  }

  /**
   * 긴급 상황 상태 조회
   * 
   * @param emergencyId 긴급 상황 ID
   * @return 긴급 상황 상세 정보
   * @throws ResourceNotFoundException 긴급 상황을 찾을 수 없는 경우
   */
  public EmergencyResponse getEmergencyStatus(Long emergencyId) {
    Emergency emergency = emergencyRepository.findById(emergencyId)
        .orElseThrow(() -> new ResourceNotFoundException("긴급 상황을 찾을 수 없습니다"));
    
    return EmergencyResponse.from(emergency);
  }

  /**
   * 사용자의 긴급 상황 이력 조회
   * 
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 긴급 상황 이력 페이지
   */
  public Page<EmergencyResponse> getUserEmergencyHistory(Long userId, Pageable pageable) {
    Page<Emergency> emergencies = emergencyRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    return emergencies.map(EmergencyResponse::from);
  }

  /**
   * 활성 긴급 상황 목록 조회
   * 
   * <p>현재 활성화되어 있는 모든 긴급 상황을 조회합니다.
   * ACTIVE와 NOTIFIED 상태의 긴급 상황만 포함됩니다.</p>
   * 
   * @return 활성 긴급 상황 목록
   */
  public List<EmergencyResponse> getActiveEmergencies() {
    List<EmergencyStatus> activeStatuses = Arrays.asList(
        EmergencyStatus.ACTIVE, 
        EmergencyStatus.NOTIFIED
    );
    
    List<Emergency> activeEmergencies = emergencyRepository.findActiveEmergencies(activeStatuses);
    return activeEmergencies.stream()
        .map(EmergencyResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 긴급 상황 해결 처리
   * 
   * <p>활성 긴급 상황을 해결 완료로 처리합니다.
   * 해결 시간과 응답 시간을 기록합니다.</p>
   * 
   * @param emergencyId 긴급 상황 ID
   * @param resolvedBy 해결한 사람
   * @param notes 해결 메모
   * @return 업데이트된 긴급 상황 정보
   */
  @Transactional
  public EmergencyResponse resolveEmergency(Long emergencyId, String resolvedBy, String notes) {
    Emergency emergency = emergencyRepository.findById(emergencyId)
        .orElseThrow(() -> new ResourceNotFoundException("긴급 상황을 찾을 수 없습니다"));

    emergency.setStatus(EmergencyStatus.RESOLVED);
    emergency.setResolvedAt(LocalDateTime.now());
    emergency.setResolvedBy(resolvedBy);
    emergency.setResolutionNotes(notes);
    
    // 응답 시간 계산
    long responseSeconds = ChronoUnit.SECONDS.between(emergency.getCreatedAt(), LocalDateTime.now());
    emergency.setResponseTimeSeconds((int) responseSeconds);
    
    emergency = emergencyRepository.save(emergency);
    
    log.info("긴급 상황 해결: emergencyId={}, resolvedBy={}, responseTime={}초", 
        emergencyId, resolvedBy, responseSeconds);

    return EmergencyResponse.from(emergency);
  }
  
  /**
   * 긴급 상황 해결 처리 (DTO 버전)
   * 
   * @param emergencyId 긴급 상황 ID
   * @param request 해결 요청 정보
   * @return 업데이트된 긴급 상황 정보
   */
  @Transactional
  public EmergencyResponse resolveEmergency(Long emergencyId, ResolveEmergencyRequest request) {
    return resolveEmergency(emergencyId, request.getResolvedBy(), request.getResolutionNotes());
  }

  /**
   * 보호자에게 알림 전송
   * 
   * <p>긴급 상황 발생 시 모든 활성 보호자에게 알림을 전송합니다.
   * 알림 전송에 성공한 보호자 정보를 기록합니다.</p>
   * 
   * @param emergency 긴급 상황 엔티티
   */
  private void notifyGuardians(Emergency emergency) {
    // 활성 보호자 목록 조회
    List<Guardian> guardians = guardianRepository.findActiveGuardiansByUserId(emergency.getUser().getId());
    
    if (guardians.isEmpty()) {
      log.warn("알림을 받을 보호자가 없습니다: userId={}", emergency.getUser().getId());
      return;
    }

    // 알림 전송
    List<String> notifiedEmails = guardians.stream()
        .map(guardian -> {
          try {
            notificationService.sendEmergencyNotification(guardian, emergency);
            return guardian.getGuardianUser().getEmail();
          } catch (Exception e) {
            log.error("보호자 알림 전송 실패: guardianId={}", guardian.getId(), e);
            return null;
          }
        })
        .filter(email -> email != null)
        .collect(Collectors.toList());

    // 알림 받은 보호자 기록
    emergency.setNotifiedGuardians(String.join(",", notifiedEmails));
    emergency.setStatus(EmergencyStatus.NOTIFIED);
    emergencyRepository.save(emergency);
  }

  /**
   * 낙상 신뢰도에 따른 심각도 결정
   * 
   * <p>낙상 감지 신뢰도를 기반으로 긴급 상황의 심각도를 결정합니다.
   * 90% 이상: 위급, 70% 이상: 높음, 50% 이상: 중간, 그 외: 낮음</p>
   * 
   * @param confidence 낙상 감지 신뢰도 (0-100)
   * @return 결정된 심각도
   */
  private EmergencySeverity determineSeverity(Double confidence) {
    if (confidence >= 90) {
      return EmergencySeverity.CRITICAL;
    } else if (confidence >= 70) {
      return EmergencySeverity.HIGH;
    } else if (confidence >= 50) {
      return EmergencySeverity.MEDIUM;
    } else {
      return EmergencySeverity.LOW;
    }
  }

  /**
   * 사용자가 자신의 긴급 상황인지 확인
   * 
   * @param emergencyId 긴급 상황 ID
   * @return 본인의 긴급 상황 여부
   */
  public boolean isOwnEmergency(Long emergencyId) {
    Emergency emergency = emergencyRepository.findById(emergencyId)
        .orElseThrow(() -> new ResourceNotFoundException("긴급 상황을 찾을 수 없습니다"));
        
    return emergency.getUser().getId().equals(getCurrentUserId());
  }

  /**
   * 보호자가 담당 사용자의 긴급 상황인지 확인
   * 
   * @param emergencyId 긴급 상황 ID
   * @return 보호자의 담당 사용자 긴급 상황 여부
   */
  public boolean isGuardianOfEmergency(Long emergencyId) {
    Emergency emergency = emergencyRepository.findById(emergencyId)
        .orElseThrow(() -> new ResourceNotFoundException("긴급 상황을 찾을 수 없습니다"));
    
    // 해당 사용자의 보호자인지 확인
    return guardianRepository.existsByUserIdAndGuardianUserId(
        emergency.getUser().getId(), 
        getCurrentUserId()
    );
  }
}