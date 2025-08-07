package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 위치 정보 엔티티
 * BIF 사용자의 실시간 위치 추적 및 안전 구역 관리
 */
@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_user_id_created_at", columnList = "user_id, createdAt"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_location_type", columnList = "locationType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "location_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "latitude", nullable = false)
  private Double latitude;

  @Column(name = "longitude", nullable = false)
  private Double longitude;

  @Column(name = "altitude")
  private Double altitude;

  @Column(name = "accuracy")
  private Double accuracy;

  @Column(name = "speed")
  private Double speed;

  @Column(name = "heading")
  private Double heading;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "location_type", length = 30)
  @Enumerated(EnumType.STRING)
  private LocationType locationType;

  @Column(name = "is_in_safe_zone")
  private Boolean isInSafeZone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "geofence_id")
  private Geofence currentGeofence;

  @Column(name = "device_id", length = 100)
  private String deviceId;

  @Column(name = "battery_level")
  private Integer batteryLevel;

  @Column(name = "is_charging")
  private Boolean isCharging;

  @Column(name = "network_type", length = 20)
  private String networkType;

  @Column(name = "provider", length = 50)
  private String provider;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "activity_type", length = 50)
  @Enumerated(EnumType.STRING)
  private ActivityType activityType;

  @Column(name = "activity_confidence")
  private Integer activityConfidence;

  /**
   * 위치 유형
   */
  public enum LocationType {
    REAL_TIME("실시간"),
    PERIODIC("주기적"),
    EMERGENCY("긴급"),
    GEOFENCE_ENTRY("안전구역 진입"),
    GEOFENCE_EXIT("안전구역 이탈"),
    MANUAL("수동");

    private final String description;

    LocationType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 활동 유형
   */
  public enum ActivityType {
    STILL("정지"),
    WALKING("걷기"),
    RUNNING("달리기"),
    IN_VEHICLE("차량 이동"),
    ON_BICYCLE("자전거"),
    UNKNOWN("알 수 없음");

    private final String description;

    ActivityType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (locationType == null) {
      locationType = LocationType.REAL_TIME;
    }
    if (isInSafeZone == null) {
      isInSafeZone = true;
    }
  }
}