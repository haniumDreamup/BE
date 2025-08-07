-- OAuth2 관련 필드 추가
ALTER TABLE users
ADD COLUMN provider VARCHAR(20),
ADD COLUMN provider_id VARCHAR(255);

-- 인덱스 추가 (OAuth2 로그인 시 빠른 조회를 위해)
CREATE INDEX idx_users_provider_id ON users(provider, provider_id);