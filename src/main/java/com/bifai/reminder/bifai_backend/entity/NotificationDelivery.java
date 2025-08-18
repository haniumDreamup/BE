package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 알림 전송 추적 엔티티
 * 각 채널별 알림 전송 상태와 결과를 상세하게 기록
 */
@Entity
@Table(name = "notification_deliveries", indexes = {
    @Index(name = "idx_delivery_notification_id", columnList = "notification_id"),
    @Index(name = "idx_delivery_channel", columnList = "delivery_channel"),
    @Index(name = "idx_delivery_status", columnList = "delivery_status"),
    @Index(name = "idx_delivery_attempt_time", columnList = "attempt_time"),
    @Index(name = "idx_delivery_success", columnList = "is_successful")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"notification"})
public class NotificationDelivery extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 연관된 알림
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    @NotNull
    private Notification notification;

    /**
     * 전송 채널
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel", nullable = false, length = 30)
    @NotNull
    private Notification.DeliveryChannel deliveryChannel;

    /**
     * 전송 시도 시간
     */
    @Column(name = "attempt_time", nullable = false)
    @NotNull
    private LocalDateTime attemptTime;

    /**
     * 전송 완료 시간
     */
    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    /**
     * 전송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private DeliveryStatus deliveryStatus = DeliveryStatus.ATTEMPTING;

    /**
     * 전송 성공 여부
     */
    @Column(name = "is_successful", nullable = false)
    private Boolean isSuccessful = false;

    /**
     * 시도 횟수
     */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    /**
     * 외부 서비스 응답 코드 (HTTP 상태 코드 등)
     */
    @Column(name = "response_code")
    private String responseCode;

    /**
     * 외부 서비스 응답 메시지
     */
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    /**
     * 오류 메시지 (실패 시)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 오류 코드
     */
    @Column(name = "error_code", length = 50)
    private String errorCode;

    /**
     * 수신자 정보 (전화번호, 이메일 등)
     */
    @Column(name = "recipient_address", length = 255)
    private String recipientAddress;

    /**
     * 외부 서비스 메시지 ID (푸시 알림 ID, SMS ID 등)
     */
    @Column(name = "external_message_id", length = 255)
    private String externalMessageId;

    /**
     * 전송 제공업체 (FCM, SMS 게이트웨이 등)
     */
    @Column(name = "provider", length = 100)
    private String provider;

    /**
     * 처리 시간 (밀리초)
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * 비용 (필요시)
     */
    @Column(name = "delivery_cost", precision = 10)
    private java.math.BigDecimal deliveryCost;

    /**
     * BIF 사용자의 기기 응답 여부
     */
    @Column(name = "device_acknowledged")
    private Boolean deviceAcknowledged = false;

    /**
     * 기기 응답 시간
     */
    @Column(name = "device_acknowledged_at")
    private LocalDateTime deviceAcknowledgedAt;

    /**
     * 재시도 예정 시간
     */
    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    /**
     * 우선순위 처리 여부
     */
    @Column(name = "priority_delivery")
    private Boolean priorityDelivery = false;

    /**
     * 메타데이터 (JSON 형태로 추가 정보 저장)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 전송 상태 열거형
     */
    public enum DeliveryStatus {
        ATTEMPTING("전송 시도 중"),
        SENT("전송 완료"),
        DELIVERED("전달 확인"),
        FAILED("전송 실패"),
        EXPIRED("만료됨"),
        CANCELLED("취소됨"),
        DEFERRED("지연됨"),
        PENDING_RETRY("재시도 대기");

        private final String description;

        DeliveryStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 (필수 필드)
     */
    public NotificationDelivery(Notification notification, 
                               Notification.DeliveryChannel deliveryChannel, 
                               LocalDateTime attemptTime) {
        this.notification = notification;
        this.deliveryChannel = deliveryChannel;
        this.attemptTime = attemptTime;
    }

    /**
     * 전송 성공 처리
     */
    public void markAsSuccessful(String responseCode, String responseMessage, String externalMessageId) {
        this.isSuccessful = true;
        this.deliveryStatus = DeliveryStatus.SENT;
        this.completedTime = LocalDateTime.now();
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.externalMessageId = externalMessageId;
        
        calculateProcessingTime();
    }

    /**
     * 전달 확인 처리
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        if (this.completedTime == null) {
            this.completedTime = LocalDateTime.now();
        }
    }

    /**
     * 전송 실패 처리
     */
    public void markAsFailed(String errorCode, String errorMessage) {
        this.isSuccessful = false;
        this.deliveryStatus = DeliveryStatus.FAILED;
        this.completedTime = LocalDateTime.now();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        
        calculateProcessingTime();
    }

    /**
     * 전송 취소 처리
     */
    public void cancel() {
        this.deliveryStatus = DeliveryStatus.CANCELLED;
        this.completedTime = LocalDateTime.now();
        
        calculateProcessingTime();
    }

    /**
     * 만료 처리
     */
    public void expire() {
        this.deliveryStatus = DeliveryStatus.EXPIRED;
        this.completedTime = LocalDateTime.now();
        
        calculateProcessingTime();
    }

    /**
     * 재시도 준비
     */
    public void prepareForRetry(LocalDateTime nextRetryTime) {
        this.attemptCount++;
        this.nextRetryTime = nextRetryTime;
        this.deliveryStatus = DeliveryStatus.PENDING_RETRY;
        this.errorMessage = null;
        this.errorCode = null;
    }

    /**
     * 기기 확인 처리 (BIF 사용자가 알림을 확인했을 때)
     */
    public void acknowledgeByDevice() {
        this.deviceAcknowledged = true;
        this.deviceAcknowledgedAt = LocalDateTime.now();
    }

    /**
     * 처리 시간 계산
     */
    private void calculateProcessingTime() {
        if (attemptTime != null && completedTime != null) {
            this.processingTimeMs = java.time.Duration.between(attemptTime, completedTime).toMillis();
        }
    }

    /**
     * 재시도 가능한지 확인
     */
    public boolean canRetry() {
        return deliveryStatus == DeliveryStatus.FAILED || deliveryStatus == DeliveryStatus.PENDING_RETRY;
    }

    /**
     * 재시도 시간이 되었는지 확인
     */
    public boolean isRetryTime() {
        return nextRetryTime != null && LocalDateTime.now().isAfter(nextRetryTime);
    }

    /**
     * 완료된 상태인지 확인
     */
    public boolean isCompleted() {
        return deliveryStatus == DeliveryStatus.SENT || 
               deliveryStatus == DeliveryStatus.DELIVERED ||
               deliveryStatus == DeliveryStatus.FAILED ||
               deliveryStatus == DeliveryStatus.CANCELLED ||
               deliveryStatus == DeliveryStatus.EXPIRED;
    }

    /**
     * BIF 사용자를 위한 간단한 상태 설명
     */
    public String getSimpleStatusDescription() {
        switch (deliveryStatus) {
            case ATTEMPTING:
                return "보내는 중이에요";
            case SENT:
                return "성공적으로 보냈어요";
            case DELIVERED:
                return "전달되었어요";
            case FAILED:
                return "보내지 못했어요";
            case EXPIRED:
                return "시간이 지났어요";
            case CANCELLED:
                return "취소되었어요";
            case DEFERRED:
                return "잠시 기다려주세요";
            case PENDING_RETRY:
                return "다시 시도할게요";
            default:
                return "처리 중이에요";
        }
    }

    /**
     * 전송 채널에 따른 설명
     */
    public String getChannelDescription() {
        switch (deliveryChannel) {
            case PUSH_NOTIFICATION:
                return "앱 알림으로";
            case SMS:
                return "문자 메시지로";
            case EMAIL:
                return "이메일로";
            case VOICE_CALL:
                return "전화로";
            case IN_APP:
                return "앱에서";
            case WEARABLE:
                return "웨어러블 기기로";
            default:
                return "알림으로";
        }
    }

    /**
     * 전송 결과 요약
     */
    public String getDeliverySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(getChannelDescription());
        summary.append(" ");
        summary.append(getSimpleStatusDescription());
        
        if (attemptCount > 1) {
            summary.append(" (").append(attemptCount).append("번째 시도)");
        }
        
        return summary.toString();
    }

    /**
     * 전송 성공률 계산을 위한 헬퍼 메서드
     */
    public boolean isSuccessfulDelivery() {
        return isSuccessful && (deliveryStatus == DeliveryStatus.SENT || deliveryStatus == DeliveryStatus.DELIVERED);
    }
} 