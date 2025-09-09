package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.statistics.DailyActivityStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.GeofenceStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.SafetyStatsDto;
import com.bifai.reminder.bifai_backend.service.StatisticsService;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 통계 API 컨트롤러
 * 지오펜스, 일일 활동, 안전 통계 정보 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StatisticsController {

  private final StatisticsService statisticsService;
  private final JwtAuthUtils jwtAuthUtils;

  /**
   * 지오펜스 통계 조회
   * @param startDate 조회 시작 날짜 (선택사항, 기본값: 30일 전)
   * @param endDate 조회 종료 날짜 (선택사항, 기본값: 오늘)
   * @return 지오펜스 통계 정보
   */
  @GetMapping("/geofence")
  public ResponseEntity<ApiResponse<GeofenceStatsDto>> getGeofenceStatistics(
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.error("인증된 사용자 정보를 찾을 수 없습니다");
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("USER_NOT_FOUND", "인증된 사용자 정보를 찾을 수 없습니다", "다시 로그인해주세요"));
    }

    try {
      GeofenceStatsDto stats = statisticsService.getGeofenceStatistics(userId, startDate, endDate);
      log.info("지오펜스 통계 조회 완료: 사용자 {}", userId);
      
      return ResponseEntity.ok(ApiResponse.success(stats, "지오펜스 통계를 성공적으로 조회했습니다"));
      
    } catch (Exception e) {
      log.error("지오펜스 통계 조회 실패: 사용자 {}", userId, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("STATS_ERROR", "통계 정보를 불러올 수 없습니다", "잠시 후 다시 시도해주세요"));
    }
  }

  /**
   * 일일 활동 통계 조회 (여러 날짜)
   * @param startDate 조회 시작 날짜 (선택사항, 기본값: 7일 전)
   * @param endDate 조회 종료 날짜 (선택사항, 기본값: 오늘)
   * @return 일일 활동 통계 목록
   */
  @GetMapping("/daily-activity")
  public ResponseEntity<ApiResponse<List<DailyActivityStatsDto>>> getDailyActivityStatistics(
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.error("인증된 사용자 정보를 찾을 수 없습니다");
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("USER_NOT_FOUND", "인증된 사용자 정보를 찾을 수 없습니다", "다시 로그인해주세요"));
    }

    try {
      List<DailyActivityStatsDto> stats = statisticsService.getDailyActivityStatistics(userId, startDate, endDate);
      log.info("일일 활동 통계 조회 완료: 사용자 {}, {} 건", userId, stats.size());
      
      return ResponseEntity.ok(ApiResponse.success(stats, "일일 활동 통계를 성공적으로 조회했습니다"));
      
    } catch (Exception e) {
      log.error("일일 활동 통계 조회 실패: 사용자 {}", userId, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("STATS_ERROR", "통계 정보를 불러올 수 없습니다", "잠시 후 다시 시도해주세요"));
    }
  }

  /**
   * 특정 날짜 일일 활동 통계 조회
   * @param date 조회할 날짜 (선택사항, 기본값: 오늘)
   * @return 해당 날짜 활동 통계
   */
  @GetMapping("/daily-activity/single")
  public ResponseEntity<ApiResponse<DailyActivityStatsDto>> getSingleDayActivityStatistics(
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.error("인증된 사용자 정보를 찾을 수 없습니다");
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("USER_NOT_FOUND", "인증된 사용자 정보를 찾을 수 없습니다", "다시 로그인해주세요"));
    }

    try {
      if (date == null) {
        date = LocalDate.now();
      }
      
      DailyActivityStatsDto stats = statisticsService.getDailyActivityStatistics(userId, date);
      log.info("일일 활동 통계 조회 완료: 사용자 {}, 날짜 {}", userId, date);
      
      return ResponseEntity.ok(ApiResponse.success(stats, 
          String.format("%s 활동 통계를 성공적으로 조회했습니다", date)));
      
    } catch (Exception e) {
      log.error("일일 활동 통계 조회 실패: 사용자 {}, 날짜 {}", userId, date, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("STATS_ERROR", "통계 정보를 불러올 수 없습니다", "잠시 후 다시 시도해주세요"));
    }
  }

  /**
   * 안전 통계 조회
   * @param startDate 조회 시작 날짜 (선택사항, 기본값: 30일 전)
   * @param endDate 조회 종료 날짜 (선택사항, 기본값: 오늘)
   * @return 안전 통계 정보
   */
  @GetMapping("/safety")
  public ResponseEntity<ApiResponse<SafetyStatsDto>> getSafetyStatistics(
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.error("인증된 사용자 정보를 찾을 수 없습니다");
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("USER_NOT_FOUND", "인증된 사용자 정보를 찾을 수 없습니다", "다시 로그인해주세요"));
    }

    try {
      SafetyStatsDto stats = statisticsService.getSafetyStatistics(userId, startDate, endDate);
      log.info("안전 통계 조회 완료: 사용자 {}", userId);
      
      return ResponseEntity.ok(ApiResponse.success(stats, "안전 통계를 성공적으로 조회했습니다"));
      
    } catch (Exception e) {
      log.error("안전 통계 조회 실패: 사용자 {}", userId, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("STATS_ERROR", "통계 정보를 불러올 수 없습니다", "잠시 후 다시 시도해주세요"));
    }
  }

  /**
   * 전체 통계 요약 조회
   * @param startDate 조회 시작 날짜 (선택사항, 기본값: 7일 전)
   * @param endDate 조회 종료 날짜 (선택사항, 기본값: 오늘)
   * @return 전체 통계 요약
   */
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<StatisticsSummaryDto>> getStatisticsSummary(
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) 
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.error("인증된 사용자 정보를 찾을 수 없습니다");
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("USER_NOT_FOUND", "인증된 사용자 정보를 찾을 수 없습니다", "다시 로그인해주세요"));
    }

    try {
      // 기본값 설정
      if (startDate == null) {
        startDate = LocalDate.now().minusDays(7);
      }
      if (endDate == null) {
        endDate = LocalDate.now();
      }

      // 각 통계 조회
      GeofenceStatsDto geofenceStats = statisticsService.getGeofenceStatistics(userId, startDate, endDate);
      List<DailyActivityStatsDto> activityStats = statisticsService.getDailyActivityStatistics(userId, startDate, endDate);
      SafetyStatsDto safetyStats = statisticsService.getSafetyStatistics(userId, startDate, endDate);

      // 요약 DTO 생성
      StatisticsSummaryDto summary = StatisticsSummaryDto.builder()
          .userId(userId)
          .startDate(startDate)
          .endDate(endDate)
          .geofenceStats(geofenceStats)
          .dailyActivityStats(activityStats)
          .safetyStats(safetyStats)
          .totalDays((int) startDate.until(endDate).getDays() + 1)
          .build();

      log.info("통계 요약 조회 완료: 사용자 {}, 기간 {} ~ {}", userId, startDate, endDate);
      
      return ResponseEntity.ok(ApiResponse.success(summary, "통계 요약을 성공적으로 조회했습니다"));
      
    } catch (Exception e) {
      log.error("통계 요약 조회 실패: 사용자 {}", userId, e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("STATS_ERROR", "통계 정보를 불러올 수 없습니다", "잠시 후 다시 시도해주세요"));
    }
  }

  /**
   * 통계 요약 DTO
   */
  public static class StatisticsSummaryDto {
    public final Long userId;
    public final LocalDate startDate;
    public final LocalDate endDate;
    public final int totalDays;
    public final GeofenceStatsDto geofenceStats;
    public final List<DailyActivityStatsDto> dailyActivityStats;
    public final SafetyStatsDto safetyStats;

    public StatisticsSummaryDto(Long userId, LocalDate startDate, LocalDate endDate, int totalDays,
                               GeofenceStatsDto geofenceStats, List<DailyActivityStatsDto> dailyActivityStats,
                               SafetyStatsDto safetyStats) {
      this.userId = userId;
      this.startDate = startDate;
      this.endDate = endDate;
      this.totalDays = totalDays;
      this.geofenceStats = geofenceStats;
      this.dailyActivityStats = dailyActivityStats;
      this.safetyStats = safetyStats;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private Long userId;
      private LocalDate startDate;
      private LocalDate endDate;
      private int totalDays;
      private GeofenceStatsDto geofenceStats;
      private List<DailyActivityStatsDto> dailyActivityStats;
      private SafetyStatsDto safetyStats;

      public Builder userId(Long userId) {
        this.userId = userId;
        return this;
      }

      public Builder startDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
      }

      public Builder endDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
      }

      public Builder totalDays(int totalDays) {
        this.totalDays = totalDays;
        return this;
      }

      public Builder geofenceStats(GeofenceStatsDto geofenceStats) {
        this.geofenceStats = geofenceStats;
        return this;
      }

      public Builder dailyActivityStats(List<DailyActivityStatsDto> dailyActivityStats) {
        this.dailyActivityStats = dailyActivityStats;
        return this;
      }

      public Builder safetyStats(SafetyStatsDto safetyStats) {
        this.safetyStats = safetyStats;
        return this;
      }

      public StatisticsSummaryDto build() {
        return new StatisticsSummaryDto(userId, startDate, endDate, totalDays,
            geofenceStats, dailyActivityStats, safetyStats);
      }
    }
  }
}