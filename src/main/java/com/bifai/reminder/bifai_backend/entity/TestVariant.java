package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * A/B 테스트 변형(Variant) 엔티티
 * 실험의 각 변형 버전 관리
 */
@Entity
@Table(name = "test_variants",
  indexes = {
    @Index(name = "idx_variant_experiment", columnList = "experiment_id"),
    @Index(name = "idx_variant_key", columnList = "variant_key")
  },
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"experiment_id", "variant_key"})
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestVariant {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "variant_id")
  private Long variantId;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;
  
  @Column(name = "variant_key", nullable = false, length = 50)
  private String variantKey; // 예: "control", "blue_button", "large_font"
  
  @Column(name = "variant_name", nullable = false, length = 100)
  private String variantName;
  
  @Column(columnDefinition = "TEXT")
  private String description;
  
  @Column(name = "is_control")
  @Builder.Default
  private Boolean isControl = false;
  
  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;
  
  // Feature Flag 값들
  @Column(name = "feature_flags", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> featureFlags = new HashMap<>();
  
  // 변형 설정
  @Column(name = "config", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> config = new HashMap<>();
  
  // 성능 메트릭
  @Column(name = "conversion_rate")
  private Double conversionRate;
  
  @Column(name = "engagement_score")
  private Double engagementScore;
  
  @Column(name = "error_rate")
  private Double errorRate;
  
  @Column(name = "avg_session_duration")
  private Double avgSessionDuration; // 초 단위
  
  @Column(name = "participants")
  @Builder.Default
  private Integer participants = 0;
  
  // 통계적 유의성
  @Column(name = "p_value")
  private Double pValue;
  
  @Column(name = "confidence_level")
  private Double confidenceLevel;
  
  @Column(name = "is_winner")
  private Boolean isWinner;
  
  // 변형별 결과
  @Column(name = "results", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> results = new HashMap<>();
  
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  /**
   * Feature Flag 값 가져오기
   */
  public Object getFeatureFlag(String key) {
    return featureFlags.get(key);
  }
  
  /**
   * Feature Flag 값 설정
   */
  public void setFeatureFlag(String key, Object value) {
    featureFlags.put(key, value);
  }
  
  
  /**
   * 참여자 수 증가
   */
  public void incrementParticipants() {
    this.participants++;
  }
  
  /**
   * 전환율 업데이트
   */
  public void updateConversionRate(int conversions) {
    if (participants > 0) {
      this.conversionRate = (double) conversions / participants * 100;
    }
  }
  
  /**
   * 통계적 유의성 확인
   */
  public boolean isStatisticallySignificant() {
    return pValue != null && pValue < 0.05;
  }
  
  /**
   * 성능 점수 계산
   */
  public double calculatePerformanceScore() {
    double score = 0.0;
    
    if (conversionRate != null) {
      score += conversionRate * 0.4; // 40% 가중치
    }
    
    if (engagementScore != null) {
      score += engagementScore * 0.3; // 30% 가중치
    }
    
    if (errorRate != null) {
      score += (100 - errorRate) * 0.2; // 20% 가중치 (낮을수록 좋음)
    }
    
    if (avgSessionDuration != null) {
      score += Math.min(avgSessionDuration / 60, 100) * 0.1; // 10% 가중치
    }
    
    return score;
  }
}