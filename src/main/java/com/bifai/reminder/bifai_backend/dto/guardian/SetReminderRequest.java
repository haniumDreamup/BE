package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 리마인더 설정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetReminderRequest {
  private String title;
  private String description;
  private LocalDateTime scheduledTime;
  private String scheduleType;
  private String recurrenceType;
}