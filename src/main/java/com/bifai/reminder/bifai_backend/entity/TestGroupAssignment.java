package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 테스트 그룹 할당 엔티티
 * 사용자와 테스트 그룹 간의 매핑 관리
 */
@Entity
@Table(name = "test_group_assignments",
  indexes = {
    @Index(name = "idx_assignment_user", columnList = "user_id"),
    @Index(name = "idx_assignment_group", columnList = "test_group_id"),
    @Index(name = "idx_assignment_experiment", columnList = "experiment_id"),
    @Index(name = "idx_assignment_created", columnList = "assigned_at")
  },
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "experiment_id"})
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestGroupAssignment {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "test_group_id", nullable = false)
  private TestGroup testGroup;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  private TestVariant variant;
  
  @Column(name = "assignment_hash", length = 64)
  private String assignmentHash; // 할당 해시 (재현 가능한 할당을 위해)
  
  @Column(name = "assignment_reason", length = 100)
  private String assignmentReason; // 할당 이유 (예: "random", "segment_match", "manual")
  
  @CreationTimestamp
  @Column(name = "assigned_at", updatable = false)
  private LocalDateTime assignedAt;
  
  @Column(name = "first_exposure_at")
  private LocalDateTime firstExposureAt; // 첫 노출 시간
  
  @Column(name = "last_exposure_at")
  private LocalDateTime lastExposureAt; // 마지막 노출 시간
  
  @Column(name = "exposure_count")
  @Builder.Default
  private Integer exposureCount = 0; // 노출 횟수
  
  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;
  
  @Column(name = "opted_out")
  @Builder.Default
  private Boolean optedOut = false; // 사용자가 실험에서 제외 요청
  
  // 사용자별 메타데이터
  @Column(name = "user_metadata", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> userMetadata = new HashMap<>();
  
  // 사용자 행동 추적
  @Column(name = "conversion_achieved")
  @Builder.Default
  private Boolean conversionAchieved = false;
  
  @Column(name = "conversion_at")
  private LocalDateTime conversionAt;
  
  @Column(name = "conversion_value")
  private Double conversionValue;
  
  // 사용자 피드백
  @Column(name = "user_feedback", columnDefinition = "TEXT")
  private String userFeedback;
  
  @Column(name = "satisfaction_score")
  private Integer satisfactionScore; // 1-5 scale
  
  /**
   * 노출 기록
   */
  public void recordExposure() {
    if (firstExposureAt == null) {
      firstExposureAt = LocalDateTime.now();
    }
    lastExposureAt = LocalDateTime.now();
    exposureCount++;
  }
  
  /**
   * 전환 기록
   */
  public void recordConversion(Double value) {
    this.conversionAchieved = true;
    this.conversionAt = LocalDateTime.now();
    this.conversionValue = value;
  }
  
  /**
   * 실험 제외
   */
  public void optOut() {
    this.optedOut = true;
    this.isActive = false;
  }
  
  /**
   * 할당 해시 생성
   */
  public static String generateHash(Long userId, String experimentKey) {
    // 간단한 해시 생성 (실제로는 더 복잡한 해시 함수 사용)
    return String.valueOf((userId + experimentKey).hashCode());
  }
  
  /**
   * 노출 기간 계산 (일)
   */
  public long getExposureDurationDays() {
    if (firstExposureAt == null || lastExposureAt == null) {
      return 0;
    }
    return java.time.Duration.between(firstExposureAt, lastExposureAt).toDays();
  }
}