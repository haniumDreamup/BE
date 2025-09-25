-- V19__Create_Missing_Core_Tables.sql
-- 누락된 핵심 테이블들 생성

-- 1. Guardian 테이블 (핵심 엔티티)
CREATE TABLE IF NOT EXISTS guardians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '보호받는 사용자 ID',
    guardian_user_id BIGINT NOT NULL COMMENT '보호자 사용자 ID',
    name VARCHAR(100) NOT NULL COMMENT '보호자 이름',
    relationship VARCHAR(50) NOT NULL COMMENT '관계',
    relationship_type VARCHAR(20) DEFAULT 'OTHER' COMMENT '관계 유형',
    primary_phone VARCHAR(20) NOT NULL COMMENT '주 연락처',
    secondary_phone VARCHAR(20) COMMENT '보조 연락처',
    email VARCHAR(255) COMMENT '이메일',
    address VARCHAR(500) COMMENT '주소',
    emergency_priority INT DEFAULT 1 COMMENT '응급시 우선순위',
    notification_preferences TEXT COMMENT '알림 설정 (JSON)',
    is_primary BOOLEAN DEFAULT FALSE COMMENT '주 보호자 여부',
    can_modify_settings BOOLEAN DEFAULT FALSE COMMENT '설정 수정 권한',
    can_view_location BOOLEAN DEFAULT TRUE COMMENT '위치 조회 권한',
    can_receive_alerts BOOLEAN DEFAULT TRUE COMMENT '알림 수신 권한',
    approval_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '승인 상태',
    approved_at DATETIME COMMENT '승인 시간',
    approval_note TEXT COMMENT '승인 메모',
    last_activity_at DATETIME COMMENT '마지막 활동 시간',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    rejection_reason TEXT COMMENT '거절 사유',
    terminated_at DATETIME COMMENT '관계 종료 시간',
    terminated_by VARCHAR(20) COMMENT '종료자',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT COMMENT '생성자 ID',
    updated_by BIGINT COMMENT '수정자 ID',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (guardian_user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_guardian (user_id, guardian_user_id),

    INDEX idx_guardian_user_active_primary (user_id, is_active, is_primary DESC),
    INDEX idx_guardian_user_approval (user_id, approval_status, is_active),
    INDEX idx_guardian_user_emergency (user_id, is_active, emergency_priority),
    INDEX idx_guardian_guardian_active (guardian_user_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='보호자 정보';

-- 2. Notifications 테이블
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    schedule_id BIGINT,
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    action_guidance TEXT,
    send_time DATETIME NOT NULL,
    sent_at DATETIME,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 2,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME,
    is_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at DATETIME,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    visual_indicator VARCHAR(50),
    voice_enabled BOOLEAN DEFAULT FALSE,
    vibration_enabled BOOLEAN DEFAULT TRUE,
    user_response VARCHAR(50),
    metadata TEXT COMMENT '메타데이터 (JSON 형식)',
    expires_at DATETIME,
    notify_guardian BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL,

    INDEX idx_notification_user_status (user_id, status),
    INDEX idx_notification_send_time (send_time),
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_priority (priority DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='알림';

-- 3. Roles 테이블
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    authority VARCHAR(100) UNIQUE NOT NULL,
    is_system_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    INDEX idx_role_name (name),
    INDEX idx_role_authority (authority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할';

-- 4. User_Roles 테이블
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role_id),

    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자-역할 매핑';

-- 기본 역할 데이터 삽입
INSERT IGNORE INTO roles (name, description, authority, is_system_role) VALUES
('ROLE_USER', '일반 사용자', 'ROLE_USER', true),
('ROLE_GUARDIAN', '보호자', 'ROLE_GUARDIAN', true),
('ROLE_ADMIN', '관리자', 'ROLE_ADMIN', true);