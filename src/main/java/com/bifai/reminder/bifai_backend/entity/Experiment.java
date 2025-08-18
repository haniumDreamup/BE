package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A/B 테스트 실험 엔티티
 * BIF 사용자를 위한 실험 관리
 */
@Entity
@Table(name = "experiments",
  indexes = {
    @Index(name = "idx_experiment_status", columnList = "status"),
    @Index(name = "idx_experiment_dates", columnList = "start_date,end_date"),
    @Index(name = "idx_experiment_active", columnList = "is_active,status")
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experiment {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "experiment_id")
  private Long experimentId;
  
  @Column(name = "experiment_key", nullable = false, unique = true, length = 100)
  private String experimentKey; // 고유 식별자 (예: "new_ui_test_2024")
  
  @Column(nullable = false, length = 200)
  private String name;
  
  @Column(columnDefinition = "TEXT")
  private String description;
  
  @Column(name = "hypothesis", columnDefinition = "TEXT")
  private String hypothesis; // 실험 가설
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExperimentStatus status;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "experiment_type", nullable = false, length = 30)
  private ExperimentType experimentType;
  
  @Column(name = "start_date")
  private LocalDateTime startDate;
  
  @Column(name = "end_date")
  private LocalDateTime endDate;
  
  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = false;
  
  // 실험 대상 조건
  @Column(name = "target_criteria", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> targetCriteria = new HashMap<>();
  
  // 실험 설정
  @Column(name = "configuration", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> configuration = new HashMap<>();
  
  // 트래픽 분배 비율 (예: {"control": 50, "variant_a": 25, "variant_b": 25})
  @Column(name = "traffic_allocation", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Integer> trafficAllocation = new HashMap<>();
  
  @Column(name = "sample_size_target")
  private Integer sampleSizeTarget; // 목표 샘플 크기
  
  @Column(name = "current_participants")
  @Builder.Default
  private Integer currentParticipants = 0;
  
  // 메타데이터
  @Column(columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> metadata = new HashMap<>();
  
  // 실제 시작/종료 시간
  private LocalDateTime actualStartDate;
  private LocalDateTime actualEndDate;
  
  // 성공 메트릭
  @Column(name = "success_metrics", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> successMetrics = new HashMap<>();
  
  // 실험 결과
  @Column(name = "results", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> results = new HashMap<>();
  
  @Column(name = "created_by", length = 100)
  private String createdBy;
  
  @Column(name = "updated_by", length = 100)
  private String updatedBy;
  
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  // 관계 매핑
  @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<TestGroup> testGroups = new ArrayList<>();
  
  @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<TestVariant> variants = new ArrayList<>();
  
  /**
   * 실험 상태
   */
  public enum ExperimentStatus {
    DRAFT("초안"),
    SCHEDULED("예정됨"),
    ACTIVE("진행중"),
    PAUSED("일시중지"),
    COMPLETED("완료"),
    CANCELLED("취소됨");
    
    private final String description;
    
    ExperimentStatus(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 실험 타입
   */
  public enum ExperimentType {
    AB_TEST("A/B 테스트"),
    MULTIVARIATE("다변량 테스트"),
    FEATURE_FLAG("기능 플래그"),
    ROLLOUT("점진적 출시"),
    CANARY("카나리 배포");
    
    private final String description;
    
    ExperimentType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 실험 시작
   */
  public void start() {
    if (this.status != ExperimentStatus.SCHEDULED && this.status != ExperimentStatus.DRAFT) {
      throw new IllegalStateException("실험을 시작할 수 없는 상태입니다: " + this.status);
    }
    this.status = ExperimentStatus.ACTIVE;
    this.isActive = true;
    this.startDate = LocalDateTime.now();
  }
  
  /**
   * 실험 일시중지
   */
  public void pause() {
    if (this.status != ExperimentStatus.ACTIVE) {
      throw new IllegalStateException("활성 상태의 실험만 일시중지할 수 있습니다");
    }
    this.status = ExperimentStatus.PAUSED;
    this.isActive = false;
  }
  
  /**
   * 실험 재개
   */
  public void resume() {
    if (this.status != ExperimentStatus.PAUSED) {
      throw new IllegalStateException("일시중지된 실험만 재개할 수 있습니다");
    }
    this.status = ExperimentStatus.ACTIVE;
    this.isActive = true;
  }
  
  /**
   * 실험 완료
   */
  public void complete() {
    this.status = ExperimentStatus.COMPLETED;
    this.isActive = false;
    this.endDate = LocalDateTime.now();
  }
  
  /**
   * 참여자 증가
   */
  public void incrementParticipants() {
    this.currentParticipants++;
  }
  
  /**
   * 실험 진행률 계산
   */
  public double getProgress() {
    if (sampleSizeTarget == null || sampleSizeTarget == 0) {
      return 0.0;
    }
    return Math.min((double) currentParticipants / sampleSizeTarget * 100, 100.0);
  }
}