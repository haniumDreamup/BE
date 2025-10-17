package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 전용 컨트롤러
 * 시스템 관리 및 모니터링 기능
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@org.springframework.context.annotation.Profile("!test")
public class AdminController {

    private final AdminService adminService;

    /**
     * 시스템 통계 조회
     * GET /api/admin/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatistics() {
        log.info("시스템 통계 조회 요청");
        
        try {
            Map<String, Object> statistics = adminService.getSystemStatistics();
            return ResponseEntity.ok(
                ApiResponse.success(statistics, "시스템 통계를 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("시스템 통계 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "통계 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 활성 사용자 세션 조회
     * GET /api/admin/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveSessions() {
        log.info("활성 세션 조회 요청");
        
        try {
            Map<String, Object> sessions = adminService.getActiveSessions();
            return ResponseEntity.ok(
                ApiResponse.success(sessions, "활성 세션 목록을 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("활성 세션 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "세션 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 특정 사용자 세션 강제 종료
     * DELETE /api/v1/admin/sessions/{userId}
     */
    @DeleteMapping("/sessions/{userId}")
    public ResponseEntity<ApiResponse<Void>> terminateUserSession(@PathVariable Long userId) {
        log.info("사용자 세션 종료 요청: userId={}", userId);
        
        try {
            adminService.terminateUserSession(userId);
            return ResponseEntity.ok(
                ApiResponse.success(null, "사용자 세션이 종료되었습니다")
            );
        } catch (Exception e) {
            log.error("세션 종료 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "세션 종료 중 오류가 발생했습니다"));
        }
    }

    /**
     * 인증 로그 조회
     * GET /api/v1/admin/auth-logs
     */
    @GetMapping("/auth-logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthenticationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String eventType) {
        log.info("인증 로그 조회 요청: page={}, size={}", page, size);
        
        try {
            Map<String, Object> logs = adminService.getAuthenticationLogs(page, size, username, eventType);
            return ResponseEntity.ok(
                ApiResponse.success(logs, "인증 로그를 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("인증 로그 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "로그 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 시스템 설정 조회
     * GET /api/v1/admin/settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemSettings() {
        log.info("시스템 설정 조회 요청");
        
        try {
            Map<String, Object> settings = adminService.getSystemSettings();
            return ResponseEntity.ok(
                ApiResponse.success(settings, "시스템 설정을 가져왔습니다")
            );
        } catch (Exception e) {
            log.error("시스템 설정 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "설정 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * 시스템 설정 수정
     * PUT /api/v1/admin/settings
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSystemSettings(
            @RequestBody Map<String, Object> settings) {
        log.info("시스템 설정 수정 요청");
        
        try {
            Map<String, Object> updatedSettings = adminService.updateSystemSettings(settings);
            return ResponseEntity.ok(
                ApiResponse.success(updatedSettings, "시스템 설정이 수정되었습니다")
            );
        } catch (Exception e) {
            log.error("시스템 설정 수정 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "설정 수정 중 오류가 발생했습니다"));
        }
    }

    /**
     * 데이터베이스 백업 실행
     * POST /api/v1/admin/backup
     */
    @PostMapping("/backup")
    public ResponseEntity<ApiResponse<Map<String, String>>> createBackup() {
        log.info("데이터베이스 백업 요청");
        
        try {
            Map<String, String> backupInfo = adminService.createDatabaseBackup();
            return ResponseEntity.ok(
                ApiResponse.success(backupInfo, "백업이 생성되었습니다")
            );
        } catch (Exception e) {
            log.error("백업 생성 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "백업 생성 중 오류가 발생했습니다"));
        }
    }

    /**
     * 캐시 초기화
     * DELETE /api/v1/admin/cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> clearCache(
            @RequestParam(required = false) String cacheName) {
        log.info("캐시 초기화 요청: cacheName={}", cacheName);
        
        try {
            adminService.clearCache(cacheName);
            return ResponseEntity.ok(
                ApiResponse.success(null, "캐시가 초기화되었습니다")
            );
        } catch (Exception e) {
            log.error("캐시 초기화 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "캐시 초기화 중 오류가 발생했습니다"));
        }
    }
}