package com.bifai.reminder.bifai_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 주간 요약 리포트 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySummaryDto {
  
  // 기본 정보
  private Long userId;
  private String userName;
  private LocalDate weekStartDate;
  private LocalDate weekEndDate;
  
  // 복약 요약
  private MedicationWeeklySummary medicationSummary;
  
  // 활동 요약
  private ActivityWeeklySummary activitySummary;
  
  // 위치 요약
  private LocationWeeklySummary locationSummary;
  
  // 일정 요약
  private ScheduleWeeklySummary scheduleSummary;
  
  // 주간 트렌드
  private String weeklyTrend; // IMPROVING, STABLE, DECLINING
  private List<String> concerns; // 주의사항 목록
  private List<String> achievements; // 긍정적인 성과
  
  /**
   * 복약 주간 요약
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MedicationWeeklySummary {
    private double overallCompletionRate;     // 전체 복약률
    private Map<LocalDate, Double> dailyRates; // 일별 복약률
    private int totalMedications;              // 총 복약 횟수
    private int missedMedications;             // 놓친 복약 횟수
    private List<String> frequentlyMissed;    // 자주 놓치는 약 목록
    private String bestDay;                   // 가장 복약을 잘한 날
    private String worstDay;                  // 가장 복약을 못한 날
  }
  
  /**
   * 활동 주간 요약
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActivityWeeklySummary {
    private int totalActiveMinutes;            // 주간 총 활동 시간(분)
    private double dailyAverageMinutes;        // 일평균 활동 시간
    private Map<LocalDate, Integer> dailyActivity; // 일별 활동 시간
    private String mostActiveDay;              // 가장 활동적인 날
    private String leastActiveDay;             // 가장 비활동적인 날
    private int inactiveDays;                  // 비활동 일수
    private List<String> activityPatterns;    // 활동 패턴 (아침형/저녁형 등)
  }
  
  /**
   * 위치 주간 요약
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocationWeeklySummary {
    private Map<String, Integer> frequentLocations; // 자주 방문한 장소와 횟수
    private int safeZoneExits;                     // 안전구역 이탈 횟수
    private String mostVisitedPlace;               // 가장 많이 방문한 장소
    private int uniquePlacesVisited;               // 방문한 고유 장소 수
    private List<String> unusualLocations;         // 평소와 다른 장소 방문
  }
  
  /**
   * 일정 주간 요약
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScheduleWeeklySummary {
    private int totalSchedules;                // 총 일정 수
    private int completedSchedules;            // 완료된 일정 수
    private double completionRate;             // 일정 완료율
    private Map<LocalDate, Integer> dailySchedules; // 일별 일정 수
    private List<String> missedImportantSchedules;  // 놓친 중요 일정
  }
}