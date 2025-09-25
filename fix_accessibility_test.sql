-- AccessibilityController 테스트를 위한 H2 데이터베이스 테이블 생성

-- accessibility_settings 테이블 생성
CREATE TABLE IF NOT EXISTS accessibility_settings (
    settings_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    audio_alerts_enabled BOOLEAN DEFAULT true,
    color_scheme VARCHAR(50) DEFAULT 'default',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    custom_settings TEXT,
    font_family VARCHAR(100) DEFAULT 'system',
    font_size VARCHAR(20) DEFAULT 'medium',
    high_contrast_enabled BOOLEAN DEFAULT false,
    keyboard_shortcuts_enabled BOOLEAN DEFAULT true,
    large_touch_targets BOOLEAN DEFAULT false,
    last_synced_at TIMESTAMP,
    profile_type VARCHAR(50) DEFAULT 'standard',
    reading_level VARCHAR(20) DEFAULT 'normal',
    reduce_animations BOOLEAN DEFAULT false,
    show_focus_indicators BOOLEAN DEFAULT true,
    show_icons BOOLEAN DEFAULT true,
    simple_language_enabled BOOLEAN DEFAULT false,
    simplified_ui_enabled BOOLEAN DEFAULT false,
    sticky_keys_enabled BOOLEAN DEFAULT false,
    sync_enabled BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    use_emojis BOOLEAN DEFAULT true,
    vibration_enabled BOOLEAN DEFAULT true,
    visual_alerts_enabled BOOLEAN DEFAULT false,
    voice_guidance_enabled BOOLEAN DEFAULT false,
    voice_language VARCHAR(10) DEFAULT 'ko',
    voice_pitch FLOAT DEFAULT 1.0,
    voice_speed FLOAT DEFAULT 1.0
);

-- 테스트용 기본 데이터 삽입
INSERT INTO accessibility_settings (user_id, profile_type) VALUES
(1, 'standard'),
(999, 'low_vision'),
(1000, 'hearing_impaired');

-- users 테이블이 없다면 생성 (테스트용)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    password VARCHAR(255),
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 테스트용 사용자 데이터
INSERT INTO users (id, username, email, password, full_name) VALUES
(1, 'testuser', 'test@example.com', '$2a$12$dummy.hash.for.testing', '테스트 사용자'),
(999, 'accesstest', 'access@example.com', '$2a$12$dummy.hash.for.testing', '접근성 테스트'),
(1000, 'hearingtest', 'hearing@example.com', '$2a$12$dummy.hash.for.testing', '청각 테스트');