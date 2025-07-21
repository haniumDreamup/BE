-- BIF-AI Backend Performance Optimization Migration
-- Adds composite indexes, partitioning strategies, and additional constraints

-- Composite indexes for complex queries
-- User activity analysis
CREATE INDEX idx_activity_user_type_date ON activity_logs(user_id, activity_type, activity_date);
CREATE INDEX idx_activity_success_mood ON activity_logs(success_status, mood_before, mood_after);
CREATE INDEX idx_activity_help_guardian ON activity_logs(help_needed, guardian_notified, activity_date);

-- Medication management optimization
CREATE INDEX idx_medication_user_active_priority ON medications(user_id, is_active, priority_level);
CREATE INDEX idx_medication_active_dates ON medications(is_active, start_date, end_date);
CREATE INDEX idx_adherence_user_med_date ON medication_adherence(user_id, medication_id, adherence_date);
CREATE INDEX idx_adherence_status_reminder ON medication_adherence(adherence_status, reminder_sent, guardian_notified);

-- Health metrics performance
CREATE INDEX idx_health_user_type_alert ON health_metrics(user_id, metric_type, alert_level);
CREATE INDEX idx_health_consultation_urgent ON health_metrics(doctor_consultation_needed, alert_level, measured_at);

-- Notification system optimization
CREATE INDEX idx_notifications_user_priority_scheduled ON notifications(user_id, priority_level, scheduled_at);
CREATE INDEX idx_notifications_status_delivery ON notifications(status, scheduled_at, sent_at);
CREATE INDEX idx_delivery_status_attempt ON notification_delivery(delivery_status, attempt_count, created_at);

-- Content and analysis optimization
CREATE INDEX idx_captured_user_type_quality ON captured_images(user_id, image_type, quality_score);
CREATE INDEX idx_analysis_type_confidence_status ON analysis_results(analysis_type, confidence_score, status);

-- Schedule management
CREATE INDEX idx_schedules_user_type_execution ON schedules(user_id, schedule_type, next_execution_at);
CREATE INDEX idx_schedules_active_priority_date ON schedules(is_active, priority_level, start_date);

-- Device and connectivity optimization
CREATE INDEX idx_devices_user_active_sync ON devices(user_id, is_active, last_sync_at);
CREATE INDEX idx_battery_device_level_time ON battery_history(device_id, battery_level, recorded_at);
CREATE INDEX idx_connectivity_device_status_time ON connectivity_logs(device_id, connection_status, created_at);

-- Guardian and emergency management
CREATE INDEX idx_guardians_user_primary_active ON guardians(user_id, is_primary, is_active);

-- Coverage indexes for common WHERE clauses
CREATE INDEX idx_captured_privacy_deleted ON captured_images(privacy_level, deleted_at, captured_at);
CREATE INDEX idx_medications_status_priority_user ON medications(medication_status, priority_level, user_id);
CREATE INDEX idx_health_alert_consultation_user ON health_metrics(alert_level, doctor_consultation_needed, user_id);

-- Full-text search indexes for content search
-- Note: MySQL full-text indexes for searching descriptions and notes
CREATE FULLTEXT INDEX ft_activity_description ON activity_logs(activity_description, notes);
CREATE FULLTEXT INDEX ft_medication_description ON medications(simple_description, important_notes, user_notes);
CREATE FULLTEXT INDEX ft_health_symptoms ON health_metrics(symptoms, notes);
CREATE FULLTEXT INDEX ft_schedule_description ON schedules(schedule_description);

-- Add check constraints for data integrity
-- User constraints
ALTER TABLE users 
ADD CONSTRAINT chk_users_cognitive_level 
CHECK (cognitive_level IN ('MILD', 'MODERATE', 'SEVERE', 'UNKNOWN'));

ALTER TABLE users 
ADD CONSTRAINT chk_users_gender 
CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'));

-- Activity log constraints
ALTER TABLE activity_logs
ADD CONSTRAINT chk_activity_confidence_score
CHECK (confidence_score >= 1 AND confidence_score <= 10);

