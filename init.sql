-- BIF-AI 초기 데이터베이스 설정
-- 데이터베이스가 이미 생성되어 있으므로 테이블만 생성

-- 권한 부여
GRANT ALL PRIVILEGES ON bifai_db.* TO 'bifai_user'@'%';
FLUSH PRIVILEGES;

-- 초기 데이터 (필요시)
-- INSERT INTO roles (name, description, is_active) VALUES 
-- ('ROLE_USER', 'BIF 사용자', true),
-- ('ROLE_GUARDIAN', '보호자', true),
-- ('ROLE_ADMIN', '관리자', true);