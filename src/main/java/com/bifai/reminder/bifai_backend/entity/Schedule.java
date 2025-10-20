package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * BIF 사용자를 위한 스케줄 관리 엔티티
 * 복잡한 Cron 표현식 대신 사용자 친화적인 반복 패턴 제공
 */
@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedule_user_active_next", columnList = "user_id, is_active, next_execution_time"),
    @Index(name = "idx_schedule_user_type_active", columnList = "user_id, schedule_type, is_active"),
    @Index(name = "idx_schedule_active_next", columnList = "is_active, next_execution_time"),
    @Index(name = "idx_schedule_user_priority", columnList = "user_id, priority DESC, next_execution_time"),
    @Index(name = "idx_schedule_creator_active", columnList = "created_by_type, is_active, user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
public class Schedule extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스케줄을 소유한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    /**
     * BIF 사용자를 위한 간단한 제목
     */
    @Column(name = "title", nullable = false, length = 100)
    @NotBlank
    private String title;

    /**
     * 상세 설명 (선택사항)
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 스케줄 유형 (약물, 운동, 식사 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 50)
    @NotNull
    private ScheduleType scheduleType;

    /**
     * 반복 패턴 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 30)
    @NotNull
    private RecurrenceType recurrenceType;

    /**
     * 실행 시간
     */
    @Column(name = "execution_time", nullable = false)
    @NotNull
    private LocalTime executionTime;

    /**
     * 다음 실행 예정 시간 (계산된 값)
     */
    @Column(name = "next_execution_time")
    private LocalDateTime nextExecutionTime;

    /**
     * 마지막 실행 시간
     */
    @Column(name = "last_execution_time")
    private LocalDateTime lastExecutionTime;

    /**
     * 주간 반복의 경우 요일 선택 (비트마스크 또는 JSON)
     */
    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "schedule_days", joinColumns = @JoinColumn(name = "schedule_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> selectedDays;

    /**
     * 월간 반복의 경우 날짜 (1-31)
     */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /**
     * 반복 간격 (매 N일, 매 N주 등)
     */
    @Column(name = "interval_value")
    @Builder.Default
    private Integer intervalValue = 1;

    /**
     * 스케줄 시작 날짜
     */
    @Column(name = "start_date", nullable = false)
    @NotNull
    private LocalDateTime startDate;

    /**
     * 스케줄 종료 날짜 (선택사항)
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * 스케줄 활성화 상태
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 우선순위 (1=낮음, 2=보통, 3=높음, 4=매우 높음)
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 2;

    /**
     * BIF 사용자를 위한 아이콘 또는 색상 코드
     */
    @Column(name = "visual_indicator", length = 50)
    private String visualIndicator;

    /**
     * 미리 알림 시간 (분 단위)
     */
    @Column(name = "reminder_minutes_before")
    private Integer reminderMinutesBefore;

    /**
     * 완료 확인 필요 여부
     */
    @Column(name = "requires_confirmation")
    @Builder.Default
    private Boolean requiresConfirmation = false;

    /**
     * 스케줄 생성자 (사용자, 보호자, 시스템)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", length = 20)
    @Builder.Default
    private CreatorType createdByType = CreatorType.USER;

    /**
     * 스케줄 타입 열거형
     */
    public enum ScheduleType {
        MEDICATION("약물 복용"),
        MEAL("식사"),
        EXERCISE("운동"),
        APPOINTMENT("약속"),
        REMINDER("일반 알림"),
        EMERGENCY_CHECK("응급상황 확인"),
        HEALTH_CHECK("건강 체크"),
        SOCIAL_ACTIVITY("사회 활동"),
        PERSONAL_CARE("개인 관리");

        private final String description;

        ScheduleType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 반복 유형 열거형
     */
    public enum RecurrenceType {
        ONCE("한 번만"),
        DAILY("매일"),
        WEEKLY("매주"),
        MONTHLY("매월"),
        CUSTOM_DAYS("요일 선택"),
        INTERVAL_DAYS("N일마다"),
        INTERVAL_WEEKS("N주마다");

        private final String description;

        RecurrenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 타입 열거형
     */
    public enum CreatorType {
        USER("사용자"),
        GUARDIAN("보호자"),
        SYSTEM("시스템");

        private final String description;

        CreatorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 (필수 필드)
     */
    public Schedule(User user, String title, ScheduleType scheduleType, 
                   RecurrenceType recurrenceType, LocalTime executionTime, LocalDateTime startDate) {
        this.user = user;
        this.title = title;
        this.scheduleType = scheduleType;
        this.recurrenceType = recurrenceType;
        this.executionTime = executionTime;
        this.startDate = startDate;
    }

    /**
     * 스케줄 활성화/비활성화
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    /**
     * 스케줄 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 다음 실행 시간 계산 및 설정
     */
    public void calculateNextExecution() {
        if (!isActive || (endDate != null && LocalDateTime.now().isAfter(endDate))) {
            this.nextExecutionTime = null;
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution = null;

        switch (recurrenceType) {
            case ONCE:
                if (lastExecutionTime == null && startDate.isAfter(now)) {
                    nextExecution = startDate.with(executionTime);
                }
                break;
            case DAILY:
                nextExecution = calculateDailyNext(now);
                break;
            case WEEKLY:
                nextExecution = calculateWeeklyNext(now);
                break;
            case MONTHLY:
                nextExecution = calculateMonthlyNext(now);
                break;
            case CUSTOM_DAYS:
                nextExecution = calculateCustomDaysNext(now);
                break;
            case INTERVAL_DAYS:
                nextExecution = calculateIntervalDaysNext(now);
                break;
            case INTERVAL_WEEKS:
                nextExecution = calculateIntervalWeeksNext(now);
                break;
        }

        this.nextExecutionTime = nextExecution;
    }

    private LocalDateTime calculateDailyNext(LocalDateTime now) {
        LocalDateTime candidate = now.toLocalDate().atTime(executionTime);
        if (candidate.isBefore(now) || candidate.isEqual(now)) {
            candidate = candidate.plusDays(intervalValue != null ? intervalValue : 1);
        }
        return candidate;
    }

    private LocalDateTime calculateWeeklyNext(LocalDateTime now) {
        // 주간 반복 로직 구현
        LocalDateTime candidate = now.toLocalDate().atTime(executionTime);
        if (candidate.isBefore(now) || candidate.isEqual(now)) {
            candidate = candidate.plusWeeks(intervalValue != null ? intervalValue : 1);
        }
        return candidate;
    }

    private LocalDateTime calculateMonthlyNext(LocalDateTime now) {
        // 월간 반복 로직 구현
        if (dayOfMonth == null) return null;
        
        LocalDateTime candidate = now.toLocalDate().withDayOfMonth(dayOfMonth).atTime(executionTime);
        if (candidate.isBefore(now) || candidate.isEqual(now)) {
            candidate = candidate.plusMonths(intervalValue != null ? intervalValue : 1);
        }
        return candidate;
    }

    private LocalDateTime calculateCustomDaysNext(LocalDateTime now) {
        // 사용자 정의 요일 반복 로직 구현
        if (selectedDays == null || selectedDays.isEmpty()) return null;
        
        LocalDateTime candidate = now.toLocalDate().atTime(executionTime);
        for (int i = 0; i < 7; i++) {
            if (selectedDays.contains(candidate.getDayOfWeek()) && 
                (candidate.isAfter(now) || candidate.isEqual(now))) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        return null;
    }

    private LocalDateTime calculateIntervalDaysNext(LocalDateTime now) {
        if (lastExecutionTime == null) {
            return startDate.with(executionTime);
        }
        return lastExecutionTime.plusDays(intervalValue != null ? intervalValue : 1);
    }

    private LocalDateTime calculateIntervalWeeksNext(LocalDateTime now) {
        if (lastExecutionTime == null) {
            return startDate.with(executionTime);
        }
        return lastExecutionTime.plusWeeks(intervalValue != null ? intervalValue : 1);
    }

    /**
     * 실행 완료 처리
     */
    public void markExecuted() {
        this.lastExecutionTime = LocalDateTime.now();
        calculateNextExecution();
    }

    /**
     * 높은 우선순위 여부 확인
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 3;
    }

    /**
     * 곧 실행될 스케줄인지 확인 (다음 1시간 이내)
     */
    public boolean isDueSoon() {
        return nextExecutionTime != null && 
               nextExecutionTime.isBefore(LocalDateTime.now().plusHours(1));
    }

    /**
     * BIF 사용자를 위한 간단한 설명 생성
     */
    public String getSimpleDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(scheduleType.getDescription());
        desc.append(" - ");
        desc.append(executionTime.toString());
        
        switch (recurrenceType) {
            case DAILY:
                desc.append(" (매일)");
                break;
            case WEEKLY:
                desc.append(" (매주)");
                break;
            case CUSTOM_DAYS:
                if (selectedDays != null && !selectedDays.isEmpty()) {
                    desc.append(" (").append(String.join(", ", 
                        selectedDays.stream().map(Enum::name).toArray(String[]::new))).append(")");
                }
                break;
        }
        
        return desc.toString();
    }
} 