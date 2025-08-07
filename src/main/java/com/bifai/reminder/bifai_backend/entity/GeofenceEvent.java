package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 지오펜스 이벤트 엔티티
 * 안전 구역 진입/이탈 이벤트 기록
 */
@Entity
@Table(name = "geofence_events", indexes = {
    @Index(name = "idx_geofence_event_user_id", columnList = "user_id"),
    @Index(name = "idx_geofence_event_geofence_id", columnList = "geofence_id"),
    @Index(name = "idx_geofence_event_created_at", columnList = "created_at"),
    @Index(name = "idx_geofence_event_type", columnList = "event_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeofenceEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "event_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "geofence_id", nullable = false)
  private Geofence geofence;

  @Column(name = "event_type", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private EventType eventType;

  @Column(name = "latitude", nullable = false)
  private Double latitude;

  @Column(name = "longitude", nullable = false)
  private Double longitude;

  @Column(name = "accuracy")
  private Double accuracy;

  @Column(name = "speed")
  private Double speed;

  @Column(name = "heading")
  private Double heading;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "notification_sent")
  private Boolean notificationSent;

  @Column(name = "notification_sent_at")
  private LocalDateTime notificationSentAt;

  @Column(name = "notification_recipients", length = 1000)
  private String notificationRecipients;

  @Column(name = "acknowledged")
  private Boolean acknowledged;

  @Column(name = "acknowledged_by")
  private String acknowledgedBy;

  @Column(name = "acknowledged_at")
  private LocalDateTime acknowledgedAt;

  @Column(name = "notes", length = 1000)
  private String notes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "duration_seconds")
  private Long durationSeconds;

  @Column(name = "risk_level", length = 20)
  @Enumerated(EnumType.STRING)
  private RiskLevel riskLevel;

  /**
   * 이벤트 유형
   */
  public enum EventType {
    ENTRY("진입"),
    EXIT("이탈"),
    DWELL("체류"),
    WARNING("경고");

    private final String description;

    EventType(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /**
   * 위험도 수준
   */
  public enum RiskLevel {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    CRITICAL("위급");

    private final String description;

    RiskLevel(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (notificationSent == null) {
      notificationSent = false;
    }
    if (acknowledged == null) {
      acknowledged = false;
    }
    if (riskLevel == null) {
      riskLevel = RiskLevel.LOW;
    }
  }

  /**
   * 이벤트가 위험한지 확인
   */
  public boolean isHighRisk() {
    return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
  }

  /**
   * 알림이 필요한지 확인
   */
  public boolean requiresNotification() {
    if (eventType == EventType.EXIT && geofence.getAlertOnExit()) {
      return true;
    }
    if (eventType == EventType.ENTRY && geofence.getAlertOnEntry()) {
      return true;
    }
    return eventType == EventType.WARNING;
  }
}