ALTER TABLE activity_logs
ADD CONSTRAINT chk_activity_duration
CHECK (duration_minutes >= 0 AND duration_minutes <= 1440); -- max 24 hours

-- Medication constraints
ALTER TABLE medications
ADD CONSTRAINT chk_medication_daily_frequency
CHECK (daily_frequency >= 1 AND daily_frequency <= 24);

ALTER TABLE medications
ADD CONSTRAINT chk_medication_dosage_amount
CHECK (dosage_amount > 0);

ALTER TABLE medications
ADD CONSTRAINT chk_medication_total_days
CHECK (total_days IS NULL OR total_days > 0);

-- Medication adherence constraints
ALTER TABLE medication_adherence
ADD CONSTRAINT chk_adherence_difficulty_score
CHECK (difficulty_score IS NULL OR (difficulty_score >= 1 AND difficulty_score <= 10));

ALTER TABLE medication_adherence
ADD CONSTRAINT chk_adherence_satisfaction_score
CHECK (satisfaction_score IS NULL OR (satisfaction_score >= 1 AND satisfaction_score <= 10));

ALTER TABLE medication_adherence
ADD CONSTRAINT chk_adherence_delay_minutes
CHECK (delay_minutes >= -1440 AND delay_minutes <= 1440); -- within 24 hours

ALTER TABLE medication_adherence
ADD CONSTRAINT chk_adherence_reminder_count
CHECK (reminder_count >= 0 AND reminder_count <= 10);

-- Health metrics constraints
ALTER TABLE health_metrics
ADD CONSTRAINT chk_health_subjective_feeling
CHECK (subjective_feeling IS NULL OR (subjective_feeling >= 1 AND subjective_feeling <= 10));

ALTER TABLE health_metrics
ADD CONSTRAINT chk_health_value_positive
CHECK (value >= 0);

ALTER TABLE health_metrics
ADD CONSTRAINT chk_health_secondary_value_positive
CHECK (secondary_value IS NULL OR secondary_value >= 0);

-- Notification constraints
ALTER TABLE notifications
ADD CONSTRAINT chk_notification_retry_count
CHECK (retry_count >= 0 AND retry_count <= max_retry_count);

ALTER TABLE notifications
ADD CONSTRAINT chk_notification_max_retry
CHECK (max_retry_count >= 0 AND max_retry_count <= 10);

-- Notification delivery constraints
ALTER TABLE notification_delivery
ADD CONSTRAINT chk_delivery_attempt_count
CHECK (attempt_count >= 0 AND attempt_count <= 10);

ALTER TABLE notification_delivery
ADD CONSTRAINT chk_delivery_cost_amount
CHECK (cost_amount IS NULL OR cost_amount >= 0);

-- Battery history constraints
ALTER TABLE battery_history
ADD CONSTRAINT chk_battery_level
CHECK (battery_level >= 0 AND battery_level <= 100);

-- Connectivity logs constraints  
ALTER TABLE connectivity_logs
ADD CONSTRAINT chk_signal_strength
CHECK (signal_strength IS NULL OR (signal_strength >= 0 AND signal_strength <= 100));

-- Image quality constraints
ALTER TABLE captured_images
ADD CONSTRAINT chk_image_quality_score
CHECK (quality_score IS NULL OR (quality_score >= 0.0 AND quality_score <= 1.0));

ALTER TABLE captured_images
ADD CONSTRAINT chk_image_dimensions
CHECK ((image_width IS NULL AND image_height IS NULL) OR 
       (image_width > 0 AND image_height > 0));

-- Analysis result constraints
ALTER TABLE analysis_results
ADD CONSTRAINT chk_analysis_confidence_score
CHECK (confidence_score IS NULL OR (confidence_score >= 0.0 AND confidence_score <= 1.0));

