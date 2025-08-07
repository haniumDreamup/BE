-- 안전 구역(Geofence) 테이블 생성
CREATE TABLE geofences (
    geofence_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    center_latitude DOUBLE NOT NULL,
    center_longitude DOUBLE NOT NULL,
    radius_meters INT NOT NULL,
    address VARCHAR(500),
    geofence_type VARCHAR(30),
    is_active BOOLEAN DEFAULT TRUE,
    alert_on_entry BOOLEAN DEFAULT FALSE,
    alert_on_exit BOOLEAN DEFAULT TRUE,
    start_time TIME,
    end_time TIME,
    active_days VARCHAR(20),
    priority INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    
    CONSTRAINT fk_geofence_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_geofence_created_by FOREIGN KEY (created_by) REFERENCES users(user_id),
    
    INDEX idx_user_id (user_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 위치 정보 테이블 생성
CREATE TABLE locations (
    location_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    altitude DOUBLE,
    accuracy DOUBLE,
    speed DOUBLE,
    heading DOUBLE,
    address VARCHAR(500),
    location_type VARCHAR(30),
    is_in_safe_zone BOOLEAN DEFAULT TRUE,
    geofence_id BIGINT,
    device_id VARCHAR(100),
    battery_level INT,
    is_charging BOOLEAN,
    network_type VARCHAR(20),
    provider VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activity_type VARCHAR(50),
    activity_confidence INT,
    
    CONSTRAINT fk_location_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_location_geofence FOREIGN KEY (geofence_id) REFERENCES geofences(geofence_id),
    
    INDEX idx_user_id_created_at (user_id, created_at),
    INDEX idx_created_at (created_at),
    INDEX idx_location_type (location_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Geofence 유형 설명 주석
COMMENT ON COLUMN geofences.geofence_type IS 'HOME, WORK, SCHOOL, HOSPITAL, SAFE_ZONE, DANGER_ZONE, CUSTOM';

-- 위치 유형 설명 주석
COMMENT ON COLUMN locations.location_type IS 'REAL_TIME, PERIODIC, EMERGENCY, GEOFENCE_ENTRY, GEOFENCE_EXIT, MANUAL';

-- 활동 유형 설명 주석
COMMENT ON COLUMN locations.activity_type IS 'STILL, WALKING, RUNNING, IN_VEHICLE, ON_BICYCLE, UNKNOWN';