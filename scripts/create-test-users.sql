-- 테스트 사용자 생성
-- 비밀번호: Test1234!@#$ (모두 동일)

-- 1. 일반 사용자
INSERT INTO users (
  username, email, password_hash, name, full_name,
  phone_number, cognitive_level, date_of_birth, gender,
  is_active, email_verified, phone_verified,
  created_at, updated_at
) VALUES (
  'testuser',
  'testuser@bifai.co.kr',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy',
  '테스트사용자',
  '테스트 일반 사용자',
  '010-1234-5678',
  'MILD',
  '1970-01-01',
  'MALE',
  1, 1, 1,
  NOW(), NOW()
);

SET @user_id = LAST_INSERT_ID();
INSERT INTO user_roles (user_id, role_id) VALUES (@user_id, 1);

-- 2. 보호자
INSERT INTO users (
  username, email, password_hash, name, full_name,
  phone_number, date_of_birth, gender,
  is_active, email_verified, phone_verified,
  created_at, updated_at
) VALUES (
  'testguardian',
  'guardian@bifai.co.kr',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy',
  '테스트보호자',
  '테스트 보호자',
  '010-9876-5432',
  '1965-01-01',
  'FEMALE',
  1, 1, 1,
  NOW(), NOW()
);

SET @guardian_user_id = LAST_INSERT_ID();
INSERT INTO user_roles (user_id, role_id) VALUES (@guardian_user_id, 2);

-- 3. 관리자
INSERT INTO users (
  username, email, password_hash, name, full_name,
  phone_number, is_active, email_verified, phone_verified,
  created_at, updated_at
) VALUES (
  'admin',
  'admin@bifai.co.kr',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy',
  '관리자',
  '시스템 관리자',
  '010-0000-0000',
  1, 1, 1,
  NOW(), NOW()
);

SET @admin_id = LAST_INSERT_ID();
INSERT INTO user_roles (user_id, role_id) VALUES (@admin_id, 3);

-- 결과 확인
SELECT '✅ 테스트 사용자 3명 생성 완료' as Status;
SELECT u.id, u.username, u.email, u.name, r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
ORDER BY u.id;
