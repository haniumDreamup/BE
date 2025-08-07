package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 안전 경로 엔티티
 * 사용자를 위한 안전한 귀가 경로 관리
 */
@Entity
@Table(name = "safe_routes", indexes = {
    @Index(name = "idx_route_user", columnList = "user_id"),
    @Index(name = "idx_route_active", columnList = "is_active"),
    @Index(name = "idx_route_type", columnList = "route_type"),
    @Index(name = "idx_route_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafeRoute {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "route_name", length = 100)
  private String routeName; // 예: "집으로 가는 길", "병원 가는 길"

  @Column(name = "route_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private RouteType routeType;

  @Column(name = "start_latitude", nullable = false)
  private Double startLatitude;

  @Column(name = "start_longitude", nullable = false)
  private Double startLongitude;

  @Column(name = "start_address", length = 500)
  private String startAddress;

  @Column(name = "start_landmark", length = 200)
  private String startLandmark; // 시작 랜드마크

  @Column(name = "end_latitude", nullable = false)
  private Double endLatitude;

  @Column(name = "end_longitude", nullable = false)
  private Double endLongitude;

  @Column(name = "end_address", length = 500)
  private String endAddress;

  @Column(name = "end_landmark", length = 200)
  private String endLandmark; // 도착 랜드마크

  @Column(name = "route_data", columnDefinition = "JSON")
  private String routeData; // 전체 경로 데이터

  @Column(name = "waypoints", columnDefinition = "JSON")
  private String waypoints; // 경유지 목록

  @Column(name = "landmarks", columnDefinition = "JSON")
  private String landmarks; // 주요 랜드마크 목록

  @Column(name = "total_distance")
  private Float totalDistance; // 총 거리 (km)

  @Column(name = "estimated_time")
  private Integer estimatedTime; // 예상 소요 시간 (분)

  @Column(name = "difficulty_level", length = 20)
  @Enumerated(EnumType.STRING)
  private DifficultyLevel difficultyLevel;

  @Column(name = "safety_score")
  private Float safetyScore; // 안전도 점수 (0.0 ~ 1.0)

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "is_primary")
  private Boolean isPrimary; // 주 경로 여부

  @Column(name = "usage_count")
  private Integer usageCount; // 사용 횟수

  @Column(name = "last_used")
  private LocalDateTime lastUsed;

  @Column(name = "has_crosswalks")
  private Boolean hasCrosswalks; // 횡단보도 포함 여부

  @Column(name = "has_traffic_lights")
  private Boolean hasTrafficLights; // 신호등 포함 여부

  @Column(name = "well_lit")
  private Boolean wellLit; // 조명 양호 여부

  @Column(name = "has_sidewalk")
  private Boolean hasSidewalk; // 보도 여부

  @Column(name = "avoid_areas", columnDefinition = "JSON")
  private String avoidAreas; // 회피 지역

  @Column(name = "rest_points", columnDefinition = "JSON")
  private String restPoints; // 휴식 가능 지점

  @Column(name = "emergency_contacts", columnDefinition = "JSON")
  private String emergencyContacts; // 경로상 긴급 연락처

  @Column(name = "voice_guidance", columnDefinition = "TEXT")
  private String voiceGuidance; // 음성 안내 스크립트

  @Column(name = "simple_directions", columnDefinition = "TEXT")
  private String simpleDirections; // 간단한 길 안내

  @Column(name = "visual_cues", columnDefinition = "JSON")
  private String visualCues; // 시각적 단서

  @Column(name = "transportation_mode", length = 50)
  private String transportationMode; // 도보, 대중교통 등

  @Column(name = "weather_suitable", length = 100)
  private String weatherSuitable; // 적합한 날씨 조건

  @Column(name = "time_restrictions", length = 200)
  private String timeRestrictions; // 시간 제약

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_by", length = 100)
  private String createdBy; // 생성자 (사용자/보호자/시스템)

  @Column(name = "validated")
  private Boolean validated; // 검증 완료 여부

  @Column(name = "validated_at")
  private LocalDateTime validatedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /**
   * 경로 타입
   */
  public enum RouteType {
    HOME("집"),
    HOSPITAL("병원"),
    PHARMACY("약국"),
    MARKET("시장/마트"),
    PARK("공원"),
    COMMUNITY_CENTER("복지관"),
    FRIEND("친구집"),
    CUSTOM("사용자정의");

    private final String description;

    RouteType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 난이도
   */
  public enum DifficultyLevel {
    VERY_EASY("매우쉬움"),
    EASY("쉬움"),
    MODERATE("보통"),
    HARD("어려움"),
    VERY_HARD("매우어려움");

    private final String description;

    DifficultyLevel(String description) {
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
    if (isPrimary == null) {
      isPrimary = false;
    }
    if (usageCount == null) {
      usageCount = 0;
    }
    if (validated == null) {
      validated = false;
    }
    if (hasCrosswalks == null) {
      hasCrosswalks = false;
    }
    if (hasTrafficLights == null) {
      hasTrafficLights = false;
    }
    if (wellLit == null) {
      wellLit = false;
    }
    if (hasSidewalk == null) {
      hasSidewalk = false;
    }
  }

  /**
   * 경로 사용 기록
   */
  public void recordUsage() {
    this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    this.lastUsed = LocalDateTime.now();
  }

  /**
   * 경로 검증
   */
  public void validate() {
    this.validated = true;
    this.validatedAt = LocalDateTime.now();
  }

  /**
   * 주 경로로 설정
   */
  public void setAsPrimary() {
    this.isPrimary = true;
  }

  /**
   * 안전도 평가
   */
  public void calculateSafetyScore() {
    float score = 0.5f; // 기본 점수
    
    if (Boolean.TRUE.equals(hasCrosswalks)) score += 0.1f;
    if (Boolean.TRUE.equals(hasTrafficLights)) score += 0.1f;
    if (Boolean.TRUE.equals(wellLit)) score += 0.1f;
    if (Boolean.TRUE.equals(hasSidewalk)) score += 0.1f;
    if (difficultyLevel == DifficultyLevel.VERY_EASY || difficultyLevel == DifficultyLevel.EASY) score += 0.1f;
    
    this.safetyScore = Math.min(1.0f, score);
  }

  /**
   * 사용 가능 여부 확인
   */
  public boolean isUsable() {
    return Boolean.TRUE.equals(isActive) && Boolean.TRUE.equals(validated);
  }

  /**
   * 야간 사용 가능 여부
   */
  public boolean isSafeAtNight() {
    return Boolean.TRUE.equals(wellLit) && safetyScore != null && safetyScore >= 0.7f;
  }
}