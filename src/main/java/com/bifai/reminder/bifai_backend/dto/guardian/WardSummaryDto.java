package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardSummaryDto {
  private Long id;
  private String name;
  private String profileImage;
  private String phoneNumber;
  private Integer age;
  private String relationship; // 부모, 자녀, 배우자 등
  private String status; // ONLINE, OFFLINE, EMERGENCY
  private LocalDateTime lastActiveAt;
  private Integer batteryLevel;
  private Boolean hasUnreadAlerts;
  private Integer todayMedicationProgress; // 0-100%
  private Integer todayScheduleProgress; // 0-100%
  
  // 간단한 상태 메시지
  private String statusMessage;
  
  // 최근 위치
  private String lastKnownLocation;
  
  // 긴급 연락 가능 여부
  private Boolean emergencyContactAvailable;
}