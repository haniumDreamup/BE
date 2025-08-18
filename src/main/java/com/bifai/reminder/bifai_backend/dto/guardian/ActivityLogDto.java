package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDto {
  private Long id;
  private String activityType; // MEDICATION_TAKEN, SCHEDULE_COMPLETED, LOCATION_CHANGE, etc.
  private String title;
  private String description;
  private LocalDateTime timestamp;
  private String status; // SUCCESS, FAILED, PENDING
  private String category; // HEALTH, DAILY_ROUTINE, SAFETY, SOCIAL
  private String icon;
  private String colorCode;
  
  // 추가 데이터 (활동 유형에 따라 다름)
  private Map<String, Object> metadata;
  
  // 위치 정보 (해당하는 경우)
  private Double latitude;
  private Double longitude;
  private String address;
  
  // 관련 이미지 (있는 경우)
  private String imageUrl;
  
  // 중요도
  private String importance; // HIGH, MEDIUM, LOW
}