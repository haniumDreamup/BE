package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.guardian.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.GuardianRelationship;
import com.bifai.reminder.bifai_backend.entity.GuardianRelationship.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRelationshipRepository;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 보호자 관계 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GuardianRelationshipService {
  
  private final GuardianRelationshipRepository relationshipRepository;
  private final GuardianRepository guardianRepository;
  private final UserRepository userRepository;
  private final JavaMailSender mailSender;
  private final ObjectMapper objectMapper;
  
  @Value("${app.base-url:http://localhost:8080}")
  private String baseUrl;
  
  @Value("${app.invitation.expiry-hours:72}")
  private int invitationExpiryHours;
  
  /**
   * 보호자 초대
   */
  public GuardianInvitationResponse inviteGuardian(GuardianInvitationRequest request) {
    log.info("보호자 초대 시작 - 사용자: {}, 보호자 이메일: {}", request.getUserId(), request.getGuardianEmail());
    
    // 사용자 확인
    User user = userRepository.findById(request.getUserId())
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // 보호자 계정 확인 또는 생성
    Guardian guardian = guardianRepository.findByEmail(request.getGuardianEmail())
      .orElseGet(() -> createPendingGuardian(request));
    
    // 중복 관계 확인
    if (relationshipRepository.existsByGuardian_IdAndUser_UserIdAndStatusNot(
        guardian.getId(), user.getUserId(), RelationshipStatus.TERMINATED)) {
      log.warn("중복 보호자 관계 시도 - 보호자: {}, 사용자: {}", guardian.getId(), user.getUserId());
      throw new IllegalStateException("이미 존재하거나 대기 중인 보호자 관계입니다");
    }
    
    // 초대 토큰 생성
    String invitationToken = generateInvitationToken();
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(invitationExpiryHours);
    
    // 관계 생성
    GuardianRelationship relationship = GuardianRelationship.builder()
      .guardian(guardian)
      .user(user)
      .relationshipType(request.getRelationshipType())
      .permissionLevel(request.getPermissionLevel())
      .status(RelationshipStatus.PENDING)
      .invitationToken(invitationToken)
      .invitationExpiresAt(expiresAt)
      .emergencyPriority(request.getEmergencyPriority())
      .notes(request.getNotes())
      .build();
    
    // 권한 설정 초기화
    if (request.getPermissionSettings() != null) {
      try {
        String permissionJson = objectMapper.writeValueAsString(request.getPermissionSettings());
        relationship.setPermissionSettings(permissionJson);
      } catch (Exception e) {
        log.error("권한 설정 직렬화 실패", e);
      }
    }
    
    relationship = relationshipRepository.save(relationship);
    
    // 초대 이메일 발송
    sendInvitationEmail(guardian, user, invitationToken);
    
    return GuardianInvitationResponse.builder()
      .relationshipId(relationship.getRelationshipId())
      .invitationToken(invitationToken)
      .expiresAt(expiresAt)
      .guardianEmail(request.getGuardianEmail())
      .status("INVITATION_SENT")
      .build();
  }
  
  /**
   * 초대 수락
   */
  public GuardianRelationshipDto acceptInvitation(String invitationToken, Long guardianId) {
    log.info("초대 수락 - 토큰: {}, 보호자: {}", invitationToken, guardianId);
    
    GuardianRelationship relationship = relationshipRepository.findByInvitationToken(invitationToken)
      .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 토큰입니다"));
    
    // 만료 확인
    if (relationship.isInvitationExpired()) {
      relationship.setStatus(RelationshipStatus.EXPIRED);
      relationshipRepository.save(relationship);
      throw new IllegalStateException("초대가 만료되었습니다");
    }
    
    // 보호자 확인
    if (!relationship.getGuardian().getId().equals(guardianId)) {
      throw new IllegalArgumentException("권한이 없습니다");
    }
    
    // 관계 활성화
    relationship.activate("guardian:" + guardianId);
    relationship = relationshipRepository.save(relationship);
    
    log.info("초대 수락 완료 - 관계 ID: {}", relationship.getRelationshipId());
    
    return convertToDto(relationship);
  }
  
  /**
   * 초대 거부
   */
  public void rejectInvitation(String invitationToken, Long guardianId) {
    log.info("초대 거부 - 토큰: {}, 보호자: {}", invitationToken, guardianId);
    
    GuardianRelationship relationship = relationshipRepository.findByInvitationToken(invitationToken)
      .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 토큰입니다"));
    
    // 보호자 확인
    if (!relationship.getGuardian().getId().equals(guardianId)) {
      throw new IllegalArgumentException("권한이 없습니다");
    }
    
    relationship.setStatus(RelationshipStatus.REJECTED);
    relationship.setInvitationToken(null);
    relationshipRepository.save(relationship);
    
    log.info("초대 거부 완료 - 관계 ID: {}", relationship.getRelationshipId());
  }
  
  /**
   * 관계 권한 수정
   */
  public GuardianRelationshipDto updatePermissions(Long relationshipId, PermissionUpdateRequest request, Long requesterId) {
    log.info("권한 수정 - 관계 ID: {}, 요청자: {}", relationshipId, requesterId);
    
    GuardianRelationship relationship = relationshipRepository.findById(relationshipId)
      .orElseThrow(() -> new IllegalArgumentException("관계를 찾을 수 없습니다"));
    
    // 권한 확인 (전체 권한을 가진 보호자 또는 사용자 본인만 가능)
    if (!hasPermissionToModify(relationship, requesterId)) {
      throw new IllegalArgumentException("권한 수정 권한이 없습니다");
    }
    
    // 권한 레벨 변경
    if (request.getPermissionLevel() != null) {
      relationship.setPermissionLevel(request.getPermissionLevel());
    }
    
    // 세부 권한 설정
    if (request.getPermissionSettings() != null) {
      try {
        String permissionJson = objectMapper.writeValueAsString(request.getPermissionSettings());
        relationship.setPermissionSettings(permissionJson);
      } catch (Exception e) {
        log.error("권한 설정 직렬화 실패", e);
      }
    }
    
    // 긴급 우선순위 변경
    if (request.getEmergencyPriority() != null) {
      relationship.setEmergencyPriority(request.getEmergencyPriority());
    }
    
    relationship = relationshipRepository.save(relationship);
    
    log.info("권한 수정 완료 - 새 권한 레벨: {}", relationship.getPermissionLevel());
    
    return convertToDto(relationship);
  }
  
  /**
   * 관계 일시 중지
   */
  public void suspendRelationship(Long relationshipId, Long requesterId) {
    log.info("관계 일시 중지 - 관계 ID: {}, 요청자: {}", relationshipId, requesterId);
    
    GuardianRelationship relationship = relationshipRepository.findById(relationshipId)
      .orElseThrow(() -> new IllegalArgumentException("관계를 찾을 수 없습니다"));
    
    // 권한 확인
    if (!hasPermissionToModify(relationship, requesterId)) {
      throw new IllegalArgumentException("관계 수정 권한이 없습니다");
    }
    
    relationship.suspend();
    relationshipRepository.save(relationship);
    
    log.info("관계 일시 중지 완료");
  }
  
  /**
   * 관계 재활성화
   */
  public void reactivateRelationship(Long relationshipId, Long requesterId) {
    log.info("관계 재활성화 - 관계 ID: {}, 요청자: {}", relationshipId, requesterId);
    
    GuardianRelationship relationship = relationshipRepository.findById(relationshipId)
      .orElseThrow(() -> new IllegalArgumentException("관계를 찾을 수 없습니다"));
    
    // 권한 확인
    if (!hasPermissionToModify(relationship, requesterId)) {
      throw new IllegalArgumentException("관계 수정 권한이 없습니다");
    }
    
    relationship.reactivate();
    relationshipRepository.save(relationship);
    
    log.info("관계 재활성화 완료");
  }
  
  /**
   * 관계 종료
   */
  public void terminateRelationship(Long relationshipId, String reason, Long requesterId) {
    log.info("관계 종료 - 관계 ID: {}, 요청자: {}, 사유: {}", relationshipId, requesterId, reason);
    
    GuardianRelationship relationship = relationshipRepository.findById(relationshipId)
      .orElseThrow(() -> new IllegalArgumentException("관계를 찾을 수 없습니다"));
    
    // 권한 확인
    if (!hasPermissionToModify(relationship, requesterId)) {
      throw new IllegalArgumentException("관계 종료 권한이 없습니다");
    }
    
    relationship.terminate(reason);
    relationshipRepository.save(relationship);
    
    log.info("관계 종료 완료");
  }
  
  /**
   * 사용자의 보호자 목록 조회
   */
  @Transactional(readOnly = true)
  public List<GuardianRelationshipDto> getUserGuardians(Long userId, boolean activeOnly) {
    List<GuardianRelationship> relationships;
    
    if (activeOnly) {
      relationships = relationshipRepository.findByUserUserIdAndStatus(userId, RelationshipStatus.ACTIVE);
    } else {
      relationships = relationshipRepository.findByUserUserIdOrderByEmergencyPriority(userId);
    }
    
    return relationships.stream()
      .map(this::convertToDto)
      .collect(Collectors.toList());
  }
  
  /**
   * 보호자의 피보호자 목록 조회
   */
  @Transactional(readOnly = true)
  public List<GuardianRelationshipDto> getGuardianUsers(Long guardianId) {
    List<GuardianRelationship> relationships = 
      relationshipRepository.findByGuardian_IdAndStatus(guardianId, RelationshipStatus.ACTIVE);
    
    return relationships.stream()
      .map(this::convertToDto)
      .collect(Collectors.toList());
  }
  
  /**
   * 긴급 연락 보호자 목록 조회
   */
  @Transactional(readOnly = true)
  public List<EmergencyContactDto> getEmergencyContacts(Long userId) {
    List<GuardianRelationship> relationships = relationshipRepository.findEmergencyGuardians(userId);
    
    return relationships.stream()
      .map(this::convertToEmergencyContact)
      .collect(Collectors.toList());
  }
  
  /**
   * 보호자 권한 확인
   */
  @Transactional(readOnly = true)
  public boolean hasPermission(Long guardianId, Long userId, String permissionType) {
    switch (permissionType.toUpperCase()) {
      case "VIEW":
        return relationshipRepository.hasViewPermission(guardianId, userId);
      case "MANAGE":
        return relationshipRepository.hasManagePermission(guardianId, userId);
      default:
        return false;
    }
  }
  
  /**
   * 활동 시간 업데이트
   */
  public void updateLastActiveTime(Long guardianId, Long userId) {
    relationshipRepository.findByGuardian_IdAndUser_UserId(guardianId, userId)
      .ifPresent(relationship -> {
        relationship.updateLastActiveTime();
        relationshipRepository.save(relationship);
      });
  }
  
  /**
   * 만료된 초대 정리 (스케줄러)
   */
  @Scheduled(cron = "0 0 */6 * * *") // 6시간마다 실행
  public void cleanupExpiredInvitations() {
    log.info("만료된 초대 정리 시작");
    
    List<GuardianRelationship> expiredInvitations = 
      relationshipRepository.findExpiredInvitations(LocalDateTime.now());
    
    for (GuardianRelationship relationship : expiredInvitations) {
      relationship.setStatus(RelationshipStatus.EXPIRED);
      relationship.setInvitationToken(null);
    }
    
    if (!expiredInvitations.isEmpty()) {
      relationshipRepository.saveAll(expiredInvitations);
      log.info("만료된 초대 {} 건 정리 완료", expiredInvitations.size());
    }
  }
  
  /**
   * 보호자 계정 생성 (대기 상태)
   *
   * 이메일 초대 시 guardian_user_id는 NULL
   * 초대 수락 시 acceptInvitation()에서 guardianUser 연결
   */
  private Guardian createPendingGuardian(GuardianInvitationRequest request) {
    Guardian guardian = Guardian.builder()
      .email(request.getGuardianEmail())
      .name(request.getGuardianName())
      .guardianUser(null) // 초대 시에는 NULL, 수락 후 설정
      .isActive(false) // 초대 수락 전까지 비활성
      .build();

    return guardianRepository.save(guardian);
  }
  
  /**
   * 초대 토큰 생성
   */
  private String generateInvitationToken() {
    return UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();
  }
  
  /**
   * 초대 이메일 발송
   */
  private void sendInvitationEmail(Guardian guardian, User user, String invitationToken) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(guardian.getEmail());
      message.setSubject("BIF-AI 보호자 초대");
      message.setText(String.format(
        "안녕하세요, %s님.\n\n" +
        "%s님이 BIF-AI 서비스의 보호자로 초대했습니다.\n\n" +
        "아래 링크를 클릭하여 초대를 수락해주세요:\n" +
        "%s/guardian/accept-invitation?token=%s\n\n" +
        "이 링크는 %d시간 후 만료됩니다.\n\n" +
        "감사합니다.\n" +
        "BIF-AI 팀",
        guardian.getName(),
        user.getUsername(),
        baseUrl,
        invitationToken,
        invitationExpiryHours
      ));
      
      mailSender.send(message);
      log.info("초대 이메일 발송 완료 - 수신자: {}", guardian.getEmail());
      
    } catch (Exception e) {
      log.error("초대 이메일 발송 실패", e);
      // 이메일 발송 실패해도 초대는 생성됨 (수동으로 토큰 전달 가능)
    }
  }
  
  /**
   * 수정 권한 확인
   */
  private boolean hasPermissionToModify(GuardianRelationship relationship, Long requesterId) {
    // 사용자 본인
    if (relationship.getUser().getUserId().equals(requesterId)) {
      return true;
    }
    
    // 전체 권한을 가진 다른 보호자
    List<GuardianRelationship> otherGuardians = relationshipRepository
      .findByUserAndPermissionLevel(relationship.getUser().getUserId(), PermissionLevel.FULL);
    
    return otherGuardians.stream()
      .anyMatch(gr -> gr.getGuardian().getId().equals(requesterId));
  }
  
  /**
   * 엔티티를 DTO로 변환
   */
  private GuardianRelationshipDto convertToDto(GuardianRelationship relationship) {
    GuardianRelationshipDto dto = new GuardianRelationshipDto();
    dto.setRelationshipId(relationship.getRelationshipId());
    dto.setGuardianId(relationship.getGuardian().getId());
    dto.setGuardianName(relationship.getGuardian().getName());
    dto.setGuardianEmail(relationship.getGuardian().getEmail());
    dto.setUserId(relationship.getUser().getUserId());
    dto.setUserName(relationship.getUser().getUsername());
    dto.setRelationshipType(relationship.getRelationshipType());
    dto.setPermissionLevel(relationship.getPermissionLevel());
    dto.setStatus(relationship.getStatus());
    dto.setEmergencyPriority(relationship.getEmergencyPriority());
    dto.setNotes(relationship.getNotes());
    dto.setLastActiveAt(relationship.getLastActiveAt());
    dto.setCreatedAt(relationship.getCreatedAt());
    
    // 권한 설정 파싱
    if (relationship.getPermissionSettings() != null) {
      try {
        Map<String, Boolean> permissions = objectMapper.readValue(
          relationship.getPermissionSettings(), 
          objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Boolean.class)
        );
        dto.setPermissionSettings(permissions);
      } catch (Exception e) {
        log.error("권한 설정 파싱 실패", e);
      }
    }
    
    return dto;
  }
  
  /**
   * 긴급 연락처 DTO 변환
   */
  private EmergencyContactDto convertToEmergencyContact(GuardianRelationship relationship) {
    return EmergencyContactDto.builder()
      .id(relationship.getGuardian().getId())
      .name(relationship.getGuardian().getName())
      .phoneNumber(relationship.getGuardian().getPrimaryPhone())
      .relationship(relationship.getRelationshipType().getDescription())
      .isPrimary(relationship.getGuardian().getIsPrimary())
      .build();
  }
}