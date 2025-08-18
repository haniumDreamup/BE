package com.bifai.reminder.bifai_backend.dto.mobile.medication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayMedicationsResponse {
  private List<MobileMedicationDto> medications;
  private int taken;
  private int remaining;
  private NextMedication nextMedication;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NextMedication {
    private String name;
    private String timeUntil;
  }
}