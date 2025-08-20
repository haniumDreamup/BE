package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 접근성 설정 DTO
 */
@Data
@Builder
public class AccessibilitySettingsDto {
  
  private Long settingsId;
  
  private Long userId;
  
  // 시각 설정
  private Boolean highContrastEnabled;
  private String colorScheme;
  private String fontSize;
  private String fontFamily;
  
  // 음성 안내 설정
  private Boolean voiceGuidanceEnabled;
  private Float voiceSpeed;
  private Float voicePitch;
  private String voiceLanguage;
  
  // 인터페이스 설정
  private Boolean simplifiedUiEnabled;
  private Boolean largeTouchTargets;
  private Boolean reduceAnimations;
  private Boolean showFocusIndicators;
  
  // 콘텐츠 설정
  private Boolean simpleLanguageEnabled;
  private String readingLevel;
  private Boolean showIcons;
  private Boolean useEmojis;
  
  // 알림 설정
  private Boolean vibrationEnabled;
  private Boolean visualAlertsEnabled;
  private Boolean audioAlertsEnabled;
  
  // 키보드 설정
  private Boolean stickyKeysEnabled;
  private Boolean keyboardShortcutsEnabled;
  
  // 추가 설정
  private Map<String, Object> customSettings;
  
  // 프로파일 정보
  private String profileType;
  
  // 동기화 정보
  private LocalDateTime lastSyncedAt;
  private Boolean syncEnabled;
  
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}