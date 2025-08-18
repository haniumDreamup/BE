package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;

/**
 * 건강 지표 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthMetricsDto {
  private int periodDays;
  private double medicationAdherence;
  private int averageStepCount;
  private int averageHeartRate;
  private double sleepQualityScore;
  private String activityLevel;
}