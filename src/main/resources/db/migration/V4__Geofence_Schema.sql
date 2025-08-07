-- V4__Geofence_Schema.sql
-- 지오펜스(안전 구역) 관련 테이블 생성

-- 지오펜스 테이블
CREATE TABLE IF NOT EXISTS geofences (
    geofence_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    center_latitude DOUBLE NOT NULL,
    center_longitude DOUBLE NOT NULL,
    radius_meters INT NOT NULL,
    address VARCHAR(500),
    geofence_type VARCHAR(30) DEFAULT 'CUSTOM',
    is_active BOOLEAN DEFAULT TRUE,
    alert_on_entry BOOLEAN DEFAULT FALSE,
    alert_on_exit BOOLEAN DEFAULT TRUE,
    start_time TIME,
    end_time TIME,
    active_days VARCHAR(50),
    priority INT DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6),
    created_by BIGINT,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
    
    INDEX idx_geofence_user_id (user_id),
    INDEX idx_geofence_is_active (is_active),
    INDEX idx_geofence_type (geofence_type),
    INDEX idx_geofence_priority (priority),
    
    -- 공간 인덱스 (MySQL 8.0+)
    SPATIAL INDEX idx_geofence_location (POINT(center_longitude, center_latitude))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지오펜스 이벤트 테이블
CREATE TABLE IF NOT EXISTS geofence_events (
    event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    geofence_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    accuracy DOUBLE,
    speed DOUBLE,
    heading DOUBLE,
    address VARCHAR(500),
    notification_sent BOOLEAN DEFAULT FALSE,
    notification_sent_at DATETIME(6),
    notification_recipients VARCHAR(1000),
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at DATETIME(6),
    notes VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    duration_seconds BIGINT,
    risk_level VARCHAR(20) DEFAULT 'LOW',
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (geofence_id) REFERENCES geofences(geofence_id) ON DELETE CASCADE,
    
    INDEX idx_geofence_event_user_id (user_id),
    INDEX idx_geofence_event_geofence_id (geofence_id),
    INDEX idx_geofence_event_created_at (created_at),
    INDEX idx_geofence_event_type (event_type),
    INDEX idx_geofence_event_risk_level (risk_level),
    INDEX idx_geofence_event_notification (notification_sent, acknowledged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지오펜스 알림 설정 테이블 (향후 확장용)
CREATE TABLE IF NOT EXISTS geofence_alert_settings (
    setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    geofence_id BIGINT NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    recipient_type VARCHAR(30) NOT NULL, -- USER, GUARDIAN, EMERGENCY
    recipient_id BIGINT,
    alert_method VARCHAR(30), -- PUSH, SMS, EMAIL
    delay_minutes INT DEFAULT 0,
    repeat_interval_minutes INT,
    max_alerts_per_day INT DEFAULT 10,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6),
    
    FOREIGN KEY (geofence_id) REFERENCES geofences(geofence_id) ON DELETE CASCADE,
    
    INDEX idx_alert_settings_geofence (geofence_id),
    INDEX idx_alert_settings_active (is_active),
    UNIQUE KEY uk_geofence_alert_type (geofence_id, alert_type, recipient_type, recipient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지오펜스 공유 테이블 (보호자와 공유)
CREATE TABLE IF NOT EXISTS geofence_shares (
    share_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    geofence_id BIGINT NOT NULL,
    shared_with_user_id BIGINT NOT NULL,
    permission_level VARCHAR(20) DEFAULT 'VIEW', -- VIEW, EDIT, ADMIN
    shared_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    shared_by BIGINT NOT NULL,
    expires_at DATETIME(6),
    
    FOREIGN KEY (geofence_id) REFERENCES geofences(geofence_id) ON DELETE CASCADE,
    FOREIGN KEY (shared_with_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_geofence_share_geofence (geofence_id),
    INDEX idx_geofence_share_user (shared_with_user_id),
    UNIQUE KEY uk_geofence_share (geofence_id, shared_with_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지오펜스 방문 통계 테이블
CREATE TABLE IF NOT EXISTS geofence_visit_stats (
    stat_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    geofence_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    total_visits INT DEFAULT 0,
    total_duration_seconds BIGINT DEFAULT 0,
    first_entry_time TIME,
    last_exit_time TIME,
    avg_duration_seconds BIGINT,
    max_duration_seconds BIGINT,
    alerts_triggered INT DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6),
    
    FOREIGN KEY (geofence_id) REFERENCES geofences(geofence_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_visit_stats_geofence (geofence_id),
    INDEX idx_visit_stats_user (user_id),
    INDEX idx_visit_stats_date (visit_date),
    UNIQUE KEY uk_geofence_visit_date (geofence_id, user_id, visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 샘플 데이터 (테스트용)
-- INSERT INTO geofences (user_id, name, description, center_latitude, center_longitude, radius_meters, geofence_type, alert_on_exit)
-- VALUES 
-- (1, '우리집', '안전한 집', 37.5665, 126.9780, 100, 'HOME', TRUE),
-- (1, '직장', '회사 사무실', 37.5172, 127.0473, 200, 'WORK', FALSE),
-- (1, '병원', '자주 가는 병원', 37.5635, 126.9975, 150, 'HOSPITAL', FALSE);