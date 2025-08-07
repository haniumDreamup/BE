package com.bifai.reminder.bifai_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 선호도 엔티티 - 경계성 지능 장애인을 위한 개인화 설정
 */
@Entity
@Table(name = "user_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPreference extends BaseEntity {
    
    @Id
    @GeneratedValue
    private Long prefId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, unique = true)
    @JsonIgnore
    private User user;
    
    @Builder.Default
    private Boolean notificationEnabled = true;
    
    @Builder.Default
    private Boolean voiceGuidanceEnabled = true;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TextSize textSize = TextSize.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UiComplexityLevel uiComplexityLevel = UiComplexityLevel.SIMPLE;
    
    @Column(length = 10)
    @Builder.Default
    private String languageCode = "ko-KR";
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ThemePreference themePreference = ThemePreference.LIGHT;
    
    @Builder.Default
    private Integer reminderFrequency = 3; // 시간 단위
    
    @Builder.Default
    private Boolean emergencyAutoCall = false;
    
    /**
     * 알림 설정 업데이트
     */
    public void updateNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }
    
    /**
     * 음성 안내 설정 업데이트
     */
    public void updateVoiceGuidanceEnabled(Boolean voiceGuidanceEnabled) {
        this.voiceGuidanceEnabled = voiceGuidanceEnabled;
    }
    
    /**
     * UI 접근성 설정 업데이트
     */
    public void updateAccessibilitySettings(TextSize textSize, UiComplexityLevel uiComplexityLevel, ThemePreference themePreference) {
        this.textSize = textSize;
        this.uiComplexityLevel = uiComplexityLevel;
        this.themePreference = themePreference;
    }
    
    /**
     * 리마인더 빈도 업데이트
     */
    public void updateReminderFrequency(Integer reminderFrequency) {
        this.reminderFrequency = reminderFrequency;
    }
    
    /**
     * 베스트 프랙티스: ID 기반 equals 구현
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPreference)) return false;
        UserPreference other = (UserPreference) o;
        
        if (this.prefId == null || other.prefId == null) {
            return false;
        }
        
        return this.prefId.equals(other.prefId);
    }
    
    /**
     * 베스트 프랙티스: 안정적인 hashCode 구현
     */
    @Override
    public int hashCode() {
        return prefId != null ? prefId.hashCode() : getClass().hashCode();
    }
    
    /**
     * 베스트 프랙티스: 간결한 toString (연관관계 제외)
     */
    @Override
    public String toString() {
        return String.format("UserPreference{id=%d, textSize=%s, uiLevel=%s, lang='%s'}", 
                           prefId, textSize, uiComplexityLevel, languageCode);
    }
    
    /**
     * 텍스트 크기 열거형
     */
    public enum TextSize {
        SMALL, MEDIUM, LARGE, EXTRA_LARGE
    }
    
    /**
     * UI 복잡도 수준 열거형
     */
    public enum UiComplexityLevel {
        SIMPLE, STANDARD, DETAILED
    }
    
    /**
     * 테마 선호도 열거형
     */
    public enum ThemePreference {
        LIGHT, DARK, HIGH_CONTRAST
    }
} 