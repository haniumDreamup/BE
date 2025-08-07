-- BIF-AI Backend Initial Schema Migration
-- Creates all necessary tables and indexes for the cognitive assistance system

-- Users and Guardian tables
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL COMMENT '사용자 이메일 (로그인 ID)',
    password_hash VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    name VARCHAR(100) NOT NULL COMMENT '사용자 실명',
    nickname VARCHAR(50) COMMENT '별명 또는 호칭',
    phone_number VARCHAR(20) COMMENT '전화번호',
    date_of_birth DATE COMMENT '생년월일',
    gender VARCHAR(10) COMMENT '성별',
    cognitive_level VARCHAR(20) DEFAULT 'MODERATE' COMMENT 'BIF 인지 수준',
    emergency_contact_name VARCHAR(100) COMMENT '응급 연락처 이름',
    emergency_contact_phone VARCHAR(20) COMMENT '응급 연락처 전화번호',
    profile_image_url VARCHAR(500) COMMENT '프로필 사진 URL',
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul' COMMENT '사용자 시간대',
    language_preference VARCHAR(10) DEFAULT 'ko' COMMENT '선호 언어',
    is_active BOOLEAN DEFAULT TRUE COMMENT '계정 활성화 상태',
    last_login_at DATETIME COMMENT '마지막 로그인 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성 시간',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '정보 수정 시간'
) COMMENT 'BIF 사용자 정보';

CREATE TABLE guardians (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '보호 대상 사용자 ID',
    name VARCHAR(100) NOT NULL COMMENT '보호자 이름',
    relationship VARCHAR(50) NOT NULL COMMENT '관계 (부모, 배우자, 자녀, 친구 등)',
    primary_phone VARCHAR(20) NOT NULL COMMENT '주 연락처',
    secondary_phone VARCHAR(20) COMMENT '보조 연락처',
    email VARCHAR(255) COMMENT '이메일 주소',
    address VARCHAR(500) COMMENT '주소',
    emergency_priority INTEGER DEFAULT 1 COMMENT '응급시 연락 우선순위',
    notification_preferences TEXT COMMENT '알림 선호도 (JSON)',
    is_primary BOOLEAN DEFAULT FALSE COMMENT '주 보호자 여부',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT 'BIF 사용자 보호자 정보';

-- Device management tables
CREATE TABLE devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '소유자 사용자 ID',
    device_identifier VARCHAR(255) UNIQUE NOT NULL COMMENT '기기 고유 식별자 (MAC 주소 등)',
    device_name VARCHAR(100) NOT NULL COMMENT '기기 이름',
    device_type VARCHAR(50) NOT NULL COMMENT '기기 유형',
    manufacturer VARCHAR(100) COMMENT '제조사',
    model VARCHAR(100) COMMENT '모델명',
    os_type VARCHAR(50) COMMENT '운영체제',
    os_version VARCHAR(50) COMMENT '운영체제 버전',
    app_version VARCHAR(50) COMMENT '앱 버전',
    push_token VARCHAR(500) COMMENT '푸시 알림 토큰',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    last_sync_at DATETIME COMMENT '마지막 동기화 시간',
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT '사용자 디바이스 정보';

CREATE TABLE battery_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '디바이스 ID',
    battery_level INTEGER NOT NULL COMMENT '배터리 잔량 (%)',
    is_charging BOOLEAN DEFAULT FALSE COMMENT '충전 중 여부',
    battery_status VARCHAR(20) COMMENT '배터리 상태',
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '기록 시간',
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
) COMMENT '디바이스 배터리 이력';

CREATE TABLE connectivity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '디바이스 ID',
    connection_type VARCHAR(20) NOT NULL COMMENT '연결 유형 (WIFI, CELLULAR, BLUETOOTH)',
    connection_status VARCHAR(20) NOT NULL COMMENT '연결 상태',
    signal_strength INTEGER COMMENT '신호 강도',
    network_name VARCHAR(100) COMMENT '네트워크 이름',
    connected_at DATETIME COMMENT '연결 시간',
    disconnected_at DATETIME COMMENT '연결 해제 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '디바이스 연결 로그';

