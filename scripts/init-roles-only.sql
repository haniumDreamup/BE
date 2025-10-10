-- Roles 데이터만 먼저 삽입
INSERT INTO roles (name, korean_name, description, is_active, created_at, updated_at)
VALUES
  ('ROLE_USER', '일반 사용자', '인지 지원이 필요한 사용자', 1, NOW(), NOW()),
  ('ROLE_GUARDIAN', '보호자', '사용자를 보호하고 관리하는 역할', 1, NOW(), NOW()),
  ('ROLE_ADMIN', '관리자', '전체 시스템 관리', 1, NOW(), NOW());

SELECT * FROM roles;
