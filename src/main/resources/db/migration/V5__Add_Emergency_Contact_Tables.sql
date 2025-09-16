-- V4__Add_Emergency_Contact_Tables.sql
-- 긴급 연락처 관리 테이블 추가

-- 긴급 연락처 테이블
CREATE TABLE IF NOT EXISTS emergency_contacts (
  contact_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  relationship VARCHAR(50) NOT NULL,
  phone_number VARCHAR(255) NOT NULL, -- 암호화된 전화번호
  email VARCHAR(100),
  contact_type VARCHAR(30) NOT NULL,
  priority INT NOT NULL DEFAULT 1,
  is_primary BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  can_receive_alerts BOOLEAN DEFAULT TRUE,
  can_access_location BOOLEAN DEFAULT FALSE,
  can_access_health_data BOOLEAN DEFAULT FALSE,
  can_make_decisions BOOLEAN DEFAULT FALSE,
  available_start_time TIME,
  available_end_time TIME,
  available_days VARCHAR(50),
  preferred_contact_method VARCHAR(20) DEFAULT 'PHONE',
  language_preference VARCHAR(10) DEFAULT 'ko',
  notes TEXT,
  medical_professional BOOLEAN DEFAULT FALSE,
  specialization VARCHAR(100),
  hospital_name VARCHAR(200),
  license_number VARCHAR(50),
  last_contacted_at TIMESTAMP NULL,
  contact_count INT DEFAULT 0,
  response_rate DOUBLE DEFAULT 0.0,
  average_response_time_minutes INT,
  verified BOOLEAN DEFAULT FALSE,
  verified_at TIMESTAMP NULL,
  verification_code VARCHAR(10),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT,
  
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
  
  INDEX idx_emergency_contact_user_id (user_id),
  INDEX idx_emergency_contact_priority (priority),
  INDEX idx_emergency_contact_type (contact_type),
  INDEX idx_emergency_contact_is_active (is_active),
  INDEX idx_emergency_contact_verified (verified),
  INDEX idx_emergency_contact_medical (medical_professional)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지오펜스 테이블
CREATE TABLE IF NOT EXISTS geofences (
  geofence_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  center_latitude DOUBLE NOT NULL,
  center_longitude DOUBLE NOT NULL,
  radius_meters DOUBLE NOT NULL,
  geofence_type VARCHAR(30) NOT NULL,
  risk_level VARCHAR(20),
  is_active BOOLEAN DEFAULT TRUE,
  alert_on_entry BOOLEAN DEFAULT TRUE,
  alert_on_exit BOOLEAN DEFAULT TRUE,
  alert_cooldown_minutes INT DEFAULT 30,
  schedule_start_time TIME,
  schedule_end_time TIME,
  schedule_days VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT,
  
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL,
  
  INDEX idx_geofence_user_id (user_id),
  INDEX idx_geofence_active (is_active),
  INDEX idx_geofence_type (geofence_type),
  INDEX idx_geofence_location (center_latitude, center_longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지오펜스 이벤트 테이블
CREATE TABLE IF NOT EXISTS geofence_events (
  event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  geofence_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(20) NOT NULL,
  event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  accuracy DOUBLE,
  risk_level VARCHAR(20),
  notification_sent BOOLEAN DEFAULT FALSE,
  notification_sent_at TIMESTAMP NULL,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (geofence_id) REFERENCES geofences(geofence_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  
  INDEX idx_geofence_event_user (user_id),
  INDEX idx_geofence_event_geofence (geofence_id),
  INDEX idx_geofence_event_type (event_type),
  INDEX idx_geofence_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 샘플 데이터 (개발 환경용)
-- INSERT INTO emergency_contacts (user_id, name, relationship, phone_number, email, contact_type, priority)
-- VALUES 
-- (1, '김보호', '아버지', '010-1234-5678', 'father@example.com', 'FAMILY', 1),
-- (1, '이간호', '담당간호사', '010-9876-5432', 'nurse@hospital.com', 'NURSE', 2);