-- Content storage tables
CREATE TABLE captured_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    device_id BIGINT COMMENT '촬영 디바이스 ID',
    s3_url VARCHAR(500) NOT NULL COMMENT 'S3 저장 URL',
    s3_bucket VARCHAR(100) COMMENT 'S3 버킷 이름',
    s3_key VARCHAR(500) COMMENT 'S3 객체 키',
    image_type VARCHAR(30) COMMENT 'BIF 친화적 이미지 유형',
    file_size_bytes BIGINT COMMENT '파일 크기 (바이트)',
    image_width INTEGER COMMENT '이미지 너비',
    image_height INTEGER COMMENT '이미지 높이',
    quality_score DECIMAL(3,2) COMMENT '이미지 품질 점수 (0.0-1.0)',
    is_analyzed BOOLEAN DEFAULT FALSE COMMENT 'AI 분석 완료 여부',
    user_feedback VARCHAR(20) COMMENT '사용자 피드백',
    privacy_level VARCHAR(20) DEFAULT 'PRIVATE' COMMENT '프라이버시 레벨',
    gps_latitude DECIMAL(10, 8) COMMENT 'GPS 위도',
    gps_longitude DECIMAL(11, 8) COMMENT 'GPS 경도',
    gps_accuracy DECIMAL(8, 2) COMMENT 'GPS 정확도 (미터)',
    captured_at DATETIME NOT NULL COMMENT '촬영 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '소프트 삭제 시간',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) COMMENT '사용자 촬영 이미지 정보';

CREATE TABLE analysis_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    captured_image_id BIGINT COMMENT '관련 이미지 ID',
    analysis_type VARCHAR(50) NOT NULL COMMENT '분석 유형',
    ai_model_name VARCHAR(100) COMMENT '사용된 AI 모델',
    ai_model_version VARCHAR(50) COMMENT 'AI 모델 버전',
    raw_response TEXT COMMENT '원본 AI 응답 (JSON)',
    simplified_result TEXT COMMENT 'BIF 사용자용 간단한 결과',
    confidence_score DECIMAL(5,4) COMMENT '신뢰도 점수 (0.0-1.0)',
    action_recommendations TEXT COMMENT '행동 권장사항 (JSON)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '분석 상태',
    error_message TEXT COMMENT '오류 메시지',
    processing_time_ms INTEGER COMMENT '처리 시간 (밀리초)',
    analyzed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '분석 완료 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (captured_image_id) REFERENCES captured_images(id) ON DELETE CASCADE
) COMMENT 'AI 분석 결과';

CREATE TABLE content_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    captured_image_id BIGINT NOT NULL COMMENT '이미지 ID',
    exif_data TEXT COMMENT 'EXIF 메타데이터 (JSON)',
    camera_make VARCHAR(100) COMMENT '카메라 제조사',
    camera_model VARCHAR(100) COMMENT '카메라 모델',
    lens_info VARCHAR(200) COMMENT '렌즈 정보',
    focal_length DECIMAL(5,2) COMMENT '초점 거리',
    aperture DECIMAL(3,1) COMMENT '조리개 값',
    shutter_speed VARCHAR(20) COMMENT '셔터 속도',
    iso_value INTEGER COMMENT 'ISO 감도',
    white_balance VARCHAR(50) COMMENT '화이트 밸런스',
    flash_used BOOLEAN COMMENT '플래시 사용 여부',
    quality_assessment TEXT COMMENT '품질 평가 (JSON)',
    privacy_filtered BOOLEAN DEFAULT TRUE COMMENT '프라이버시 필터링 적용 여부',
    extraction_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '메타데이터 추출 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (captured_image_id) REFERENCES captured_images(id) ON DELETE CASCADE
) COMMENT '이미지 메타데이터';

