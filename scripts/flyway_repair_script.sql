-- Flyway 복구를 위한 진단 및 수정 스크립트
-- AWS RDS Console이나 MySQL Workbench에서 실행

USE bifai_db;

-- 1. 현재 flyway_schema_history 테이블 상태 확인
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 2. 실패한 마이그레이션 확인
SELECT * FROM flyway_schema_history WHERE success = 0;

-- 3. 현재 존재하는 테이블 목록 확인
SHOW TABLES;

-- 4. users 테이블 구조 확인
DESCRIBE users;

-- 5. 실패한 마이그레이션 기록 삭제 (V2가 문제였다면)
-- DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0;

-- 6. 누락된 테이블들이 있는지 확인
SELECT
    TABLE_NAME,
    TABLE_COMMENT
FROM
    INFORMATION_SCHEMA.TABLES
WHERE
    TABLE_SCHEMA = 'bifai_db'
    AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;

-- 7. flyway_schema_history 테이블 리셋 (필요시)
-- TRUNCATE TABLE flyway_schema_history;

COMMIT;