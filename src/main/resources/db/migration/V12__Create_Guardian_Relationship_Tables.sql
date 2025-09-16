-- V11__Create_Guardian_Relationship_Tables.sql
-- 보호자 관계 관리 테이블 생성

-- 보호자와 사용자 간의 관계 테이블
CREATE TABLE guardian_relationships (
  relationship_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guardian_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  
  -- 관계 정보
  relationship_type VARCHAR(30) NOT NULL,
  permission_level VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  
  -- 초대 관련
  invitation_token VARCHAR(100) UNIQUE,
  invitation_expires_at DATETIME,
  approved_at DATETIME,
  approved_by VARCHAR(50),
  
  -- 권한 및 설정
  permission_settings JSON,
  emergency_priority INT,
  notification_preferences JSON,
  
  -- 추가 정보
  notes VARCHAR(500),
  last_active_at DATETIME,
  
  -- 관계 종료
  terminated_at DATETIME,
  termination_reason VARCHAR(200),
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  -- 외래키
  FOREIGN KEY (guardian_id) REFERENCES guardians(guardian_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  
  -- 인덱스
  INDEX idx_guardian_user (guardian_id, user_id),
  INDEX idx_relationship_status (status),
  INDEX idx_invitation_token (invitation_token),
  INDEX idx_emergency_priority (user_id, emergency_priority),
  INDEX idx_last_active (last_active_at),
  
  -- 유니크 제약
  UNIQUE KEY uk_guardian_user (guardian_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보호자 활동 로그 테이블
CREATE TABLE guardian_activity_logs (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  relationship_id BIGINT NOT NULL,
  guardian_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  
  -- 활동 정보
  activity_type VARCHAR(50) NOT NULL,
  activity_description VARCHAR(500),
  accessed_data VARCHAR(100),
  ip_address VARCHAR(45),
  user_agent VARCHAR(255),
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  -- 외래키
  FOREIGN KEY (relationship_id) REFERENCES guardian_relationships(relationship_id) ON DELETE CASCADE,
  FOREIGN KEY (guardian_id) REFERENCES guardians(guardian_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  
  -- 인덱스
  INDEX idx_activity_relationship (relationship_id),
  INDEX idx_activity_guardian (guardian_id),
  INDEX idx_activity_user (user_id),
  INDEX idx_activity_type (activity_type),
  INDEX idx_activity_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보호자 권한 템플릿 테이블
CREATE TABLE guardian_permission_templates (
  template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_name VARCHAR(100) NOT NULL,
  relationship_type VARCHAR(30),
  permission_level VARCHAR(20) NOT NULL,
  
  -- 권한 설정
  permission_settings JSON NOT NULL,
  notification_settings JSON,
  
  -- 메타데이터
  description VARCHAR(500),
  is_default BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  -- 인덱스
  INDEX idx_template_type (relationship_type),
  INDEX idx_template_level (permission_level),
  INDEX idx_template_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 권한 템플릿 삽입
INSERT INTO guardian_permission_templates (template_name, relationship_type, permission_level, permission_settings, description, is_default) VALUES
('부모 기본 권한', 'PARENT', 'FULL', 
 '{"viewLocation": true, "viewHealth": true, "manageMedication": true, "viewActivity": true, "manageSchedule": true, "viewEmergency": true, "manageSettings": true}',
 '부모를 위한 전체 권한 템플릿', TRUE),

('배우자 기본 권한', 'SPOUSE', 'MANAGE', 
 '{"viewLocation": true, "viewHealth": true, "manageMedication": true, "viewActivity": true, "manageSchedule": true, "viewEmergency": true, "manageSettings": false}',
 '배우자를 위한 관리 권한 템플릿', TRUE),

('간병인 기본 권한', 'CAREGIVER', 'MANAGE', 
 '{"viewLocation": true, "viewHealth": true, "manageMedication": true, "viewActivity": true, "manageSchedule": false, "viewEmergency": true, "manageSettings": false}',
 '간병인을 위한 제한된 관리 권한 템플릿', TRUE),

('친구 기본 권한', 'FRIEND', 'VIEW_ONLY', 
 '{"viewLocation": true, "viewHealth": false, "manageMedication": false, "viewActivity": true, "manageSchedule": false, "viewEmergency": false, "manageSettings": false}',
 '친구를 위한 읽기 전용 권한 템플릿', TRUE),

('긴급 연락처 권한', 'OTHER', 'EMERGENCY', 
 '{"viewLocation": true, "viewHealth": true, "manageMedication": false, "viewActivity": false, "manageSchedule": false, "viewEmergency": true, "manageSettings": false}',
 '긴급 상황 전용 권한 템플릿', FALSE);

-- 보호자 초대 이력 테이블
CREATE TABLE guardian_invitation_history (
  history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  relationship_id BIGINT,
  user_id BIGINT NOT NULL,
  guardian_email VARCHAR(255) NOT NULL,
  
  -- 초대 정보
  invitation_token VARCHAR(100),
  invitation_status VARCHAR(20) NOT NULL,
  sent_at DATETIME,
  responded_at DATETIME,
  
  -- 메타데이터
  sent_by VARCHAR(100),
  response_ip VARCHAR(45),
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  -- 외래키
  FOREIGN KEY (relationship_id) REFERENCES guardian_relationships(relationship_id) ON DELETE SET NULL,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  
  -- 인덱스
  INDEX idx_invitation_user (user_id),
  INDEX idx_invitation_email (guardian_email),
  INDEX idx_invitation_status (invitation_status),
  INDEX idx_invitation_sent (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;