-- Schedule and notification tables
CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    schedule_title VARCHAR(200) NOT NULL COMMENT '일정 제목',
    schedule_description TEXT COMMENT '일정 설명',
    schedule_type VARCHAR(30) NOT NULL COMMENT '일정 유형',
    recurrence_type VARCHAR(20) DEFAULT 'ONCE' COMMENT '반복 유형',
    recurrence_pattern VARCHAR(100) COMMENT '반복 패턴',
    custom_days VARCHAR(20) COMMENT '사용자 지정 요일',
    interval_value INTEGER COMMENT '간격 값',
    start_date DATE NOT NULL COMMENT '시작 날짜',
    end_date DATE COMMENT '종료 날짜',
    schedule_time TIME NOT NULL COMMENT '실행 시간',
    duration_minutes INTEGER COMMENT '예상 소요 시간',
    priority_level VARCHAR(15) DEFAULT 'MEDIUM' COMMENT '우선순위',
    requires_confirmation BOOLEAN DEFAULT FALSE COMMENT '확인 필요 여부',
    visual_indicator VARCHAR(50) COMMENT '시각적 표시',
    guardian_notification BOOLEAN DEFAULT FALSE COMMENT '보호자 알림 여부',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    last_executed_at DATETIME COMMENT '마지막 실행 시간',
    next_execution_at DATETIME COMMENT '다음 실행 시간',
    execution_count INTEGER DEFAULT 0 COMMENT '실행 횟수',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT 'BIF 사용자 일정 관리';

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '수신 사용자 ID',
    schedule_id BIGINT COMMENT '관련 일정 ID',
    notification_type VARCHAR(30) NOT NULL COMMENT '알림 유형',
    title VARCHAR(200) NOT NULL COMMENT '알림 제목',
    content TEXT NOT NULL COMMENT '알림 내용',
    simple_content VARCHAR(500) COMMENT 'BIF 사용자용 간단한 내용',
    delivery_channels VARCHAR(200) COMMENT '전달 채널 (JSON 배열)',
    priority_level VARCHAR(15) DEFAULT 'MEDIUM' COMMENT '우선순위',
    urgency_level VARCHAR(15) DEFAULT 'NORMAL' COMMENT '긴급도',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '알림 상태',
    scheduled_at DATETIME NOT NULL COMMENT '발송 예정 시간',
    sent_at DATETIME COMMENT '실제 발송 시간',
    read_at DATETIME COMMENT '읽음 시간',
    action_taken VARCHAR(100) COMMENT '사용자 취한 행동',
    guardian_copy BOOLEAN DEFAULT FALSE COMMENT '보호자 사본 전송 여부',
    voice_enabled BOOLEAN DEFAULT FALSE COMMENT '음성 알림 사용',
    vibration_enabled BOOLEAN DEFAULT TRUE COMMENT '진동 알림 사용',
    sound_file VARCHAR(200) COMMENT '알림음 파일',
    retry_count INTEGER DEFAULT 0 COMMENT '재시도 횟수',
    max_retry_count INTEGER DEFAULT 3 COMMENT '최대 재시도 횟수',
    expires_at DATETIME COMMENT '알림 만료 시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
) COMMENT 'BIF 사용자 알림';

CREATE TABLE notification_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL COMMENT '알림 ID',
    delivery_channel VARCHAR(30) NOT NULL COMMENT '전달 채널',
    delivery_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '전달 상태',
    attempt_count INTEGER DEFAULT 0 COMMENT '시도 횟수',
    delivered_at DATETIME COMMENT '전달 완료 시간',
    failed_at DATETIME COMMENT '전달 실패 시간',
    error_code VARCHAR(50) COMMENT '오류 코드',
    error_message TEXT COMMENT '오류 메시지',
    external_message_id VARCHAR(200) COMMENT '외부 서비스 메시지 ID',
    processing_time_ms INTEGER COMMENT '처리 시간',
    cost_amount DECIMAL(10,4) COMMENT '전송 비용',
    cost_currency VARCHAR(3) DEFAULT 'KRW' COMMENT '비용 통화',
    device_acknowledged BOOLEAN DEFAULT FALSE COMMENT '기기 수신 확인',
    user_acknowledged BOOLEAN DEFAULT FALSE COMMENT '사용자 확인',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
) COMMENT '알림 전달 상세 추적';

CREATE TABLE reminder_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    template_name VARCHAR(100) NOT NULL COMMENT '템플릿 이름',
    template_type VARCHAR(30) NOT NULL COMMENT '템플릿 유형',
    complexity_level VARCHAR(15) DEFAULT 'SIMPLE' COMMENT '복잡도 레벨',
    emotional_tone VARCHAR(20) DEFAULT 'FRIENDLY' COMMENT '감정적 톤',
    title_template VARCHAR(200) NOT NULL COMMENT '제목 템플릿',
    content_template TEXT NOT NULL COMMENT '내용 템플릿',
    personalization_fields TEXT COMMENT '개인화 필드 (JSON)',
    usage_count INTEGER DEFAULT 0 COMMENT '사용 횟수',
    success_rate DECIMAL(5,2) COMMENT '성공률 (%)',
    user_satisfaction DECIMAL(3,2) COMMENT '사용자 만족도 (1-5)',
    avg_response_time INTEGER COMMENT '평균 응답 시간 (초)',
    performance_score DECIMAL(5,2) COMMENT '성과 점수',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT '알림 템플릿';

