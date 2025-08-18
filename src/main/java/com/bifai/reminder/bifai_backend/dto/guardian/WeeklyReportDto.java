package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 주간 보고서 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReportDto {
  private LocalDate startDate;
  private LocalDate endDate;
  private double averageMedicationAdherence;
  private int totalActivities;
  private List<DailyReportDto> dailyReports;
  private String weekSummary;
}