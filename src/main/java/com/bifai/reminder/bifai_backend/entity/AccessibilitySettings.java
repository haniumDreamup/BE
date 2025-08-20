package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 사용자별 접근성 설정 엔티티
 * WCAG 2.1 AA 준수 및 BIF 사용자 맞춤 설정
 */
@Entity
@Table(name = "accessibility_settings",
  indexes = {
    @Index(name = "idx_accessibility_user", columnList = "user_id"),
    @Index(name = "idx_accessibility_updated", columnList = "updated_at")
  })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessibilitySettings {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "settings_id")
  private Long settingsId;
  
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;
  
  // 시각 설정
  @Column(name = "high_contrast_enabled")
  @Builder.Default
  private Boolean highContrastEnabled = false;
  
  @Column(name = "color_scheme", length = 50)
  @Builder.Default
  private String colorScheme = "default"; // default, dark, light, color-blind
  
  @Column(name = "font_size", length = 20)
  @Builder.Default
  private String fontSize = "medium"; // small, medium, large, extra-large
  
  @Column(name = "font_family", length = 50)
  @Builder.Default
  private String fontFamily = "default"; // default, sans-serif, dyslexic-friendly
  
  // 음성 안내 설정
  @Column(name = "voice_guidance_enabled")
  @Builder.Default
  private Boolean voiceGuidanceEnabled = true;
  
  @Column(name = "voice_speed")
  @Builder.Default
  private Float voiceSpeed = 1.0f; // 0.5 ~ 2.0
  
  @Column(name = "voice_pitch")
  @Builder.Default
  private Float voicePitch = 1.0f; // 0.5 ~ 2.0
  
  @Column(name = "voice_language", length = 10)
  @Builder.Default
  private String voiceLanguage = "ko-KR";
  
  // 인터페이스 설정
  @Column(name = "simplified_ui_enabled")
  @Builder.Default
  private Boolean simplifiedUiEnabled = true;
  
  @Column(name = "large_touch_targets")
  @Builder.Default
  private Boolean largeTouchTargets = true;
  
  @Column(name = "reduce_animations")
  @Builder.Default
  private Boolean reduceAnimations = false;
  
  @Column(name = "show_focus_indicators")
  @Builder.Default
  private Boolean showFocusIndicators = true;
  
  // 콘텐츠 설정
  @Column(name = "simple_language_enabled")
  @Builder.Default
  private Boolean simpleLanguageEnabled = true;
  
  @Column(name = "reading_level", length = 20)
  @Builder.Default
  private String readingLevel = "grade5"; // grade3, grade5, grade7, adult
  
  @Column(name = "show_icons")
  @Builder.Default
  private Boolean showIcons = true;
  
  @Column(name = "use_emojis")
  @Builder.Default
  private Boolean useEmojis = true;
  
  // 알림 설정
  @Column(name = "vibration_enabled")
  @Builder.Default
  private Boolean vibrationEnabled = true;
  
  @Column(name = "visual_alerts_enabled")
  @Builder.Default
  private Boolean visualAlertsEnabled = true;
  
  @Column(name = "audio_alerts_enabled")
  @Builder.Default
  private Boolean audioAlertsEnabled = true;
  
  // 키보드 설정
  @Column(name = "sticky_keys_enabled")
  @Builder.Default
  private Boolean stickyKeysEnabled = false;
  
  @Column(name = "keyboard_shortcuts_enabled")
  @Builder.Default
  private Boolean keyboardShortcutsEnabled = true;
  
  // 추가 설정 (JSON)
  @Column(name = "custom_settings", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  @Builder.Default
  private Map<String, Object> customSettings = new HashMap<>();
  
  // 설정 프로파일
  @Column(name = "profile_type", length = 50)
  private String profileType; // visual-impaired, cognitive, motor, hearing, custom
  
  // 동기화 정보
  @Column(name = "last_synced_at")
  private LocalDateTime lastSyncedAt;
  
  @Column(name = "sync_enabled")
  @Builder.Default
  private Boolean syncEnabled = true;
  
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;
  
  /**
   * 프로파일에 따른 기본 설정 적용
   */
  public void applyProfile(String profileType) {
    this.profileType = profileType;
    
    switch (profileType) {
      case "visual-impaired":
        this.highContrastEnabled = true;
        this.fontSize = "extra-large";
        this.voiceGuidanceEnabled = true;
        this.showFocusIndicators = true;
        break;
        
      case "cognitive":
        this.simplifiedUiEnabled = true;
        this.simpleLanguageEnabled = true;
        this.readingLevel = "grade3";
        this.useEmojis = true;
        this.reduceAnimations = true;
        break;
        
      case "motor":
        this.largeTouchTargets = true;
        this.stickyKeysEnabled = true;
        this.reduceAnimations = true;
        break;
        
      case "hearing":
        this.visualAlertsEnabled = true;
        this.vibrationEnabled = true;
        this.audioAlertsEnabled = false;
        break;
    }
  }
  
  /**
   * 설정 검증
   */
  public boolean validateSettings() {
    // 음성 속도 범위 검증
    if (voiceSpeed < 0.5f || voiceSpeed > 2.0f) {
      voiceSpeed = 1.0f;
    }
    
    // 음성 피치 범위 검증
    if (voicePitch < 0.5f || voicePitch > 2.0f) {
      voicePitch = 1.0f;
    }
    
    return true;
  }
  
  /**
   * 동기화 시간 업데이트
   */
  public void updateSyncTime() {
    this.lastSyncedAt = LocalDateTime.now();
  }
}