-- Activity and medication tracking tables
CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    device_id BIGINT COMMENT '기록 디바이스 ID',
    activity_type VARCHAR(50) NOT NULL COMMENT 'BIF 친화적 활동 유형',
    activity_title VARCHAR(100) NOT NULL COMMENT '활동 제목',
    activity_description VARCHAR(500) COMMENT '활동 설명',
    activity_date DATETIME NOT NULL COMMENT '활동 발생 시간',
    success_status VARCHAR(20) NOT NULL COMMENT '활동 성공 여부',
    duration_minutes INTEGER COMMENT '활동 지속 시간',
    difficulty_level VARCHAR(20) COMMENT '활동 난이도',
    mood_before VARCHAR(20) COMMENT '활동 전 기분',
    mood_after VARCHAR(20) COMMENT '활동 후 기분',
    confidence_score INTEGER COMMENT '자신감 점수 (1-10)',
    help_needed BOOLEAN DEFAULT FALSE COMMENT '도움 필요 여부',
    guardian_notified BOOLEAN DEFAULT FALSE COMMENT '보호자 알림 여부',
    location_description VARCHAR(100) COMMENT '활동 장소 설명',
    activity_latitude DECIMAL(10, 8) COMMENT '활동 위치 위도',
    activity_longitude DECIMAL(11, 8) COMMENT '활동 위치 경도',
    activity_location_accuracy DECIMAL(8, 2) COMMENT '위치 정확도',
    notes VARCHAR(1000) COMMENT '사용자 메모',
    logged_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '로그 기록 시간',
    sync_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '동기화 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) COMMENT 'BIF 사용자 활동 로그';

CREATE TABLE activity_metadata (
    activity_log_id BIGINT NOT NULL COMMENT '활동 로그 ID',
    metadata_key VARCHAR(100) NOT NULL COMMENT '메타데이터 키',
    metadata_value VARCHAR(1000) COMMENT '메타데이터 값',
    PRIMARY KEY (activity_log_id, metadata_key),
    FOREIGN KEY (activity_log_id) REFERENCES activity_logs(id) ON DELETE CASCADE
) COMMENT '활동 메타데이터';

CREATE TABLE medications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    medication_name VARCHAR(100) NOT NULL COMMENT '약 이름',
    generic_name VARCHAR(100) COMMENT '일반명',
    simple_description VARCHAR(500) COMMENT '쉬운 설명',
    medication_type VARCHAR(30) NOT NULL COMMENT '약물 유형',
    dosage_form VARCHAR(20) NOT NULL COMMENT '제형',
    dosage_amount DECIMAL(11,3) NOT NULL COMMENT '1회 복용량',
    dosage_unit VARCHAR(20) NOT NULL COMMENT '복용량 단위',
    daily_frequency INTEGER NOT NULL COMMENT '하루 복용 횟수',
    timing_instruction VARCHAR(20) COMMENT '복용 시점',
    start_date DATE NOT NULL COMMENT '복용 시작일',
    end_date DATE COMMENT '복용 종료일',
    total_days INTEGER COMMENT '총 복용 일수',
    priority_level VARCHAR(15) DEFAULT 'MEDIUM' COMMENT '우선순위',
    is_active BOOLEAN DEFAULT TRUE COMMENT '현재 복용 중 여부',
    medication_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '복약 상태',
    prescribing_doctor VARCHAR(100) COMMENT '처방의사',
    pharmacy_name VARCHAR(100) COMMENT '조제 약국',
    prescription_number VARCHAR(20) COMMENT '처방전 번호',
    pill_color VARCHAR(20) COMMENT '약 색깔',
    pill_shape VARCHAR(20) COMMENT '약 모양',
    pill_image_url VARCHAR(500) COMMENT '약 이미지 URL',
    side_effects VARCHAR(1000) COMMENT '부작용 정보',
    important_notes VARCHAR(1000) COMMENT '중요 주의사항',
    storage_instructions VARCHAR(500) COMMENT '보관 방법',
    requires_food BOOLEAN COMMENT '음식과 함께 복용 필요',
    avoid_alcohol BOOLEAN DEFAULT FALSE COMMENT '알코올 금지 여부',
    guardian_alert_needed BOOLEAN DEFAULT FALSE COMMENT '보호자 알림 필요',
    user_notes VARCHAR(1000) COMMENT '사용자 메모',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT 'BIF 사용자 복약 정보';

