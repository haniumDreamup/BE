package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.common.BaseController;
import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleRequest;
import com.bifai.reminder.bifai_backend.dto.schedule.ScheduleResponse;
import com.bifai.reminder.bifai_backend.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 관리 컨트롤러
 *
 * <p>BIF 사용자를 위한 일정 CRUD 및 반복 일정 관리 API를 제공합니다.
 * 복잡한 Cron 표현식 대신 직관적인 반복 패턴을 사용합니다.</p>
 *
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2025-10-02
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule API", description = "일정 관리 API - BIF 사용자를 위한 직관적인 일정 CRUD")
public class ScheduleController extends BaseController {

  private final ScheduleService scheduleService;

  // ===========================================================================================
  // CRUD 기본 API
  // ===========================================================================================

  /**
   * 일정 생성
   *
   * <p>새로운 일정을 등록합니다. 반복 패턴에 따라 다음 실행 시간이 자동으로 계산됩니다.</p>
   *
   * @param request 일정 생성 요청
   * @return 생성된 일정 정보
   */
  @PostMapping
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 생성", description = "새로운 일정을 등록합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
      @Valid @RequestBody ScheduleRequest request) {
    log.info("일정 생성 요청: title={}, type={}, recurrence={}",
        request.getTitle(), request.getScheduleType(), request.getRecurrenceType());

    try {
      ScheduleResponse response = scheduleService.createSchedule(request);
      return createCreatedResponse(response, "일정이 등록되었습니다");
    } catch (Exception e) {
      return handleException("일정 생성 실패", e, "일정 등록 중 오류가 발생했습니다");
    }
  }

  /**
   * 일정 상세 조회
   *
   * <p>특정 일정의 상세 정보를 조회합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 일정 상세 정보
   */
  @GetMapping("/{scheduleId}")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 상세 조회", description = "특정 일정의 상세 정보를 조회합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("일정 조회 요청: scheduleId={}", scheduleId);

    try {
      ScheduleResponse response = scheduleService.getSchedule(scheduleId);
      return createSuccessResponse(response, "일정 정보를 가져왔습니다");
    } catch (Exception e) {
      return handleNotFoundException("일정 조회 실패: scheduleId=" + scheduleId, e,
          "일정을 찾을 수 없습니다");
    }
  }

  /**
   * 사용자의 모든 일정 조회 (페이징)
   *
   * <p>현재 사용자의 모든 일정을 페이지 단위로 조회합니다.</p>
   *
   * @param pageable 페이지 정보
   * @return 일정 목록 (페이지)
   */
  @GetMapping
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 목록 조회", description = "사용자의 모든 일정을 페이지 단위로 조회합니다")
  public ResponseEntity<ApiResponse<Page<ScheduleResponse>>> getAllSchedules(
      @PageableDefault(size = 20) Pageable pageable) {
    log.info("일정 목록 조회 요청: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

    try {
      Page<ScheduleResponse> response = scheduleService.getAllSchedules(pageable);
      return createSuccessResponse(response, "일정 목록을 가져왔습니다");
    } catch (Exception e) {
      return handleException("일정 목록 조회 실패", e, "일정 목록 조회 중 오류가 발생했습니다");
    }
  }

  /**
   * 일정 수정
   *
   * <p>기존 일정의 정보를 수정합니다. 수정 후 다음 실행 시간이 재계산됩니다.</p>
   *
   * @param scheduleId 일정 ID
   * @param request    수정 요청
   * @return 수정된 일정 정보
   */
  @PutMapping("/{scheduleId}")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 수정", description = "기존 일정의 정보를 수정합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId,
      @Valid @RequestBody ScheduleRequest request) {
    log.info("일정 수정 요청: scheduleId={}, title={}", scheduleId, request.getTitle());

    try {
      ScheduleResponse response = scheduleService.updateSchedule(scheduleId, request);
      return createSuccessResponse(response, "일정이 수정되었습니다");
    } catch (Exception e) {
      return handleException("일정 수정 실패: scheduleId=" + scheduleId, e,
          "일정 수정 중 오류가 발생했습니다");
    }
  }

  /**
   * 일정 삭제
   *
   * <p>특정 일정을 삭제합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 삭제 성공 메시지
   */
  @DeleteMapping("/{scheduleId}")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 삭제", description = "특정 일정을 삭제합니다")
  public ResponseEntity<ApiResponse<Void>> deleteSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("일정 삭제 요청: scheduleId={}", scheduleId);

    try {
      scheduleService.deleteSchedule(scheduleId);
      return createSuccessResponse(null, "일정이 삭제되었습니다");
    } catch (Exception e) {
      return handleException("일정 삭제 실패: scheduleId=" + scheduleId, e,
          "일정 삭제 중 오류가 발생했습니다");
    }
  }

  // ===========================================================================================
  // 조회 필터 API
  // ===========================================================================================

  /**
   * 오늘의 일정 조회
   *
   * <p>오늘 실행될 일정 목록을 조회합니다.</p>
   *
   * @return 오늘의 일정 목록
   */
  @GetMapping("/today")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "오늘의 일정 조회", description = "오늘 실행될 일정 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getTodaySchedules() {
    log.info("오늘의 일정 조회 요청");

    try {
      List<ScheduleResponse> response = scheduleService.getTodaySchedules();
      return createSuccessResponse(response, "오늘의 일정 " + response.size() + "건을 가져왔습니다");
    } catch (Exception e) {
      return handleException("오늘의 일정 조회 실패", e, "일정 조회 중 오류가 발생했습니다");
    }
  }

  /**
   * 다가오는 일정 조회
   *
   * <p>향후 N일 이내에 실행될 일정을 조회합니다.</p>
   *
   * @param days 조회할 일수 (기본값: 7일)
   * @return 다가오는 일정 목록
   */
  @GetMapping("/upcoming")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "다가오는 일정 조회", description = "향후 N일 이내에 실행될 일정을 조회합니다")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getUpcomingSchedules(
      @Parameter(description = "조회할 일수 (기본값: 7일)")
      @RequestParam(defaultValue = "7") int days) {
    log.info("다가오는 일정 조회 요청: days={}", days);

    try {
      List<ScheduleResponse> response = scheduleService.getUpcomingSchedules(days);
      return createSuccessResponse(response,
          days + "일 이내 일정 " + response.size() + "건을 가져왔습니다");
    } catch (Exception e) {
      return handleException("다가오는 일정 조회 실패", e, "일정 조회 중 오류가 발생했습니다");
    }
  }

  /**
   * 특정 날짜의 일정 조회
   *
   * <p>지정한 날짜에 실행될 일정을 조회합니다.</p>
   *
   * @param date 조회할 날짜 (YYYY-MM-DD)
   * @return 해당 날짜의 일정 목록
   */
  @GetMapping("/date")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "특정 날짜 일정 조회", description = "지정한 날짜에 실행될 일정을 조회합니다")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByDate(
      @Parameter(description = "조회할 날짜 (YYYY-MM-DD)")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    log.info("특정 날짜 일정 조회 요청: date={}", date);

    try {
      List<ScheduleResponse> response = scheduleService.getSchedulesByDate(date);
      return createSuccessResponse(response,
          date + " 일정 " + response.size() + "건을 가져왔습니다");
    } catch (Exception e) {
      return handleException("특정 날짜 일정 조회 실패", e, "일정 조회 중 오류가 발생했습니다");
    }
  }

  /**
   * 기간별 일정 조회
   *
   * <p>시작일부터 종료일까지의 일정을 조회합니다.</p>
   *
   * @param startDate 시작 날짜 (YYYY-MM-DD)
   * @param endDate   종료 날짜 (YYYY-MM-DD)
   * @return 기간 내 일정 목록
   */
  @GetMapping("/range")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "기간별 일정 조회", description = "시작일부터 종료일까지의 일정을 조회합니다")
  public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedulesByDateRange(
      @Parameter(description = "시작 날짜 (YYYY-MM-DD)")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @Parameter(description = "종료 날짜 (YYYY-MM-DD)")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    log.info("기간별 일정 조회 요청: startDate={}, endDate={}", startDate, endDate);

    try {
      List<ScheduleResponse> response = scheduleService.getSchedulesByDateRange(startDate, endDate);
      return createSuccessResponse(response,
          "기간 내 일정 " + response.size() + "건을 가져왔습니다");
    } catch (Exception e) {
      return handleException("기간별 일정 조회 실패", e, "일정 조회 중 오류가 발생했습니다");
    }
  }

  // ===========================================================================================
  // 상태 관리 API
  // ===========================================================================================

  /**
   * 일정 완료 처리
   *
   * <p>일정을 완료로 표시하고, 반복 일정인 경우 다음 실행 시간을 계산합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @PostMapping("/{scheduleId}/complete")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 완료 처리", description = "일정을 완료로 표시하고 다음 실행 시간을 계산합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> completeSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("일정 완료 처리 요청: scheduleId={}", scheduleId);

    try {
      ScheduleResponse response = scheduleService.completeSchedule(scheduleId);
      return createSuccessResponse(response, "일정이 완료 처리되었습니다");
    } catch (Exception e) {
      return handleException("일정 완료 처리 실패: scheduleId=" + scheduleId, e,
          "완료 처리 중 오류가 발생했습니다");
    }
  }

  /**
   * 일정 완료 취소
   *
   * <p>완료 표시를 취소하고 일정을 다시 활성화합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @PostMapping("/{scheduleId}/uncomplete")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 완료 취소", description = "완료 표시를 취소하고 일정을 다시 활성화합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> uncompleteSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("일정 완료 취소 요청: scheduleId={}", scheduleId);

    try {
      ScheduleResponse response = scheduleService.uncompleteSchedule(scheduleId);
      return createSuccessResponse(response, "일정 완료가 취소되었습니다");
    } catch (Exception e) {
      return handleException("일정 완료 취소 실패: scheduleId=" + scheduleId, e,
          "완료 취소 중 오류가 발생했습니다");
    }
  }

  /**
   * 일정 활성화
   *
   * <p>비활성화된 일정을 다시 활성화합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @PutMapping("/{scheduleId}/activate")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 활성화", description = "비활성화된 일정을 다시 활성화합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> activateSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("일정 활성화 요청: scheduleId={}", scheduleId);

    try {
      ScheduleResponse response = scheduleService.activateSchedule(scheduleId);
      return createSuccessResponse(response, "일정이 활성화되었습니다");
    } catch (Exception e) {
      return handleException("일정 활성화 실패: scheduleId=" + scheduleId, e,
          "활성화 중 오류가 발생했습니다");
    }
  }

  /**
   * 일정 비활성화
   *
   * <p>일정을 일시적으로 비활성화합니다. 실행되지 않습니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @PutMapping("/{scheduleId}/deactivate")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "일정 비활성화", description = "일정을 일시적으로 비활성화합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> deactivateSchedule(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("일정 비활성화 요청: scheduleId={}", scheduleId);

    try {
      ScheduleResponse response = scheduleService.deactivateSchedule(scheduleId);
      return createSuccessResponse(response, "일정이 비활성화되었습니다");
    } catch (Exception e) {
      return handleException("일정 비활성화 실패: scheduleId=" + scheduleId, e,
          "비활성화 중 오류가 발생했습니다");
    }
  }

  // ===========================================================================================
  // 반복 일정 API
  // ===========================================================================================

  /**
   * 다음 실행 건너뛰기
   *
   * <p>반복 일정의 다음 실행을 건너뛰고 그 다음 실행 시간을 계산합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @return 업데이트된 일정 정보
   */
  @PostMapping("/{scheduleId}/skip-next")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "다음 실행 건너뛰기", description = "반복 일정의 다음 실행을 건너뛰고 그 다음 시간을 계산합니다")
  public ResponseEntity<ApiResponse<ScheduleResponse>> skipNextExecution(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId) {
    log.info("다음 실행 건너뛰기 요청: scheduleId={}", scheduleId);

    try {
      ScheduleResponse response = scheduleService.skipNextExecution(scheduleId);
      return createSuccessResponse(response, "다음 실행이 건너뛰어졌습니다");
    } catch (Exception e) {
      return handleException("다음 실행 건너뛰기 실패: scheduleId=" + scheduleId, e,
          "건너뛰기 중 오류가 발생했습니다");
    }
  }

  /**
   * 반복 일정 목록 조회
   *
   * <p>반복 일정의 향후 실행 시간 목록을 조회합니다.</p>
   *
   * @param scheduleId 일정 ID
   * @param count      조회할 횟수 (기본값: 10)
   * @return 향후 실행 시간 목록
   */
  @GetMapping("/{scheduleId}/occurrences")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "반복 일정 목록 조회", description = "반복 일정의 향후 실행 시간 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<LocalDateTime>>> getScheduleOccurrences(
      @Parameter(description = "일정 ID") @PathVariable Long scheduleId,
      @Parameter(description = "조회할 횟수 (기본값: 10)")
      @RequestParam(defaultValue = "10") int count) {
    log.info("반복 일정 목록 조회 요청: scheduleId={}, count={}", scheduleId, count);

    try {
      List<LocalDateTime> response = scheduleService.getScheduleOccurrences(scheduleId, count);
      return createSuccessResponse(response,
          "향후 " + response.size() + "회 실행 시간을 가져왔습니다");
    } catch (Exception e) {
      return handleException("반복 일정 목록 조회 실패: scheduleId=" + scheduleId, e,
          "조회 중 오류가 발생했습니다");
    }
  }
}
