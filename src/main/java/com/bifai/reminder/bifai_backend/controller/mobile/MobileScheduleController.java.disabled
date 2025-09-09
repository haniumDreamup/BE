package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileScheduleResponse;
import com.bifai.reminder.bifai_backend.dto.mobile.ScheduleCompleteRequest;
import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.service.mobile.MobileScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 모바일 일정 관리 API 컨트롤러
 * 
 * BIF 사용자를 위한 간단한 일정 관리 기능을 제공합니다.
 * 오늘의 할 일, 일정 완료 체크, 일정 기록 등을 포함합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/schedules")
@RequiredArgsConstructor
@Tag(name = "Mobile Schedule", description = "모바일 일정 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class MobileScheduleController {
  
  private final MobileScheduleService mobileScheduleService;
  
  /**
   * 오늘의 일정 목록 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 오늘 해야 할 일정 목록
   */
  @GetMapping("/today")
  @Operation(
      summary = "오늘의 일정 목록",
      description = "오늘 예정된 일정 목록과 완료 상태를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "일정 목록 조회 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 필요"
      )
  })
  public ResponseEntity<MobileApiResponse<List<MobileScheduleResponse>>> getTodaySchedules(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("오늘의 일정 목록 조회: user={}", userDetails.getUsername());
    
    try {
      List<MobileScheduleResponse> schedules = 
          mobileScheduleService.getTodaySchedules(userDetails.getUsername());
      
      String message = schedules.isEmpty() ? 
          "오늘 예정된 일정이 없어요" : 
          String.format("오늘 할 일이 %d개 있어요", schedules.size());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(schedules, message)
      );
      
    } catch (Exception e) {
      log.error("일정 목록 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "SCH_001",
              "일정 정보를 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 특정 날짜의 일정 목록 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @param date 조회할 날짜
   * @return 해당 날짜의 일정 목록
   */
  @GetMapping
  @Operation(
      summary = "특정 날짜 일정 목록",
      description = "지정한 날짜의 일정 목록과 완료 기록을 조회합니다."
  )
  public ResponseEntity<MobileApiResponse<List<MobileScheduleResponse>>> getSchedulesByDate(
      @AuthenticationPrincipal UserDetails userDetails,
      @Parameter(description = "조회할 날짜 (YYYY-MM-DD)", example = "2024-01-15")
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
    
    log.info("일정 목록 조회: user={}, date={}", userDetails.getUsername(), date);
    
    try {
      List<MobileScheduleResponse> schedules = 
          mobileScheduleService.getSchedulesByDate(userDetails.getUsername(), date);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(schedules, "일정 정보를 불러왔어요")
      );
      
    } catch (Exception e) {
      log.error("일정 목록 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "SCH_002",
              "일정 정보를 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 일정 완료 체크
   * 
   * @param scheduleId 일정 ID
   * @param request 일정 완료 요청
   * @param userDetails 인증된 사용자 정보
   * @return 일정 완료 결과
   */
  @PostMapping("/{scheduleId}/complete")
  @Operation(
      summary = "일정 완료 체크",
      description = "일정 완료를 체크하고 기록합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "일정 완료 체크 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "일정을 찾을 수 없음"
      )
  })
  public ResponseEntity<MobileApiResponse<MobileScheduleResponse>> completeSchedule(
      @Parameter(description = "일정 ID", example = "1")
      @PathVariable Long scheduleId,
      @Valid @RequestBody ScheduleCompleteRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("일정 완료 체크: user={}, scheduleId={}, completed={}", 
        userDetails.getUsername(), scheduleId, request.isCompleted());
    
    try {
      MobileScheduleResponse response = mobileScheduleService.completeSchedule(
          userDetails.getUsername(), scheduleId, request);
      
      String message = request.isCompleted() ? 
          "일정 완료! 잘하고 있어요 🎉" : 
          "일정 완료를 취소했어요";
      
      return ResponseEntity.ok(
          MobileApiResponse.success(response, message)
      );
      
    } catch (Exception e) {
      log.error("일정 완료 체크 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "SCH_003",
              "일정 완료 체크를 할 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 일정 상세 정보 조회
   * 
   * @param scheduleId 일정 ID
   * @param userDetails 인증된 사용자 정보
   * @return 일정 상세 정보
   */
  @GetMapping("/{scheduleId}")
  @Operation(
      summary = "일정 상세 정보",
      description = "특정 일정의 상세 정보를 조회합니다."
  )
  public ResponseEntity<MobileApiResponse<MobileScheduleResponse>> getScheduleDetail(
      @Parameter(description = "일정 ID", example = "1")
      @PathVariable Long scheduleId,
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("일정 상세 조회: user={}, scheduleId={}", 
        userDetails.getUsername(), scheduleId);
    
    try {
      MobileScheduleResponse schedule = mobileScheduleService.getScheduleDetail(
          userDetails.getUsername(), scheduleId);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(schedule, "일정 정보를 불러왔어요")
      );
      
    } catch (Exception e) {
      log.error("일정 상세 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(404).body(
          MobileApiResponse.error(
              "SCH_004",
              "일정 정보를 찾을 수 없어요",
              "다른 일정을 확인해보세요"
          )
      );
    }
  }
  
  /**
   * 일주일 일정 요약 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 주간 일정 요약
   */
  @GetMapping("/week-summary")
  @Operation(
      summary = "주간 일정 요약",
      description = "이번 주 일정 요약 정보를 조회합니다."
  )
  public ResponseEntity<MobileApiResponse<List<MobileScheduleResponse>>> getWeekSummary(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("주간 일정 요약 조회: user={}", userDetails.getUsername());
    
    try {
      List<MobileScheduleResponse> weekSchedules = 
          mobileScheduleService.getWeekSummary(userDetails.getUsername());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(weekSchedules, "이번 주 일정이에요")
      );
      
    } catch (Exception e) {
      log.error("주간 일정 요약 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "SCH_005",
              "주간 일정을 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
}