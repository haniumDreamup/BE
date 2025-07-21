-- BIF-AI Backend Core Entity Schema Updates Migration
-- Updates user and guardian tables based on task requirements

-- Update users table with additional fields for cognitive profile
ALTER TABLE users
ADD COLUMN username VARCHAR(50) UNIQUE AFTER id COMMENT '사용자명 (고유 식별자)',
ADD COLUMN full_name VARCHAR(100) AFTER name COMMENT '사용자 전체 이름',
ADD COLUMN language_preference_secondary VARCHAR(10) AFTER language_preference COMMENT '보조 언어 선호',
ADD COLUMN emergency_mode_enabled BOOLEAN DEFAULT FALSE AFTER is_active COMMENT '응급 모드 활성화 여부',
ADD COLUMN last_activity_at DATETIME AFTER last_login_at COMMENT '마지막 활동 시간',
ADD COLUMN password_reset_token VARCHAR(255) COMMENT '비밀번호 재설정 토큰',
ADD COLUMN password_reset_expires_at DATETIME COMMENT '비밀번호 재설정 토큰 만료 시간',
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE COMMENT '이메일 인증 여부',
ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE COMMENT '전화번호 인증 여부',
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

-- Update guardians table with enhanced relationship management
ALTER TABLE guardians
ADD COLUMN guardian_user_id BIGINT AFTER user_id COMMENT '보호자 사용자 ID (사용자 테이블 참조)',
ADD COLUMN relationship_type ENUM('PARENT', 'SIBLING', 'CAREGIVER', 'DOCTOR', 'OTHER') 
    DEFAULT 'OTHER' AFTER relationship COMMENT '관계 유형 (열거형)',
ADD COLUMN can_modify_settings BOOLEAN DEFAULT FALSE AFTER is_primary COMMENT '설정 수정 권한',
ADD COLUMN can_view_location BOOLEAN DEFAULT TRUE AFTER can_modify_settings COMMENT '위치 조회 권한',
ADD COLUMN can_receive_alerts BOOLEAN DEFAULT TRUE AFTER can_view_location COMMENT '알림 수신 권한',
ADD COLUMN approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') 
    DEFAULT 'PENDING' AFTER can_receive_alerts COMMENT '승인 상태',
ADD COLUMN approved_at DATETIME AFTER approval_status COMMENT '승인 시간',
ADD COLUMN approval_note TEXT AFTER approved_at COMMENT '승인 관련 메모',
ADD COLUMN last_activity_at DATETIME AFTER approval_note COMMENT '마지막 활동 시간',
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

-- Add foreign key for guardian_user_id
ALTER TABLE guardians
ADD CONSTRAINT fk_guardian_user 
FOREIGN KEY (guardian_user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add unique constraint for user-guardian relationship
ALTER TABLE guardians
ADD CONSTRAINT uk_user_guardian 
UNIQUE KEY (user_id, guardian_user_id);

-- Update devices table with additional fields from task requirements
ALTER TABLE devices
ADD COLUMN device_serial_number VARCHAR(100) AFTER device_identifier COMMENT '기기 일련번호',
ADD COLUMN firmware_version VARCHAR(50) AFTER app_version COMMENT '펌웨어 버전',
ADD COLUMN battery_level INTEGER AFTER firmware_version COMMENT '현재 배터리 레벨 (%)',
ADD COLUMN pairing_code VARCHAR(20) AFTER is_active COMMENT '페어링 코드',
ADD COLUMN paired_at DATETIME AFTER pairing_code COMMENT '페어링 완료 시간',
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

-- Create locations table (not in existing schema but required by task)
CREATE TABLE locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    device_id BIGINT COMMENT '위치 기록 디바이스 ID',
    latitude DECIMAL(10, 8) NOT NULL COMMENT '위도',
    longitude DECIMAL(11, 8) NOT NULL COMMENT '경도',
    accuracy DECIMAL(8, 2) COMMENT '정확도 (미터)',
    altitude DECIMAL(8, 2) COMMENT '고도 (미터)',
    speed DECIMAL(6, 2) COMMENT '속도 (m/s)',
    heading DECIMAL(5, 2) COMMENT '방향 (도)',
    location_type VARCHAR(30) COMMENT '위치 유형 (HOME, WORK, OUTDOOR, etc.)',
    address VARCHAR(500) COMMENT '주소',
    captured_at DATETIME NOT NULL COMMENT '위치 캡처 시간',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',
    deleted_at DATETIME COMMENT '소프트 삭제 시간',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) COMMENT '사용자 위치 정보';

-- Create activities table to replace activity_logs for better structure
CREATE TABLE activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    activity_type VARCHAR(50) NOT NULL COMMENT '활동 유형',
    activity_name VARCHAR(200) NOT NULL COMMENT '활동 이름',
    description TEXT COMMENT '활동 설명',
    start_time DATETIME NOT NULL COMMENT '시작 시간',
    end_time DATETIME COMMENT '종료 시간',
    location_id BIGINT COMMENT '활동 위치 ID',
    confidence_score DECIMAL(3, 2) COMMENT '확신도 점수 (0.0-1.0)',
    is_routine BOOLEAN DEFAULT FALSE COMMENT '일상적인 활동 여부',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',
    deleted_at DATETIME COMMENT '소프트 삭제 시간',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(id) ON DELETE SET NULL
) COMMENT '사용자 활동 정보';