CREATE TABLE medication_times (
    medication_id BIGINT NOT NULL COMMENT '약물 ID',
    intake_time TIME NOT NULL COMMENT '복용 시간',
    PRIMARY KEY (medication_id, intake_time),
    FOREIGN KEY (medication_id) REFERENCES medications(id) ON DELETE CASCADE
) COMMENT '복약 시간';

CREATE TABLE medication_adherence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    medication_id BIGINT NOT NULL COMMENT '약물 ID',
    schedule_id BIGINT COMMENT '관련 일정 ID',
    adherence_date DATE NOT NULL COMMENT '복약 예정/실행 날짜',
    scheduled_time TIME NOT NULL COMMENT '복약 예정 시간',
    actual_taken_time DATETIME COMMENT '실제 복약 시간',
    adherence_status VARCHAR(20) DEFAULT 'SCHEDULED' COMMENT '복약 순응 상태',
    actual_dosage DECIMAL(6,3) COMMENT '실제 복용량',
    dosage_unit VARCHAR(20) COMMENT '복용량 단위',
    delay_minutes INTEGER COMMENT '지연 시간 (분)',
    skip_reason VARCHAR(30) COMMENT '건너뛴 이유',
    skip_description VARCHAR(500) COMMENT '건너뛴 이유 설명',
    side_effect_reported BOOLEAN DEFAULT FALSE COMMENT '부작용 보고 여부',
    side_effect_description VARCHAR(1000) COMMENT '부작용 설명',
    difficulty_score INTEGER COMMENT '복약 난이도 (1-10)',
    satisfaction_score INTEGER COMMENT '복약 만족도 (1-10)',
    reminder_sent BOOLEAN DEFAULT FALSE COMMENT '알림 전송 여부',
    reminder_sent_time DATETIME COMMENT '알림 전송 시간',
    reminder_count INTEGER DEFAULT 0 COMMENT '알림 횟수',
    guardian_notified BOOLEAN DEFAULT FALSE COMMENT '보호자 알림 여부',
    guardian_notification_time DATETIME COMMENT '보호자 알림 시간',
    confirmation_method VARCHAR(30) COMMENT '복약 확인 방법',
    confirmation_image_url VARCHAR(500) COMMENT '복약 확인 이미지',
    location_description VARCHAR(100) COMMENT '복약 장소 설명',
    taken_latitude DECIMAL(10, 8) COMMENT '복약 위치 위도',
    taken_longitude DECIMAL(11, 8) COMMENT '복약 위치 경도',
    taken_location_accuracy DECIMAL(8, 2) COMMENT '위치 정확도',
    notes VARCHAR(1000) COMMENT '복약 관련 메모',
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '기록 생성 시간',
    sync_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '동기화 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (medication_id) REFERENCES medications(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL
) COMMENT 'BIF 사용자 복약 순응도';

