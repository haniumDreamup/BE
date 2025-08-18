package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.*;

/**
 * 보호자 설정 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianSettingsDto {
  private boolean notificationsEnabled;
  private boolean emergencyAlertsEnabled;
  private boolean dailyReportsEnabled;
  private String preferredContactMethod;
  private String reportFrequency;
}