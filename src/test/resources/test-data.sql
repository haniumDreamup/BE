-- Test data cleanup and setup
-- This script ensures clean test data for each test run

-- Clean up existing data
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE users;
TRUNCATE TABLE guardians;
TRUNCATE TABLE devices;
TRUNCATE TABLE schedules;
TRUNCATE TABLE notifications;
TRUNCATE TABLE activity_logs;
TRUNCATE TABLE medications;
TRUNCATE TABLE location_history;
TRUNCATE TABLE emergency_contacts;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert test users with unique constraints handled
INSERT INTO users (id, email, password_hash, name, nickname, phone_number, username, is_active, created_at)
VALUES 
    (1, 'test@example.com', '$2a$10$test', '테스트 사용자', '테스트', '010-1234-5678', 'testuser', true, NOW()),
    (2, 'guardian@example.com', '$2a$10$test', '보호자 사용자', '보호자', '010-9876-5432', 'guardian', true, NOW()),
    (3, 'patient@example.com', '$2a$10$test', '환자 사용자', '환자', '010-5555-5555', 'patient', true, NOW())
ON DUPLICATE KEY UPDATE 
    updated_at = NOW();

-- Reset auto increment for consistent IDs
ALTER TABLE users AUTO_INCREMENT = 4;
ALTER TABLE guardians AUTO_INCREMENT = 1;
ALTER TABLE devices AUTO_INCREMENT = 1;
ALTER TABLE schedules AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE activity_logs AUTO_INCREMENT = 1;
ALTER TABLE medications AUTO_INCREMENT = 1;
ALTER TABLE location_history AUTO_INCREMENT = 1;
ALTER TABLE emergency_contacts AUTO_INCREMENT = 1;