-- Health metrics table
CREATE TABLE health_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    device_id BIGINT COMMENT '측정 디바이스 ID',
    metric_type VARCHAR(30) NOT NULL COMMENT '건강 지표 유형',
    value DECIMAL(11,3) NOT NULL COMMENT '측정값',
    unit VARCHAR(20) COMMENT '측정 단위',
    secondary_value DECIMAL(11,3) COMMENT '보조 측정값',
    secondary_unit VARCHAR(20) COMMENT '보조 측정값 단위',
    measured_at DATETIME NOT NULL COMMENT '측정 시간',
    measurement_status VARCHAR(20) DEFAULT 'COMPLETED' COMMENT '측정 상태',
    alert_level VARCHAR(15) DEFAULT 'NORMAL' COMMENT '경고 수준',
    reference_min DECIMAL(8,3) COMMENT '정상 범위 최솟값',
    reference_max DECIMAL(8,3) COMMENT '정상 범위 최댓값',
    measurement_method VARCHAR(20) COMMENT '측정 방법',
    measurement_device VARCHAR(100) COMMENT '측정 기기명',
    timing_context VARCHAR(20) COMMENT '측정 시점 맥락',
    context_description VARCHAR(500) COMMENT '측정 상황 설명',
    subjective_feeling INTEGER COMMENT '주관적 컨디션 (1-10)',
    symptoms VARCHAR(1000) COMMENT '관련 증상',
    guardian_notified BOOLEAN DEFAULT FALSE COMMENT '보호자 알림 여부',
    guardian_notification_time DATETIME COMMENT '보호자 알림 시간',
    doctor_consultation_needed BOOLEAN DEFAULT FALSE COMMENT '의사 상담 필요',
    notes VARCHAR(1000) COMMENT '추가 메모',
    image_url VARCHAR(500) COMMENT '측정 결과 이미지',
    measurement_latitude DECIMAL(10, 8) COMMENT '측정 위치 위도',
    measurement_longitude DECIMAL(11, 8) COMMENT '측정 위치 경도',
    measurement_location_accuracy DECIMAL(8, 2) COMMENT '위치 정확도',
    sync_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '동기화 상태',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
) COMMENT 'BIF 사용자 건강 지표';

-- User preferences table
CREATE TABLE user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL COMMENT '사용자 ID',
    notification_settings TEXT COMMENT '알림 설정 (JSON)',
    privacy_settings TEXT COMMENT '프라이버시 설정 (JSON)',
    accessibility_settings TEXT COMMENT '접근성 설정 (JSON)',
    ui_preferences TEXT COMMENT 'UI 선호도 (JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) COMMENT '사용자 선호도 설정';

-- Create indexes for performance optimization
-- Users table indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_active_login ON users(is_active, last_login_at);
CREATE INDEX idx_users_cognitive_level ON users(cognitive_level);

-- Guardians table indexes
CREATE INDEX idx_guardians_user ON guardians(user_id);
CREATE INDEX idx_guardians_primary ON guardians(user_id, is_primary);
CREATE INDEX idx_guardians_emergency ON guardians(emergency_priority, is_active);

-- Devices table indexes
CREATE INDEX idx_devices_user ON devices(user_id);
CREATE INDEX idx_devices_identifier ON devices(device_identifier);
CREATE INDEX idx_devices_active_sync ON devices(is_active, last_sync_at);
CREATE INDEX idx_devices_type ON devices(device_type);

-- Battery history indexes
CREATE INDEX idx_battery_device_time ON battery_history(device_id, recorded_at);
CREATE INDEX idx_battery_level_low ON battery_history(battery_level, recorded_at);

-- Connectivity logs indexes
CREATE INDEX idx_connectivity_device_time ON connectivity_logs(device_id, created_at);
CREATE INDEX idx_connectivity_status ON connectivity_logs(connection_status, created_at);

-- Captured images indexes
CREATE INDEX idx_captured_user_time ON captured_images(user_id, captured_at);
CREATE INDEX idx_captured_type_analyzed ON captured_images(image_type, is_analyzed);
CREATE INDEX idx_captured_quality ON captured_images(quality_score, user_feedback);
CREATE INDEX idx_captured_privacy ON captured_images(privacy_level, deleted_at);
CREATE INDEX idx_captured_gps ON captured_images(gps_latitude, gps_longitude);

-- Analysis results indexes
CREATE INDEX idx_analysis_user_time ON analysis_results(user_id, analyzed_at);
CREATE INDEX idx_analysis_image ON analysis_results(captured_image_id);
CREATE INDEX idx_analysis_type_status ON analysis_results(analysis_type, status);
CREATE INDEX idx_analysis_confidence ON analysis_results(confidence_score, status);

-- Content metadata indexes
CREATE INDEX idx_metadata_image ON content_metadata(captured_image_id);
CREATE INDEX idx_metadata_status ON content_metadata(extraction_status);
CREATE INDEX idx_metadata_quality ON content_metadata(privacy_filtered);

