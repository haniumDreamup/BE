package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * MediaPipe Pose 데이터 엔티티
 * 시계열 데이터로 대량 저장을 고려한 설계
 */
@Entity
@Table(name = "pose_data",
    indexes = {
        @Index(name = "idx_pose_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_pose_session", columnList = "session_id"),
        @Index(name = "idx_pose_timestamp", columnList = "timestamp")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "poseSession"})
public class PoseData extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_id")
  private PoseSession poseSession;
  
  @Column(nullable = false)
  private LocalDateTime timestamp;
  
  @Column(name = "frame_number")
  private Integer frameNumber;
  
  @Column(name = "landmarks_json", columnDefinition = "JSON", nullable = false)
  private String landmarksJson;
  
  @Column(name = "overall_confidence")
  private Float overallConfidence;
  
  // 낙상 감지 관련 계산값들
  @Column(name = "center_y")
  private Float centerY; // 신체 중심점 Y 좌표
  
  @Column(name = "velocity_y")
  private Float velocityY; // Y축 속도
  
  @Column(name = "is_horizontal")
  private Boolean isHorizontal; // 수평 자세 여부
  
  @Column(name = "motion_score")
  private Float motionScore; // 움직임 점수
  
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}