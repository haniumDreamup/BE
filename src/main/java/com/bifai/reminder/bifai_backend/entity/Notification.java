package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * BIF 사용자를 위한 알림 엔티티
 * 다양한 타입의 알림을 관리하며 인지적 부담을 최소화하는 설계
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_id", columnList = "user_id"),
    @Index(name = "idx_notification_schedule_id", columnList = "schedule_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_send_time", columnList = "send_time"),
    @Index(name = "idx_notification_read", columnList = "is_read")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user", "schedule"})
public class Notification extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 받을 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    /**
     * 연관된 스케줄 (선택사항)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    /**
     * 알림 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    @NotNull
    private NotificationType notificationType;

    /**
     * BIF 사용자를 위한 간단하고 명확한 제목
     */
    @Column(name = "title", nullable = false, length = 100)
    @NotBlank
    private String title;

    /**
     * 간단하고 이해하기 쉬운 메시지 내용
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String message;

    /**
     * BIF 사용자를 위한 행동 가이드
     */
    @Column(name = "action_guidance", columnDefinition = "TEXT")
    private String actionGuidance;

    /**
     * 알림 전송 예정 시간
     */
    @Column(name = "send_time", nullable = false)
    @NotNull
    private LocalDateTime sendTime;

    /**
     * 실제 전송 시간
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 알림 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * 우선순위 (1=낮음, 2=보통, 3=높음, 4=응급)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority = 2;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 응답/확인 여부
     */
    @Column(name = "is_acknowledged", nullable = false)
    private Boolean isAcknowledged = false;

    /**
     * 응답/확인 시간
     */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * 최대 재시도 횟수
     */
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    /**
     * 전송 채널 (푸시, SMS, 이메일 등)
     */
    @ElementCollection(targetClass = DeliveryChannel.class)
    @CollectionTable(name = "notification_channels", joinColumns = @JoinColumn(name = "notification_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private java.util.Set<DeliveryChannel> deliveryChannels;

    /**
     * BIF 사용자를 위한 시각적 표시 (아이콘, 색상 등)
     */
    @Column(name = "visual_indicator", length = 50)
    private String visualIndicator;

    /**
     * 음성 안내 여부
     */
    @Column(name = "voice_enabled")
    private Boolean voiceEnabled = false;

    /**
     * 진동 여부
     */
    @Column(name = "vibration_enabled")
    private Boolean vibrationEnabled = true;

    /**
     * 사용자 응답 (단순한 확인/거부 등)
     */
    @Column(name = "user_response", length = 50)
    private String userResponse;

    /**
     * 추가 메타데이터 (JSON 형태)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata;

    /**
     * 만료 시간 (이후에는 알림 무효)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 보호자에게도 전송 여부
     */
    @Column(name = "notify_guardian")
    private Boolean notifyGuardian = false;

    /**
     * 알림 타입 열거형
     */
    public enum NotificationType {
        MEDICATION_REMINDER("약 복용 알림"),
        MEAL_REMINDER("식사 알림"),
        EXERCISE_REMINDER("운동 알림"),
        APPOINTMENT_REMINDER("약속 알림"),
        EMERGENCY_ALERT("응급 알림"),
        HEALTH_CHECK("건강 체크"),
        SOCIAL_REMINDER("사회 활동 알림"),
        SAFETY_CHECK("안전 확인"),
        ENCOURAGEMENT("격려 메시지"),
        ACHIEVEMENT("성취 알림"),
        SYSTEM_NOTIFICATION("시스템 알림"),
        GUARDIAN_MESSAGE("보호자 메시지");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 알림 상태 열거형
     */
    public enum NotificationStatus {
        PENDING("전송 대기"),
        SENDING("전송 중"),
        SENT("전송 완료"),
        DELIVERED("전달 완료"),
        READ("읽음"),
        ACKNOWLEDGED("확인됨"),
        FAILED("전송 실패"),
        EXPIRED("만료됨"),
        CANCELLED("취소됨");

        private final String description;

        NotificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 전송 채널 열거형
     */
    public enum DeliveryChannel {
        PUSH_NOTIFICATION("푸시 알림"),
        SMS("문자 메시지"),
        EMAIL("이메일"),
        VOICE_CALL("음성 통화"),
        IN_APP("앱 내 알림"),
        WEARABLE("웨어러블 기기");

        private final String description;

        DeliveryChannel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 (필수 필드)
     */
    public Notification(User user, NotificationType notificationType, String title, 
                       String message, LocalDateTime sendTime) {
        this.user = user;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.sendTime = sendTime;
    }

    /**
     * 스케줄 기반 알림 생성자
     */
    public Notification(User user, Schedule schedule, NotificationType notificationType, 
                       String title, String message, LocalDateTime sendTime) {
        this(user, notificationType, title, message, sendTime);
        this.schedule = schedule;
    }

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        if (this.status == NotificationStatus.DELIVERED) {
            this.status = NotificationStatus.READ;
        }
    }

    /**
     * 알림 확인 처리
     */
    public void acknowledge(String response) {
        this.isAcknowledged = true;
        this.acknowledgedAt = LocalDateTime.now();
        this.userResponse = response;
        this.status = NotificationStatus.ACKNOWLEDGED;
    }

    /**
     * 알림 전송 완료 처리
     */
    public void markAsSent() {
        this.sentAt = LocalDateTime.now();
        this.status = NotificationStatus.SENT;
    }

    /**
     * 알림 전달 완료 처리
     */
    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
    }

    /**
     * 알림 전송 실패 처리
     */
    public void markAsFailed() {
        this.status = NotificationStatus.FAILED;
        this.retryCount++;
    }

    /**
     * 알림 취소 처리
     */
    public void cancel() {
        this.status = NotificationStatus.CANCELLED;
    }

    /**
     * 알림 만료 처리
     */
    public void expire() {
        this.status = NotificationStatus.EXPIRED;
    }

    /**
     * 재시도 가능 여부 확인
     */
    public boolean canRetry() {
        return retryCount < maxRetries && 
               status == NotificationStatus.FAILED &&
               (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }

    /**
     * 높은 우선순위 여부 확인
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 3;
    }

    /**
     * 응급 알림 여부 확인
     */
    public boolean isEmergency() {
        return priority != null && priority == 4 || 
               notificationType == NotificationType.EMERGENCY_ALERT;
    }

    /**
     * 만료되었는지 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 전송 시간이 된지 확인
     */
    public boolean isTimeToSend() {
        return LocalDateTime.now().isAfter(sendTime) || LocalDateTime.now().isEqual(sendTime);
    }

    /**
     * BIF 사용자를 위한 간단한 상태 설명
     */
    public String getSimpleStatusDescription() {
        switch (status) {
            case PENDING:
                return "곧 알려드릴게요";
            case SENT:
                return "알림을 보냈어요";
            case READ:
                return "확인하셨네요";
            case ACKNOWLEDGED:
                return "잘하셨어요!";
            case FAILED:
                return "다시 시도할게요";
            case EXPIRED:
                return "시간이 지났어요";
            case CANCELLED:
                return "취소되었어요";
            default:
                return "처리 중이에요";
        }
    }

    /**
     * BIF 사용자를 위한 우선순위 설명
     */
    public String getPriorityDescription() {
        if (priority == null) return "보통";
        
        switch (priority) {
            case 1:
                return "여유로워요";
            case 2:
                return "보통이에요";
            case 3:
                return "중요해요";
            case 4:
                return "매우 중요해요!";
            default:
                return "보통이에요";
        }
    }
} 