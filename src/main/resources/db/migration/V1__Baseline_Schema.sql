-- V1: BifAI Backend 베이스라인 스키마
-- 베스트 프렉티스: 기존 Entity 기반 초기 스키마 정의

-- 사용자 테이블
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '사용자명',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '이메일',
    password VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    full_name VARCHAR(100) COMMENT '실명',
    date_of_birth DATE COMMENT '생년월일',
    gender ENUM('MALE', 'FEMALE', 'OTHER') DEFAULT 'OTHER' COMMENT '성별',
    language_preference VARCHAR(10) DEFAULT 'ko' COMMENT '주 언어',
    language_preference_secondary VARCHAR(10) DEFAULT 'ko' COMMENT '보조 언어',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',

    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_created_at (created_at),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 기본 정보';

-- 디바이스 테이블 (FCM 토큰 관리)
CREATE TABLE devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_token VARCHAR(255) UNIQUE NOT NULL COMMENT 'FCM 토큰',
    device_type ENUM('ANDROID', 'IOS', 'WEB') NOT NULL COMMENT '디바이스 타입',
    device_id VARCHAR(100) COMMENT '디바이스 고유 ID',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_devices_user_id (user_id),
    INDEX idx_devices_token (device_token),
    INDEX idx_devices_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 디바이스 정보';

-- 보호자-피보호자 관계 테이블
CREATE TABLE guardian_relationships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guardian_id BIGINT NOT NULL COMMENT '보호자 ID',
    ward_id BIGINT NOT NULL COMMENT '피보호자 ID',
    relationship_type ENUM('PARENT', 'CAREGIVER', 'FAMILY', 'FRIEND', 'PROFESSIONAL') NOT NULL COMMENT '관계 유형',
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'BLOCKED') DEFAULT 'PENDING' COMMENT '관계 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (guardian_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (ward_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_guardian_ward (guardian_id, ward_id),
    INDEX idx_guardian_relationships_guardian (guardian_id),
    INDEX idx_guardian_relationships_ward (ward_id),
    INDEX idx_guardian_relationships_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='보호자 관계 관리';

-- 응급상황 테이블
CREATE TABLE emergencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '응급상황 발생 사용자',
    emergency_type ENUM('MEDICAL', 'SAFETY', 'LOCATION', 'BEHAVIORAL', 'OTHER') NOT NULL COMMENT '응급상황 유형',
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM' COMMENT '심각도',
    status ENUM('ACTIVE', 'RESOLVED', 'FALSE_ALARM', 'CANCELLED') DEFAULT 'ACTIVE' COMMENT '처리 상태',
    description TEXT COMMENT '응급상황 설명',
    location_latitude DECIMAL(10, 8) COMMENT '위도',
    location_longitude DECIMAL(11, 8) COMMENT '경도',
    location_address TEXT COMMENT '주소',
    triggered_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '발생 시간',
    resolved_at DATETIME COMMENT '해결 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_emergencies_user_id (user_id),
    INDEX idx_emergencies_type (emergency_type),
    INDEX idx_emergencies_severity (severity),
    INDEX idx_emergencies_status (status),
    INDEX idx_emergencies_triggered_at (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='응급상황 관리';

-- 알림 테이블
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '수신자',
    title VARCHAR(200) NOT NULL COMMENT '알림 제목',
    message TEXT NOT NULL COMMENT '알림 내용',
    notification_type ENUM('EMERGENCY', 'REMINDER', 'SOCIAL', 'SYSTEM', 'HEALTH') NOT NULL COMMENT '알림 유형',
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL' COMMENT '우선순위',
    is_read BOOLEAN DEFAULT FALSE COMMENT '읽음 여부',
    is_sent BOOLEAN DEFAULT FALSE COMMENT '전송 여부',
    sent_at DATETIME COMMENT '전송 시간',
    read_at DATETIME COMMENT '읽은 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_type (notification_type),
    INDEX idx_notifications_read (is_read),
    INDEX idx_notifications_sent (is_sent),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림 관리';

-- 사용자 위치 정보 테이블
CREATE TABLE user_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL COMMENT '위도',
    longitude DECIMAL(11, 8) NOT NULL COMMENT '경도',
    address TEXT COMMENT '주소',
    accuracy FLOAT COMMENT '정확도(미터)',
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '위치 기록 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_locations_user_id (user_id),
    INDEX idx_user_locations_recorded_at (recorded_at),
    INDEX idx_user_locations_coordinates (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 위치 기록';

-- 미디어 파일 테이블 (이미지, 동영상 등)
CREATE TABLE media_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '업로드 사용자',
    file_name VARCHAR(255) NOT NULL COMMENT '파일명',
    original_name VARCHAR(255) NOT NULL COMMENT '원본 파일명',
    file_path VARCHAR(500) NOT NULL COMMENT '파일 경로',
    file_size BIGINT NOT NULL COMMENT '파일 크기(바이트)',
    content_type VARCHAR(100) NOT NULL COMMENT 'MIME 타입',
    media_type ENUM('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT') NOT NULL COMMENT '미디어 유형',
    is_processed BOOLEAN DEFAULT FALSE COMMENT '처리 완료 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_media_files_user_id (user_id),
    INDEX idx_media_files_type (media_type),
    INDEX idx_media_files_processed (is_processed),
    INDEX idx_media_files_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='미디어 파일 관리';

-- 통계 데이터 테이블 (일별 집계)
CREATE TABLE daily_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    statistics_date DATE NOT NULL COMMENT '통계 날짜',
    total_notifications INT DEFAULT 0 COMMENT '총 알림 수',
    emergency_count INT DEFAULT 0 COMMENT '응급상황 발생 수',
    location_updates INT DEFAULT 0 COMMENT '위치 업데이트 수',
    app_usage_minutes INT DEFAULT 0 COMMENT '앱 사용 시간(분)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, statistics_date),
    INDEX idx_daily_statistics_date (statistics_date),
    INDEX idx_daily_statistics_user_date (user_id, statistics_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일별 통계 데이터';

-- 시스템 설정 테이블
CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL COMMENT '설정 키',
    setting_value TEXT COMMENT '설정 값',
    description TEXT COMMENT '설정 설명',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_system_settings_key (setting_key),
    INDEX idx_system_settings_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='시스템 설정';

COMMIT;