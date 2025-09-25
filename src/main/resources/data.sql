-- H2 데이터베이스 테스트용 테이블 생성 및 데이터 삽입

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

-- users 테이블이 없다면 생성 (테스트용)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    nickname VARCHAR(50),
    phone_number VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    cognitive_level VARCHAR(20),
    email_verified BOOLEAN DEFAULT false,
    phone_verified BOOLEAN DEFAULT false,
    profile_image_url VARCHAR(500),
    provider VARCHAR(50),
    provider_id VARCHAR(100),
    timezone VARCHAR(50),
    language_preference VARCHAR(10),
    language_preference_secondary VARCHAR(10),
    is_active BOOLEAN DEFAULT true,
    emergency_mode_enabled BOOLEAN DEFAULT false,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    last_login_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- roles 테이블 생성 (테스트용)
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(200),
    korean_name VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- user_roles 테이블 생성 (테스트용)
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE KEY unique_user_role (user_id, role_id)
);

-- emergencies 테이블 생성 (SOS 기능용) - Emergency 엔티티에 맞게 완전 수정
CREATE TABLE IF NOT EXISTS emergencies (
    emergency_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    emergency_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'TRIGGERED',
    latitude DOUBLE,
    longitude DOUBLE,
    address VARCHAR(500),
    description TEXT,
    severity VARCHAR(20),
    triggered_by VARCHAR(50),
    fall_confidence DOUBLE,
    image_url VARCHAR(500),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified_guardians TEXT,
    response_time_seconds INT,
    notification_sent BOOLEAN DEFAULT false,
    responder_count INT,
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    notification_count INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- emergency_contacts 테이블 생성 (긴급 연락처용) - EmergencyContact 엔티티에 맞게 완전 수정
CREATE TABLE IF NOT EXISTS emergency_contacts (
    contact_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    contact_type VARCHAR(30) NOT NULL,
    priority INT NOT NULL,
    is_primary BOOLEAN,
    is_active BOOLEAN,
    can_receive_alerts BOOLEAN,
    can_access_location BOOLEAN,
    can_access_health_data BOOLEAN,
    can_make_decisions BOOLEAN,
    available_start_time TIME,
    available_end_time TIME,
    available_days VARCHAR(50),
    preferred_contact_method VARCHAR(20),
    language_preference VARCHAR(10),
    notes VARCHAR(500),
    medical_professional BOOLEAN,
    specialization VARCHAR(100),
    hospital_name VARCHAR(200),
    license_number VARCHAR(50),
    last_contacted_at TIMESTAMP,
    contact_count INT,
    response_rate DOUBLE,
    average_response_time_minutes INT,
    verified BOOLEAN,
    verified_at TIMESTAMP,
    verification_code VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 테스트용 사용자 데이터 (H2 MERGE 문법 사용)
MERGE INTO users (id, username, email, password_hash, name, full_name, cognitive_level, is_active, emergency_mode_enabled) VALUES
(1, 'testuser', 'test@example.com', '$2a$12$dummy.hash.for.testing', '테스트 사용자', '김테스트', 'MODERATE', true, false),
(999, 'accesstest', 'access@example.com', '$2a$12$dummy.hash.for.testing', '접근성 테스트', '이접근', 'MILD', true, false),
(1000, 'hearingtest', 'hearing@example.com', '$2a$12$dummy.hash.for.testing', '청각 테스트', '박청각', 'MODERATE', true, false);

-- 테스트용 역할 데이터 삽입
MERGE INTO roles (id, name) VALUES
(1, 'ROLE_USER'),
(2, 'ROLE_GUARDIAN'),
(3, 'ROLE_ADMIN');

-- 테스트용 사용자 역할 할당
MERGE INTO user_roles (id, user_id, role_id) VALUES
(1, 1, 1),     -- testuser에게 USER 역할
(2, 999, 1),   -- accesstest에게 USER 역할
(3, 1000, 2);  -- hearingtest에게 GUARDIAN 역할

-- 테스트용 긴급 연락처 데이터 (새 스키마에 맞게 수정)
MERGE INTO emergency_contacts (contact_id, user_id, name, phone_number, email, relationship, contact_type, priority,
is_primary, is_active, can_receive_alerts, can_access_location, can_access_health_data, can_make_decisions,
preferred_contact_method, language_preference, medical_professional, verified, contact_count, response_rate) VALUES
(1, 1, '가족 1', '010-1234-5678', 'family1@example.com', 'FAMILY', 'FAMILY', 1, true, true, true, true, false, false, 'PHONE', 'ko', false, true, 0, 0.0),
(2, 1, '가족 2', '010-2345-6789', 'family2@example.com', 'FAMILY', 'FAMILY', 2, false, true, true, false, false, false, 'SMS', 'ko', false, true, 0, 0.0),
(3, 1, '친구 1', '010-3456-7890', 'friend1@example.com', 'FRIEND', 'FRIEND', 3, false, true, true, false, false, false, 'PHONE', 'ko', false, false, 0, 0.0);

-- 테스트용 응급상황 데이터 (SOS 기능 테스트용) - emergency_id로 수정
MERGE INTO emergencies (emergency_id, user_id, emergency_type, description, latitude, longitude, status) VALUES
(1, 1, 'PANIC_BUTTON', '테스트 긴급상황', 37.5665, 126.9780, 'TRIGGERED'),
(2, 1, 'FALL_DETECTED', '낙상 감지됨', 37.5665, 126.9780, 'RESOLVED');

-- user_preferences 테이블 생성 (사용자 환경설정용)
CREATE TABLE IF NOT EXISTS user_preferences (
    prefId BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    languageCode VARCHAR(10) DEFAULT 'ko',
    themePreference VARCHAR(20) DEFAULT 'LIGHT',
    textSize VARCHAR(20) DEFAULT 'MEDIUM',
    uiComplexityLevel VARCHAR(20) DEFAULT 'STANDARD',
    notificationEnabled BOOLEAN DEFAULT true,
    voiceGuidanceEnabled BOOLEAN DEFAULT false,
    reminderFrequency INT DEFAULT 3,
    emergencyAutoCall BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 테스트용 기본 데이터 삽입 (H2 MERGE 문법 사용)
MERGE INTO accessibility_settings (settings_id, user_id, profile_type) VALUES
(1, 1, 'standard'),
(2, 999, 'low_vision'),
(3, 1000, 'hearing_impaired');

-- 테스트용 사용자 환경설정 데이터 삽입
MERGE INTO user_preferences (prefId, user_id, languageCode, themePreference, notificationEnabled, reminderFrequency) VALUES
(1, 1, 'ko', 'LIGHT', true, 3),
(2, 999, 'ko', 'HIGH_CONTRAST', true, 2),
(3, 1000, 'ko', 'LIGHT', false, 5);