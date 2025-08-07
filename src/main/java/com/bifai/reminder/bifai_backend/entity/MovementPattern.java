package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 이동 패턴 엔티티
 * 사용자의 일상적인 이동 패턴을 학습하고 저장
 */
@Entity
@Table(name = "movement_patterns", indexes = {
    @Index(name = "idx_pattern_user", columnList = "user_id"),
    @Index(name = "idx_pattern_type", columnList = "pattern_type"),
    @Index(name = "idx_pattern_day", columnList = "day_of_week"),
    @Index(name = "idx_pattern_confidence", columnList = "confidence_score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovementPattern {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "pattern_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "pattern_name", length = 100)
  private String patternName; // 예: "아침 산책", "병원 방문", "식료품 쇼핑"

  @Column(name = "pattern_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private PatternType patternType;

  @Column(name = "day_of_week", length = 20)
  private String dayOfWeek; // MONDAY, TUESDAY, ... 또는 WEEKDAY, WEEKEND

  @Column(name = "time_of_day", length = 20)
  private String timeOfDay; // MORNING, AFTERNOON, EVENING, NIGHT

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "frequency", length = 50)
  private String frequency; // DAILY, WEEKLY, MONTHLY 등

  @Column(name = "start_latitude")
  private Double startLatitude;

  @Column(name = "start_longitude")
  private Double startLongitude;

  @Column(name = "start_address", length = 500)
  private String startAddress;

  @Column(name = "end_latitude")
  private Double endLatitude;

  @Column(name = "end_longitude")
  private Double endLongitude;

  @Column(name = "end_address", length = 500)
  private String endAddress;

  @Column(name = "waypoints", columnDefinition = "JSON")
  private String waypoints; // 경유지 정보

  @Column(name = "typical_route", columnDefinition = "JSON")
  private String typicalRoute; // 일반적인 경로

  @Column(name = "average_speed")
  private Float averageSpeed; // km/h

  @Column(name = "total_distance")
  private Float totalDistance; // km

  @Column(name = "occurrence_count")
  private Integer occurrenceCount; // 패턴 발생 횟수

  @Column(name = "confidence_score")
  private Float confidenceScore; // 패턴 확신도 (0.0 ~ 1.0)

  @Column(name = "last_occurred")
  private LocalDateTime lastOccurred;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "is_safe")
  private Boolean isSafe; // 안전한 패턴인지 여부

  @Column(name = "purpose", length = 100)
  private String purpose; // 이동 목적

  @Column(name = "transportation_mode", length = 50)
  private String transportationMode; // 도보, 대중교통, 차량 등

  @Column(name = "companion_type", length = 50)
  private String companionType; // 혼자, 보호자동행, 친구동행 등

  @Column(name = "deviation_threshold")
  private Float deviationThreshold; // 허용 편차 임계값 (km)

  @Column(name = "alert_on_deviation")
  private Boolean alertOnDeviation; // 편차 시 경고 여부

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "metadata", columnDefinition = "JSON")
  private String metadata; // 추가 메타데이터

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /**
   * 패턴 타입
   */
  public enum PatternType {
    ROUTINE("일상적"),
    REGULAR("규칙적"),
    OCCASIONAL("간헐적"),
    RARE("드문"),
    ONE_TIME("일회성"),
    LEARNED("학습됨"),
    PREDICTED("예측됨");

    private final String description;

    PatternType(String description) {
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
    if (isSafe == null) {
      isSafe = true;
    }
    if (alertOnDeviation == null) {
      alertOnDeviation = false;
    }
    if (occurrenceCount == null) {
      occurrenceCount = 0;
    }
    if (confidenceScore == null) {
      confidenceScore = 0.0f;
    }
  }

  /**
   * 패턴 발생 기록
   */
  public void recordOccurrence() {
    this.occurrenceCount = (this.occurrenceCount == null ? 0 : this.occurrenceCount) + 1;
    this.lastOccurred = LocalDateTime.now();
    updateConfidence();
  }

  /**
   * 확신도 업데이트
   */
  private void updateConfidence() {
    // 발생 횟수에 따른 확신도 계산
    if (occurrenceCount >= 10) {
      confidenceScore = 0.9f;
    } else if (occurrenceCount >= 5) {
      confidenceScore = 0.7f;
    } else if (occurrenceCount >= 3) {
      confidenceScore = 0.5f;
    } else {
      confidenceScore = 0.3f;
    }
  }

  /**
   * 편차 거리 계산
   */
  public double calculateDeviation(Double currentLat, Double currentLon) {
    // Haversine formula를 사용한 거리 계산
    if (startLatitude == null || startLongitude == null) {
      return -1;
    }
    
    final double R = 6371; // 지구 반경 (km)
    double latDistance = Math.toRadians(currentLat - startLatitude);
    double lonDistance = Math.toRadians(currentLon - startLongitude);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(startLatitude)) * Math.cos(Math.toRadians(currentLat))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /**
   * 편차 경고 필요 여부
   */
  public boolean needsDeviationAlert(Double currentLat, Double currentLon) {
    if (!Boolean.TRUE.equals(alertOnDeviation)) {
      return false;
    }
    
    double deviation = calculateDeviation(currentLat, currentLon);
    return deviation > 0 && deviationThreshold != null && deviation > deviationThreshold;
  }

  /**
   * 활성 패턴인지 확인
   */
  public boolean isActivePattern() {
    return Boolean.TRUE.equals(isActive) && confidenceScore >= 0.5f;
  }
}