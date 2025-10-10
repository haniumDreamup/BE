-- V22: users 테이블에 cognitive_level 컬럼 추가

ALTER TABLE users 
ADD COLUMN cognitive_level VARCHAR(20) DEFAULT 'MODERATE';

-- 기존 사용자에게 기본값 설정
UPDATE users 
SET cognitive_level = 'MODERATE' 
WHERE cognitive_level IS NULL;

-- 코멘트 추가
ALTER TABLE users 
MODIFY COLUMN cognitive_level VARCHAR(20) DEFAULT 'MODERATE' COMMENT '인지 수준 (MILD/MODERATE/SEVERE/UNKNOWN)';
