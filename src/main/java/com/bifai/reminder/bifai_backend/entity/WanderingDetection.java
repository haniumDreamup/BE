package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 배회 감지 엔티티
 * 사용자의 비정상적인 이동 패턴 감지 및 관리
 */
@Entity
@Table(name = "wandering_detections", indexes = {
    @Index(name = "idx_wandering_user", columnList = "user_id"),
    @Index(name = "idx_wandering_detected", columnList = "detected_at"),
    @Index(name = "idx_wandering_status", columnList = "status"),
    @Index(name = "idx_wandering_risk", columnList = "risk_level")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WanderingDetection {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "detection_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "detected_at", nullable = false)
  private LocalDateTime detectedAt;

  @Column(name = "status", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private WanderingStatus status;

  @Column(name = "risk_level", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private RiskLevel riskLevel;

  @Column(name = "detection_type", length = 50)
  private String detectionType; // 원형패턴, 반복이동, 목적지이탈 등

  @Column(name = "confidence_score")
  private Float confidenceScore; // 감지 확신도 (0.0 ~ 1.0)

  @Column(name = "duration_minutes")
  private Integer durationMinutes; // 배회 지속 시간

  @Column(name = "distance_traveled")
  private Float distanceTraveled; // 이동 거리 (km)

  @Column(name = "start_latitude")
  private Double startLatitude;

  @Column(name = "start_longitude")
  private Double startLongitude;

  @Column(name = "start_address", length = 500)
  private String startAddress;

  @Column(name = "current_latitude")
  private Double currentLatitude;

  @Column(name = "current_longitude")
  private Double currentLongitude;

  @Column(name = "current_address", length = 500)
  private String currentAddress;

  @Column(name = "home_distance")
  private Float homeDistance; // 집으로부터의 거리 (km)

  @Column(name = "safe_zone_violated")
  private Boolean safeZoneViolated;

  @Column(name = "pattern_description", columnDefinition = "TEXT")
  private String patternDescription; // 패턴 설명

  @Column(name = "movement_data", columnDefinition = "JSON")
  private String movementData; // 이동 경로 데이터

  @Column(name = "analysis_result", columnDefinition = "JSON")
  private String analysisResult; // ML 분석 결과

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "resolution_method", length = 100)
  private String resolutionMethod; // 귀가, 보호자픽업, 자체해결 등

  @Column(name = "guardian_notified")
  private Boolean guardianNotified;

  @Column(name = "emergency_triggered")
  private Boolean emergencyTriggered;

  @Column(name = "navigation_provided")
  private Boolean navigationProvided;

  @Column(name = "user_response", length = 50)
  private String userResponse; // 괜찮음, 도움필요, 무응답 등

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "weather_condition", length = 50)
  private String weatherCondition; // 날씨 상태

  @Column(name = "time_of_day", length = 20)
  private String timeOfDay; // 아침, 낮, 저녁, 밤

  @Column(name = "recurring_pattern")
  private Boolean recurringPattern; // 반복 패턴 여부

  @Column(name = "previous_occurrence_id")
  private Long previousOccurrenceId; // 이전 발생 ID

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /**
   * 배회 상태
   */
  public enum WanderingStatus {
    DETECTED("감지됨"),
    MONITORING("모니터링중"),
    CONFIRMED("확인됨"),
    INTERVENTION_NEEDED("개입필요"),
    NAVIGATING("안내중"),
    RESOLVED("해결됨"),
    FALSE_POSITIVE("오탐지");

    private final String description;

    WanderingStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 위험 수준
   */
  public enum RiskLevel {
    CRITICAL("매우위험"),
    HIGH("위험"),
    MEDIUM("주의"),
    LOW("경미");

    private final String description;

    RiskLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (status == null) {
      status = WanderingStatus.DETECTED;
    }
    if (detectedAt == null) {
      detectedAt = LocalDateTime.now();
    }
    if (safeZoneViolated == null) {
      safeZoneViolated = false;
    }
    if (guardianNotified == null) {
      guardianNotified = false;
    }
    if (emergencyTriggered == null) {
      emergencyTriggered = false;
    }
    if (navigationProvided == null) {
      navigationProvided = false;
    }
    if (recurringPattern == null) {
      recurringPattern = false;
    }
  }

  /**
   * 모니터링 시작
   */
  public void startMonitoring() {
    this.status = WanderingStatus.MONITORING;
  }

  /**
   * 배회 확인
   */
  public void confirm() {
    this.status = WanderingStatus.CONFIRMED;
  }

  /**
   * 개입 필요 표시
   */
  public void markInterventionNeeded() {
    this.status = WanderingStatus.INTERVENTION_NEEDED;
  }

  /**
   * 내비게이션 시작
   */
  public void startNavigation() {
    this.status = WanderingStatus.NAVIGATING;
    this.navigationProvided = true;
  }

  /**
   * 해결됨
   */
  public void resolve(String method) {
    this.status = WanderingStatus.RESOLVED;
    this.resolvedAt = LocalDateTime.now();
    this.resolutionMethod = method;
  }

  /**
   * 오탐지 처리
   */
  public void markAsFalsePositive() {
    this.status = WanderingStatus.FALSE_POSITIVE;
    this.resolvedAt = LocalDateTime.now();
  }

  /**
   * 보호자 알림
   */
  public void notifyGuardian() {
    this.guardianNotified = true;
  }

  /**
   * 긴급상황 발동
   */
  public void triggerEmergency() {
    this.emergencyTriggered = true;
  }

  /**
   * 위치 업데이트
   */
  public void updateCurrentLocation(Double latitude, Double longitude, String address) {
    this.currentLatitude = latitude;
    this.currentLongitude = longitude;
    this.currentAddress = address;
  }

  /**
   * 활성 상태 확인
   */
  public boolean isActive() {
    return status != WanderingStatus.RESOLVED && 
           status != WanderingStatus.FALSE_POSITIVE;
  }

  /**
   * 긴급 여부 확인
   */
  public boolean isCritical() {
    return riskLevel == RiskLevel.CRITICAL || 
           Boolean.TRUE.equals(emergencyTriggered);
  }
}