ALTER TABLE analysis_results
ADD CONSTRAINT chk_analysis_processing_time
CHECK (processing_time_ms IS NULL OR processing_time_ms >= 0);

-- Schedule constraints
ALTER TABLE schedules
ADD CONSTRAINT chk_schedule_duration
CHECK (duration_minutes IS NULL OR (duration_minutes > 0 AND duration_minutes <= 1440));

ALTER TABLE schedules
ADD CONSTRAINT chk_schedule_execution_count
CHECK (execution_count >= 0);

ALTER TABLE schedules
ADD CONSTRAINT chk_schedule_dates
CHECK (end_date IS NULL OR end_date >= start_date);

-- Guardian constraints
ALTER TABLE guardians
ADD CONSTRAINT chk_guardian_emergency_priority
CHECK (emergency_priority >= 1 AND emergency_priority <= 10);

-- Reminder template constraints
ALTER TABLE reminder_templates
ADD CONSTRAINT chk_template_usage_count
CHECK (usage_count >= 0);

ALTER TABLE reminder_templates
ADD CONSTRAINT chk_template_success_rate
CHECK (success_rate IS NULL OR (success_rate >= 0.0 AND success_rate <= 100.0));

ALTER TABLE reminder_templates
ADD CONSTRAINT chk_template_satisfaction
CHECK (user_satisfaction IS NULL OR (user_satisfaction >= 1.0 AND user_satisfaction <= 5.0));

ALTER TABLE reminder_templates
ADD CONSTRAINT chk_template_response_time
CHECK (avg_response_time IS NULL OR avg_response_time >= 0);

-- Create views for common queries
-- User dashboard view
CREATE VIEW v_user_dashboard AS
SELECT 
    u.id as user_id,
    u.name,
    u.cognitive_level,
    u.is_active,
    COUNT(DISTINCT al.id) as total_activities_today,
    COUNT(DISTINCT CASE WHEN al.success_status IN ('SUCCESS', 'PARTIAL_SUCCESS') THEN al.id END) as successful_activities_today,
    COUNT(DISTINCT m.id) as active_medications,
    COUNT(DISTINCT CASE WHEN ma.adherence_status IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') 
                         AND ma.adherence_date = CURRENT_DATE THEN ma.id END) as medications_taken_today,
    COUNT(DISTINCT CASE WHEN hm.alert_level IN ('CRITICAL', 'HIGH') 
                         AND DATE(hm.measured_at) = CURRENT_DATE THEN hm.id END) as health_alerts_today,
    COUNT(DISTINCT CASE WHEN n.status = 'PENDING' THEN n.id END) as pending_notifications
FROM users u
LEFT JOIN activity_logs al ON u.id = al.user_id AND DATE(al.activity_date) = CURRENT_DATE
LEFT JOIN medications m ON u.id = m.user_id AND m.is_active = TRUE
LEFT JOIN medication_adherence ma ON u.id = ma.user_id AND ma.adherence_date = CURRENT_DATE
LEFT JOIN health_metrics hm ON u.id = hm.user_id AND DATE(hm.measured_at) = CURRENT_DATE
LEFT JOIN notifications n ON u.id = n.user_id AND n.status = 'PENDING'
WHERE u.is_active = TRUE
GROUP BY u.id, u.name, u.cognitive_level, u.is_active;

