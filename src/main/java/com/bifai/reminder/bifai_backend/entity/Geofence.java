package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 안전 구역(Geofence) 엔티티
 * BIF 사용자의 안전 구역 설정 및 모니터링
 */
@Entity
@Table(name = "geofences", indexes = {
    @Index(name = "idx_geofence_user_id", columnList = "user_id"),
    @Index(name = "idx_geofence_is_active", columnList = "isActive")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "geofence_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "center_latitude", nullable = false)
  private Double centerLatitude;

  @Column(name = "center_longitude", nullable = false)
  private Double centerLongitude;

  @Column(name = "radius_meters", nullable = false)
  private Integer radiusMeters;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "geofence_type", length = 30)
  @Enumerated(EnumType.STRING)
  private GeofenceType type;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "alert_on_entry")
  private Boolean alertOnEntry;

  @Column(name = "alert_on_exit")
  private Boolean alertOnExit;

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "active_days", length = 20)
  private String activeDays;

  @Column(name = "priority")
  private Integer priority;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  /**
   * 안전 구역 유형
   */
  public enum GeofenceType {
    HOME("집"),
    WORK("직장"),
    SCHOOL("학교"),
    HOSPITAL("병원"),
    SAFE_ZONE("안전 구역"),
    DANGER_ZONE("위험 구역"),
    CUSTOM("사용자 정의");

    private final String description;

    GeofenceType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (isActive == null) {
      isActive = true;
    }
    if (alertOnEntry == null) {
      alertOnEntry = false;
    }
    if (alertOnExit == null) {
      alertOnExit = true;
    }
    if (priority == null) {
      priority = 1;
    }
    if (type == null) {
      type = GeofenceType.CUSTOM;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /**
   * 특정 시간에 활성화된 안전 구역인지 확인
   */
  public boolean isActiveAt(LocalDateTime dateTime) {
    if (!isActive) {
      return false;
    }

    // 시간 범위 확인
    if (startTime != null && endTime != null) {
      LocalTime currentTime = dateTime.toLocalTime();
      if (startTime.isBefore(endTime)) {
        if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
          return false;
        }
      } else {
        if (currentTime.isBefore(startTime) && currentTime.isAfter(endTime)) {
          return false;
        }
      }
    }

    // 요일 확인
    if (activeDays != null && !activeDays.isEmpty()) {
      String dayOfWeek = dateTime.getDayOfWeek().name().substring(0, 3).toUpperCase();
      return activeDays.contains(dayOfWeek);
    }

    return true;
  }

  /**
   * 좌표가 안전 구역 내에 있는지 확인
   */
  public boolean containsLocation(double latitude, double longitude) {
    double distance = calculateDistance(centerLatitude, centerLongitude, latitude, longitude);
    return distance <= radiusMeters;
  }

  /**
   * 두 좌표 간 거리 계산 (미터 단위)
   */
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371000; // 지구 반지름 (미터)
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}