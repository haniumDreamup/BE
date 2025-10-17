package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.user.UserUpdateRequest;
import com.bifai.reminder.bifai_backend.dto.user.RoleUpdateRequest;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관리 컨트롤러
 * BIF 사용자 정보 조회 및 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 본인 정보 조회
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        log.info("현재 사용자 정보 조회 요청");
        
        try {
            User currentUser = userService.getCurrentUser();
            return ResponseEntity.ok(
                ApiResponse.success(currentUser, "사용자 정보를 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다"));
        }
    }

    /**
     * 본인 정보 수정
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("사용자 정보 수정 요청");
        
        try {
            User updatedUser = userService.updateCurrentUser(request);
            return ResponseEntity.ok(
                ApiResponse.success(updatedUser, "정보가 수정되었습니다")
            );
        } catch (Exception e) {
            log.error("사용자 정보 수정 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다"));
        }
    }

    /**
     * 특정 사용자 조회 (관리자 또는 보호자)
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @guardianService.isGuardianOf(#userId)")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        log.info("사용자 정보 조회 요청: userId={}", userId);
        
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(
                ApiResponse.success(user, "사용자 정보를 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: userId={}", userId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "사용자를 찾을 수 없습니다"));
        }
    }

    /**
     * 전체 사용자 목록 조회 (관리자 전용)
     * GET /api/v1/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(Pageable pageable) {
        log.info("전체 사용자 목록 조회 요청");
        
        try {
            Page<User> users = userService.getAllUsers(pageable);
            return ResponseEntity.ok(
                ApiResponse.success(users, "사용자 목록을 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("사용자 목록 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "사용자 목록을 가져오는 중 오류가 발생했습니다"));
        }
    }

    /**
     * 사용자 비활성화 (관리자 전용)
     * PUT /api/v1/users/{userId}/deactivate
     */
    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        log.info("사용자 비활성화 요청: userId={}", userId);
        
        try {
            userService.deactivateUser(userId);
            return ResponseEntity.ok(
                ApiResponse.success(null, "사용자가 비활성화되었습니다")
            );
        } catch (Exception e) {
            log.error("사용자 비활성화 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "사용자 비활성화 중 오류가 발생했습니다"));
        }
    }

    /**
     * 사용자 활성화 (관리자 전용)
     * PUT /api/v1/users/{userId}/activate
     */
    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        log.info("사용자 활성화 요청: userId={}", userId);
        
        try {
            userService.activateUser(userId);
            return ResponseEntity.ok(
                ApiResponse.success(null, "사용자가 활성화되었습니다")
            );
        } catch (Exception e) {
            log.error("사용자 활성화 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "사용자 활성화 중 오류가 발생했습니다"));
        }
    }

    /**
     * 사용자 역할 수정 (관리자 전용)
     * PUT /api/v1/users/{userId}/roles
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody RoleUpdateRequest request) {
        log.info("사용자 역할 수정 요청: userId={}", userId);

        try {
            User updatedUser = userService.updateUserRoles(userId, request.getRoleIds());
            return ResponseEntity.ok(
                ApiResponse.success(updatedUser, "사용자 역할이 수정되었습니다")
            );
        } catch (Exception e) {
            log.error("사용자 역할 수정 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "사용자 역할 수정 중 오류가 발생했습니다"));
        }
    }

}