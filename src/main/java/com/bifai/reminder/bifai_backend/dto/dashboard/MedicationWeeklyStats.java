package com.bifai.reminder.bifai_backend.dto.dashboard;

import com.bifai.reminder.bifai_backend.entity.Medication;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주간 복약 통계 DTO
 * JPQL 생성자 표현식에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationWeeklyStats {
  
  private Long medicationId;
  private String medicationName;
  private Medication.PriorityLevel priorityLevel;
  private Long totalCount;
  private Long takenCount;
  private Double averageDelayMinutes;
  
  /**
   * 복약률 계산
   */
  public double getAdherenceRate() {
    if (totalCount == null || totalCount == 0) {
      return 0.0;
    }
    return (takenCount != null ? takenCount : 0) * 100.0 / totalCount;
  }
  
  /**
   * 지연 시간 포맷팅
   */
  public String getFormattedDelay() {
    if (averageDelayMinutes == null) {
      return "정시";
    }
    
    int minutes = averageDelayMinutes.intValue();
    if (minutes < 0) {
      return Math.abs(minutes) + "분 일찍";
    } else if (minutes > 0) {
      return minutes + "분 늦게";
    } else {
      return "정시";
    }
  }
  
  /**
   * 우선순위 한글 표시
   */
  public String getPriorityText() {
    if (priorityLevel == null) {
      return "보통";
    }
    
    switch (priorityLevel) {
      case CRITICAL:
        return "매우 중요";
      case HIGH:
        return "중요";
      case MEDIUM:
        return "보통";
      case LOW:
        return "낮음";
      default:
        return "보통";
    }
  }
}