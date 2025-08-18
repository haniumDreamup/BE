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
 * A/B 테스트 그룹 엔티티
 * 실험 참여 사용자 그룹 관리
 */
@Entity
@Table(name = "test_groups",
  indexes = {
    @Index(name = "idx_test_group_experiment", columnList = "experiment_id"),
    @Index(name = "idx_test_group_name", columnList = "group_name"),
    @Index(name = "idx_test_group_type", columnList = "group_type")
  },
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"experiment_id", "group_name"})
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestGroup {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "group_id")
  private Long groupId;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id", nullable = false)
  private Experiment experiment;
  
  @Column(name = "group_name", nullable = false, length = 50)
  private String groupName; // 예: "control", "variant_a", "variant_b"
  
  @Column(length = 200)
  private String description;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "group_type", nullable = false, length = 20)
  private GroupType groupType;
  
  @Column(name = "allocation_percentage")
  private Integer allocationPercentage; // 할당 비율 (%)
  
  @Column(name = "min_sample_size")
  private Integer minSampleSize; // 최소 샘플 크기
  
  @Column(name = "max_sample_size")
  private Integer maxSampleSize; // 최대 샘플 크기
  
  @Column(name = "current_size")
  @Builder.Default
  private Integer currentSize = 0; // 현재 그룹 크기
  
  // 그룹별 설정 (Feature Flag 값 등)
  @Column(name = "group_config", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> groupConfig = new HashMap<>();
  
  // 그룹 세그먼트 조건
  @Column(name = "segment_criteria", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> segmentCriteria = new HashMap<>();
  
  @Column(name = "is_active")
  @Builder.Default
  private Boolean isActive = true;
  
  @Column(name = "is_control")
  @Builder.Default
  private Boolean isControl = false; // 대조군 여부
  
  // 그룹 성과 메트릭
  @Column(name = "performance_metrics", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> performanceMetrics = new HashMap<>();
  
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  // 관계 매핑
  @OneToMany(mappedBy = "testGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<TestGroupAssignment> assignments = new ArrayList<>();
  
  /**
   * 그룹 타입
   */
  public enum GroupType {
    CONTROL("대조군"),
    TREATMENT("실험군"),
    VARIANT("변형군"),
    HOLDOUT("홀드아웃");
    
    private final String description;
    
    GroupType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 사용자 추가
   */
  public void addUser() {
    this.currentSize++;
    if (maxSampleSize != null && currentSize > maxSampleSize) {
      throw new IllegalStateException("그룹 최대 크기를 초과했습니다");
    }
  }
  
  /**
   * 사용자 제거
   */
  public void removeUser() {
    if (this.currentSize > 0) {
      this.currentSize--;
    }
  }
  
  /**
   * 그룹이 가득 찼는지 확인
   */
  public boolean isFull() {
    return maxSampleSize != null && currentSize >= maxSampleSize;
  }
  
  /**
   * 최소 샘플 크기 달성 여부
   */
  public boolean hasMinimumSample() {
    return minSampleSize == null || currentSize >= minSampleSize;
  }
  
  /**
   * 그룹 활용률 계산
   */
  public double getUtilization() {
    if (maxSampleSize == null || maxSampleSize == 0) {
      return 0.0;
    }
    return (double) currentSize / maxSampleSize * 100;
  }
}