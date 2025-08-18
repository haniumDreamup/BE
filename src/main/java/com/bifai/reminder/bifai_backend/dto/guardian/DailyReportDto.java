package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;
import java.time.LocalDate;

/**
 * 일일 보고서 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReportDto {
  private LocalDate date;
  private int completedTasks;
  private int totalTasks;
  private double medicationAdherence;
  private int stepCount;
  private String summary;
}