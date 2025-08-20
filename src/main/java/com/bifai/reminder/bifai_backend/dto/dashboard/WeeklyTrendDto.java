package com.bifai.reminder.bifai_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주간 트렌드 분석 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyTrendDto {
  
  // 복약 통계
  private List<MedicationWeeklyStats> medicationStats;
  
  // 활동 강도
  private List<ActivityIntensity> activityIntensity;
  
  // 트렌드 방향
  private String trendDirection; // IMPROVING, STABLE, DECLINING
  
  // 주요 지표
  private Double averageMedicationRate;
  private Double averageActivityMinutes;
  private Integer missedCriticalMedications;
  private Integer inactiveDays;
  
  /**
   * 트렌드 설명 생성
   */
  public String getTrendDescription() {
    switch (trendDirection) {
      case "IMPROVING":
        return "지난 주보다 좋아지고 있습니다";
      case "STABLE":
        return "안정적으로 유지되고 있습니다";
      case "DECLINING":
        return "관심이 필요한 상황입니다";
      default:
        return "트렌드를 분석 중입니다";
    }
  }
  
  /**
   * 개선 제안 생성
   */
  public List<String> getImprovementSuggestions() {
    List<String> suggestions = new java.util.ArrayList<>();
    
    if (averageMedicationRate != null && averageMedicationRate < 80) {
      suggestions.add("복약 알림을 더 자주 설정해보세요");
    }
    
    if (averageActivityMinutes != null && averageActivityMinutes < 60) {
      suggestions.add("하루 1시간 이상 활동을 목표로 해보세요");
    }
    
    if (missedCriticalMedications != null && missedCriticalMedications > 0) {
      suggestions.add("중요한 약물은 식사 시간과 함께 복용하세요");
    }
    
    if (inactiveDays != null && inactiveDays > 2) {
      suggestions.add("매일 짧은 산책이라도 해보세요");
    }
    
    return suggestions;
  }
  
  /**
   * 주간 점수 계산 (0-100)
   */
  public int getWeeklyScore() {
    int score = 100;
    
    if (averageMedicationRate != null) {
      score = (int) (averageMedicationRate * 0.4); // 40% 가중치
    }
    
    if (averageActivityMinutes != null) {
      double activityScore = Math.min(100, (averageActivityMinutes / 120.0) * 100);
      score += (int) (activityScore * 0.3); // 30% 가중치
    }
    
    if (missedCriticalMedications != null && missedCriticalMedications > 0) {
      score -= missedCriticalMedications * 5; // 중요 약물 놓칠 때마다 -5점
    }
    
    if (inactiveDays != null && inactiveDays > 0) {
      score -= inactiveDays * 3; // 비활동 날마다 -3점
    }
    
    return Math.max(0, Math.min(100, score));
  }
}