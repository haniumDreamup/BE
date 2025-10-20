-- V24__Add_guardian_email_unique_constraint.sql
-- 같은 사용자가 같은 이메일로 중복 보호자 초대 방지

-- user_id + email 조합에 UNIQUE 제약조건 추가
ALTER TABLE guardians
ADD CONSTRAINT uk_user_email UNIQUE (user_id, email);
