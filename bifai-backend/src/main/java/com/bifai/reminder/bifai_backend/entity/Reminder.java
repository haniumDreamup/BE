package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 엔티티 - 경계성 지능 장애인을 위한 맞춤형 알림 관리
 * 인지적 특성을 고려한 다양한 알림 방식 지원
 */
@Entity
@Table(name = "reminders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reminder extends BaseEntity {
    
    @Id
    @GeneratedValue
    private Long reminderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private Schedule schedule;
    
    @Column(nullable = false)
    private LocalDateTime reminderTime;
    
    /**
     * 알림 타입 - 다양한 감각 활용
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReminderType reminderType = ReminderType.VISUAL_AUDIO;
    
    /**
     * 시각적 단서 타입
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VisualCueType visualCueType = VisualCueType.SIMPLE_TEXT;
    
    /**
     * 청각적 단서 타입
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AudioCueType audioCueType = AudioCueType.SOFT_BELL;
    
    /**
     * 전송 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSent = false;
    
    /**
     * 실제 전송 시간
     */
    private LocalDateTime sentAt;
    
    /**
     * 사용자 응답 여부
     */
    @Builder.Default
    private Boolean userResponded = false;
    
    /**
     * 사용자 응답 시간
     */
    private LocalDateTime respondedAt;
    
    /**
     * 알림 활성화 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * 재알림 설정 (분 단위)
     */
    @Builder.Default
    private Integer snoozeMinutes = 5;
    
    /**
     * 우선순위 (1: 높음, 2: 보통, 3: 낮음)
     */
    @Builder.Default
    private Integer reminderPriority = 2;
    
    // 관계 매핑
    // Notification은 Schedule과 연결되어 있으므로 Reminder와의 직접 관계 제거
    
    /**
     * 알림 전송 처리
     */
    public void markAsSent() {
        this.isSent = true;
        this.sentAt = LocalDateTime.now();
    }
    
    /**
     * 사용자 응답 처리
     */
    public void markAsResponded() {
        this.userResponded = true;
        this.respondedAt = LocalDateTime.now();
    }
    
    /**
     * 알림 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * 재알림 시간 계산
     */
    public LocalDateTime getSnoozeTime() {
        return reminderTime.plusMinutes(snoozeMinutes);
    }
    
    /**
     * equals/hashCode 구현
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reminder)) return false;
        Reminder other = (Reminder) o;
        
        if (this.reminderId == null || other.reminderId == null) {
            return false;
        }
        
        return this.reminderId.equals(other.reminderId);
    }
    
    @Override
    public int hashCode() {
        return reminderId != null ? reminderId.hashCode() : getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Reminder{id=%d, time=%s, type=%s, sent=%s}", 
                           reminderId, reminderTime, reminderType, isSent);
    }
    
    /**
     * 알림 타입 열거형
     */
    public enum ReminderType {
        VISUAL_ONLY,        // 시각적 알림만
        AUDIO_ONLY,         // 청각적 알림만
        VISUAL_AUDIO,       // 시각+청각 알림
        VIBRATION,          // 진동 알림
        MULTI_SENSORY       // 다감각 알림
    }
    
    /**
     * 시각적 단서 타입 열거형
     */
    public enum VisualCueType {
        SIMPLE_TEXT,        // 단순 텍스트
        LARGE_TEXT,         // 큰 글씨
        ICON_WITH_TEXT,     // 아이콘 + 텍스트
        COLOR_CODED,        // 색상 구분
        PICTURE_CARD,       // 그림 카드
        VIDEO_GUIDE         // 영상 가이드
    }
    
    /**
     * 청각적 단서 타입 열거형
     */
    public enum AudioCueType {
        SOFT_BELL,          // 부드러운 벨소리
        VOICE_GUIDE,        // 음성 안내
        FAMILIAR_SOUND,     // 친숙한 소리
        MUSIC_SNIPPET,      // 음악 조각
        NATURE_SOUND,       // 자연 소리
        CUSTOM_SOUND        // 사용자 정의 소리
    }
} 