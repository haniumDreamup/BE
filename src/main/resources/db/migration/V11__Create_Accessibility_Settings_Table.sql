-- V10__Create_Accessibility_Settings_Table.sql
-- 접근성 설정 테이블 생성

CREATE TABLE accessibility_settings (
  settings_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  
  -- 시각 설정
  high_contrast_enabled BOOLEAN DEFAULT FALSE,
  color_scheme VARCHAR(50) DEFAULT 'default',
  font_size VARCHAR(20) DEFAULT 'medium',
  font_family VARCHAR(50) DEFAULT 'default',
  
  -- 음성 안내 설정
  voice_guidance_enabled BOOLEAN DEFAULT TRUE,
  voice_speed FLOAT DEFAULT 1.0,
  voice_pitch FLOAT DEFAULT 1.0,
  voice_language VARCHAR(10) DEFAULT 'ko-KR',
  
  -- 인터페이스 설정
  simplified_ui_enabled BOOLEAN DEFAULT TRUE,
  large_touch_targets BOOLEAN DEFAULT TRUE,
  reduce_animations BOOLEAN DEFAULT FALSE,
  show_focus_indicators BOOLEAN DEFAULT TRUE,
  
  -- 콘텐츠 설정
  simple_language_enabled BOOLEAN DEFAULT TRUE,
  reading_level VARCHAR(20) DEFAULT 'grade5',
  show_icons BOOLEAN DEFAULT TRUE,
  use_emojis BOOLEAN DEFAULT TRUE,
  
  -- 알림 설정
  vibration_enabled BOOLEAN DEFAULT TRUE,
  visual_alerts_enabled BOOLEAN DEFAULT TRUE,
  audio_alerts_enabled BOOLEAN DEFAULT TRUE,
  
  -- 키보드 설정
  sticky_keys_enabled BOOLEAN DEFAULT FALSE,
  keyboard_shortcuts_enabled BOOLEAN DEFAULT TRUE,
  
  -- 추가 설정 (JSON)
  custom_settings JSON,
  
  -- 설정 프로파일
  profile_type VARCHAR(50),
  
  -- 동기화 정보
  last_synced_at DATETIME,
  sync_enabled BOOLEAN DEFAULT TRUE,
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_accessibility_user (user_id),
  INDEX idx_accessibility_updated (updated_at),
  INDEX idx_accessibility_profile (profile_type),
  INDEX idx_accessibility_voice (voice_guidance_enabled),
  INDEX idx_accessibility_simplified (simplified_ui_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 설정 프로파일 데이터 삽입 (선택적)
INSERT INTO accessibility_settings (user_id, profile_type, simplified_ui_enabled, simple_language_enabled, large_touch_targets)
SELECT user_id, 'cognitive', TRUE, TRUE, TRUE
FROM users
WHERE cognitive_level IN ('MILD', 'MODERATE')
AND NOT EXISTS (
  SELECT 1 FROM accessibility_settings WHERE accessibility_settings.user_id = users.user_id
);