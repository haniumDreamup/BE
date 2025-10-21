-- StatisticsController용 필수 데이터베이스 테이블 생성

USE bifai_db;

-- Geofences 테이블 생성
CREATE TABLE IF NOT EXISTS geofences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    radius DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_location (latitude, longitude),
    INDEX idx_active (is_active)
);

-- Location History 테이블 생성
CREATE TABLE IF NOT EXISTS location_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    accuracy DECIMAL(8, 2),
    speed DECIMAL(8, 2),
    bearing DECIMAL(5, 2),
    altitude DECIMAL(8, 2),
    recorded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_recorded_at (recorded_at),
    INDEX idx_user_time (user_id, recorded_at)
);

-- 테스트용 샘플 데이터 삽입 (user_id 1이 존재한다고 가정)
INSERT IGNORE INTO geofences (user_id, name, description, latitude, longitude, radius, is_active) VALUES
(1, '집', '우리집 주변 안전구역', 37.5665, 126.9780, 100.0, TRUE),
(1, '학교', '학교 주변 안전구역', 37.5660, 126.9775, 150.0, TRUE),
(1, '직장', '직장 주변 안전구역', 37.5670, 126.9785, 80.0, TRUE);

INSERT IGNORE INTO location_history (user_id, latitude, longitude, accuracy, speed, bearing, altitude, recorded_at) VALUES
(1, 37.5665, 126.9780, 5.0, 0.0, 0.0, 50.0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 37.5666, 126.9781, 4.5, 1.2, 45.0, 51.0, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 37.5667, 126.9782, 3.8, 2.1, 90.0, 52.0, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(1, 37.5668, 126.9783, 4.2, 0.8, 135.0, 53.0, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(1, 37.5669, 126.9784, 5.1, 0.0, 180.0, 54.0, NOW());