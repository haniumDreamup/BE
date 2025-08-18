package com.bifai.reminder.bifai_backend.dto.guardian;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianDashboardDto {
  
  // 보호 대상자 정보
  private WardInfo wardInfo;
  
  // 오늘의 요약
  private TodaySummary todaySummary;
  
  // 최근 활동
  private List<RecentActivity> recentActivities;
  
  // 긴급 알림
  private List<Alert> alerts;
  
  // 건강 지표
  private HealthSummary healthSummary;
  
  // 위치 정보
  private LocationSummary locationSummary;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WardInfo {
    private Long id;
    private String name;
    private String profileImage;
    private String phoneNumber;
    private Integer age;
    private String status; // ONLINE, OFFLINE, EMERGENCY
    private LocalDateTime lastActiveAt;
    private Integer batteryLevel;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TodaySummary {
    private Integer medicationsTaken;
    private Integer medicationsTotal;
    private Integer schedulesCompleted;
    private Integer schedulesTotal;
    private Integer activityScore; // 0-100
    private String overallStatus; // GOOD, NORMAL, NEEDS_ATTENTION
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentActivity {
    private String type; // MEDICATION, SCHEDULE, LOCATION, EMERGENCY
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private String icon;
    private String status; // COMPLETED, MISSED, IN_PROGRESS
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Alert {
    private String level; // HIGH, MEDIUM, LOW
    private String type;
    private String message;
    private LocalDateTime timestamp;
    private Boolean isRead;
    private String actionRequired;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HealthSummary {
    private Double medicationAdherence; // 0-100%
    private Integer stepCount;
    private Integer heartRate;
    private String sleepQuality; // GOOD, FAIR, POOR
    private LocalDateTime lastHealthCheck;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocationSummary {
    private Double latitude;
    private Double longitude;
    private String address;
    private String safeZoneStatus; // INSIDE, OUTSIDE, UNKNOWN
    private LocalDateTime lastUpdated;
    private Double distanceFromHome; // in meters
  }
}