-- V23__Make_guardian_user_id_nullable.sql
-- 보호자 초대 시 guardian_user_id가 NULL일 수 있도록 변경
-- 이유: 이메일로 보호자 초대 시 아직 계정이 없을 수 있음

-- guardian_user_id를 nullable로 변경
ALTER TABLE guardians
MODIFY COLUMN guardian_user_id BIGINT NULL COMMENT '보호자 사용자 ID (초대 시 NULL 가능)';

-- 외래키 제약조건 재생성 (ON DELETE CASCADE 유지)
-- 기존 외래키가 있다면 제거
ALTER TABLE guardians
DROP FOREIGN KEY IF EXISTS guardians_ibfk_2;

-- 새 외래키 추가 (NULL 허용)
ALTER TABLE guardians
ADD CONSTRAINT fk_guardians_guardian_user
FOREIGN KEY (guardian_user_id) REFERENCES users(id) ON DELETE CASCADE;
