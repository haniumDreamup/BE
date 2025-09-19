-- BIF-AI Backend - Fix Users Table Schema Issues
-- 문제: User 엔티티에 정의된 필드들이 데이터베이스에 누락됨
-- 해결: 누락된 필드들을 안전하게 추가

-- 1. Users 테이블에 누락된 필드들 추가 (존재하지 않는 경우에만)
ALTER TABLE users
ADD COLUMN IF NOT EXISTS name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '사용자 이름',
ADD COLUMN IF NOT EXISTS nickname VARCHAR(50) COMMENT '별명',
ADD COLUMN IF NOT EXISTS date_of_birth DATE COMMENT '생년월일',
ADD COLUMN IF NOT EXISTS gender VARCHAR(10) COMMENT '성별',
ADD COLUMN IF NOT EXISTS address VARCHAR(500) COMMENT '주소',
ADD COLUMN IF NOT EXISTS cognitive_level VARCHAR(20) DEFAULT 'MODERATE' COMMENT '인지 수준',
ADD COLUMN IF NOT EXISTS emergency_contact_name VARCHAR(100) COMMENT '응급 연락처 이름',
ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20) COMMENT '응급 연락처 전화번호',
ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'Asia/Seoul' COMMENT '시간대',
ADD COLUMN IF NOT EXISTS language_preference_secondary VARCHAR(10) COMMENT '보조 언어',
ADD COLUMN IF NOT EXISTS last_activity_at DATETIME COMMENT '마지막 활동 시간',
ADD COLUMN IF NOT EXISTS password_reset_token VARCHAR(255) COMMENT '비밀번호 재설정 토큰',
ADD COLUMN IF NOT EXISTS password_reset_expires_at DATETIME COMMENT '비밀번호 재설정 만료시간',
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE COMMENT '이메일 인증 여부',
ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE COMMENT '전화번호 인증 여부';

-- 2. cognitive_level 컬럼의 타입을 ENUM으로 변경 (안전하게)
-- 먼저 데이터 타입이 INT인지 확인하고 변경
ALTER TABLE users MODIFY COLUMN cognitive_level VARCHAR(20) DEFAULT 'MODERATE';

-- 3. 기존 데이터가 있는 경우 cognitive_level 값 정규화
UPDATE users SET cognitive_level = 'MODERATE' WHERE cognitive_level IS NULL OR cognitive_level = '';
UPDATE users SET cognitive_level = 'MILD' WHERE cognitive_level IN ('70', '71', '72', '73', '74', '75');
UPDATE users SET cognitive_level = 'MODERATE' WHERE cognitive_level IN ('76', '77', '78', '79', '80', '81', '82');
UPDATE users SET cognitive_level = 'SEVERE' WHERE cognitive_level IN ('83', '84', '85');

-- 4. 인덱스 추가 (존재하지 않는 경우에만)
CREATE INDEX IF NOT EXISTS idx_user_active_username ON users(is_active, username);
CREATE INDEX IF NOT EXISTS idx_user_active_email ON users(is_active, email);
CREATE INDEX IF NOT EXISTS idx_user_provider ON users(provider, provider_id);
CREATE INDEX IF NOT EXISTS idx_user_last_login ON users(last_login_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_emergency_mode ON users(emergency_mode_enabled, is_active);

-- 5. 제약조건 추가
ALTER TABLE users ADD CONSTRAINT IF NOT EXISTS chk_cognitive_level
    CHECK (cognitive_level IN ('MILD', 'MODERATE', 'SEVERE', 'UNKNOWN'));