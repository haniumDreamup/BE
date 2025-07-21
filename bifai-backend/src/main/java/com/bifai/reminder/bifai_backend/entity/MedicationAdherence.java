package com.bifai.reminder.bifai_backend.entity;

import com.bifai.reminder.bifai_backend.entity.listener.UserEntityListener;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * MedicationAdherence Entity - BIF User Medication Compliance Tracking
 * 
 * BIF 사용자의 복약 순응도를 추적하는 엔티티입니다.
 * 실제 복약 기록, 지연, 누락 등을 관리하여 복약 패턴을 분석합니다.
 */
@Entity
@Table(name = "medication_adherence", indexes = {
    @Index(name = "idx_adherence_user_date", columnList = "user_id, adherence_date"),
    @Index(name = "idx_adherence_medication", columnList = "medication_id, adherence_date"),
    @Index(name = "idx_adherence_status", columnList = "adherence_status, adherence_date"),
    @Index(name = "idx_adherence_scheduled_time", columnList = "scheduled_time, adherence_date"),
    @Index(name = "idx_adherence_reminder", columnList = "reminder_sent, guardian_notified")
})
@EntityListeners(UserEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@Comment("BIF 사용자 복약 순응도 추적")
public class MedicationAdherence extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("복약 순응도 기록 고유 식별자")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("복약한 사용자")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    @Comment("복약 대상 약물")
    private Medication medication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @Comment("관련된 일정 (있는 경우)")
    private Schedule schedule;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "adherence_date", nullable = false)
    @Comment("복약 예정/실행 날짜")
    private LocalDate adherenceDate;

    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "scheduled_time", nullable = false)
    @Comment("복약 예정 시간")
    private LocalTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "actual_taken_time")
    @Comment("실제 복약 시간")
    private LocalDateTime actualTakenTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "adherence_status", nullable = false, length = 20)
    @Comment("복약 순응 상태")
    private AdherenceStatus adherenceStatus = AdherenceStatus.SCHEDULED;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "999.999")
    @Column(name = "actual_dosage", precision = 6, scale = 3)
    @Comment("실제 복용량")
    private BigDecimal actualDosage;

    @Column(name = "dosage_unit", length = 20)
    @Comment("복용량 단위")
    private String dosageUnit;

    @Min(-1440)
    @Max(1440)
    @Column(name = "delay_minutes")
    @Comment("예정 시간 대비 지연 시간 (분, 음수는 일찍 복용)")
    private Integer delayMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 30)
    @Comment("복약 건너뛴 이유")
    private SkipReason skipReason;

    @Column(name = "skip_description", length = 500)
    @Comment("건너뛴 이유 상세 설명")
    private String skipDescription;

    @Column(name = "side_effect_reported")
    @Comment("부작용 보고 여부")
    private Boolean sideEffectReported = false;

    @Column(name = "side_effect_description", length = 1000)
    @Comment("부작용 상세 설명")
    private String sideEffectDescription;

    @Min(1)
    @Max(10)
    @Column(name = "difficulty_score")
    @Comment("복약 난이도 점수 (1: 매우 쉬움, 10: 매우 어려움)")
    private Integer difficultyScore;

    @Min(1)
    @Max(10)
    @Column(name = "satisfaction_score")
    @Comment("복약 만족도 점수 (1: 매우 불만, 10: 매우 만족)")
    private Integer satisfactionScore;

    @Column(name = "reminder_sent")
    @Comment("알림 전송 여부")
    private Boolean reminderSent = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "reminder_sent_time")
    @Comment("알림 전송 시간")
    private LocalDateTime reminderSentTime;

    @Min(0)
    @Max(10)
    @Column(name = "reminder_count")
    @Comment("전송된 알림 횟수")
    private Integer reminderCount = 0;

    @Column(name = "guardian_notified")
    @Comment("보호자 알림 여부")
    private Boolean guardianNotified = false;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "guardian_notification_time")
    @Comment("보호자 알림 시간")
    private LocalDateTime guardianNotificationTime;

    @Column(name = "confirmation_method", length = 30)
    @Comment("복약 확인 방법 (사진, 음성, 수동 입력 등)")
    private String confirmationMethod;

    @Column(name = "confirmation_image_url", length = 500)
    @Comment("복약 확인 이미지 URL")
    private String confirmationImageUrl;

    @Column(name = "location_description", length = 100)
    @Comment("복약 장소 설명")
    private String locationDescription;

    @Column(name = "taken_latitude", precision = 10, scale = 8)
    @Comment("복약 위치 위도")
    private BigDecimal latitude;

    @Column(name = "taken_longitude", precision = 11, scale = 8)
    @Comment("복약 위치 경도")
    private BigDecimal longitude;

    @Column(name = "notes", length = 1000)
    @Comment("복약 관련 메모")
    private String notes;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "recorded_at", nullable = false, updatable = false)
    @Comment("기록 생성 시간")
    private LocalDateTime recordedAt;

    @Column(name = "sync_status", length = 20)
    @Comment("동기화 상태")
    private String syncStatus = "PENDING";

    // 복약 순응 상태
    public enum AdherenceStatus {
        SCHEDULED("예정됨"),
        TAKEN("복용함"),
        TAKEN_LATE("늦게 복용"),
        TAKEN_EARLY("일찍 복용"),
        SKIPPED("건너뜀"),
        MISSED("놓침"),
        PARTIAL("부분 복용"),
        WRONG_DOSAGE("잘못된 용량"),
        CANCELLED("취소됨");

        private final String description;

        AdherenceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 건너뛴 이유
    public enum SkipReason {
        FORGOT("깜빡함"),
        SIDE_EFFECTS("부작용"),
        FEELING_BETTER("컨디션 좋음"),
        FEELING_SICK("몸이 아픔"),
        NO_MEDICATION("약이 없음"),
        AWAY_FROM_HOME("외출 중"),
        SLEEPING("잠자고 있음"),
        BUSY("바빠서"),
        INTENTIONAL("의도적으로"),
        CONFUSED("헷갈림"),
        DIFFICULTY_SWALLOWING("삼키기 어려움"),
        OTHER("기타");

        private final String description;

        SkipReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 편의 메서드
    public boolean isCompliant() {
        return adherenceStatus == AdherenceStatus.TAKEN ||
               adherenceStatus == AdherenceStatus.TAKEN_LATE ||
               adherenceStatus == AdherenceStatus.TAKEN_EARLY;
    }

    public boolean isLate() {
        return delayMinutes != null && delayMinutes > 30;
    }

    public boolean isVeryLate() {
        return delayMinutes != null && delayMinutes > 120;
    }

    public boolean needsGuardianAttention() {
        return adherenceStatus == AdherenceStatus.MISSED ||
               adherenceStatus == AdherenceStatus.SKIPPED ||
               sideEffectReported ||
               (delayMinutes != null && delayMinutes > 180);
    }

    public boolean hasProblems() {
        return sideEffectReported ||
               adherenceStatus == AdherenceStatus.WRONG_DOSAGE ||
               adherenceStatus == AdherenceStatus.PARTIAL ||
               (difficultyScore != null && difficultyScore >= 7);
    }

    public void markAsTaken() {
        this.adherenceStatus = AdherenceStatus.TAKEN;
        this.actualTakenTime = LocalDateTime.now();
        calculateDelay();
    }

    public void markAsTakenLate() {
        this.adherenceStatus = AdherenceStatus.TAKEN_LATE;
        this.actualTakenTime = LocalDateTime.now();
        calculateDelay();
    }

    public void markAsSkipped(SkipReason reason, String description) {
        this.adherenceStatus = AdherenceStatus.SKIPPED;
        this.skipReason = reason;
        this.skipDescription = description;
    }

    public void reportSideEffect(String description) {
        this.sideEffectReported = true;
        this.sideEffectDescription = description;
        this.guardianNotified = true;
        this.guardianNotificationTime = LocalDateTime.now();
    }

    private void calculateDelay() {
        if (actualTakenTime != null) {
            LocalDateTime scheduledDateTime = adherenceDate.atTime(scheduledTime);
            this.delayMinutes = (int) java.time.Duration.between(
                scheduledDateTime, actualTakenTime).toMinutes();
            
            if (delayMinutes > 30) {
                this.adherenceStatus = AdherenceStatus.TAKEN_LATE;
            } else if (delayMinutes < -30) {
                this.adherenceStatus = AdherenceStatus.TAKEN_EARLY;
            }
        }
    }

    public String getAdherenceStatusDescription() {
        if (isCompliant()) {
            if (delayMinutes != null) {
                if (delayMinutes > 30) {
                    return "늦게 복용 (" + delayMinutes + "분 지연)";
                } else if (delayMinutes < -30) {
                    return "일찍 복용 (" + Math.abs(delayMinutes) + "분 일찍)";
                }
            }
            return "정시 복용";
        }
        return adherenceStatus.getDescription();
    }
} 