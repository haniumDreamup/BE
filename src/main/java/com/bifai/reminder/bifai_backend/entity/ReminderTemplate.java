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

import java.util.Map;
import java.util.Set;

/**
 * BIF 사용자를 위한 알림 템플릿 엔티티
 * 개인화되고 재사용 가능한 알림 메시지 템플릿을 관리
 */
@Entity
@Table(name = "reminder_templates", indexes = {
    @Index(name = "idx_reminder_template_user_id", columnList = "user_id"),
    @Index(name = "idx_reminder_template_type", columnList = "template_type"),
    @Index(name = "idx_reminder_template_active", columnList = "is_active"),
    @Index(name = "idx_reminder_template_system", columnList = "is_system_template")
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user"})
public class ReminderTemplate extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 템플릿 소유자 (시스템 템플릿의 경우 null)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 템플릿 이름 (사용자가 식별하기 위한)
     */
    @Column(name = "template_name", nullable = false, length = 100)
    @NotBlank
    private String templateName;

    /**
     * 템플릿 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "template_type", nullable = false, length = 50)
    @NotNull
    private TemplateType templateType;

    /**
     * BIF 사용자를 위한 간단하고 명확한 제목 템플릿
     */
    @Column(name = "title_template", nullable = false, length = 100)
    @NotBlank
    private String titleTemplate;

    /**
     * 메시지 내용 템플릿
     */
    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String messageTemplate;

    /**
     * 행동 가이드 템플릿
     */
    @Column(name = "action_template", columnDefinition = "TEXT")
    private String actionTemplate;

    /**
     * 시스템 제공 템플릿 여부
     */
    @Column(name = "is_system_template", nullable = false)
    private Boolean isSystemTemplate = false;

    /**
     * 활성화 상태
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 기본 우선순위
     */
    @Column(name = "default_priority", nullable = false)
    private Integer defaultPriority = 2;

    /**
     * 기본 전송 채널들
     */
    @ElementCollection(targetClass = Notification.DeliveryChannel.class)
    @CollectionTable(name = "template_channels", joinColumns = @JoinColumn(name = "template_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private Set<Notification.DeliveryChannel> defaultChannels;

    /**
     * BIF 사용자를 위한 시각적 표시 설정
     */
    @Column(name = "visual_indicator", length = 50)
    private String visualIndicator;

    /**
     * 음성 안내 활성화 여부
     */
    @Column(name = "voice_enabled")
    private Boolean voiceEnabled = false;

    /**
     * 진동 활성화 여부
     */
    @Column(name = "vibration_enabled")
    private Boolean vibrationEnabled = true;

    /**
     * 보호자에게도 알림 여부
     */
    @Column(name = "notify_guardian")
    private Boolean notifyGuardian = false;

    /**
     * 템플릿 변수들 (JSON 형태)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_variables", columnDefinition = "JSON")
    private Map<String, Object> templateVariables;

    /**
     * 템플릿 설정 (JSON 형태)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_settings", columnDefinition = "JSON")
    private Map<String, Object> templateSettings;

    /**
     * 사용 횟수 (통계용)
     */
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    /**
     * 사용자 만족도 평균 (1-5점)
     */
    @Column(name = "satisfaction_score", precision = 3)
    private java.math.BigDecimal satisfactionScore;

    /**
     * 성공률 (%)
     */
    @Column(name = "success_rate", precision = 5)
    private java.math.BigDecimal successRate;

    /**
     * 템플릿 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * BIF 사용자의 인지 수준에 맞는 난이도
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "complexity_level", length = 20)
    private ComplexityLevel complexityLevel = ComplexityLevel.SIMPLE;

    /**
     * 감정적 톤
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "emotional_tone", length = 30)
    private EmotionalTone emotionalTone = EmotionalTone.FRIENDLY;

    /**
     * 템플릿 유형 열거형
     */
    public enum TemplateType {
        MEDICATION_REMINDER("약물 복용 알림"),
        MEAL_REMINDER("식사 알림"),
        EXERCISE_REMINDER("운동 알림"),
        APPOINTMENT_REMINDER("약속 알림"),
        EMERGENCY_ALERT("응급 알림"),
        HEALTH_CHECK("건강 체크"),
        ENCOURAGEMENT("격려 메시지"),
        ACHIEVEMENT("성취 알림"),
        SAFETY_CHECK("안전 확인"),
        SOCIAL_REMINDER("사회 활동 알림"),
        BEDTIME_REMINDER("취침 알림"),
        WAKE_UP_REMINDER("기상 알림"),
        HYDRATION_REMINDER("수분 섭취 알림"),
        CUSTOM("사용자 정의");

        private final String description;

        TemplateType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 복잡도 수준 열거형
     */
    public enum ComplexityLevel {
        VERY_SIMPLE("매우 간단"),
        SIMPLE("간단"),
        MODERATE("보통"),
        DETAILED("상세");

        private final String description;

        ComplexityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 감정적 톤 열거형
     */
    public enum EmotionalTone {
        FRIENDLY("친근한"),
        ENCOURAGING("격려하는"),
        URGENT("긴급한"),
        CALM("차분한"),
        PLAYFUL("재미있는"),
        FORMAL("정중한"),
        CARING("배려하는");

        private final String description;

        EmotionalTone(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 생성자 (필수 필드)
     */
    public ReminderTemplate(String templateName, TemplateType templateType, 
                           String titleTemplate, String messageTemplate) {
        this.templateName = templateName;
        this.templateType = templateType;
        this.titleTemplate = titleTemplate;
        this.messageTemplate = messageTemplate;
    }

    /**
     * 사용자별 템플릿 생성자
     */
    public ReminderTemplate(User user, String templateName, TemplateType templateType, 
                           String titleTemplate, String messageTemplate) {
        this(templateName, templateType, titleTemplate, messageTemplate);
        this.user = user;
    }

    /**
     * 템플릿 변수로 메시지 생성
     */
    public String generateTitle(Map<String, String> variables) {
        String result = titleTemplate;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }

    /**
     * 템플릿 변수로 메시지 생성
     */
    public String generateMessage(Map<String, String> variables) {
        String result = messageTemplate;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }

    /**
     * 템플릿 변수로 행동 가이드 생성
     */
    public String generateActionGuidance(Map<String, String> variables) {
        if (actionTemplate == null) return null;
        
        String result = actionTemplate;
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return result;
    }

    /**
     * 사용 횟수 증가
     */
    public void incrementUsage() {
        this.usageCount++;
    }

    /**
     * 만족도 점수 업데이트
     */
    public void updateSatisfactionScore(int score) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("만족도 점수는 1-5 사이여야 합니다.");
        }
        
        if (this.satisfactionScore == null) {
            this.satisfactionScore = new java.math.BigDecimal(score);
        } else {
            // 가중 평균 계산 (기존 점수와 새 점수의 평균)
            java.math.BigDecimal newScore = this.satisfactionScore
                .multiply(new java.math.BigDecimal(0.8))
                .add(new java.math.BigDecimal(score).multiply(new java.math.BigDecimal(0.2)));
            this.satisfactionScore = newScore.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * 성공률 업데이트
     */
    public void updateSuccessRate(boolean wasSuccessful) {
        if (this.successRate == null) {
            this.successRate = wasSuccessful ? new java.math.BigDecimal("100.00") : new java.math.BigDecimal("0.00");
        } else {
            // 성공률 계산 (가중 평균)
            java.math.BigDecimal currentRate = this.successRate.multiply(new java.math.BigDecimal(0.9));
            java.math.BigDecimal newContribution = wasSuccessful ? 
                new java.math.BigDecimal("10.00") : java.math.BigDecimal.ZERO;
            this.successRate = currentRate.add(newContribution).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    /**
     * 템플릿 복제
     */
    public ReminderTemplate clone(User newUser, String newName) {
        ReminderTemplate cloned = new ReminderTemplate(
            newUser, newName, this.templateType, this.titleTemplate, this.messageTemplate
        );
        
        cloned.setActionTemplate(this.actionTemplate);
        cloned.setDefaultPriority(this.defaultPriority);
        cloned.setDefaultChannels(this.defaultChannels);
        cloned.setVisualIndicator(this.visualIndicator);
        cloned.setVoiceEnabled(this.voiceEnabled);
        cloned.setVibrationEnabled(this.vibrationEnabled);
        cloned.setNotifyGuardian(this.notifyGuardian);
        cloned.setComplexityLevel(this.complexityLevel);
        cloned.setEmotionalTone(this.emotionalTone);
        cloned.setDescription(this.description);
        
        return cloned;
    }

    /**
     * 템플릿 활성화/비활성화
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    /**
     * 높은 성과의 템플릿인지 확인
     */
    public boolean isHighPerforming() {
        return (satisfactionScore != null && satisfactionScore.compareTo(new java.math.BigDecimal("4.0")) >= 0) ||
               (successRate != null && successRate.compareTo(new java.math.BigDecimal("80.0")) >= 0);
    }

    /**
     * 인기 있는 템플릿인지 확인
     */
    public boolean isPopular() {
        return usageCount >= 10;
    }

    /**
     * BIF 사용자를 위한 간단한 설명
     */
    public String getSimpleDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(templateType.getDescription());
        
        if (emotionalTone != null) {
            desc.append(" (").append(emotionalTone.getDescription()).append(" 톤)");
        }
        
        if (complexityLevel != null) {
            desc.append(" [").append(complexityLevel.getDescription()).append("]");
        }
        
        if (usageCount > 0) {
            desc.append(" - ").append(usageCount).append("번 사용됨");
        }
        
        return desc.toString();
    }

    /**
     * 템플릿 평가 요약
     */
    public String getPerformanceSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (satisfactionScore != null) {
            summary.append("만족도: ").append(satisfactionScore).append("/5 ");
        }
        
        if (successRate != null) {
            summary.append("성공률: ").append(successRate).append("% ");
        }
        
        summary.append("사용횟수: ").append(usageCount).append("회");
        
        return summary.toString();
    }
} 