package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 일정 알림 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleReminderRequest {
  
  @NotBlank(message = "일정 이름은 필수입니다")
  private String scheduleName;
  
  @NotBlank(message = "시간은 필수입니다")
  private String time;
  
  private String location;
  
  private String items; // 준비물
}