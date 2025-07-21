package com.bifai.reminder.bifai_backend.entity;

import com.bifai.reminder.bifai_backend.entity.listener.UserEntityListener;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * ActivityLog Entity - Event Sourcing Pattern for BIF User Activity Tracking
 * 
 * BIF 사용자의 모든 활동을 추적하는 엔티티입니다.
 * 인지 부담을 줄이기 위해 단순한 활동 분류와 쉬운 인터페이스를 제공합니다.
 */
@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_activity_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_activity_type_created", columnList = "activity_type, created_at"),
    @Index(name = "idx_activity_date_type", columnList = "activity_date, activity_type"),
    @Index(name = "idx_activity_success_status", columnList = "success_status, created_at"),
    @Index(name = "idx_activity_device", columnList = "device_id, created_at")
})
@EntityListeners(UserEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@Comment("BIF 사용자 활동 로그 - Event Sourcing Pattern")
public class ActivityLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("활동 로그 고유 식별자")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("활동을 수행한 사용자")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    @Comment("활동을 기록한 디바이스")
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    @Comment("BIF 친화적 활동 유형")
    private ActivityType activityType;

    @Column(name = "activity_title", nullable = false, length = 100)
    @Comment("활동 제목 (단순하고 이해하기 쉬운 설명)")
    private String activityTitle;

    @Column(name = "activity_description", length = 500)
    @Comment("활동 상세 설명 (5학년 수준 언어)")
    private String activityDescription;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "activity_date", nullable = false)
    @Comment("실제 활동 발생 시간")
    private LocalDateTime activityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "success_status", nullable = false, length = 20)
    @Comment("활동 성공 여부")
    private SuccessStatus successStatus;

    @Column(name = "duration_minutes")
    @Comment("활동 지속 시간 (분)")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20)
    @Comment("활동 난이도 레벨")
    private DifficultyLevel difficultyLevel;

    @Column(name = "mood_before", length = 20)
    @Comment("활동 전 기분 상태")
    private String moodBefore;

    @Column(name = "mood_after", length = 20)
    @Comment("활동 후 기분 상태")
    private String moodAfter;

    @Column(name = "confidence_score")
    @Comment("활동에 대한 자신감 점수 (1-10)")
    private Integer confidenceScore;

    @Column(name = "help_needed")
    @Comment("도움이 필요했는지 여부")
    private Boolean helpNeeded = false;

    @Column(name = "guardian_notified")
    @Comment("보호자에게 알림이 전송되었는지 여부")
    private Boolean guardianNotified = false;

    @ElementCollection
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 1000)
    @CollectionTable(
        name = "activity_metadata",
        joinColumns = @JoinColumn(name = "activity_log_id"),
        indexes = @Index(name = "idx_activity_metadata_key", columnList = "metadata_key")
    )
    @Comment("활동 관련 추가 메타데이터 (JSON 형태)")
    private Map<String, String> metadata;

    @Column(name = "location_description", length = 100)
    @Comment("활동 장소 설명 (간단한 텍스트)")
    private String locationDescription;

    @Column(name = "activity_latitude", precision = 10, scale = 8)
    @Comment("활동 위치 위도")
    private BigDecimal latitude;

    @Column(name = "activity_longitude", precision = 11, scale = 8)
    @Comment("활동 위치 경도")
    private BigDecimal longitude;

    @Column(name = "notes", length = 1000)
    @Comment("사용자가 추가한 메모")
    private String notes;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "logged_at", nullable = false, updatable = false)
    @Comment("로그 기록 시간")
    private LocalDateTime loggedAt;

    @Column(name = "sync_status", length = 20)
    @Comment("데이터 동기화 상태")
    private String syncStatus = "PENDING";

    // BIF 친화적 활동 유형
    public enum ActivityType {
        // 기본 일상 활동
        MEDICATION("약 복용"),
        MEAL("식사"),
        EXERCISE("운동"),
        SLEEP("수면"),
        
        // 자기관리 활동
        HYGIENE("개인위생"),
        GROOMING("몸단장"),
        HOUSEHOLD("집안일"),
        
        // 사회 활동
        SOCIAL_INTERACTION("사람 만나기"),
        PHONE_CALL("전화 통화"),
        VIDEO_CALL("영상 통화"),
        
        // 학습 및 인지 활동
        READING("읽기"),
        LEARNING("배우기"),
        MEMORY_TRAINING("기억 훈련"),
        COGNITIVE_EXERCISE("인지 운동"),
        
        // 여가 활동
        HOBBY("취미 활동"),
        ENTERTAINMENT("오락"),
        MUSIC("음악 감상"),
        ART("예술 활동"),
        
        // 외출 및 이동
        TRANSPORTATION("교통 이용"),
        SHOPPING("쇼핑"),
        APPOINTMENT("약속"),
        
        // 응급 상황
        EMERGENCY("응급 상황"),
        HELP_REQUEST("도움 요청"),
        
        // 시스템 상호작용
        APP_USAGE("앱 사용"),
        DEVICE_INTERACTION("기기 사용"),
        NOTIFICATION_RESPONSE("알림 응답");

        private final String description;

        ActivityType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 성공 상태
    public enum SuccessStatus {
        SUCCESS("성공"),
        PARTIAL_SUCCESS("부분 성공"),
        FAILED("실패"),
        SKIPPED("건너뜀"),
        CANCELLED("취소됨"),
        IN_PROGRESS("진행 중");

        private final String description;

        SuccessStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 난이도 레벨
    public enum DifficultyLevel {
        VERY_EASY("매우 쉬움"),
        EASY("쉬움"),
        MODERATE("보통"),
        HARD("어려움"),
        VERY_HARD("매우 어려움");

        private final String description;

        DifficultyLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 편의 메서드
    public boolean isSuccessful() {
        return successStatus == SuccessStatus.SUCCESS || 
               successStatus == SuccessStatus.PARTIAL_SUCCESS;
    }

    public boolean needsGuardianAttention() {
        return successStatus == SuccessStatus.FAILED || 
               activityType == ActivityType.EMERGENCY ||
               activityType == ActivityType.HELP_REQUEST ||
               helpNeeded == Boolean.TRUE;
    }

    public void markAsCompleted() {
        this.successStatus = SuccessStatus.SUCCESS;
        this.syncStatus = "COMPLETED";
    }

    public void markAsNeedingHelp() {
        this.helpNeeded = true;
        this.guardianNotified = true;
    }
} 