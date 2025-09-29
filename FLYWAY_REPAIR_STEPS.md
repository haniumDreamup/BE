# Flyway 복구 단계별 가이드

## 🚨 실행 전 필수 단계

### 1. RDS에 직접 접속 (AWS Console / MySQL Workbench)
```bash
Host: bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com
Port: 3306
Database: bifai_db
Username: admin
Password: Wkdlvmfflaldj12@
```

### 2. 현재 상태 진단 (flyway_repair_script.sql 실행)
```sql
-- flyway_schema_history 상태 확인
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### 3. 실패한 마이그레이션 제거
```sql
-- V2가 실패 상태라면 삭제
DELETE FROM flyway_schema_history WHERE version = '2' AND success = 0;
```

### 4. 필요한 경우 전체 히스토리 리셋 (⚠️ 주의)
```sql
-- 모든 히스토리 삭제 후 다시 시작
TRUNCATE TABLE flyway_schema_history;
```

## 🔧 코드 변경사항

### 완료된 작업:
✅ V2 문제 마이그레이션 비활성화 (`V2__Performance_Optimization.sql.disabled`)
✅ 새로운 안전한 V2 마이그레이션 생성 (`V2__Safe_Core_Schema_Updates.sql`)
✅ Flyway 재활성화 설정 (`application-prod.yml`)

### Flyway 설정:
```yaml
flyway:
  enabled: true
  baseline-on-migrate: true    # 기존 DB에 대응
  validate-on-migrate: false  # 임시로 검증 비활성화
  repair: true                 # 자동 복구 시도
```

## 📋 배포 및 테스트 순서

### 1단계: DB 수동 복구
1. RDS 접속
2. `flyway_repair_script.sql` 실행
3. 실패한 마이그레이션 제거

### 2단계: 코드 배포
```bash
git add .
git commit -m "fix: Flyway 복구 및 안전한 V2 마이그레이션"
git push origin main
```

### 3단계: 배포 모니터링
1. GitHub Actions 로그 확인
2. 애플리케이션 시작 로그 확인
3. Flyway 마이그레이션 성공 확인

### 4단계: API 테스트
```bash
curl -X POST http://43.200.49.171:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"test1234","confirmPassword":"test1234","agreeToTerms":true,"agreeToPrivacyPolicy":true}'
```

## ⚠️ 주의사항

1. **백업**: 작업 전 RDS 스냅샷 생성 권장
2. **점진적 적용**: 먼저 테스트 환경에서 검증
3. **롤백 계획**: 문제 발생 시 Flyway 다시 비활성화
4. **모니터링**: 배포 후 로그 면밀히 관찰

## 🔄 문제 발생 시 롤백

```yaml
# application-prod.yml에서 다시 비활성화
flyway:
  enabled: false
```

그 후 수동으로 fix_db_schema_direct.sql 실행