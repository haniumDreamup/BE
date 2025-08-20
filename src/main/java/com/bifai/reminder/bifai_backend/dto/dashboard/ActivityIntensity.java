package com.bifai.reminder.bifai_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일별 활동 강도 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityIntensity {
  
  private LocalDate date;
  private Long activityCount;
  private Long totalDurationMinutes;
  private Double averageIntensityScore;
  
  /**
   * 활동 수준 판단
   */
  public String getActivityLevel() {
    if (totalDurationMinutes == null || totalDurationMinutes < 30) {
      return "매우 낮음";
    } else if (totalDurationMinutes < 60) {
      return "낮음";
    } else if (totalDurationMinutes < 120) {
      return "보통";
    } else if (totalDurationMinutes < 180) {
      return "높음";
    } else {
      return "매우 높음";
    }
  }
  
  /**
   * 강도 수준 텍스트
   */
  public String getIntensityLevel() {
    if (averageIntensityScore == null) {
      return "측정 안 됨";
    }
    
    if (averageIntensityScore < 2) {
      return "가벼운 활동";
    } else if (averageIntensityScore < 4) {
      return "중간 활동";
    } else if (averageIntensityScore < 6) {
      return "활발한 활동";
    } else {
      return "매우 활발한 활동";
    }
  }
  
  /**
   * 시간 포맷팅 (시간:분)
   */
  public String getFormattedDuration() {
    if (totalDurationMinutes == null || totalDurationMinutes == 0) {
      return "0분";
    }
    
    long hours = totalDurationMinutes / 60;
    long minutes = totalDurationMinutes % 60;
    
    if (hours > 0) {
      return hours + "시간 " + minutes + "분";
    } else {
      return minutes + "분";
    }
  }
  
  /**
   * 목표 달성률 (하루 120분 기준)
   */
  public double getGoalAchievementRate() {
    final long dailyGoal = 120; // 2시간
    if (totalDurationMinutes == null) {
      return 0.0;
    }
    return Math.min(100.0, (totalDurationMinutes * 100.0) / dailyGoal);
  }
}