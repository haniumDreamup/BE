package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.dashboard.DailyStatusSummaryDto;
import com.bifai.reminder.bifai_backend.dto.dashboard.WeeklySummaryDto;
import com.bifai.reminder.bifai_backend.dto.response.ApiResponse;
import com.bifai.reminder.bifai_backend.service.DailyStatusSummaryService;
import com.bifai.reminder.bifai_backend.service.WeeklySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 보호자 대시보드 컨트롤러
 */
@RestController
@RequestMapping("/api/guardian/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guardian Dashboard", description = "보호자 대시보드 API")
public class GuardianDashboardController {
  
  private final DailyStatusSummaryService dailySummaryService;
  private final WeeklySummaryService weeklySummaryService;
  
  /**
   * 오늘의 상태 요약 조회
   */
  @GetMapping("/daily-summary/{userId}")
  @PreAuthorize("hasRole('GUARDIAN') or hasRole('ADMIN')")
  @Operation(summary = "오늘의 상태 요약", description = "BIF 사용자의 오늘 상태를 간단히 요약합니다")
  public ResponseEntity<ApiResponse<DailyStatusSummaryDto>> getDailySummary(
      @PathVariable Long userId,
      @RequestParam Long guardianId) {
    
    log.info("오늘의 상태 요약 조회 - 사용자: {}, 보호자: {}", userId, guardianId);
    
    try {
      DailyStatusSummaryDto summary = dailySummaryService.getDailySummary(userId, guardianId);
      
      String message = summary.getStatusMessage();
      if ("WARNING".equals(summary.getOverallStatus())) {
        log.warn("사용자 {} 상태 경고: {}", userId, message);
      }
      
      return ResponseEntity.ok(ApiResponse.success(summary, message));
      
    } catch (IllegalArgumentException e) {
      log.error("권한 오류: {}", e.getMessage());
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("PERMISSION_DENIED", e.getMessage()));
    } catch (Exception e) {
      log.error("대시보드 조회 실패", e);
      return ResponseEntity.internalServerError()
        .body(ApiResponse.error("INTERNAL_ERROR", "대시보드 조회 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 주간 요약 리포트 조회
   */
  @GetMapping("/weekly-summary/{userId}")
  @PreAuthorize("hasRole('GUARDIAN') or hasRole('ADMIN')")
  @Operation(summary = "주간 요약 리포트", description = "최근 7일간의 활동을 요약합니다")
  public ResponseEntity<ApiResponse<WeeklySummaryDto>> getWeeklySummary(
      @PathVariable Long userId,
      @RequestParam Long guardianId,
      @Parameter(description = "주 오프셋 (0: 이번 주, 1: 지난 주)")
      @RequestParam(defaultValue = "0") int weekOffset) {
    
    log.info("주간 요약 조회 - 사용자: {}, 보호자: {}, 오프셋: {}", userId, guardianId, weekOffset);
    
    try {
      WeeklySummaryDto summary = weeklySummaryService.getWeeklySummary(userId, guardianId, weekOffset);
      
      String message = String.format("%s님의 %s ~ %s 주간 리포트", 
        summary.getUserName(), 
        summary.getWeekStartDate(),
        summary.getWeekEndDate());
      
      // 주의사항이 있으면 로그 기록
      if (!summary.getConcerns().isEmpty()) {
        log.warn("사용자 {} 주간 주의사항: {}", userId, summary.getConcerns());
      }
      
      return ResponseEntity.ok(ApiResponse.success(summary, message));
      
    } catch (IllegalArgumentException e) {
      log.error("권한 오류: {}", e.getMessage());
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("PERMISSION_DENIED", e.getMessage()));
    } catch (Exception e) {
      log.error("주간 리포트 조회 실패", e);
      return ResponseEntity.internalServerError()
        .body(ApiResponse.error("INTERNAL_ERROR", "주간 리포트 조회 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 통합 대시보드 조회 (오늘 + 주간 요약)
   */
  @GetMapping("/integrated/{userId}")
  @PreAuthorize("hasRole('GUARDIAN') or hasRole('ADMIN')")
  @Operation(summary = "통합 대시보드", description = "오늘의 상태와 주간 요약을 한 번에 조회합니다")
  public ResponseEntity<ApiResponse<IntegratedDashboard>> getIntegratedDashboard(
      @PathVariable Long userId,
      @RequestParam Long guardianId) {
    
    log.info("통합 대시보드 조회 - 사용자: {}, 보호자: {}", userId, guardianId);
    
    try {
      DailyStatusSummaryDto dailySummary = dailySummaryService.getDailySummary(userId, guardianId);
      WeeklySummaryDto weeklySummary = weeklySummaryService.getWeeklySummary(userId, guardianId, 0);
      
      IntegratedDashboard dashboard = IntegratedDashboard.builder()
        .userId(userId)
        .userName(dailySummary.getUserName())
        .dailySummary(dailySummary)
        .weeklySummary(weeklySummary)
        .lastUpdated(dailySummary.getSummaryDate())
        .build();
      
      return ResponseEntity.ok(ApiResponse.success(dashboard, "대시보드를 조회했습니다"));
      
    } catch (IllegalArgumentException e) {
      log.error("권한 오류: {}", e.getMessage());
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("PERMISSION_DENIED", e.getMessage()));
    } catch (Exception e) {
      log.error("통합 대시보드 조회 실패", e);
      return ResponseEntity.internalServerError()
        .body(ApiResponse.error("INTERNAL_ERROR", "대시보드 조회 중 오류가 발생했습니다"));
    }
  }
  
  /**
   * 통합 대시보드 DTO
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class IntegratedDashboard {
    private Long userId;
    private String userName;
    private DailyStatusSummaryDto dailySummary;
    private WeeklySummaryDto weeklySummary;
    private java.time.LocalDateTime lastUpdated;
  }
}