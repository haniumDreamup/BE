-- V4__Composite_Index_Optimization.sql
-- 복합 인덱스 추가를 통한 쿼리 성능 최적화
-- 김영한 JPA 베스트 프랙티스에 따른 인덱스 설계

-- ============================================================
-- User 테이블 복합 인덱스
-- ============================================================

-- 활성 사용자 + 사용자명 검색 (로그인, 사용자 조회)
CREATE INDEX IF NOT EXISTS idx_user_active_username 
ON users(is_active, username);

-- 활성 사용자 + 이메일 검색 (로그인, 이메일 조회)
CREATE INDEX IF NOT EXISTS idx_user_active_email 
ON users(is_active, email);

-- OAuth 프로바이더 로그인
CREATE INDEX IF NOT EXISTS idx_user_provider 
ON users(provider, provider_id);

-- 최근 로그인 사용자 조회
CREATE INDEX IF NOT EXISTS idx_user_last_login 
ON users(last_login_at DESC);

-- 응급 모드 활성 사용자 조회
CREATE INDEX IF NOT EXISTS idx_user_emergency_mode 
ON users(emergency_mode_enabled, is_active);

-- ============================================================
-- Schedule 테이블 복합 인덱스
-- ============================================================

-- 사용자별 활성 스케줄 + 다음 실행 시간
CREATE INDEX IF NOT EXISTS idx_schedule_user_active_next 
ON schedules(user_id, is_active, next_execution_time);

-- 사용자별 스케줄 타입 + 활성 상태
CREATE INDEX IF NOT EXISTS idx_schedule_user_type_active 
ON schedules(user_id, schedule_type, is_active);

-- 활성 스케줄 + 실행 시간 (시스템 알림용)
CREATE INDEX IF NOT EXISTS idx_schedule_active_next 
ON schedules(is_active, next_execution_time);

-- 사용자별 우선순위 스케줄
CREATE INDEX IF NOT EXISTS idx_schedule_user_priority 
ON schedules(user_id, priority DESC, next_execution_time);

-- 생성자별 스케줄 조회
CREATE INDEX IF NOT EXISTS idx_schedule_creator_active 
ON schedules(created_by_type, is_active, user_id);

-- ============================================================
-- Medication 테이블 복합 인덱스
-- ============================================================

-- 사용자별 활성 약물 + 우선순위
CREATE INDEX IF NOT EXISTS idx_medication_user_active_priority 
ON medications(user_id, is_active, priority_level DESC);

-- 사용자별 약물 상태
CREATE INDEX IF NOT EXISTS idx_medication_user_status 
ON medications(user_id, medication_status, priority_level DESC);

-- 사용자별 약물 유형
CREATE INDEX IF NOT EXISTS idx_medication_user_type 
ON medications(user_id, medication_type, is_active);

-- 약물 기간 조회
CREATE INDEX IF NOT EXISTS idx_medication_dates 
ON medications(start_date, end_date, is_active);

-- 보호자 알림 필요 약물
CREATE INDEX IF NOT EXISTS idx_medication_guardian_alert 
ON medications(guardian_alert_needed, priority_level, user_id);

-- 약물명 검색
CREATE INDEX IF NOT EXISTS idx_medication_name_search 
ON medications(medication_name, generic_name);

-- ============================================================
-- Guardian 테이블 복합 인덱스
-- ============================================================

-- 사용자별 활성 주 보호자
CREATE INDEX IF NOT EXISTS idx_guardian_user_active_primary 
ON guardians(user_id, is_active, is_primary DESC);

-- 사용자별 승인 상태
CREATE INDEX IF NOT EXISTS idx_guardian_user_approval 
ON guardians(user_id, approval_status, is_active);

-- 응급 상황 우선순위
CREATE INDEX IF NOT EXISTS idx_guardian_user_emergency 
ON guardians(user_id, is_active, emergency_priority);

-- 보호자가 관리하는 사용자들
CREATE INDEX IF NOT EXISTS idx_guardian_guardian_active 
ON guardians(guardian_user_id, is_active);

-- 알림 수신 가능 보호자
CREATE INDEX IF NOT EXISTS idx_guardian_alerts 
ON guardians(can_receive_alerts, is_active, user_id);

-- 위치 조회 권한 보호자
CREATE INDEX IF NOT EXISTS idx_guardian_location_view 
ON guardians(can_view_location, is_active, user_id);

-- ============================================================
-- 기존 인덱스 정리 (중복되거나 비효율적인 인덱스 제거)
-- ============================================================

-- Schedule 테이블의 단일 컬럼 인덱스 제거 (복합 인덱스로 대체)
DROP INDEX IF EXISTS idx_schedule_user_id;
DROP INDEX IF EXISTS idx_schedule_next_execution;
DROP INDEX IF EXISTS idx_schedule_active;
DROP INDEX IF EXISTS idx_schedule_type;

-- Medication 테이블의 단일 컬럼 인덱스 제거 (복합 인덱스로 대체)
DROP INDEX IF EXISTS idx_medication_user;
DROP INDEX IF EXISTS idx_medication_status;
DROP INDEX IF EXISTS idx_medication_priority;
DROP INDEX IF EXISTS idx_medication_active_date;
DROP INDEX IF EXISTS idx_medication_name;

-- Guardian 테이블의 단일 컬럼 인덱스 제거 (복합 인덱스로 대체)
DROP INDEX IF EXISTS idx_guardian_user;
DROP INDEX IF EXISTS idx_guardian_guardian_user;
DROP INDEX IF EXISTS idx_guardian_relationship;
DROP INDEX IF EXISTS idx_guardian_approval;

-- ============================================================
-- 통계 업데이트
-- ============================================================

-- MySQL의 경우 테이블 통계 업데이트
ANALYZE TABLE users;
ANALYZE TABLE schedules;
ANALYZE TABLE medications;
ANALYZE TABLE guardians;
ANALYZE TABLE location_history;