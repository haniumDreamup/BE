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
public class LocationInfoDto {
  private Double latitude;
  private Double longitude;
  private String address;
  private String placeName;
  private LocalDateTime timestamp;
  private Double accuracy; // in meters
  private String movementStatus; // STATIONARY, WALKING, DRIVING
  private Double speed; // km/h
  
  // 안전 구역 정보
  private SafeZoneInfo safeZone;
  
  // 최근 방문 장소
  private List<RecentPlace> recentPlaces;
  
  // 이동 경로 (최근 몇 시간)
  private List<LocationPoint> trajectory;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SafeZoneInfo {
    private String status; // INSIDE, OUTSIDE, NEAR_BOUNDARY
    private String zoneName;
    private Double distanceFromCenter; // meters
    private Double radius; // meters
    private Boolean alertEnabled;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentPlace {
    private String name;
    private String address;
    private LocalDateTime arrivedAt;
    private LocalDateTime leftAt;
    private Integer durationMinutes;
    private String placeType; // HOME, HOSPITAL, PHARMACY, etc.
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocationPoint {
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
  }
}