package com.bifai.reminder.bifai_backend.entity;

import com.bifai.reminder.bifai_backend.entity.listener.UserEntityListener;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * HealthMetric Entity - BIF User Health Indicator Tracking
 * 
 * BIF 사용자의 건강 지표를 추적하는 엔티티입니다.
 * 복잡한 의학 용어 대신 이해하기 쉬운 설명과 간단한 입력 방식을 제공합니다.
 */
@Entity
@Table(name = "health_metrics", indexes = {
    @Index(name = "idx_health_user_date", columnList = "user_id, measured_at"),
    @Index(name = "idx_health_metric_type", columnList = "metric_type, measured_at"),
    @Index(name = "idx_health_alert_level", columnList = "alert_level, measured_at"),
    @Index(name = "idx_health_device", columnList = "device_id, measured_at"),
    @Index(name = "idx_health_status", columnList = "measurement_status, measured_at")
})
@EntityListeners(UserEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@Comment("BIF 사용자 건강 지표 추적")
public class HealthMetric extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("건강 지표 고유 식별자")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("건강 지표를 측정한 사용자")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @Comment("측정에 사용된 디바이스")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 30)
    @Comment("건강 지표 유형")
    private MetricType metricType;

    @NotNull
    @DecimalMin(value = "0.0")
    @Digits(integer = 8, fraction = 3)
    @Column(name = "metric_value", nullable = false, precision = 11)
    @Comment("측정값")
    private BigDecimal value;

    @Size(max = 20)
    @Column(name = "unit", length = 20)
    @Comment("측정 단위")
    private String unit;

    @DecimalMin(value = "0.0")
    @Digits(integer = 8, fraction = 3)
    @Column(name = "secondary_value", precision = 11)
    @Comment("보조 측정값 (혈압의 이완기 혈압 등)")
    private BigDecimal secondaryValue;

    @Size(max = 20)
    @Column(name = "secondary_unit", length = 20)
    @Comment("보조 측정값 단위")
    private String secondaryUnit;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "measured_at", nullable = false)
    @Comment("측정 시간")
    private LocalDateTime measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_status", nullable = false, length = 20)
    @Comment("측정 상태")
    private MeasurementStatus measurementStatus = MeasurementStatus.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_level", length = 15)
    @Comment("경고 수준")
    private AlertLevel alertLevel = AlertLevel.NORMAL;

    @Column(name = "reference_min", precision = 8)
    @Comment("정상 범위 최솟값")
    private BigDecimal referenceMin;

    @Column(name = "reference_max", precision = 8)
    @Comment("정상 범위 최댓값")
    private BigDecimal referenceMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_method", length = 20)
    @Comment("측정 방법")
    private MeasurementMethod measurementMethod;

    @Size(max = 100)
    @Column(name = "measurement_device", length = 100)
    @Comment("측정 기기명")
    private String measurementDevice;

    @Enumerated(EnumType.STRING)
    @Column(name = "timing_context", length = 20)
    @Comment("측정 시점 맥락")
    private TimingContext timingContext;

    @Size(max = 500)
    @Column(name = "context_description", length = 500)
    @Comment("측정 상황 설명")
    private String contextDescription;

    @Min(1)
    @Max(10)
    @Column(name = "subjective_feeling")
    @Comment("주관적 컨디션 점수 (1: 매우 나쁨, 10: 매우 좋음)")
    private Integer subjectiveFeeling;

    @Size(max = 1000)
    @Column(name = "symptoms", length = 1000)
    @Comment("관련 증상 (쉬운 설명)")
    private String symptoms;

    @Column(name = "guardian_notified")
    @Comment("보호자 알림 여부")
    private Boolean guardianNotified = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "guardian_notification_time")
    @Comment("보호자 알림 시간")
    private LocalDateTime guardianNotificationTime;

    @Column(name = "doctor_consultation_needed")
    @Comment("의사 상담 필요 여부")
    private Boolean doctorConsultationNeeded = false;

    @Size(max = 1000)
    @Column(name = "notes", length = 1000)
    @Comment("추가 메모")
    private String notes;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    @Comment("측정 결과 이미지 URL")
    private String imageUrl;

    @Column(name = "measurement_latitude", precision = 10)
    @Comment("측정 위치 위도")
    private BigDecimal measurementLatitude;
    
    @Column(name = "measurement_longitude", precision = 11)
    @Comment("측정 위치 경도")
    private BigDecimal measurementLongitude;

    @Column(name = "sync_status", length = 20)
    @Comment("동기화 상태")
    private String syncStatus = "PENDING";

    // 건강 지표 유형
    public enum MetricType {
        BLOOD_PRESSURE("혈압"),
        BLOOD_SUGAR("혈당"),
        WEIGHT("체중"),
        HEIGHT("키"),
        HEART_RATE("심박수"),
        BODY_TEMPERATURE("체온"),
        OXYGEN_SATURATION("산소포화도"),
        SLEEP_DURATION("수면시간"),
        STEPS("걸음수"),
        CALORIES_BURNED("소모 칼로리"),
        WATER_INTAKE("수분 섭취량"),
        MOOD_SCORE("기분 점수"),
        ENERGY_LEVEL("에너지 레벨"),
        STRESS_LEVEL("스트레스 레벨"),
        PAIN_LEVEL("통증 레벨"),
        MEDICATION_COMPLIANCE("복약 순응도"),
        EXERCISE_MINUTES("운동 시간"),
        BMI("체질량 지수"),
        WAIST_CIRCUMFERENCE("허리둘레"),
        OTHER("기타");

        private final String description;

        MetricType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 측정 상태
    public enum MeasurementStatus {
        COMPLETED("측정 완료"),
        PARTIAL("부분 측정"),
        FAILED("측정 실패"),
        ESTIMATED("추정값"),
        MANUAL_INPUT("수동 입력"),
        DEVICE_ERROR("기기 오류");

        private final String description;

        MeasurementStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 경고 수준
    public enum AlertLevel {
        CRITICAL("위험"),
        HIGH("주의"),
        NORMAL("정상"),
        LOW("낮음"),
        UNKNOWN("알 수 없음");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 측정 방법
    public enum MeasurementMethod {
        AUTOMATIC_DEVICE("자동 측정기"),
        MANUAL_DEVICE("수동 측정기"),
        SMARTPHONE_APP("스마트폰 앱"),
        WEARABLE_DEVICE("웨어러블 기기"),
        MANUAL_INPUT("직접 입력"),
        VOICE_INPUT("음성 입력"),
        PHOTO_RECOGNITION("사진 인식"),
        OTHER("기타");

        private final String description;

        MeasurementMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 측정 시점 맥락
    public enum TimingContext {
        MORNING("아침"),
        AFTERNOON("오후"),
        EVENING("저녁"),
        BEFORE_MEAL("식사 전"),
        AFTER_MEAL("식사 후"),
        BEFORE_EXERCISE("운동 전"),
        AFTER_EXERCISE("운동 후"),
        BEFORE_MEDICATION("복약 전"),
        AFTER_MEDICATION("복약 후"),
        BEDTIME("잠자리"),
        WAKEUP("기상 후"),
        STRESS_SITUATION("스트레스 상황"),
        ROUTINE_CHECK("정기 체크"),
        FEELING_UNWELL("몸이 안 좋을 때"),
        OTHER("기타");

        private final String description;

        TimingContext(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 편의 메서드
    public boolean isInNormalRange() {
        if (referenceMin == null || referenceMax == null) {
            return alertLevel == AlertLevel.NORMAL;
        }
        return value.compareTo(referenceMin) >= 0 && 
               value.compareTo(referenceMax) <= 0;
    }

    public boolean needsAttention() {
        return alertLevel == AlertLevel.CRITICAL || 
               alertLevel == AlertLevel.HIGH ||
               doctorConsultationNeeded;
    }

    public boolean needsGuardianNotification() {
        return needsAttention() || 
               (subjectiveFeeling != null && subjectiveFeeling <= 3);
    }

    public String getValueWithUnit() {
        String result = value.toString();
        if (unit != null) {
            result += " " + unit;
        }
        if (secondaryValue != null) {
            result += " / " + secondaryValue.toString();
            if (secondaryUnit != null) {
                result += " " + secondaryUnit;
            }
        }
        return result;
    }

    public void evaluateAlertLevel() {
        if (referenceMin == null || referenceMax == null) {
            this.alertLevel = AlertLevel.UNKNOWN;
            return;
        }

        if (value.compareTo(referenceMin) < 0) {
            this.alertLevel = AlertLevel.LOW;
        } else if (value.compareTo(referenceMax) > 0) {
            BigDecimal criticalThreshold = referenceMax.multiply(new BigDecimal("1.2"));
            if (value.compareTo(criticalThreshold) > 0) {
                this.alertLevel = AlertLevel.CRITICAL;
                this.doctorConsultationNeeded = true;
            } else {
                this.alertLevel = AlertLevel.HIGH;
            }
        } else {
            this.alertLevel = AlertLevel.NORMAL;
        }

        if (needsGuardianNotification() && !guardianNotified) {
            this.guardianNotified = true;
            this.guardianNotificationTime = LocalDateTime.now();
        }
    }

    public String getSimpleInterpretation() {
        switch (alertLevel) {
            case CRITICAL:
                return "매우 위험한 수치입니다. 즉시 병원에 가세요.";
            case HIGH:
                return "주의가 필요한 수치입니다. 의사와 상담하세요.";
            case NORMAL:
                return "정상 범위입니다.";
            case LOW:
                return "평소보다 낮은 수치입니다.";
            default:
                return "측정 결과를 확인해 주세요.";
        }
    }

    public void markAsCompleted() {
        this.measurementStatus = MeasurementStatus.COMPLETED;
        this.syncStatus = "COMPLETED";
        evaluateAlertLevel();
    }
} 