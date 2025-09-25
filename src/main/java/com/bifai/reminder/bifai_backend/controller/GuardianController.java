package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.guardian.GuardianRequest;
import com.bifai.reminder.bifai_backend.dto.guardian.GuardianPermissionRequest;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.service.GuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 보호자 관리 컨트롤러
 * BIF 사용자의 보호자 등록 및 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    /**
     * 나의 보호자 목록 조회
     * GET /api/v1/guardians/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<Guardian>>> getMyGuardians() {
        log.info("나의 보호자 목록 조회 요청");
        
        try {
            List<Guardian> guardians = guardianService.getMyGuardians();
            return ResponseEntity.ok(
                ApiResponse.success(guardians, "보호자 목록을 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("보호자 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다"));
        }
    }

    /**
     * 내가 보호 중인 사용자 목록 조회
     * GET /api/v1/guardians/protected-users
     */
    @GetMapping("/protected-users")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<ApiResponse<List<Guardian>>> getProtectedUsers() {
        log.info("보호 중인 사용자 목록 조회 요청");
        
        try {
            List<Guardian> protectedUsers = guardianService.getProtectedUsers();
            return ResponseEntity.ok(
                ApiResponse.success(protectedUsers, "보호 중인 사용자 목록을 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("보호 중인 사용자 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다"));
        }
    }

    /**
     * 보호자 등록 요청
     * POST /api/v1/guardians
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Guardian>> requestGuardian(
            @Valid @RequestBody GuardianRequest request) {
        log.info("보호자 등록 요청: guardianEmail={}", request.getGuardianEmail());
        
        try {
            Guardian guardian = guardianService.requestGuardian(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(guardian, "보호자 등록 요청이 전송되었습니다"));
        } catch (IllegalArgumentException e) {
            log.warn("보호자 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("보호자 등록 중 오류", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("보호자 등록 중 오류가 발생했습니다"));
        }
    }

    /**
     * 보호자 요청 승인
     * PUT /api/v1/guardians/{guardianId}/approve
     */
    @PutMapping("/{guardianId}/approve")
    @PreAuthorize("hasRole('GUARDIAN') and @guardianService.canApproveGuardian(#guardianId)")
    public ResponseEntity<ApiResponse<Guardian>> approveGuardian(
            @PathVariable Long guardianId) {
        log.info("보호자 요청 승인: guardianId={}", guardianId);
        
        try {
            Guardian guardian = guardianService.approveGuardian(guardianId);
            return ResponseEntity.ok(
                ApiResponse.success(guardian, "보호자 요청이 승인되었습니다")
            );
        } catch (Exception e) {
            log.error("보호자 승인 실패: guardianId={}", guardianId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다"));
        }
    }

    /**
     * 보호자 요청 거절
     * PUT /api/v1/guardians/{guardianId}/reject
     */
    @PutMapping("/{guardianId}/reject")
    @PreAuthorize("hasRole('GUARDIAN') and @guardianService.canRejectGuardian(#guardianId)")
    public ResponseEntity<ApiResponse<Void>> rejectGuardian(
            @PathVariable Long guardianId,
            @RequestBody(required = false) String reason) {
        log.info("보호자 요청 거절: guardianId={}", guardianId);
        
        try {
            guardianService.rejectGuardian(guardianId, reason);
            return ResponseEntity.ok(
                ApiResponse.success(null, "보호자 요청이 거절되었습니다")
            );
        } catch (Exception e) {
            log.error("보호자 거절 실패: guardianId={}", guardianId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다"));
        }
    }

    /**
     * 보호자 권한 수정
     * PUT /api/v1/guardians/{guardianId}/permissions
     */
    @PutMapping("/{guardianId}/permissions")
    @PreAuthorize("hasRole('USER') and @guardianService.isMyGuardian(#guardianId)")
    public ResponseEntity<ApiResponse<Guardian>> updateGuardianPermissions(
            @PathVariable Long guardianId,
            @Valid @RequestBody GuardianPermissionRequest request) {
        log.info("보호자 권한 수정: guardianId={}", guardianId);
        
        try {
            Guardian guardian = guardianService.updatePermissions(guardianId, request);
            return ResponseEntity.ok(
                ApiResponse.success(guardian, "보호자 권한이 수정되었습니다")
            );
        } catch (Exception e) {
            log.error("보호자 권한 수정 실패: guardianId={}", guardianId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("권한 수정 중 오류가 발생했습니다"));
        }
    }

    /**
     * 보호자 삭제
     * DELETE /api/v1/guardians/{guardianId}
     */
    @DeleteMapping("/{guardianId}")
    @PreAuthorize("hasRole('USER') and @guardianService.isMyGuardian(#guardianId)")
    public ResponseEntity<ApiResponse<Void>> removeGuardian(
            @PathVariable Long guardianId) {
        log.info("보호자 삭제 요청: guardianId={}", guardianId);
        
        try {
            guardianService.removeGuardian(guardianId);
            return ResponseEntity.ok(
                ApiResponse.success(null, "보호자가 삭제되었습니다")
            );
        } catch (Exception e) {
            log.error("보호자 삭제 실패: guardianId={}", guardianId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다"));
        }
    }

    /**
     * 보호 관계 해제 (보호자 측에서)
     * DELETE /api/v1/guardians/relationships/{guardianId}
     */
    @DeleteMapping("/relationships/{guardianId}")
    @PreAuthorize("hasRole('GUARDIAN') and @guardianService.canRemoveRelationship(#guardianId)")
    public ResponseEntity<ApiResponse<Void>> removeGuardianRelationship(
            @PathVariable Long guardianId) {
        log.info("보호 관계 해제 요청: guardianId={}", guardianId);

        try {
            guardianService.removeRelationship(guardianId);
            return ResponseEntity.ok(
                ApiResponse.success(null, "보호 관계가 해제되었습니다")
            );
        } catch (Exception e) {
            log.error("보호 관계 해제 실패: guardianId={}", guardianId, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("인증이 필요합니다"));
        }
    }


}