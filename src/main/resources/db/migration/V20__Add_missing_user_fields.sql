-- 사용자 테이블에 누락된 필드들 추가
-- date_of_birth, gender, language_preference_secondary 필드 추가

-- users 테이블에 누락된 컬럼들 추가
ALTER TABLE users
ADD COLUMN IF NOT EXISTS date_of_birth DATE COMMENT '생년월일',
ADD COLUMN IF NOT EXISTS gender VARCHAR(10) COMMENT '성별 (MALE, FEMALE, OTHER)',
ADD COLUMN IF NOT EXISTS language_preference_secondary VARCHAR(10) COMMENT '보조 선호 언어 (ko, en 등)';

-- 기본값 설정 (optional)
UPDATE users SET
    gender = 'OTHER' WHERE gender IS NULL,
    language_preference_secondary = 'ko' WHERE language_preference_secondary IS NULL;

-- 인덱스 추가 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_users_gender ON users(gender);
CREATE INDEX IF NOT EXISTS idx_users_birth_date ON users(date_of_birth);