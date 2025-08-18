package com.bifai.reminder.bifai_backend.dto.mobile.medication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMedicationDto {
  private Long id;
  private String name;
  private String simpleDescription;
  private String time;
  private boolean taken;
  private String dosage;
  private String color;
  private String icon;
  private String image;
  private boolean important;
  private LocalDateTime takenAt;
  private String note;
}