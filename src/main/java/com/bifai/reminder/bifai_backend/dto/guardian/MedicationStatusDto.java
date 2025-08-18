package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationStatusDto {
  private LocalDate date;
  private Integer totalMedications;
  private Integer takenMedications;
  private Integer missedMedications;
  private Integer pendingMedications;
  private Double adherenceRate; // 0-100%
  
  private List<MedicationDetail> medications;
  
  // 주간 복용률 추이
  private List<DailyAdherence> weeklyTrend;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MedicationDetail {
    private Long id;
    private String name;
    private String dosage;
    private LocalTime scheduledTime;
    private LocalDateTime takenAt;
    private String status; // TAKEN, MISSED, PENDING, LATE
    private String notes;
    private String imageUrl;
    private Boolean isImportant;
    private Integer delayMinutes; // 늦은 경우
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyAdherence {
    private LocalDate date;
    private Integer totalCount;
    private Integer takenCount;
    private Double adherenceRate;
  }
}