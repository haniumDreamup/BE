package com.bifai.reminder.bifai_backend.dto.dashboard;

import com.bifai.reminder.bifai_backend.service.OptimizedDashboardService.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 통합 대시보드 DTO
 * 모든 대시보드 정보를 한 번에 전달
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprehensiveDashboardDto {
  
  private Long userId;
  private LocalDate date;
  
  // 복약 정보
  private MedicationSummary medicationSummary;
  
  // 활동 패턴 (시간별 활동 수)
  private Map<Integer, Integer> activityPattern;
  
  // 위치 정보
  private LocationSummary locationSummary;
  
  // 일정 정보
  private ScheduleSummary scheduleSummary;
  
  // 전반적인 상태
  private String overallStatus; // GOOD, WARNING, CRITICAL
  
  /**
   * 상태 메시지 생성
   */
  public String getStatusMessage() {
    switch (overallStatus) {
      case "GOOD":
        return "오늘 하루 잘 보내고 계십니다";
      case "WARNING":
        return "확인이 필요한 사항이 있습니다";
      case "CRITICAL":
        return "즉시 확인이 필요합니다";
      default:
        return "상태를 확인 중입니다";
    }
  }
  
  /**
   * 복약률 계산
   */
  public double getMedicationCompletionRate() {
    if (medicationSummary == null || medicationSummary.getTotalMedications() == 0) {
      return 100.0;
    }
    
    // 실제 복약 여부 계산 로직 필요
    return 0.0;
  }
  
  /**
   * 활동 수준 판단
   */
  public String getActivityLevel() {
    if (activityPattern == null || activityPattern.isEmpty()) {
      return "데이터 없음";
    }
    
    int totalActivity = activityPattern.values().stream()
      .mapToInt(Integer::intValue)
      .sum();
    
    if (totalActivity < 10) {
      return "매우 낮음";
    } else if (totalActivity < 30) {
      return "낮음";
    } else if (totalActivity < 60) {
      return "보통";
    } else if (totalActivity < 100) {
      return "높음";
    } else {
      return "매우 높음";
    }
  }
  
  /**
   * 일정 완료율
   */
  public double getScheduleCompletionRate() {
    if (scheduleSummary == null || scheduleSummary.getTotalSchedules() == 0) {
      return 100.0;
    }
    
    return (scheduleSummary.getCompletedSchedules() * 100.0) / scheduleSummary.getTotalSchedules();
  }
}