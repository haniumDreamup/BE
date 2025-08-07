package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pose 감지 세션 엔티티
 * 연속된 pose 데이터를 그룹화하여 관리
 */
@Entity
@Table(name = "pose_sessions",
    indexes = {
        @Index(name = "idx_session_user", columnList = "user_id"),
        @Index(name = "idx_session_start", columnList = "start_time")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "poseDataList", "fallEvents"})
public class PoseSession extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @Column(name = "session_id", unique = true, nullable = false)
  private String sessionId;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;
  
  @Column(name = "end_time")
  private LocalDateTime endTime;
  
  @Column(name = "total_frames")
  private Integer totalFrames;
  
  @Column(name = "average_fps")
  private Float averageFps;
  
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SessionStatus status;
  
  @OneToMany(mappedBy = "poseSession", cascade = CascadeType.ALL)
  @Builder.Default
  private List<PoseData> poseDataList = new ArrayList<>();
  
  @OneToMany(mappedBy = "poseSession", cascade = CascadeType.ALL)
  @Builder.Default
  private List<FallEvent> fallEvents = new ArrayList<>();
  
  public enum SessionStatus {
    ACTIVE,
    COMPLETED,
    INTERRUPTED,
    ERROR
  }
}