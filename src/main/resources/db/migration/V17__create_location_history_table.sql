-- 위치 이력(LocationHistory) 테이블 생성
-- BIF 사용자의 위치 기록을 저장하는 테이블
CREATE TABLE location_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id BIGINT,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    altitude DECIMAL(10,2),
    accuracy DECIMAL(10,2),
    speed DECIMAL(10,2),
    heading DECIMAL(10,2),
    location_type VARCHAR(30),
    address VARCHAR(500),
    captured_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    created_by VARCHAR(255),
    modified_by VARCHAR(255),
    
    CONSTRAINT fk_location_history_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_location_history_device FOREIGN KEY (device_id) REFERENCES devices(device_id),
    
    INDEX idx_location_history_user_time (user_id, captured_at DESC),
    INDEX idx_location_history_device (device_id),
    INDEX idx_location_history_type (location_type),
    INDEX idx_location_history_coordinates (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- LocationType 설명 주석
COMMENT ON COLUMN location_history.location_type IS 'HOME, WORK, SCHOOL, HOSPITAL, PHARMACY, RESTAURANT, SHOP, PARK, TRANSIT, OUTDOOR, INDOOR, UNKNOWN';