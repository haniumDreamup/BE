-- V12__Performance_Optimization_Indexes.sql
-- 성능 최적화를 위한 인덱스 추가 및 정리

-- ============================================
-- 1. 복합 인덱스 추가 (자주 함께 사용되는 컬럼들)
-- ============================================

-- Users 테이블: 활성 사용자 빠른 조회
CREATE INDEX IF NOT EXISTS idx_users_active_cognitive ON users(is_active, cognitive_level);
CREATE INDEX IF NOT EXISTS idx_users_active_created ON users(is_active, created_at DESC);

-- Medications 테이블: 사용자별 시간대별 복약 조회
CREATE INDEX IF NOT EXISTS idx_medications_user_time ON medications(user_id, scheduled_time);
CREATE INDEX IF NOT EXISTS idx_medications_user_active ON medications(user_id, is_active, scheduled_time);

-- MedicationAdherence 테이블: 복약 순응도 계산용
CREATE INDEX IF NOT EXISTS idx_adherence_medication_date ON medication_adherence(medication_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_adherence_user_date_taken ON medication_adherence(user_id, scheduled_date, taken);

-- LocationHistory 테이블: 위치 추적 최적화
CREATE INDEX IF NOT EXISTS idx_location_user_timestamp ON location_history(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_location_user_safezone ON location_history(user_id, is_in_safe_zone, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_location_timestamp ON location_history(timestamp);

-- Schedules 테이블: 일정 조회 최적화
CREATE INDEX IF NOT EXISTS idx_schedules_user_time ON schedules(user_id, scheduled_time);
CREATE INDEX IF NOT EXISTS idx_schedules_user_completed ON schedules(user_id, completed, scheduled_time);
CREATE INDEX IF NOT EXISTS idx_schedules_user_priority ON schedules(user_id, priority, scheduled_time);

-- ActivityLogs 테이블: 활동 패턴 분석용
CREATE INDEX IF NOT EXISTS idx_activity_user_timestamp ON activity_logs(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_activity_user_type_time ON activity_logs(user_id, activity_type, timestamp DESC);

-- Notifications 테이블: 알림 조회 최적화
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_type ON notifications(user_id, notification_type, created_at DESC);

-- GuardianRelationships 테이블: 보호자 권한 빠른 확인
CREATE INDEX IF NOT EXISTS idx_guardian_rel_user_status ON guardian_relationships(user_id, status, permission_level);
CREATE INDEX IF NOT EXISTS idx_guardian_rel_guardian_status ON guardian_relationships(guardian_id, status);

-- Emergency 테이블: 긴급 상황 조회
CREATE INDEX IF NOT EXISTS idx_emergency_user_status ON emergency(user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_emergency_user_resolved ON emergency(user_id, is_resolved, created_at DESC);

-- HealthMetrics 테이블: 건강 지표 시계열 조회
CREATE INDEX IF NOT EXISTS idx_health_user_recorded ON health_metrics(user_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_user_type_time ON health_metrics(user_id, metric_type, recorded_at DESC);

-- ============================================
-- 2. 커버링 인덱스 (SELECT 절의 모든 컬럼 포함)
-- ============================================

-- 자주 조회되는 사용자 기본 정보
CREATE INDEX IF NOT EXISTS idx_users_covering_basic ON users(user_id, username, email, is_active);

-- 대시보드용 복약 요약 정보
CREATE INDEX IF NOT EXISTS idx_medications_covering_summary ON medications(
  user_id, medication_id, medication_name, scheduled_time, is_active
);

-- ============================================
-- 3. 부분 인덱스 (특정 조건만 인덱싱)
-- ============================================

-- 활성 사용자만 인덱싱
CREATE INDEX IF NOT EXISTS idx_users_active_only ON users(user_id) WHERE is_active = true;

-- 미완료 일정만 인덱싱
CREATE INDEX IF NOT EXISTS idx_schedules_incomplete ON schedules(user_id, scheduled_time) 
WHERE completed = false;

-- 읽지 않은 알림만 인덱싱
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, created_at DESC) 
WHERE is_read = false;

-- 활성 보호자 관계만 인덱싱
CREATE INDEX IF NOT EXISTS idx_guardian_active ON guardian_relationships(user_id, guardian_id) 
WHERE status = 'ACTIVE';

-- ============================================
-- 4. 불필요한 인덱스 제거
-- ============================================

-- 단일 컬럼 인덱스 중 복합 인덱스로 대체 가능한 것들 제거
-- (주의: 실제 운영 환경에서는 인덱스 사용 통계를 확인 후 제거)

-- DROP INDEX IF EXISTS idx_medications_user_id;  -- idx_medications_user_time으로 대체
-- DROP INDEX IF EXISTS idx_schedules_user_id;     -- idx_schedules_user_time으로 대체
-- DROP INDEX IF EXISTS idx_activity_logs_user_id; -- idx_activity_user_timestamp으로 대체

-- ============================================
-- 5. 통계 정보 업데이트
-- ============================================

-- MySQL 8.0 이상에서 히스토그램 통계 생성
ANALYZE TABLE users UPDATE HISTOGRAM ON cognitive_level, is_active;
ANALYZE TABLE medications UPDATE HISTOGRAM ON user_id, scheduled_time;
ANALYZE TABLE medication_adherence UPDATE HISTOGRAM ON taken, scheduled_date;
ANALYZE TABLE location_history UPDATE HISTOGRAM ON user_id, is_in_safe_zone;
ANALYZE TABLE schedules UPDATE HISTOGRAM ON completed, priority;
ANALYZE TABLE activity_logs UPDATE HISTOGRAM ON activity_type;
ANALYZE TABLE notifications UPDATE HISTOGRAM ON is_read, notification_type;
ANALYZE TABLE guardian_relationships UPDATE HISTOGRAM ON status, permission_level;

-- ============================================
-- 6. 인덱스 힌트를 위한 주석
-- ============================================

-- 쿼리 최적화 힌트 예시:
-- SELECT /*+ INDEX(users idx_users_active_cognitive) */ * FROM users WHERE is_active = true AND cognitive_level = 'MILD';
-- SELECT /*+ INDEX(medications idx_medications_user_time) */ * FROM medications WHERE user_id = ? AND scheduled_time BETWEEN ? AND ?;
-- SELECT /*+ NO_INDEX(location_history idx_location_timestamp) */ * FROM location_history WHERE user_id = ?;