-- Medication adherence summary view
CREATE VIEW v_medication_adherence_summary AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    m.id as medication_id,
    m.medication_name,
    m.priority_level,
    COUNT(ma.id) as total_scheduled,
    COUNT(CASE WHEN ma.adherence_status IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') THEN 1 END) as taken_count,
    ROUND(
        100.0 * COUNT(CASE WHEN ma.adherence_status IN ('TAKEN', 'TAKEN_LATE', 'TAKEN_EARLY') THEN 1 END) 
        / NULLIF(COUNT(ma.id), 0), 2
    ) as adherence_rate,
    AVG(ma.delay_minutes) as avg_delay_minutes,
    COUNT(CASE WHEN ma.side_effect_reported = TRUE THEN 1 END) as side_effects_reported,
    MAX(ma.adherence_date) as last_taken_date
FROM users u
JOIN medications m ON u.id = m.user_id AND m.is_active = TRUE
LEFT JOIN medication_adherence ma ON m.id = ma.medication_id 
    AND ma.adherence_date >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
WHERE u.is_active = TRUE
GROUP BY u.id, u.name, m.id, m.medication_name, m.priority_level;

-- Health trends view
CREATE VIEW v_health_trends AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    hm.metric_type,
    COUNT(hm.id) as measurement_count,
    AVG(hm.value) as avg_value,
    MIN(hm.value) as min_value,
    MAX(hm.value) as max_value,
    COUNT(CASE WHEN hm.alert_level = 'NORMAL' THEN 1 END) as normal_count,
    COUNT(CASE WHEN hm.alert_level IN ('HIGH', 'CRITICAL') THEN 1 END) as alert_count,
    MAX(hm.measured_at) as last_measurement_date
FROM users u
JOIN health_metrics hm ON u.id = hm.user_id 
    AND hm.measured_at >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
    AND hm.measurement_status = 'COMPLETED'
WHERE u.is_active = TRUE
GROUP BY u.id, u.name, hm.metric_type;

-- Activity performance view
CREATE VIEW v_activity_performance AS
SELECT 
    u.id as user_id,
    u.name as user_name,
    al.activity_type,
    COUNT(al.id) as total_activities,
    COUNT(CASE WHEN al.success_status IN ('SUCCESS', 'PARTIAL_SUCCESS') THEN 1 END) as successful_activities,
    ROUND(
        100.0 * COUNT(CASE WHEN al.success_status IN ('SUCCESS', 'PARTIAL_SUCCESS') THEN 1 END) 
        / NULLIF(COUNT(al.id), 0), 2
    ) as success_rate,
    AVG(al.confidence_score) as avg_confidence,
    AVG(al.duration_minutes) as avg_duration,
    COUNT(CASE WHEN al.help_needed = TRUE THEN 1 END) as help_requests
FROM users u
JOIN activity_logs al ON u.id = al.user_id 
    AND al.activity_date >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
WHERE u.is_active = TRUE
GROUP BY u.id, u.name, al.activity_type;

-- Guardian alert summary view
CREATE VIEW v_guardian_alerts AS
SELECT 
    g.id as guardian_id,
    g.name as guardian_name,
    g.user_id,
    u.name as user_name,
    COUNT(DISTINCT al.id) as activity_alerts,
    COUNT(DISTINCT ma.id) as medication_alerts,
    COUNT(DISTINCT hm.id) as health_alerts,
    COUNT(DISTINCT n.id) as pending_notifications
FROM guardians g
JOIN users u ON g.user_id = u.id AND u.is_active = TRUE
LEFT JOIN activity_logs al ON g.user_id = al.user_id 
    AND al.guardian_notified = FALSE 
    AND (al.help_needed = TRUE OR al.success_status = 'FAILED')
    AND al.activity_date >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
LEFT JOIN medication_adherence ma ON g.user_id = ma.user_id 
    AND ma.guardian_notified = FALSE 
    AND (ma.side_effect_reported = TRUE OR ma.adherence_status IN ('MISSED', 'SKIPPED'))
    AND ma.adherence_date >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
LEFT JOIN health_metrics hm ON g.user_id = hm.user_id 
    AND hm.guardian_notified = FALSE 
    AND (hm.alert_level IN ('HIGH', 'CRITICAL') OR hm.doctor_consultation_needed = TRUE)
    AND hm.measured_at >= DATE_SUB(CURRENT_DATE, INTERVAL 7 DAY)
LEFT JOIN notifications n ON g.user_id = n.user_id 
    AND n.guardian_copy = TRUE 
    AND n.status = 'PENDING'
WHERE g.is_active = TRUE
GROUP BY g.id, g.name, g.user_id, u.name; 