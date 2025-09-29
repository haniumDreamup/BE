-- V2__Add_removed_fields.sql
-- 이전에 제거했던 필드들을 다시 추가
-- 이렇게 하면 기존 코드에서 해당 필드를 사용하는 부분이 있어도 안전

-- 제거했던 필드들 추가
ALTER TABLE users
ADD COLUMN IF NOT EXISTS date_of_birth DATE COMMENT '생년월일',
ADD COLUMN IF NOT EXISTS gender VARCHAR(10) COMMENT '성별 (MALE, FEMALE, OTHER)',
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE COMMENT '이메일 인증 여부',
ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE COMMENT '전화번호 인증 여부',
ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(100) COMMENT '긴급 연락처 이름',
ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20) COMMENT '긴급 연락처 전화번호',
ADD COLUMN IF NOT EXISTS language_preference_secondary VARCHAR(10) COMMENT '보조 언어 설정';

-- 인덱스 추가 (필요한 경우)
CREATE INDEX IF NOT EXISTS idx_user_email_verified ON users (email_verified);
CREATE INDEX IF NOT EXISTS idx_user_phone_verified ON users (phone_verified);
CREATE INDEX IF NOT EXISTS idx_user_gender ON users (gender);
CREATE INDEX IF NOT EXISTS idx_user_birth_date ON users (date_of_birth);