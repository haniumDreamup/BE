-- V8__Create_Interaction_Patterns_Table.sql
-- 인터랙션 패턴 분석 결과 저장 테이블

CREATE TABLE IF NOT EXISTS interaction_patterns (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  pattern_type VARCHAR(50) NOT NULL,
  analysis_date DATETIME NOT NULL,
  time_window_start DATETIME NOT NULL,
  time_window_end DATETIME NOT NULL,
  
  -- 패턴 메트릭스
  click_frequency DOUBLE,
  avg_session_duration DOUBLE,
  page_view_count INT,
  unique_pages_visited INT,
  error_rate DOUBLE,
  feature_usage_count INT,
  avg_response_time DOUBLE,
  
  -- JSON 컬럼들
  navigation_paths JSON,
  top_features JSON,
  hourly_activity JSON,
  
  -- 이상 패턴 감지
  is_anomaly BOOLEAN DEFAULT FALSE,
  anomaly_score DOUBLE,
  anomaly_details JSON,
  
  -- 통계 기준값
  baseline_metrics JSON,
  confidence_level DOUBLE,
  sample_size INT,
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  PRIMARY KEY (id),
  
  -- 외래키
  CONSTRAINT fk_pattern_user 
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  
  -- 인덱스
  INDEX idx_pattern_user_type (user_id, pattern_type),
  INDEX idx_pattern_date (analysis_date),
  INDEX idx_pattern_anomaly (is_anomaly, anomaly_score),
  INDEX idx_pattern_time_window (time_window_start, time_window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 패턴 타입 체크 제약
ALTER TABLE interaction_patterns 
ADD CONSTRAINT chk_pattern_type 
CHECK (pattern_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'REALTIME', 
                        'SESSION', 'FEATURE', 'NAVIGATION', 'ERROR', 'PERFORMANCE'));

-- 이상 점수 범위 체크
ALTER TABLE interaction_patterns 
ADD CONSTRAINT chk_anomaly_score 
CHECK (anomaly_score IS NULL OR (anomaly_score >= 0 AND anomaly_score <= 100));

-- 신뢰도 범위 체크
ALTER TABLE interaction_patterns 
ADD CONSTRAINT chk_confidence_level 
CHECK (confidence_level IS NULL OR (confidence_level >= 0 AND confidence_level <= 1));

-- 코멘트 추가
ALTER TABLE interaction_patterns COMMENT = '사용자 인터랙션 패턴 분석 결과';

-- 파티셔닝 (월별)
-- 대량 데이터 처리를 위한 파티셔닝 설정
ALTER TABLE interaction_patterns PARTITION BY RANGE (YEAR(analysis_date) * 100 + MONTH(analysis_date)) (
  PARTITION p202401 VALUES LESS THAN (202402),
  PARTITION p202402 VALUES LESS THAN (202403),
  PARTITION p202403 VALUES LESS THAN (202404),
  PARTITION p202404 VALUES LESS THAN (202405),
  PARTITION p202405 VALUES LESS THAN (202406),
  PARTITION p202406 VALUES LESS THAN (202407),
  PARTITION p202407 VALUES LESS THAN (202408),
  PARTITION p202408 VALUES LESS THAN (202409),
  PARTITION p202409 VALUES LESS THAN (202410),
  PARTITION p202410 VALUES LESS THAN (202411),
  PARTITION p202411 VALUES LESS THAN (202412),
  PARTITION p202412 VALUES LESS THAN (202501),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);