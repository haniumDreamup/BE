package com.bifai.reminder.bifai_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 오늘의 상태 요약 DTO
 * 보호자가 한눈에 확인할 수 있는 간단한 요약 정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStatusSummaryDto {
  
  // 사용자 정보
  private Long userId;
  private String userName;
  private LocalDateTime summaryDate;
  
  // 복약 상태
  private MedicationStatus medicationStatus;
  
  // 위치 정보
  private LocationStatus locationStatus;
  
  // 활동 정보
  private ActivityStatus activityStatus;
  
  // 일정 정보
  private ScheduleStatus scheduleStatus;
  
  // 전체 상태
  private String overallStatus; // GOOD, ATTENTION_NEEDED, WARNING
  private String statusMessage; // 간단한 상태 메시지
  
  /**
   * 복약 상태
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MedicationStatus {
    private int totalMedications;      // 오늘 복용해야 할 총 약 개수
    private int takenMedications;      // 복용 완료한 약 개수
    private int missedMedications;     // 놓친 약 개수
    private double completionRate;     // 복약 완료율 (%)
    private String nextMedicationTime; // 다음 복약 시간
    private String nextMedicationName; // 다음 복약 이름
  }
  
  /**
   * 위치 상태
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocationStatus {
    private String currentLocation;     // 현재 위치 (주소 또는 장소명)
    private LocalDateTime lastUpdated;  // 마지막 업데이트 시간
    private boolean isInSafeZone;       // 안전 구역 내 위치 여부
    private int minutesSinceUpdate;     // 마지막 업데이트 이후 경과 시간(분)
  }
  
  /**
   * 활동 상태
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActivityStatus {
    private LocalDateTime lastActiveTime;  // 마지막 활동 시간
    private int totalActiveMinutes;        // 오늘 총 활동 시간(분)
    private int screenTimeMinutes;         // 앱 사용 시간(분)
    private String activityLevel;          // HIGH, NORMAL, LOW
    private boolean isCurrentlyActive;     // 현재 활동 중 여부
  }
  
  /**
   * 일정 상태
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScheduleStatus {
    private int totalSchedules;      // 오늘 총 일정 수
    private int completedSchedules;  // 완료된 일정 수
    private int upcomingSchedules;   // 남은 일정 수
    private String nextScheduleTime; // 다음 일정 시간
    private String nextScheduleTitle;// 다음 일정 제목
  }
}