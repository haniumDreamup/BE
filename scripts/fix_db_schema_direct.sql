-- 프로덕션 RDS에서 직접 실행할 스키마 수정 SQL
-- 실행 방법: MySQL Workbench 또는 AWS Console에서 실행

USE bifai_db;

-- 1. users 테이블에 누락된 컬럼들 추가
ALTER TABLE users
ADD COLUMN IF NOT EXISTS date_of_birth DATE COMMENT '생년월일',
ADD COLUMN IF NOT EXISTS gender VARCHAR(10) COMMENT '성별 (MALE, FEMALE, OTHER)',
ADD COLUMN IF NOT EXISTS language_preference VARCHAR(10) COMMENT '선호 언어',
ADD COLUMN IF NOT EXISTS language_preference_secondary VARCHAR(10) COMMENT '보조 언어';

-- 2. 추가된 컬럼 확인
DESCRIBE users;

-- 3. 테이블 상태 확인
SELECT COUNT(*) as total_users FROM users;

-- 4. 새 컬럼들의 기본값 확인
SELECT
    COUNT(*) as total,
    COUNT(date_of_birth) as has_birth_date,
    COUNT(gender) as has_gender,
    COUNT(language_preference) as has_lang_pref
FROM users;

COMMIT;