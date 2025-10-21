-- V2: 안전한 핵심 스키마 업데이트
-- 기존 테이블에만 의존하는 안전한 변경사항들

-- users 테이블에 누락된 필드들 추가
ALTER TABLE users
ADD COLUMN IF NOT EXISTS date_of_birth DATE COMMENT '생년월일',
ADD COLUMN IF NOT EXISTS gender VARCHAR(10) COMMENT '성별 (MALE, FEMALE, OTHER)',
ADD COLUMN IF NOT EXISTS language_preference VARCHAR(10) COMMENT '선호 언어 (ko, en)',
ADD COLUMN IF NOT EXISTS language_preference_secondary VARCHAR(10) COMMENT '보조 언어 (ko, en)';

-- 기본값 설정
UPDATE users SET
    gender = 'OTHER' WHERE gender IS NULL,
    language_preference = 'ko' WHERE language_preference IS NULL,
    language_preference_secondary = 'ko' WHERE language_preference_secondary IS NULL;

-- users 테이블 기본 인덱스 추가 (안전한 것들만)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(is_active);

COMMIT;