package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 알림 발송 이력 엔티티
 * 모든 알림의 발송 기록 및 응답 추적
 */
@Entity
@Table(name = "notification_history", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_channel", columnList = "channel"),
    @Index(name = "idx_notification_sent_at", columnList = "sent_at"),
    @Index(name = "idx_notification_event_type", columnList = "event_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "history_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user; // 알림 대상 사용자

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id")
  private EmergencyContact recipient; // 실제 수신자 (보호자)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  private NotificationTemplate template;

  @Column(name = "notification_id", unique = true, length = 50)
  private String notificationId; // 외부 시스템 참조 ID

  @Column(name = "channel", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private NotificationChannel channel;

  @Column(name = "event_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private NotificationTemplate.EventType eventType;

  @Column(name = "severity_level", length = 20)
  @Enumerated(EnumType.STRING)
  private NotificationTemplate.SeverityLevel severityLevel;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  @Column(name = "title", length = 200)
  private String title;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Column(name = "recipient_address", length = 200)
  private String recipientAddress; // 전화번호, 이메일 등

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @Column(name = "responded_at")
  private LocalDateTime respondedAt;

  @Column(name = "response_type", length = 50)
  private String responseType;

  @Column(name = "response_data", columnDefinition = "TEXT")
  private String responseData;

  @Column(name = "retry_count")
  private Integer retryCount;

  @Column(name = "max_retries")
  private Integer maxRetries;

  @Column(name = "last_retry_at")
  private LocalDateTime lastRetryAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "error_code", length = 50)
  private String errorCode;

  @Column(name = "provider", length = 50)
  private String provider; // AWS SNS, Twilio, FCM, etc.

  @Column(name = "provider_message_id", length = 100)
  private String providerMessageId;

  @Column(name = "cost")
  private Double cost; // 발송 비용 (SMS 등)

  @Column(name = "metadata", columnDefinition = "JSON")
  private String metadata; // 추가 메타데이터

  @Column(name = "escalation_level")
  private Integer escalationLevel;

  @Column(name = "escalated_from")
  private Long escalatedFromId; // 이전 알림 ID

  @Column(name = "escalated_to")
  private Long escalatedToId; // 다음 알림 ID

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "emergency_id")
  private Emergency emergency; // 관련 긴급 상황

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fall_event_id")
  private FallEvent fallEvent; // 관련 낙상 이벤트

  @Column(name = "response_time_seconds")
  private Long responseTimeSeconds;

  @Column(name = "is_test")
  private Boolean isTest;

  @Column(name = "priority")
  private Integer priority;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * 알림 채널
   */
  public enum NotificationChannel {
    SMS("문자"),
    EMAIL("이메일"),
    PUSH("푸시"),
    WEBSOCKET("웹소켓"),
    IN_APP("인앱"),
    PHONE_CALL("전화"),
    KAKAO_TALK("카카오톡");

    private final String description;

    NotificationChannel(String description) {
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
    SENDING("발송중"),
    SENT("발송완료"),
    DELIVERED("전달완료"),
    READ("읽음"),
    RESPONDED("응답함"),
    FAILED("실패"),
    EXPIRED("만료"),
    CANCELLED("취소됨"),
    RETRYING("재시도중");

    private final String description;

    NotificationStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  @PrePersist
  protected void onCreate() {
    if (status == null) {
      status = NotificationStatus.PENDING;
    }
    if (retryCount == null) {
      retryCount = 0;
    }
    if (maxRetries == null) {
      maxRetries = 3;
    }
    if (isTest == null) {
      isTest = false;
    }
    if (escalationLevel == null) {
      escalationLevel = 0;
    }
  }

  /**
   * 발송 시작
   */
  public void markAsSending() {
    this.status = NotificationStatus.SENDING;
    this.sentAt = LocalDateTime.now();
  }

  /**
   * 발송 완료
   */
  public void markAsSent(String providerMessageId) {
    this.status = NotificationStatus.SENT;
    this.providerMessageId = providerMessageId;
    if (this.sentAt == null) {
      this.sentAt = LocalDateTime.now();
    }
  }

  /**
   * 전달 완료
   */
  public void markAsDelivered() {
    this.status = NotificationStatus.DELIVERED;
    this.deliveredAt = LocalDateTime.now();
  }

  /**
   * 읽음 처리
   */
  public void markAsRead() {
    this.status = NotificationStatus.READ;
    this.readAt = LocalDateTime.now();
    if (this.deliveredAt == null) {
      this.deliveredAt = this.readAt;
    }
  }

  /**
   * 응답 처리
   */
  public void markAsResponded(String responseType, String responseData) {
    this.status = NotificationStatus.RESPONDED;
    this.respondedAt = LocalDateTime.now();
    this.responseType = responseType;
    this.responseData = responseData;
    
    // 응답 시간 계산
    if (this.sentAt != null) {
      this.responseTimeSeconds = 
          java.time.Duration.between(this.sentAt, this.respondedAt).getSeconds();
    }
  }

  /**
   * 실패 처리
   */
  public void markAsFailed(String errorCode, String errorMessage) {
    this.status = NotificationStatus.FAILED;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  /**
   * 재시도
   */
  public void retry() {
    this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    this.status = NotificationStatus.RETRYING;
    this.lastRetryAt = LocalDateTime.now();
  }

  /**
   * 재시도 가능 여부
   */
  public boolean canRetry() {
    return retryCount < maxRetries && 
           status != NotificationStatus.DELIVERED &&
           status != NotificationStatus.READ &&
           status != NotificationStatus.RESPONDED;
  }

  /**
   * 에스컬레이션 설정
   */
  public void setEscalation(Long fromId, Integer level) {
    this.escalatedFromId = fromId;
    this.escalationLevel = level;
  }

  /**
   * 응답 시간 계산 (초)
   */
  public long calculateResponseTime() {
    if (sentAt != null && respondedAt != null) {
      return java.time.Duration.between(sentAt, respondedAt).getSeconds();
    }
    return -1;
  }

  /**
   * 성공 여부
   */
  public boolean isSuccessful() {
    return status == NotificationStatus.DELIVERED ||
           status == NotificationStatus.READ ||
           status == NotificationStatus.RESPONDED;
  }

  /**
   * 긴급 알림 여부
   */
  public boolean isUrgent() {
    return severityLevel == NotificationTemplate.SeverityLevel.CRITICAL ||
           (priority != null && priority >= 9);
  }
}