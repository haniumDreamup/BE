package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 이력 엔티티
 * 
 * 발송된 푸시 알림의 이력을 저장하고 관리
 */
@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_notification_type_status", columnList = "notification_type, status"),
    @Index(name = "idx_notification_created_at", columnList = "created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationHistory extends BaseEntity {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
  
  @Column(name = "notification_type", nullable = false, length = 30)
  @Enumerated(EnumType.STRING)
  private NotificationType notificationType;
  
  @Column(nullable = false, length = 200)
  private String title;
  
  @Column(columnDefinition = "TEXT")
  private String body;
  
  @Column(name = "device_token", length = 500)
  private String deviceToken;
  
  @Column(name = "device_id", length = 255)
  private String deviceId;
  
  @Column(length = 30)
  @Enumerated(EnumType.STRING)
  private NotificationStatus status;
  
  @Column(name = "sent_at")
  private LocalDateTime sentAt;
  
  @Column(name = "read_at")
  private LocalDateTime readAt;
  
  @Column(name = "clicked_at")
  private LocalDateTime clickedAt;
  
  @Column(name = "error_message", length = 500)
  private String errorMessage;
  
  @Column(name = "retry_count")
  @Builder.Default
  private Integer retryCount = 0;
  
  @Column(name = "fcm_message_id", length = 255)
  private String fcmMessageId;
  
  @Column(columnDefinition = "JSON")
  private String metadata; // 추가 데이터 JSON
  
  @Column(length = 20)
  @Enumerated(EnumType.STRING)
  private Priority priority;
  
  @Column(name = "related_entity_id")
  private Long relatedEntityId; // 관련 엔티티 ID (medication_id, schedule_id 등)
  
  @Column(name = "related_entity_type", length = 50)
  private String relatedEntityType; // 관련 엔티티 타입
  
  @Column(name = "notification_id", unique = true, length = 255)
  private String notificationId; // 알림 고유 ID
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id")
  private EmergencyContact recipient; // 수신자 (보호자 등)
  
  @Column(name = "event_type", length = 50)
  @Enumerated(EnumType.STRING)
  private NotificationTemplate.EventType eventType; // 이벤트 타입
  
  @Column(name = "severity_level", length = 20)
  private String severityLevel; // 심각도
  
  @Column(name = "max_retries")
  @Builder.Default
  private Integer maxRetries = 3;
  
  @Column(name = "responded_at")
  private LocalDateTime respondedAt;
  
  @Column(name = "response_type", length = 50)
  private String responseType;
  
  @Column(name = "response_data", columnDefinition = "TEXT")
  private String responseData;
  
  @Column(name = "response_time_seconds")
  private Integer responseTimeSeconds;
  
  @Column(name = "escalation_level")
  @Builder.Default
  private Integer escalationLevel = 0;
  
  @Column(name = "escalated_from_id")
  private Long escalatedFromId;
  
  @Column(name = "channel", length = 20)
  private String channel; // SMS, EMAIL, PUSH 등
  
  @Column(name = "cost")
  private Double cost; // 알림 발송 비용
  
  @Column(name = "is_test")
  @Builder.Default
  private Boolean isTest = false;
  
  /**
   * 알림 타입
   */
  public enum NotificationType {
    MEDICATION("약물 복용"),
    SCHEDULE("일정"),
    EMERGENCY("긴급"),
    REMINDER("일반 알림"),
    HEALTH("건강 체크"),
    LOCATION("위치 알림"),
    GUARDIAN("보호자 알림"),
    SYSTEM("시스템 알림");
    
    private final String description;
    
    NotificationType(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 알림 상태
   */
  public enum NotificationStatus {
    PENDING("대기중"),
    SENT("전송됨"),
    DELIVERED("도착함"),
    READ("읽음"),
    CLICKED("클릭됨"),
    FAILED("실패"),
    EXPIRED("만료됨");
    
    private final String description;
    
    NotificationStatus(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 우선순위
   */
  public enum Priority {
    HIGH("높음"),
    NORMAL("보통"),
    LOW("낮음");
    
    private final String description;
    
    Priority(String description) {
      this.description = description;
    }
    
    public String getDescription() {
      return description;
    }
  }
  
  /**
   * 알림 전송 완료 처리
   */
  public void markAsSent(String fcmMessageId) {
    this.status = NotificationStatus.SENT;
    this.sentAt = LocalDateTime.now();
    this.fcmMessageId = fcmMessageId;
  }
  
  /**
   * 알림 읽음 처리
   */
  public void markAsRead() {
    this.status = NotificationStatus.READ;
    this.readAt = LocalDateTime.now();
  }
  
  /**
   * 알림 클릭 처리
   */
  public void markAsClicked() {
    this.status = NotificationStatus.CLICKED;
    this.clickedAt = LocalDateTime.now();
  }
  
  /**
   * 알림 실패 처리
   */
  public void markAsFailed(String errorMessage) {
    this.status = NotificationStatus.FAILED;
    this.errorMessage = errorMessage;
    this.retryCount++;
  }
  
  /**
   * 재시도 가능 여부
   */
  public boolean canRetry() {
    return retryCount < 3 && status == NotificationStatus.FAILED;
  }
}