package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 긴급 상황 엔티티
 * BIF 사용자의 긴급 상황 발생 기록을 관리
 */
@Entity
@Table(name = "emergencies", indexes = {
    @Index(name = "idx_emergency_user_id", columnList = "user_id"),
    @Index(name = "idx_emergency_created_at", columnList = "createdAt"),
    @Index(name = "idx_emergency_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emergency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "emergency_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "emergency_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private EmergencyType type;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private EmergencyStatus status;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "severity", length = 20)
  @Enumerated(EnumType.STRING)
  private EmergencySeverity severity;

  @Column(name = "triggered_by", length = 50)
  @Enumerated(EnumType.STRING)
  private TriggerSource triggeredBy;

  @Column(name = "fall_confidence")
  private Double fallConfidence;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "resolved_by", length = 100)
  private String resolvedBy;

  @Column(name = "resolution_notes", columnDefinition = "TEXT")
  private String resolutionNotes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "notified_guardians", columnDefinition = "TEXT")
  private String notifiedGuardians;

  @Column(name = "response_time_seconds")
  private Integer responseTimeSeconds;
  
  @Column(name = "notification_sent")
  private boolean notificationSent;
  
  @Column(name = "responder_count")
  private Integer responderCount;

  @Column(name = "triggered_at")
  private LocalDateTime triggeredAt;
  
  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;
  
  @Column(name = "notification_count")
  private Integer notificationCount;

  /**
   * 긴급 상황 유형
   */
  public enum EmergencyType {
    FALL_DETECTED("낙상 감지"),
    FALL_DETECTION("낙상 감지"),
    PANIC_BUTTON("긴급 버튼"),
    MANUAL_ALERT("수동 호출"),
    LOST("길 잃음"),
    MEDICAL("의료 긴급"),
    GEOFENCE_EXIT("안전구역 이탈"),
    DEVICE_OFFLINE("기기 오프라인"),
    MEDICATION_MISSED("약물 미복용"),
    ABNORMAL_PATTERN("비정상 패턴"),
    OTHER("기타");

    private final String description;

    EmergencyType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 긴급 상황 상태
   */
  public enum EmergencyStatus {
    TRIGGERED("발동됨"),
    ACTIVE("활성"),
    NOTIFIED("알림 전송됨"),
    ACKNOWLEDGED("확인됨"),
    RESOLVED("해결됨"),
    CANCELLED("취소됨"),
    FALSE_ALARM("오작동");

    private final String description;

    EmergencyStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 긴급 상황 심각도
   */
  public enum EmergencySeverity {
    LOW("낮음"),
    MEDIUM("중간"),
    HIGH("높음"),
    CRITICAL("위급");

    private final String description;

    EmergencySeverity(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 긴급 상황 트리거 소스
   */
  public enum TriggerSource {
    USER("사용자"),
    DEVICE("디바이스"),
    AI_DETECTION("AI 감지"),
    GUARDIAN("보호자"),
    SYSTEM("시스템");

    private final String description;

    TriggerSource(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (status == null) {
      status = EmergencyStatus.ACTIVE;
    }
    if (severity == null) {
      severity = EmergencySeverity.HIGH;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}