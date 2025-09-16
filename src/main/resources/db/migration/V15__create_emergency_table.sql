-- 긴급 상황 테이블 생성
CREATE TABLE emergencies (
    emergency_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    emergency_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    notified_guardians TEXT,
    response_time_seconds INT,
    
    CONSTRAINT fk_emergency_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 긴급 상황 유형 설명 주석
COMMENT ON COLUMN emergencies.emergency_type IS 'FALL_DETECTION, MANUAL_ALERT, GEOFENCE_EXIT, DEVICE_OFFLINE, MEDICATION_MISSED, ABNORMAL_PATTERN';

-- 긴급 상황 상태 설명 주석
COMMENT ON COLUMN emergencies.status IS 'ACTIVE, NOTIFIED, ACKNOWLEDGED, RESOLVED, FALSE_ALARM';

-- 심각도 설명 주석
COMMENT ON COLUMN emergencies.severity IS 'LOW, MEDIUM, HIGH, CRITICAL';

-- 트리거 소스 설명 주석
COMMENT ON COLUMN emergencies.triggered_by IS 'USER, DEVICE, AI_DETECTION, GUARDIAN, SYSTEM';