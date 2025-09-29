-- V1__Create_baseline_schema.sql
-- 현재 User 엔티티를 기반으로 한 기본 스키마 생성
-- 이 마이그레이션은 기존 RDS 데이터베이스에 있는 스키마를 기록하는 용도

-- Users 테이블 생성 (현재 엔티티와 일치하는 필드만)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    nickname VARCHAR(50),
    phone_number VARCHAR(20),
    cognitive_level VARCHAR(20) DEFAULT 'MODERATE',
    profile_image_url VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    language_preference VARCHAR(10) DEFAULT 'ko',
    is_active BOOLEAN DEFAULT TRUE,
    emergency_mode_enabled BOOLEAN DEFAULT FALSE,
    last_login_at DATETIME,
    last_activity_at DATETIME,
    password_reset_token VARCHAR(255),
    password_reset_expires_at DATETIME,
    provider VARCHAR(20),
    provider_id VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_user_active_username ON users (is_active, username);
CREATE INDEX IF NOT EXISTS idx_user_active_email ON users (is_active, email);
CREATE INDEX IF NOT EXISTS idx_user_provider ON users (provider, provider_id);
CREATE INDEX IF NOT EXISTS idx_user_last_login ON users (last_login_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_emergency_mode ON users (emergency_mode_enabled, is_active);

-- Roles 테이블 생성
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- User_Roles 테이블 생성 (다대다 관계)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles (role_id);

-- 기본 역할 데이터 삽입
INSERT IGNORE INTO roles (name, description) VALUES
('USER', '일반 사용자'),
('ADMIN', '관리자'),
('GUARDIAN', '보호자');