-- BIF-AI Backend 초기 데이터베이스 스키마 생성
-- Flyway 마이그레이션 V1.0
-- Created: 2024-09-16

-- 역할(Role) 테이블 생성
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    korean_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    INDEX idx_role_name (name),
    INDEX idx_role_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자(User) 테이블 생성
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    nickname VARCHAR(50),
    phone_number VARCHAR(20),
    date_of_birth DATE,
    gender VARCHAR(10),
    address VARCHAR(500),

    -- 인지 수준 (MILD, MODERATE, SEVERE, UNKNOWN)
    cognitive_level ENUM('MILD', 'MODERATE', 'SEVERE', 'UNKNOWN') DEFAULT 'MODERATE',

    -- 긴급 연락처
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),

    -- 프로필 및 설정
    profile_image_url VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    language_preference VARCHAR(10) DEFAULT 'ko',
    language_preference_secondary VARCHAR(10),

    -- 상태 관리
    is_active BOOLEAN DEFAULT TRUE,
    emergency_mode_enabled BOOLEAN DEFAULT FALSE,

    -- 활동 추적
    last_login_at TIMESTAMP NULL,
    last_activity_at TIMESTAMP NULL,

    -- 비밀번호 리셋
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP NULL,

    -- 인증 상태
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,

    -- OAuth2 정보
    provider VARCHAR(20),
    provider_id VARCHAR(255),

    -- 시간 추적
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- 인덱스
    INDEX idx_user_active_username (is_active, username),
    INDEX idx_user_active_email (is_active, email),
    INDEX idx_user_provider (provider, provider_id),
    INDEX idx_user_last_login (last_login_at DESC),
    INDEX idx_user_emergency_mode (emergency_mode_enabled, is_active),
    INDEX idx_user_email_verified (email_verified),
    INDEX idx_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자-역할 관계 테이블 (Many-to-Many)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 역할 데이터 삽입
INSERT INTO roles (name, description, korean_name, is_active) VALUES
('ROLE_USER', 'Basic user role', '일반 사용자', TRUE),
('ROLE_ADMIN', 'Administrator role', '관리자', TRUE),
('ROLE_GUARDIAN', 'Guardian role for BIF users', '보호자', TRUE),
('ROLE_CAREGIVER', 'Caregiver role', '돌봄이', TRUE);

-- Spring Batch 메타데이터 테이블 (필요한 경우)
-- 이미 Spring Boot가 자동으로 생성하므로 주석 처리
-- CREATE TABLE BATCH_JOB_INSTANCE (
--     JOB_INSTANCE_ID BIGINT NOT NULL AUTO_INCREMENT,
--     VERSION BIGINT,
--     JOB_NAME VARCHAR(100) NOT NULL,
--     JOB_KEY VARCHAR(32) NOT NULL,
--     PRIMARY KEY (JOB_INSTANCE_ID),
--     UNIQUE KEY JOB_INST_UN (JOB_NAME, JOB_KEY)
-- ) ENGINE=InnoDB;