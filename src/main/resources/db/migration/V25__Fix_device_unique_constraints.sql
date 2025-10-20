-- V25__Fix_device_unique_constraints.sql
-- Device 엔티티의 unique 제약조건 수정
-- deviceId는 전역 unique가 아니라 (user_id, device_id) 조합이 unique해야 함

-- 기존 device_id unique 제약조건 제거
ALTER TABLE devices
DROP INDEX device_id;

-- (user_id, device_id) 조합에 unique 제약조건 추가
ALTER TABLE devices
ADD CONSTRAINT uk_user_device UNIQUE (user_id, device_id);