-- Schedules indexes
CREATE INDEX idx_schedules_user_active ON schedules(user_id, is_active);
CREATE INDEX idx_schedules_type_priority ON schedules(schedule_type, priority_level);
CREATE INDEX idx_schedules_execution ON schedules(next_execution_at, is_active);
CREATE INDEX idx_schedules_date_range ON schedules(start_date, end_date);
CREATE INDEX idx_schedules_guardian ON schedules(guardian_notification, is_active);

-- Notifications indexes
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX idx_notifications_schedule ON notifications(schedule_id);
CREATE INDEX idx_notifications_priority ON notifications(priority_level, urgency_level);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_at, status);
CREATE INDEX idx_notifications_guardian ON notifications(guardian_copy, status);

-- Notification delivery indexes
CREATE INDEX idx_delivery_notification ON notification_delivery(notification_id);
CREATE INDEX idx_delivery_channel_status ON notification_delivery(delivery_channel, delivery_status);
CREATE INDEX idx_delivery_time ON notification_delivery(delivered_at, failed_at);
CREATE INDEX idx_delivery_cost ON notification_delivery(cost_amount, delivery_channel);

-- Reminder templates indexes
CREATE INDEX idx_templates_user_type ON reminder_templates(user_id, template_type);
CREATE INDEX idx_templates_performance ON reminder_templates(success_rate, performance_score);
CREATE INDEX idx_templates_usage ON reminder_templates(usage_count, is_active);

-- Activity logs indexes
CREATE INDEX idx_activity_user_created ON activity_logs(user_id, created_at);
CREATE INDEX idx_activity_type_created ON activity_logs(activity_type, created_at);
CREATE INDEX idx_activity_date_type ON activity_logs(activity_date, activity_type);
CREATE INDEX idx_activity_success_status ON activity_logs(success_status, created_at);
CREATE INDEX idx_activity_device ON activity_logs(device_id, created_at);
CREATE INDEX idx_activity_guardian ON activity_logs(guardian_notified, help_needed);
CREATE INDEX idx_activity_location ON activity_logs(activity_latitude, activity_longitude);

-- Activity metadata indexes
CREATE INDEX idx_activity_metadata_key ON activity_metadata(metadata_key);

-- Medications indexes
CREATE INDEX idx_medication_user ON medications(user_id);
CREATE INDEX idx_medication_status ON medications(medication_status);
CREATE INDEX idx_medication_priority ON medications(priority_level);
CREATE INDEX idx_medication_active_date ON medications(is_active, start_date);
CREATE INDEX idx_medication_name ON medications(medication_name);
CREATE INDEX idx_medication_doctor ON medications(prescribing_doctor);
CREATE INDEX idx_medication_pharmacy ON medications(pharmacy_name);
CREATE INDEX idx_medication_prescription ON medications(prescription_number);

-- Medication times indexes
CREATE INDEX idx_medication_times ON medication_times(medication_id, intake_time);

-- Medication adherence indexes
CREATE INDEX idx_adherence_user_date ON medication_adherence(user_id, adherence_date);
CREATE INDEX idx_adherence_medication ON medication_adherence(medication_id, adherence_date);
CREATE INDEX idx_adherence_status ON medication_adherence(adherence_status, adherence_date);
CREATE INDEX idx_adherence_scheduled_time ON medication_adherence(scheduled_time, adherence_date);
CREATE INDEX idx_adherence_reminder ON medication_adherence(reminder_sent, guardian_notified);
CREATE INDEX idx_adherence_location ON medication_adherence(taken_latitude, taken_longitude);

-- Health metrics indexes
CREATE INDEX idx_health_user_date ON health_metrics(user_id, measured_at);
CREATE INDEX idx_health_metric_type ON health_metrics(metric_type, measured_at);
CREATE INDEX idx_health_alert_level ON health_metrics(alert_level, measured_at);
CREATE INDEX idx_health_device ON health_metrics(device_id, measured_at);
CREATE INDEX idx_health_status ON health_metrics(measurement_status, measured_at);
CREATE INDEX idx_health_consultation ON health_metrics(doctor_consultation_needed, guardian_notified);
CREATE INDEX idx_health_location ON health_metrics(measurement_latitude, measurement_longitude);

-- User preferences indexes
CREATE INDEX idx_preferences_user ON user_preferences(user_id); 