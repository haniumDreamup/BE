-- 미디어 파일 테이블 생성
CREATE TABLE IF NOT EXISTS media_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    media_id VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    upload_type VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    thumbnail_url VARCHAR(1000),
    cdn_url VARCHAR(1000),
    
    -- 이미지 메타데이터
    width INT,
    height INT,
    
    -- 비디오 메타데이터
    duration INT COMMENT '초 단위',
    frame_rate FLOAT,
    
    -- 업로드 상태
    upload_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    upload_id VARCHAR(100),
    uploaded_at DATETIME,
    processed_at DATETIME,
    etag VARCHAR(100),
    
    -- 추가 메타데이터
    metadata JSON,
    
    -- 소프트 삭제
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at DATETIME,
    
    -- 관련 엔티티
    related_entity_id BIGINT,
    related_entity_type VARCHAR(50),
    
    -- 감사 필드
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- 외래 키
    CONSTRAINT fk_media_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- 인덱스
    INDEX idx_media_user_created (user_id, created_at DESC),
    INDEX idx_media_type_created (upload_type, created_at DESC),
    INDEX idx_media_id (media_id),
    INDEX idx_media_upload_id (upload_id),
    INDEX idx_media_upload_status (upload_status),
    INDEX idx_media_related (related_entity_id, related_entity_type),
    INDEX idx_media_deleted (is_deleted, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='미디어 파일 정보';

-- 업로드 타입 체크 제약
ALTER TABLE media_files ADD CONSTRAINT chk_upload_type 
CHECK (upload_type IN ('PROFILE', 'MEDICATION', 'ACTIVITY', 'DOCUMENT', 'HEALTH', 'EMERGENCY'));

-- 업로드 상태 체크 제약
ALTER TABLE media_files ADD CONSTRAINT chk_upload_status 
CHECK (upload_status IN ('PENDING', 'UPLOADING', 'UPLOADED', 'PROCESSING', 'COMPLETED', 'FAILED', 'DELETED'));

-- 파일 크기 체크 제약 (100MB 제한)
ALTER TABLE media_files ADD CONSTRAINT chk_file_size 
CHECK (file_size > 0 AND file_size <= 104857600);