-- Create emergency_contacts table (required by task but not in existing schema)
CREATE TABLE emergency_contacts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    contact_name VARCHAR(100) NOT NULL COMMENT '연락처 이름',
    phone_number VARCHAR(20) NOT NULL COMMENT '전화번호',
    relationship VARCHAR(50) COMMENT '관계',
    priority_order INTEGER DEFAULT 1 COMMENT '우선순위 (1이 최우선)',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',
    deleted_at DATETIME COMMENT '소프트 삭제 시간',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT '응급 연락처';

-- Update notifications table for better structure
ALTER TABLE notifications
ADD COLUMN type ENUM('REMINDER', 'ALERT', 'EMERGENCY', 'SYSTEM') 
    DEFAULT 'REMINDER' AFTER user_id COMMENT '알림 유형 (열거형)',
ADD COLUMN message TEXT AFTER content COMMENT '알림 메시지',
ADD COLUMN priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') 
    DEFAULT 'MEDIUM' AFTER priority_level COMMENT '알림 우선순위',
ADD COLUMN metadata JSON AFTER expires_at COMMENT '추가 메타데이터',
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

-- Update notification_delivery to notification_deliveries (plural) and add fields
RENAME TABLE notification_delivery TO notification_deliveries;

ALTER TABLE notification_deliveries
ADD COLUMN channel ENUM('PUSH', 'SMS', 'EMAIL', 'IN_APP') 
    DEFAULT 'PUSH' AFTER notification_id COMMENT '전달 채널 (열거형)',
ADD COLUMN recipient VARCHAR(255) AFTER channel COMMENT '수신자 (이메일, 전화번호 등)',
ADD COLUMN status ENUM('PENDING', 'SENT', 'DELIVERED', 'FAILED') 
    DEFAULT 'PENDING' AFTER delivery_status COMMENT '전달 상태 (열거형)',
ADD COLUMN failure_reason TEXT AFTER error_message COMMENT '실패 이유',
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

-- Add audit columns to all tables that don't have them
ALTER TABLE activity_logs
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE medications
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE medication_adherence
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE health_metrics
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE schedules
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE reminder_templates
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE captured_images
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE analysis_results
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE content_metadata
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

ALTER TABLE battery_history
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE connectivity_logs
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE user_preferences
ADD COLUMN created_by BIGINT COMMENT '생성자 ID',
ADD COLUMN updated_by BIGINT COMMENT '수정자 ID',
ADD COLUMN deleted_at DATETIME COMMENT '소프트 삭제 시간',
ADD COLUMN version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전';

-- Create indexes for new fields
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_emergency_mode ON users(emergency_mode_enabled);
CREATE INDEX idx_users_last_activity ON users(last_activity_at);
CREATE INDEX idx_users_deleted ON users(deleted_at);

CREATE INDEX idx_guardians_guardian_user ON guardians(guardian_user_id);
CREATE INDEX idx_guardians_relationship_type ON guardians(relationship_type);
CREATE INDEX idx_guardians_approval ON guardians(approval_status);
CREATE INDEX idx_guardians_permissions ON guardians(can_modify_settings, can_view_location, can_receive_alerts);
CREATE INDEX idx_guardians_deleted ON guardians(deleted_at);

CREATE INDEX idx_locations_user_time ON locations(user_id, captured_at DESC);
CREATE INDEX idx_locations_device ON locations(device_id);
CREATE INDEX idx_locations_type ON locations(location_type);
CREATE INDEX idx_locations_coordinates ON locations(latitude, longitude);
CREATE INDEX idx_locations_deleted ON locations(deleted_at);

CREATE INDEX idx_activities_user_time ON activities(user_id, start_time DESC);
CREATE INDEX idx_activities_type ON activities(activity_type);
CREATE INDEX idx_activities_routine ON activities(is_routine);
CREATE INDEX idx_activities_location ON activities(location_id);
CREATE INDEX idx_activities_deleted ON activities(deleted_at);

CREATE INDEX idx_emergency_user_priority ON emergency_contacts(user_id, priority_order);
CREATE INDEX idx_emergency_active ON emergency_contacts(is_active);
CREATE INDEX idx_emergency_deleted ON emergency_contacts(deleted_at);

-- Add check constraints for new fields
ALTER TABLE users
ADD CONSTRAINT chk_users_emergency_mode CHECK (emergency_mode_enabled IN (0, 1));

ALTER TABLE guardians
ADD CONSTRAINT chk_guardians_permissions CHECK (
    can_modify_settings IN (0, 1) AND
    can_view_location IN (0, 1) AND
    can_receive_alerts IN (0, 1)
);

ALTER TABLE devices
ADD CONSTRAINT chk_devices_battery_level CHECK (battery_level IS NULL OR (battery_level >= 0 AND battery_level <= 100));

ALTER TABLE locations
ADD CONSTRAINT chk_locations_coordinates CHECK (
    latitude >= -90 AND latitude <= 90 AND
    longitude >= -180 AND longitude <= 180
);

ALTER TABLE activities
ADD CONSTRAINT chk_activities_confidence CHECK (confidence_score IS NULL OR (confidence_score >= 0.0 AND confidence_score <= 1.0));

ALTER TABLE emergency_contacts
ADD CONSTRAINT chk_emergency_priority CHECK (priority_order >= 1 AND priority_order <= 10);