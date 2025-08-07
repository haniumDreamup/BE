package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 낙상 이벤트 엔티티
 * 감지된 낙상 사건을 기록
 */
@Entity
@Table(name = "fall_events",
    indexes = {
        @Index(name = "idx_fall_user", columnList = "user_id"),
        @Index(name = "idx_fall_detected", columnList = "detected_at"),
        @Index(name = "idx_fall_severity", columnList = "severity")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "poseSession"})
public class FallEvent extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id")
  private PoseSession poseSession;
  
  @Column(name = "detected_at", nullable = false)
  private LocalDateTime detectedAt;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "severity", nullable = false)
  private FallSeverity severity;
  
  @Column(name = "confidence_score", nullable = false)
  private Float confidenceScore;
  
  @Column(name = "location_lat")
  private Double locationLat;
  
  @Column(name = "location_lng")
  private Double locationLng;
  
  @Column(name = "location_address")
  private String locationAddress;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private EventStatus status;
  
  @Column(name = "notification_sent")
  private Boolean notificationSent;
  
  @Column(name = "notification_sent_at")
  private LocalDateTime notificationSentAt;
  
  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;
  
  @Column(name = "resolved_by")
  private String resolvedBy;
  
  @Column(name = "false_positive")
  private Boolean falsePositive;
  
  @Column(name = "user_feedback", columnDefinition = "TEXT")
  private String userFeedback;
  
  // 낙상 분석 데이터
  @Column(name = "fall_duration_ms")
  private Integer fallDurationMs;
  
  @Column(name = "impact_force")
  private Float impactForce;
  
  @Column(name = "body_angle")
  private Float bodyAngle;
  
  @Column(name = "motion_before", columnDefinition = "JSON")
  private String motionBefore; // 낙상 전 5초간 움직임 데이터
  
  @Column(name = "motion_after", columnDefinition = "JSON")
  private String motionAfter; // 낙상 후 5초간 움직임 데이터
  
  public enum FallSeverity {
    LOW("경미"),
    MEDIUM("중간"),
    HIGH("심각"),
    CRITICAL("위급");
    
    private final String description;
    
    FallSeverity(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  public enum EventStatus {
    DETECTED("감지됨"),
    NOTIFIED("알림 전송됨"),
    ACKNOWLEDGED("확인됨"),
    RESOLVED("해결됨"),
    FALSE_POSITIVE("오탐지");
    
    private final String description;
    
    EventStatus(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
}