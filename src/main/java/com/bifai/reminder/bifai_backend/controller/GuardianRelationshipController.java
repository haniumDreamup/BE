package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.guardian.*;
import com.bifai.reminder.bifai_backend.dto.response.ApiResponse;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import com.bifai.reminder.bifai_backend.service.GuardianRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 보호자 관계 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/guardian-relationships")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian Relationship", description = "보호자 관계 관리 API")
public class GuardianRelationshipController {
  
  private final GuardianRelationshipService relationshipService;
  private final JwtAuthUtils jwtAuthUtils;
  
  /**
   * 보호자 초대
   */
  @PostMapping("/invite")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  @Operation(summary = "보호자 초대", description = "새로운 보호자를 초대합니다")
  public ResponseEntity<ApiResponse<GuardianInvitationResponse>> inviteGuardian(
      @Valid @RequestBody GuardianInvitationRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 보호자 초대 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    log.info("보호자 초대 요청 - 사용자: {}, 보호자 이메일: {}",
      request.getUserId(), request.getGuardianEmail());

    try {
      GuardianInvitationResponse response = relationshipService.inviteGuardian(request);
      return ResponseEntity.ok(ApiResponse.success(response, "보호자 초대가 발송되었습니다"));
    } catch (IllegalArgumentException e) {
      log.warn("보호자 초대 실패 - 잘못된 요청: {}", e.getMessage());
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
    } catch (IllegalStateException e) {
      log.warn("보호자 초대 실패 - 중복 또는 상태 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error("CONFLICT", e.getMessage()));
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      log.warn("보호자 초대 실패 - 데이터 무결성 위반: {}", e.getMessage());
      String message = "이미 등록된 보호자입니다";
      if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
        message = "이미 같은 이메일로 등록된 보호자가 있습니다";
      }
      return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error("DUPLICATE_GUARDIAN", message));
    } catch (Exception e) {
      log.error("보호자 초대 중 예상치 못한 오류: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("INVITE_ERROR", "보호자 초대 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 초대 수락
   */
  @PostMapping("/accept-invitation")
  @Operation(summary = "초대 수락", description = "보호자 초대를 수락합니다")
  public ResponseEntity<ApiResponse<GuardianRelationshipDto>> acceptInvitation(
      @RequestParam String token,
      @RequestParam Long guardianId) {
    
    log.info("초대 수락 요청 - 토큰: {}, 보호자: {}", token, guardianId);
    
    try {
      GuardianRelationshipDto relationship = relationshipService.acceptInvitation(token, guardianId);
      return ResponseEntity.ok(ApiResponse.success(relationship, "초대가 수락되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("INVALID_TOKEN", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.GONE)
        .body(ApiResponse.error("EXPIRED", e.getMessage()));
    }
  }
  
  /**
   * 초대 거부
   */
  @PostMapping("/reject-invitation")
  @Operation(summary = "초대 거부", description = "보호자 초대를 거부합니다")
  public ResponseEntity<ApiResponse<Void>> rejectInvitation(
      @RequestParam String token,
      @RequestParam Long guardianId) {
    
    log.info("초대 거부 요청 - 토큰: {}, 보호자: {}", token, guardianId);
    
    try {
      relationshipService.rejectInvitation(token, guardianId);
      return ResponseEntity.ok(ApiResponse.success(null, "초대가 거부되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("INVALID_TOKEN", e.getMessage()));
    }
  }
  
  /**
   * 관계 권한 수정
   */
  @PutMapping("/{relationshipId}/permissions")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  @Operation(summary = "권한 수정", description = "보호자 관계의 권한을 수정합니다")
  public ResponseEntity<ApiResponse<GuardianRelationshipDto>> updatePermissions(
      @PathVariable Long relationshipId,
      @Valid @RequestBody PermissionUpdateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("권한 수정 요청 - 관계 ID: {}", relationshipId);
    
    try {
      Long requesterId = jwtAuthUtils.getCurrentUserId();
      if (requesterId == null) {
        log.error("권한 수정에서 인증된 사용자 ID를 찾을 수 없습니다");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다"));
      }
      
      log.info("권한 수정 요청 - 사용자 ID: {}, 관계 ID: {}", requesterId, relationshipId);
      
      GuardianRelationshipDto relationship = 
        relationshipService.updatePermissions(relationshipId, request, requesterId);
      return ResponseEntity.ok(ApiResponse.success(relationship, "권한이 수정되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
    }
  }
  
  /**
   * 관계 일시 중지
   */
  @PostMapping("/{relationshipId}/suspend")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  @Operation(summary = "관계 일시 중지", description = "보호자 관계를 일시 중지합니다")
  public ResponseEntity<ApiResponse<Void>> suspendRelationship(
      @PathVariable Long relationshipId,
      @AuthenticationPrincipal UserDetails userDetails) {

    // 인증 확인
    Long requesterId = jwtAuthUtils.getCurrentUserId();
    if (requesterId == null) {
      log.warn("인증되지 않은 사용자의 관계 일시 중지 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // relationshipId 유효성 검증
    if (relationshipId == null || relationshipId <= 0) {
      log.warn("잘못된 관계 ID로 일시 중지 시도: {}", relationshipId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_RELATIONSHIP_ID", "올바른 관계 ID를 입력해주세요"));
    }

    log.info("관계 일시 중지 요청 - 관계 ID: {}", relationshipId);

    try {
      relationshipService.suspendRelationship(relationshipId, requesterId);
      return ResponseEntity.ok(ApiResponse.success(null, "관계가 일시 중지되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
    } catch (Exception e) {
      log.error("관계 일시 중지 중 예상치 못한 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("SUSPEND_ERROR", "관계 일시 중지 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 관계 재활성화
   */
  @PostMapping("/{relationshipId}/reactivate")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  @Operation(summary = "관계 재활성화", description = "일시 중지된 관계를 재활성화합니다")
  public ResponseEntity<ApiResponse<Void>> reactivateRelationship(
      @PathVariable Long relationshipId,
      @AuthenticationPrincipal UserDetails userDetails) {

    // 인증 확인
    Long requesterId = jwtAuthUtils.getCurrentUserId();
    if (requesterId == null) {
      log.warn("인증되지 않은 사용자의 관계 재활성화 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // relationshipId 유효성 검증
    if (relationshipId == null || relationshipId <= 0) {
      log.warn("잘못된 관계 ID로 재활성화 시도: {}", relationshipId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_RELATIONSHIP_ID", "올바른 관계 ID를 입력해주세요"));
    }

    log.info("관계 재활성화 요청 - 관계 ID: {}", relationshipId);

    try {
      relationshipService.reactivateRelationship(relationshipId, requesterId);
      return ResponseEntity.ok(ApiResponse.success(null, "관계가 재활성화되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
    } catch (Exception e) {
      log.error("관계 재활성화 중 예상치 못한 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("REACTIVATE_ERROR", "관계 재활성화 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 관계 종료
   */
  @DeleteMapping("/{relationshipId}")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  @Operation(summary = "관계 종료", description = "보호자 관계를 종료합니다")
  public ResponseEntity<ApiResponse<Void>> terminateRelationship(
      @PathVariable Long relationshipId,
      @RequestParam(required = false) String reason,
      @AuthenticationPrincipal UserDetails userDetails) {

    // 인증 확인
    Long requesterId = jwtAuthUtils.getCurrentUserId();
    if (requesterId == null) {
      log.warn("인증되지 않은 사용자의 관계 종료 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // relationshipId 유효성 검증
    if (relationshipId == null || relationshipId <= 0) {
      log.warn("잘못된 관계 ID로 종료 시도: {}", relationshipId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_RELATIONSHIP_ID", "올바른 관계 ID를 입력해주세요"));
    }

    log.info("관계 종료 요청 - 관계 ID: {}, 사유: {}", relationshipId, reason);

    try {
      relationshipService.terminateRelationship(relationshipId, reason, requesterId);
      return ResponseEntity.ok(ApiResponse.success(null, "관계가 종료되었습니다"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
    } catch (Exception e) {
      log.error("관계 종료 중 예상치 못한 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("TERMINATE_ERROR", "관계 종료 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 사용자의 보호자 목록 조회
   */
  @GetMapping("/user/{userId}")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN') or hasRole('ADMIN')")
  @Operation(summary = "사용자의 보호자 목록", description = "특정 사용자의 보호자 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<GuardianRelationshipDto>>> getUserGuardians(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "true") boolean activeOnly) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 보호자 목록 조회 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // userId 유효성 검증
    if (userId == null || userId <= 0) {
      log.warn("잘못된 사용자 ID로 보호자 목록 조회 시도: {}", userId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_USER_ID", "올바른 사용자 ID를 입력해주세요"));
    }

    log.info("사용자 보호자 조회 - 사용자 ID: {}, 활성만: {}", userId, activeOnly);

    try {
      List<GuardianRelationshipDto> guardians = relationshipService.getUserGuardians(userId, activeOnly);
      return ResponseEntity.ok(ApiResponse.success(guardians,
        String.format("%d명의 보호자를 조회했습니다", guardians.size())));
    } catch (Exception e) {
      log.error("사용자 보호자 목록 조회 중 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("GUARDIAN_LIST_ERROR", "보호자 목록 조회 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 보호자의 피보호자 목록 조회
   */
  @GetMapping("/guardian/{guardianId}")
  @PreAuthorize("hasRole('GUARDIAN') or hasRole('ADMIN')")
  @Operation(summary = "보호자의 피보호자 목록", description = "특정 보호자의 피보호자 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<GuardianRelationshipDto>>> getGuardianUsers(
      @PathVariable Long guardianId) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 피보호자 목록 조회 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // guardianId 유효성 검증
    if (guardianId == null || guardianId <= 0) {
      log.warn("잘못된 보호자 ID로 피보호자 목록 조회 시도: {}", guardianId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_GUARDIAN_ID", "올바른 보호자 ID를 입력해주세요"));
    }

    log.info("보호자 피보호자 조회 - 보호자 ID: {}", guardianId);

    try {
      List<GuardianRelationshipDto> users = relationshipService.getGuardianUsers(guardianId);
      return ResponseEntity.ok(ApiResponse.success(users,
        String.format("%d명의 피보호자를 조회했습니다", users.size())));
    } catch (Exception e) {
      log.error("보호자 피보호자 목록 조회 중 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("WARD_LIST_ERROR", "피보호자 목록 조회 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 긴급 연락 보호자 목록 조회
   */
  @GetMapping("/user/{userId}/emergency-contacts")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN') or hasRole('ADMIN')")
  @Operation(summary = "긴급 연락 보호자", description = "긴급 상황 시 연락할 보호자 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<EmergencyContactDto>>> getEmergencyContacts(
      @PathVariable Long userId) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 긴급 연락처 조회 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // userId 유효성 검증
    if (userId == null || userId <= 0) {
      log.warn("잘못된 사용자 ID로 긴급 연락처 조회 시도: {}", userId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_USER_ID", "올바른 사용자 ID를 입력해주세요"));
    }

    log.info("긴급 연락 보호자 조회 - 사용자 ID: {}", userId);

    try {
      List<EmergencyContactDto> contacts = relationshipService.getEmergencyContacts(userId);
      return ResponseEntity.ok(ApiResponse.success(contacts,
        String.format("%d명의 긴급 연락처를 조회했습니다", contacts.size())));
    } catch (Exception e) {
      log.error("긴급 연락처 조회 중 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("EMERGENCY_CONTACTS_ERROR", "긴급 연락처 조회 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 권한 확인
   */
  @GetMapping("/check-permission")
  @PreAuthorize("hasRole('GUARDIAN')")
  @Operation(summary = "권한 확인", description = "보호자의 특정 사용자에 대한 권한을 확인합니다")
  public ResponseEntity<ApiResponse<Boolean>> checkPermission(
      @RequestParam Long guardianId,
      @RequestParam Long userId,
      @RequestParam String permissionType) {
    
    log.info("권한 확인 - 보호자: {}, 사용자: {}, 권한: {}", guardianId, userId, permissionType);
    
    boolean hasPermission = relationshipService.hasPermission(guardianId, userId, permissionType);
    return ResponseEntity.ok(ApiResponse.success(hasPermission, 
      hasPermission ? "권한이 있습니다" : "권한이 없습니다"));
  }
  
  /**
   * 활동 시간 업데이트
   */
  @PostMapping("/update-activity")
  @PreAuthorize("hasRole('GUARDIAN')")
  @Operation(summary = "활동 시간 업데이트", description = "보호자의 마지막 활동 시간을 업데이트합니다")
  public ResponseEntity<ApiResponse<Void>> updateActivity(
      @RequestParam Long guardianId,
      @RequestParam Long userId) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 활동 시간 업데이트 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // 파라미터 유효성 검증
    if (guardianId == null || guardianId <= 0) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_GUARDIAN_ID", "올바른 보호자 ID를 입력해주세요"));
    }
    if (userId == null || userId <= 0) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("INVALID_USER_ID", "올바른 사용자 ID를 입력해주세요"));
    }

    log.info("활동 시간 업데이트 - 보호자: {}, 사용자: {}", guardianId, userId);

    try {
      relationshipService.updateLastActiveTime(guardianId, userId);
      return ResponseEntity.ok(ApiResponse.success(null, "활동 시간이 업데이트되었습니다"));
    } catch (Exception e) {
      log.error("활동 시간 업데이트 중 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("ACTIVITY_UPDATE_ERROR", "활동 시간 업데이트 중 오류가 발생했습니다"));
    }
  }
}