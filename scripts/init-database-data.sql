-- BIF-AI 초기 데이터베이스 데이터
-- Production RDS에 적용

-- ============================================
-- 1. 역할(Roles) 데이터
-- ============================================
INSERT INTO roles (name, korean_name, description, is_active, created_at, updated_at)
VALUES
  ('ROLE_USER', '일반 사용자', '인지 지원이 필요한 사용자', 1, NOW(), NOW()),
  ('ROLE_GUARDIAN', '보호자', '사용자를 보호하고 관리하는 역할', 1, NOW(), NOW()),
  ('ROLE_ADMIN', '관리자', '전체 시스템 관리', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  description = VALUES(description),
  updated_at = NOW();

-- ============================================
-- 2. 알림 템플릿 데이터
-- ============================================
INSERT INTO notification_templates (id, template_key, title, body, category, priority, created_at, updated_at)
VALUES
  (1, 'EMERGENCY_ALERT', '긴급 상황', '{userName}님에게 긴급 상황이 발생했습니다!', 'EMERGENCY', 'HIGH', NOW(), NOW()),
  (2, 'FALL_DETECTED', '낙상 감지', '{userName}님의 낙상이 감지되었습니다. 확인이 필요합니다.', 'HEALTH', 'HIGH', NOW(), NOW()),
  (3, 'MEDICATION_REMINDER', '약 복용 시간', '{medicationName} 복용 시간입니다.', 'MEDICATION', 'MEDIUM', NOW(), NOW()),
  (4, 'GEOFENCE_EXIT', '안전 구역 이탈', '{userName}님이 설정된 안전 구역을 벗어났습니다.', 'LOCATION', 'HIGH', NOW(), NOW()),
  (5, 'SCHEDULE_REMINDER', '일정 알림', '{scheduleTitle} 일정이 곧 시작됩니다.', 'SCHEDULE', 'MEDIUM', NOW(), NOW()),
  (6, 'DAILY_SUMMARY', '일일 요약', '오늘의 활동 요약입니다.', 'SUMMARY', 'LOW', NOW(), NOW()),
  (7, 'WEEKLY_REPORT', '주간 리포트', '이번 주 건강 리포트입니다.', 'REPORT', 'LOW', NOW(), NOW()),
  (8, 'GUARDIAN_REQUEST', '보호자 요청', '{guardianName}님이 보호자로 등록을 요청했습니다.', 'RELATIONSHIP', 'MEDIUM', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  body = VALUES(body),
  updated_at = NOW();

-- ============================================
-- 3. 테스트 사용자 (개발/테스트용)
-- ============================================
-- 비밀번호: Test1234!@#$ (BCrypt 해시)
INSERT INTO users (
  id, username, email, password, name, phone_number,
  cognitive_level, date_of_birth, gender, address,
  profile_image_url, enabled, account_non_locked,
  created_at, updated_at
) VALUES (
  1,
  'testuser',
  'testuser@bifai.co.kr',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy', -- Test1234!@#$
  '테스트 사용자',
  '010-1234-5678',
  'MILD_IMPAIRMENT',
  '1970-01-01',
  'MALE',
  '서울특별시 강남구',
  NULL,
  TRUE,
  TRUE,
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  updated_at = NOW();

-- 테스트 사용자에게 USER 역할 부여
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ============================================
-- 4. 테스트 보호자
-- ============================================
INSERT INTO users (
  id, username, email, password, name, phone_number,
  date_of_birth, gender, enabled, account_non_locked,
  created_at, updated_at
) VALUES (
  2,
  'testguardian',
  'guardian@bifai.co.kr',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy', -- Test1234!@#$
  '테스트 보호자',
  '010-9876-5432',
  '1965-01-01',
  'FEMALE',
  TRUE,
  TRUE,
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  updated_at = NOW();

-- 보호자 역할 부여
INSERT INTO user_roles (user_id, role_id)
VALUES (2, 2)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ============================================
-- 5. 관리자 계정
-- ============================================
INSERT INTO users (
  id, username, email, password, name, phone_number,
  enabled, account_non_locked,
  created_at, updated_at
) VALUES (
  3,
  'admin',
  'admin@bifai.co.kr',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCy', -- Test1234!@#$
  '시스템 관리자',
  '010-0000-0000',
  TRUE,
  TRUE,
  NOW(),
  NOW()
) ON DUPLICATE KEY UPDATE
  updated_at = NOW();

-- 관리자 역할 부여
INSERT INTO user_roles (user_id, role_id)
VALUES (3, 3)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ============================================
-- 6. 보호자 관계 설정
-- ============================================
INSERT INTO guardians (id, user_id, created_at, updated_at)
VALUES (1, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO guardian_relationships (
  id, user_id, guardian_id, relationship_type,
  status, verified, created_at, updated_at
) VALUES (
  1, 1, 1, 'FAMILY',
  'APPROVED', TRUE, NOW(), NOW()
) ON DUPLICATE KEY UPDATE
  status = 'APPROVED',
  updated_at = NOW();

-- ============================================
-- 완료 메시지
-- ============================================
SELECT '✅ 초기 데이터 삽입 완료!' as Status;
SELECT 'Roles: 3개, Users: 3개 (User, Guardian, Admin), Templates: 8개' as Summary;
