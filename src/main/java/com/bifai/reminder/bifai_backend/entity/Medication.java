package com.bifai.reminder.bifai_backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Medication Entity - BIF User Medication Management
 * 
 * BIF 사용자를 위한 복약 관리 엔티티입니다.
 * 복잡한 의학 용어 대신 이해하기 쉬운 설명과 시각적 정보를 제공합니다.
 */
@Entity
@Table(name = "medications", indexes = {
    @Index(name = "idx_medication_user_active_priority", columnList = "user_id, is_active, priority_level DESC"),
    @Index(name = "idx_medication_user_status", columnList = "user_id, medication_status, priority_level DESC"),
    @Index(name = "idx_medication_user_type", columnList = "user_id, medication_type, is_active"),
    @Index(name = "idx_medication_dates", columnList = "start_date, end_date, is_active"),
    @Index(name = "idx_medication_guardian_alert", columnList = "guardian_alert_needed, priority_level, user_id"),
    @Index(name = "idx_medication_name_search", columnList = "medication_name, generic_name")
})
@Getter
@Setter
@NoArgsConstructor
@Comment("BIF 사용자 복약 관리")
public class Medication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("복약 정보 고유 식별자")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("복약하는 사용자")
    private User user;

    @NotBlank
    @Size(max = 100)
    @Column(name = "medication_name", nullable = false, length = 100)
    @Comment("약 이름 (상품명)")
    private String medicationName;

    @Size(max = 100)
    @Column(name = "generic_name", length = 100)
    @Comment("일반명 (성분명)")
    private String genericName;

    @Size(max = 500)
    @Column(name = "simple_description", length = 500)
    @Comment("약에 대한 쉬운 설명 (5학년 수준)")
    private String simpleDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "medication_type", nullable = false, length = 30)
    @Comment("약물 유형")
    private MedicationType medicationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dosage_form", nullable = false, length = 20)
    @Comment("제형 (알약, 물약 등)")
    private DosageForm dosageForm;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 3)
    @Column(name = "dosage_amount", nullable = false, precision = 11)
    @Comment("1회 복용량")
    private BigDecimal dosageAmount;

    @NotBlank
    @Size(max = 20)
    @Column(name = "dosage_unit", nullable = false, length = 20)
    @Comment("복용량 단위 (정, 밀리리터 등)")
    private String dosageUnit;

    @NotNull
    @Min(1)
    @Max(24)
    @Column(name = "daily_frequency", nullable = false)
    @Comment("하루 복용 횟수")
    private Integer dailyFrequency;

    @ElementCollection
    @CollectionTable(
        name = "medication_times",
        joinColumns = @JoinColumn(name = "medication_id"),
        indexes = @Index(name = "idx_medication_times", columnList = "medication_id, intake_time")
    )
    @Column(name = "intake_time")
    @Comment("복용 시간들")
    private List<LocalTime> intakeTimes;

    @Enumerated(EnumType.STRING)
    @Column(name = "timing_instruction", length = 20)
    @Comment("복용 시점 (식전, 식후, 취침전 등)")
    private TimingInstruction timingInstruction;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "start_date", nullable = false)
    @Comment("복용 시작일")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "end_date")
    @Comment("복용 종료일 (처방 기간)")
    private LocalDate endDate;

    @Min(0)
    @Column(name = "total_days")
    @Comment("총 복용 일수")
    private Integer totalDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 15)
    @Comment("우선순위 레벨")
    private PriorityLevel priorityLevel = PriorityLevel.MEDIUM;

    @Column(name = "is_active", nullable = false)
    @Comment("현재 복용 중 여부")
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "medication_status", nullable = false, length = 20)
    @Comment("복약 상태")
    private MedicationStatus medicationStatus = MedicationStatus.ACTIVE;

    @Size(max = 100)
    @Column(name = "prescribing_doctor", length = 100)
    @Comment("처방의사")
    private String prescribingDoctor;

    @Size(max = 100)
    @Column(name = "pharmacy_name", length = 100)
    @Comment("조제 약국")
    private String pharmacyName;

    @Size(max = 20)
    @Column(name = "prescription_number", length = 20)
    @Comment("처방전 번호")
    private String prescriptionNumber;

    @Column(name = "pill_color", length = 20)
    @Comment("약 색깔 (시각적 식별)")
    private String pillColor;

    @Column(name = "pill_shape", length = 20)
    @Comment("약 모양 (원형, 타원형 등)")
    private String pillShape;

    @Column(name = "pill_image_url", length = 500)
    @Comment("약 이미지 URL (시각적 식별)")
    private String pillImageUrl;

    @Size(max = 1000)
    @Column(name = "side_effects", length = 1000)
    @Comment("부작용 정보 (쉬운 설명)")
    private String sideEffects;

    @Size(max = 1000)
    @Column(name = "important_notes", length = 1000)
    @Comment("중요한 주의사항 (쉬운 설명)")
    private String importantNotes;

    @Size(max = 500)
    @Column(name = "storage_instructions", length = 500)
    @Comment("보관 방법")
    private String storageInstructions;

    @Column(name = "requires_food")
    @Comment("음식과 함께 복용 필요 여부")
    private Boolean requiresFood;

    @Column(name = "avoid_alcohol")
    @Comment("알코올 금지 여부")
    private Boolean avoidAlcohol = false;

    @Column(name = "guardian_alert_needed")
    @Comment("보호자 알림 필요 여부")
    private Boolean guardianAlertNeeded = false;

    @Size(max = 1000)
    @Column(name = "user_notes", length = 1000)
    @Comment("사용자 메모")
    private String userNotes;

    // 약물 유형
    public enum MedicationType {
        CHRONIC_DISEASE("만성질환 약"),
        PAIN_RELIEF("진통제"),
        ANTIBIOTIC("항생제"),
        VITAMIN("비타민"),
        SUPPLEMENT("영양제"),
        MENTAL_HEALTH("정신건강 약"),
        HEART_MEDICATION("심장 약"),
        BLOOD_PRESSURE("혈압 약"),
        DIABETES("당뇨 약"),
        DIGESTIVE("소화제"),
        SLEEP_AID("수면제"),
        ALLERGY("알레르기 약"),
        COLD_FLU("감기약"),
        OTHER("기타");

        private final String description;

        MedicationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 제형
    public enum DosageForm {
        TABLET("알약"),
        CAPSULE("캡슐"),
        LIQUID("물약"),
        POWDER("가루약"),
        INJECTION("주사"),
        PATCH("패치"),
        CREAM("연고"),
        DROP("안약/코약"),
        SPRAY("스프레이"),
        OTHER("기타");

        private final String description;

        DosageForm(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 복용 시점
    public enum TimingInstruction {
        BEFORE_MEAL("식사 30분 전"),
        WITH_MEAL("식사와 함께"),
        AFTER_MEAL("식사 30분 후"),
        EMPTY_STOMACH("공복"),
        BEDTIME("잠자기 전"),
        MORNING("아침"),
        EVENING("저녁"),
        AS_NEEDED("필요시"),
        EVERY_HOUR("매 시간"),
        OTHER("기타");

        private final String description;

        TimingInstruction(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 우선순위 레벨
    public enum PriorityLevel {
        CRITICAL("매우 중요"),
        HIGH("중요"),
        MEDIUM("보통"),
        LOW("낮음");

        private final String description;

        PriorityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 복약 상태
    public enum MedicationStatus {
        ACTIVE("복용 중"),
        PAUSED("일시 중단"),
        COMPLETED("복용 완료"),
        DISCONTINUED("중단됨"),
        EXPIRED("기간 만료"),
        OUT_OF_STOCK("재고 없음");

        private final String description;

        MedicationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 편의 메서드
    public boolean isCurrentlyActive() {
        return isActive && medicationStatus == MedicationStatus.ACTIVE;
    }

    public boolean needsGuardianNotification() {
        return guardianAlertNeeded || priorityLevel == PriorityLevel.CRITICAL;
    }

    public boolean isExpiringSoon() {
        if (endDate == null) return false;
        return LocalDate.now().plusDays(3).isAfter(endDate);
    }

    public String getSimpleScheduleDescription() {
        if (dailyFrequency == 1) {
            return "하루에 한 번";
        } else if (dailyFrequency == 2) {
            return "하루에 두 번";
        } else if (dailyFrequency == 3) {
            return "하루에 세 번";
        } else {
            return "하루에 " + dailyFrequency + "번";
        }
    }

    public void markAsCompleted() {
        this.medicationStatus = MedicationStatus.COMPLETED;
        this.isActive = false;
    }

    public void pause() {
        this.medicationStatus = MedicationStatus.PAUSED;
    }

    public void resume() {
        this.medicationStatus = MedicationStatus.ACTIVE;
    }
} 