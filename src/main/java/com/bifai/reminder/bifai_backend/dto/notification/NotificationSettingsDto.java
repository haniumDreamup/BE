package com.bifai.reminder.bifai_backend.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsDto {
  
  private boolean medicationReminders;
  private boolean scheduleReminders;
  private boolean emergencyAlerts;
  private boolean dailySummary;
  private boolean soundEnabled;
  private boolean vibrationEnabled;
  
  // 알림 시간 설정
  private String medicationReminderTime; // "HH:mm" format
  private String dailySummaryTime; // "HH:mm" format
  
  // 알림 빈도 설정
  private int medicationReminderMinutesBefore; // 약 복용 몇 분 전 알림
  private int scheduleReminderMinutesBefore; // 일정 몇 분 전 알림
  
  public static NotificationSettingsDto getDefault() {
    return NotificationSettingsDto.builder()
        .medicationReminders(true)
        .scheduleReminders(true)
        .emergencyAlerts(true)
        .dailySummary(true)
        .soundEnabled(true)
        .vibrationEnabled(true)
        .medicationReminderTime("09:00")
        .dailySummaryTime("21:00")
        .medicationReminderMinutesBefore(10)
        .scheduleReminderMinutesBefore(30)
        .build();
  }
}