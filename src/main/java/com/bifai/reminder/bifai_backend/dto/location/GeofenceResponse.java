package com.bifai.reminder.bifai_backend.dto.location;

import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.Geofence.GeofenceType;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 안전 구역 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceResponse {

  private Long id;
  private Long userId;
  private String name;
  private String description;
  private Double centerLatitude;
  private Double centerLongitude;
  private Integer radiusMeters;
  private String address;
  private GeofenceType type;
  private String typeDescription;
  private Boolean isActive;
  private Boolean alertOnEntry;
  private Boolean alertOnExit;
  private LocalTime startTime;
  private LocalTime endTime;
  private String activeDays;
  private Integer priority;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdById;
  private String createdByName;

  /**
   * Entity를 Response DTO로 변환
   */
  public static GeofenceResponse from(Geofence geofence) {
    return GeofenceResponse.builder()
        .id(geofence.getId())
        .userId(geofence.getUser().getId())
        .name(geofence.getName())
        .description(geofence.getDescription())
        .centerLatitude(geofence.getCenterLatitude())
        .centerLongitude(geofence.getCenterLongitude())
        .radiusMeters(geofence.getRadiusMeters())
        .address(geofence.getAddress())
        .type(geofence.getType())
        .typeDescription(geofence.getType() != null ? geofence.getType().getDescription() : null)
        .isActive(geofence.getIsActive())
        .alertOnEntry(geofence.getAlertOnEntry())
        .alertOnExit(geofence.getAlertOnExit())
        .startTime(geofence.getStartTime())
        .endTime(geofence.getEndTime())
        .activeDays(geofence.getActiveDays())
        .priority(geofence.getPriority())
        .createdAt(geofence.getCreatedAt())
        .updatedAt(geofence.getUpdatedAt())
        .createdById(geofence.getCreatedBy() != null ? geofence.getCreatedBy().getId() : null)
        .createdByName(geofence.getCreatedBy() != null ? geofence.getCreatedBy().getName() : null)
        .build();
  }
}