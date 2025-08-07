package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 약 복용 알림 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationReminderRequest {
  
  @NotBlank(message = "약 이름은 필수입니다")
  private String medicationName;
  
  @NotBlank(message = "복용 시간은 필수입니다")
  private String time;
  
  @NotBlank(message = "복용량은 필수입니다")
  private String dosage;
  
  private String precautions; // 주의사항
}