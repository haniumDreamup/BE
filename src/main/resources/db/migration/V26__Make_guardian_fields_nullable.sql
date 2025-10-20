-- V26__Make_guardian_fields_nullable.sql
-- 보호자 초대 시 primaryPhone과 relationship을 선택사항으로 변경
-- 이메일 초대 시 전화번호 없을 수 있음

-- primary_phone을 nullable로 변경
ALTER TABLE guardians
MODIFY COLUMN primary_phone VARCHAR(20) NULL COMMENT '주 연락처 (초대 시 선택사항)';

-- relationship을 nullable로 변경
ALTER TABLE guardians
MODIFY COLUMN relationship VARCHAR(50) NULL COMMENT '관계 (초대 시 선택사항)';
