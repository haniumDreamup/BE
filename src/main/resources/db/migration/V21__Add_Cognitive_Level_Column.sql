-- V21__Add_Cognitive_Level_Column.sql
-- cognitive_level 컬럼 추가

ALTER TABLE users
ADD COLUMN IF NOT EXISTS cognitive_level VARCHAR(20) DEFAULT 'MODERATE' AFTER phone_number;

-- 기존 데이터에 기본값 설정
UPDATE users SET cognitive_level = 'MODERATE' WHERE cognitive_level IS NULL;
