-- 사용자 행동 로그 테이블 생성
CREATE TABLE IF NOT EXISTS user_behavior_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_detail JSON,
    device_info JSON,
    page_url VARCHAR(500),
    referrer_url VARCHAR(500),
    response_time_ms INT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    log_level VARCHAR(20) DEFAULT 'INFO',
    PRIMARY KEY (id),
    CONSTRAINT fk_user_behavior_log_user 
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스 생성
CREATE INDEX idx_user_behavior_log_user_id ON user_behavior_logs(user_id);
CREATE INDEX idx_user_behavior_log_session_id ON user_behavior_logs(session_id);
CREATE INDEX idx_user_behavior_log_timestamp ON user_behavior_logs(timestamp);
CREATE INDEX idx_user_behavior_log_action_type ON user_behavior_logs(action_type);
CREATE INDEX idx_user_behavior_log_composite ON user_behavior_logs(user_id, timestamp);

-- 파티셔닝 (월별 파티션 - 선택사항)
-- ALTER TABLE user_behavior_logs
-- PARTITION BY RANGE (YEAR(timestamp) * 100 + MONTH(timestamp)) (
--     PARTITION p202501 VALUES LESS THAN (202502),
--     PARTITION p202502 VALUES LESS THAN (202503),
--     PARTITION p202503 VALUES LESS THAN (202504),
--     PARTITION p202504 VALUES LESS THAN (202505),
--     PARTITION p202505 VALUES LESS THAN (202506),
--     PARTITION p202506 VALUES LESS THAN (202507),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

COMMENT ON TABLE user_behavior_logs IS 'BIF 사용자 행동 로그 - 모든 사용자 인터랙션 추적';
COMMENT ON COLUMN user_behavior_logs.user_id IS '사용자 ID';
COMMENT ON COLUMN user_behavior_logs.session_id IS '세션 ID';
COMMENT ON COLUMN user_behavior_logs.action_type IS '액션 타입 (PAGE_VIEW, BUTTON_CLICK 등)';
COMMENT ON COLUMN user_behavior_logs.action_detail IS '액션 상세 정보 (JSON)';
COMMENT ON COLUMN user_behavior_logs.device_info IS '디바이스 정보 (JSON)';
COMMENT ON COLUMN user_behavior_logs.page_url IS '페이지 URL';
COMMENT ON COLUMN user_behavior_logs.referrer_url IS '이전 페이지 URL';
COMMENT ON COLUMN user_behavior_logs.response_time_ms IS '응답 시간 (밀리초)';
COMMENT ON COLUMN user_behavior_logs.ip_address IS 'IP 주소 (마스킹됨)';
COMMENT ON COLUMN user_behavior_logs.user_agent IS '사용자 에이전트';
COMMENT ON COLUMN user_behavior_logs.timestamp IS '로그 시간';
COMMENT ON COLUMN user_behavior_logs.log_level IS '로그 레벨 (DEBUG, INFO, WARN, ERROR)';