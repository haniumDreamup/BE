package com.bifai.reminder.bifai_backend.dto.location;

import com.bifai.reminder.bifai_backend.entity.Location;
import com.bifai.reminder.bifai_backend.entity.Location.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 위치 정보 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {

  private Long id;
  private Long userId;
  private Double latitude;
  private Double longitude;
  private Double altitude;
  private Double accuracy;
  private Double speed;
  private Double heading;
  private String address;
  private LocationType locationType;
  private String locationTypeDescription;
  private Boolean isInSafeZone;
  private Long currentGeofenceId;
  private String currentGeofenceName;
  private String deviceId;
  private Integer batteryLevel;
  private Boolean isCharging;
  private String networkType;
  private String provider;
  private LocalDateTime createdAt;
  private ActivityType activityType;
  private String activityTypeDescription;
  private Integer activityConfidence;

  /**
   * Entity를 Response DTO로 변환
   */
  public static LocationResponse from(Location location) {
    return LocationResponse.builder()
        .id(location.getId())
        .userId(location.getUser().getId())
        .latitude(location.getLatitude())
        .longitude(location.getLongitude())
        .altitude(location.getAltitude())
        .accuracy(location.getAccuracy())
        .speed(location.getSpeed())
        .heading(location.getHeading())
        .address(location.getAddress())
        .locationType(location.getLocationType())
        .locationTypeDescription(location.getLocationType() != null ? 
            location.getLocationType().getDescription() : null)
        .isInSafeZone(location.getIsInSafeZone())
        .currentGeofenceId(location.getCurrentGeofence() != null ? 
            location.getCurrentGeofence().getId() : null)
        .currentGeofenceName(location.getCurrentGeofence() != null ? 
            location.getCurrentGeofence().getName() : null)
        .deviceId(location.getDeviceId())
        .batteryLevel(location.getBatteryLevel())
        .isCharging(location.getIsCharging())
        .networkType(location.getNetworkType())
        .provider(location.getProvider())
        .createdAt(location.getCreatedAt())
        .activityType(location.getActivityType())
        .activityTypeDescription(location.getActivityType() != null ? 
            location.getActivityType().getDescription() : null)
        .activityConfidence(location.getActivityConfidence())
        .build();
  }
}