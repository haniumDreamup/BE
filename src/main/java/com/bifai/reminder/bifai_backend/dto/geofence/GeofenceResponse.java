package com.bifai.reminder.bifai_backend.dto.geofence;

import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 지오펜스 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceResponse {

  private Long id;
  
  private String name;
  
  private String description;
  
  private Double centerLatitude;
  
  private Double centerLongitude;
  
  private Integer radiusMeters;
  
  private String address;
  
  private Geofence.GeofenceType type;
  
  private Boolean isActive;
  
  private Boolean alertOnEntry;
  
  private Boolean alertOnExit;
  
  private String startTime;
  
  private String endTime;
  
  private String activeDays;
  
  private Integer priority;
  
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;
  
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;
}