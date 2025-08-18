package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;
import java.time.LocalDateTime;

/**
 * 최근 활동 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentActivityDto {
  private Long id;
  private String activityType;
  private String description;
  private LocalDateTime activityDate;
  private String status;
  private String importance;
}