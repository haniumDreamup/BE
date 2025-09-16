-- V9__Create_AB_Test_Tables.sql
-- A/B 테스트 프레임워크 테이블 생성

-- 실험 테이블
CREATE TABLE experiments (
  experiment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  experiment_key VARCHAR(100) NOT NULL UNIQUE,
  experiment_type VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  is_active BOOLEAN DEFAULT FALSE,
  
  -- 대상 조건
  target_criteria JSON,
  
  -- 트래픽 할당
  traffic_allocation JSON,
  
  -- 샘플 크기
  sample_size_target INT,
  current_participants INT DEFAULT 0,
  
  -- 일정
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  actual_start_date DATETIME,
  actual_end_date DATETIME,
  
  -- 메타데이터
  metadata JSON,
  created_by VARCHAR(100),
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_experiment_key (experiment_key),
  INDEX idx_experiment_status (status),
  INDEX idx_experiment_active (is_active),
  INDEX idx_experiment_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 테스트 그룹 테이블
CREATE TABLE test_groups (
  group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  group_name VARCHAR(100) NOT NULL,
  group_type VARCHAR(50) NOT NULL,
  is_control BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  
  -- 샘플 크기 제약
  min_sample_size INT,
  max_sample_size INT,
  current_size INT DEFAULT 0,
  
  description TEXT,
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (experiment_id) REFERENCES experiments(experiment_id) ON DELETE CASCADE,
  UNIQUE KEY uk_experiment_group (experiment_id, group_name),
  INDEX idx_group_experiment (experiment_id),
  INDEX idx_group_type (group_type),
  INDEX idx_group_control (is_control)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 테스트 변형 테이블
CREATE TABLE test_variants (
  variant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  variant_key VARCHAR(100) NOT NULL,
  variant_name VARCHAR(255) NOT NULL,
  is_control BOOLEAN DEFAULT FALSE,
  is_winner BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,
  
  -- 설정
  config JSON,
  feature_flags JSON,
  
  -- 성과 지표
  participants INT DEFAULT 0,
  conversions INT DEFAULT 0,
  conversion_rate DECIMAL(5,2),
  engagement_score DECIMAL(5,2),
  avg_session_duration INT,
  error_rate DECIMAL(5,2),
  
  -- 통계
  p_value DECIMAL(10,8),
  confidence_level DECIMAL(5,2),
  
  description TEXT,
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (experiment_id) REFERENCES experiments(experiment_id) ON DELETE CASCADE,
  UNIQUE KEY uk_experiment_variant (experiment_id, variant_key),
  INDEX idx_variant_experiment (experiment_id),
  INDEX idx_variant_control (is_control),
  INDEX idx_variant_winner (is_winner)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 테스트 그룹 할당 테이블
CREATE TABLE test_group_assignments (
  assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  experiment_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  variant_id BIGINT,
  
  -- 할당 정보
  assignment_hash VARCHAR(64) UNIQUE,
  assignment_reason VARCHAR(100),
  is_active BOOLEAN DEFAULT TRUE,
  opted_out BOOLEAN DEFAULT FALSE,
  
  -- 노출 추적
  first_exposure_at DATETIME,
  last_exposure_at DATETIME,
  exposure_count INT DEFAULT 0,
  
  -- 전환 추적
  conversion_achieved BOOLEAN DEFAULT FALSE,
  conversion_at DATETIME,
  conversion_value DECIMAL(10,2),
  
  -- 피드백
  satisfaction_score INT,
  feedback TEXT,
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (experiment_id) REFERENCES experiments(experiment_id) ON DELETE CASCADE,
  FOREIGN KEY (group_id) REFERENCES test_groups(group_id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES test_variants(variant_id) ON DELETE SET NULL,
  
  UNIQUE KEY uk_user_experiment (user_id, experiment_id),
  INDEX idx_assignment_user (user_id),
  INDEX idx_assignment_experiment (experiment_id),
  INDEX idx_assignment_group (group_id),
  INDEX idx_assignment_variant (variant_id),
  INDEX idx_assignment_active (is_active),
  INDEX idx_assignment_conversion (conversion_achieved),
  INDEX idx_assignment_hash (assignment_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 실험 이벤트 로그 테이블 (옵션)
CREATE TABLE experiment_events (
  event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  user_id BIGINT,
  assignment_id BIGINT,
  
  event_type VARCHAR(50) NOT NULL,
  event_name VARCHAR(100),
  event_value DECIMAL(10,2),
  event_data JSON,
  
  -- 타임스탬프
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (experiment_id) REFERENCES experiments(experiment_id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (assignment_id) REFERENCES test_group_assignments(assignment_id) ON DELETE CASCADE,
  
  INDEX idx_event_experiment (experiment_id),
  INDEX idx_event_user (user_id),
  INDEX idx_event_type (event_type),
  INDEX idx_event_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;