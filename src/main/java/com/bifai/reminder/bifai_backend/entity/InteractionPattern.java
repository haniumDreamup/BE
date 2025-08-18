package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 인터랙션 패턴 분석 결과 엔티티
 * 사용자의 행동 패턴을 분석하여 저장
 */
@Entity
@Table(name = "interaction_patterns",
  indexes = {
    @Index(name = "idx_pattern_user_type", columnList = "user_id,pattern_type"),
    @Index(name = "idx_pattern_date", columnList = "analysis_date"),
    @Index(name = "idx_pattern_anomaly", columnList = "is_anomaly,anomaly_score")
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionPattern {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Column(name = "pattern_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private PatternType patternType;
  
  @Column(name = "analysis_date", nullable = false)
  private LocalDateTime analysisDate;
  
  @Column(name = "time_window_start", nullable = false)
  private LocalDateTime timeWindowStart;
  
  @Column(name = "time_window_end", nullable = false)
  private LocalDateTime timeWindowEnd;
  
  // 패턴 메트릭스
  @Column(name = "click_frequency")
  private Double clickFrequency; // 분당 클릭 횟수
  
  @Column(name = "avg_session_duration")
  private Double avgSessionDuration; // 평균 세션 시간 (초)
  
  @Column(name = "page_view_count")
  private Integer pageViewCount; // 페이지 뷰 수
  
  @Column(name = "unique_pages_visited")
  private Integer uniquePagesVisited; // 방문한 고유 페이지 수
  
  @Column(name = "error_rate")
  private Double errorRate; // 에러 발생률
  
  @Column(name = "feature_usage_count")
  private Integer featureUsageCount; // 기능 사용 횟수
  
  @Column(name = "avg_response_time")
  private Double avgResponseTime; // 평균 응답 시간 (ms)
  
  // 네비게이션 패턴
  @Column(name = "navigation_paths", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> navigationPaths = new HashMap<>();
  
  // 가장 많이 사용된 기능들
  @Column(name = "top_features", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> topFeatures = new HashMap<>();
  
  // 시간대별 활동 패턴
  @Column(name = "hourly_activity", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> hourlyActivity = new HashMap<>();
  
  // 이상 패턴 감지
  @Column(name = "is_anomaly")
  private Boolean isAnomaly;
  
  @Column(name = "anomaly_score")
  private Double anomalyScore; // 0-100, 높을수록 이상함
  
  @Column(name = "anomaly_details", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> anomalyDetails = new HashMap<>();
  
  // 통계 기준값 (평균, 표준편차)
  @Column(name = "baseline_metrics", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> baselineMetrics = new HashMap<>();
  
  @Column(name = "confidence_level")
  private Double confidenceLevel; // 패턴 신뢰도 (0-1)
  
  @Column(name = "sample_size")
  private Integer sampleSize; // 분석에 사용된 로그 수
  
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  /**
   * 패턴 타입
   */
  public enum PatternType {
    DAILY,      // 일별 패턴
    WEEKLY,     // 주별 패턴
    MONTHLY,    // 월별 패턴
    REALTIME,   // 실시간 패턴
    SESSION,    // 세션별 패턴
    FEATURE,    // 기능별 패턴
    NAVIGATION, // 네비게이션 패턴
    ERROR,      // 에러 패턴
    PERFORMANCE // 성능 패턴
  }
  
  /**
   * 3σ (3-sigma) 규칙으로 이상 패턴 감지
   */
  public void detectAnomaly(double mean, double stdDev, double currentValue) {
    double zScore = Math.abs((currentValue - mean) / stdDev);
    
    // 3σ를 벗어나면 이상 패턴
    this.isAnomaly = zScore > 3;
    this.anomalyScore = Math.min(zScore * 20, 100); // 0-100 스케일로 변환
    
    if (this.isAnomaly) {
      this.anomalyDetails.put("zScore", zScore);
      this.anomalyDetails.put("deviation", currentValue - mean);
      this.anomalyDetails.put("threshold", mean + (3 * stdDev));
    }
  }
  
  /**
   * 패턴 요약 정보 생성
   */
  public Map<String, Object> getSummary() {
    Map<String, Object> summary = new HashMap<>();
    summary.put("patternType", patternType);
    summary.put("timeWindow", timeWindowStart + " ~ " + timeWindowEnd);
    summary.put("clickFrequency", clickFrequency);
    summary.put("sessionDuration", avgSessionDuration);
    summary.put("pageViews", pageViewCount);
    summary.put("errorRate", errorRate);
    summary.put("isAnomaly", isAnomaly);
    summary.put("anomalyScore", anomalyScore);
    return summary;
  }
}