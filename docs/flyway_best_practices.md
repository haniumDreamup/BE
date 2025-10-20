# Flyway Best Practices - BIF-AI Backend

## 마이그레이션 파일 네이밍 컨벤션

### 포맷
```
V{VERSION}__{DESCRIPTION}.sql
```

- **VERSION**: 순차적 숫자 (1, 2, 3, ... 26, 27, ...)
- **SEPARATOR**: 더블 언더스코어 `__`
- **DESCRIPTION**: snake_case로 간결한 설명
- **SUFFIX**: `.sql`

### 예시
```
V1__Baseline_Schema.sql
V23__Make_guardian_user_id_nullable.sql
V24__Add_guardian_email_unique_constraint.sql
V25__Fix_device_unique_constraints.sql
V26__Make_guardian_fields_nullable.sql
```

## 프로덕션 배포 원칙

### 1. ❌ 절대 금지
- **적용된 마이그레이션 파일 수정 금지**
  - Checksum 불일치로 실패함
  - 항상 새로운 V 파일 생성

- **ddl-auto=create/update 사용 금지**
  - Flyway와 충돌
  - 프로덕션에서는 `validate` 또는 `none` 사용

- **clean 명령 사용 금지**
  - `clean-disabled: true` 설정
  - 데이터 손실 방지

### 2. ✅ 필수 사항
- **마이그레이션 전 백업**
  - RDS 자동 백업 활성화
  - 수동 스냅샷 생성

- **검증 활성화**
  - `validate-on-migrate: true`
  - Checksum 무결성 검증

- **순차적 실행 보장**
  - `out-of-order: false`
  - 버전 순서대로 실행

## 설정 파일

### application-prod.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway 후 JPA 모델 검증

  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true  # Checksum 검증
    out-of-order: false        # 순서 보장
    clean-disabled: true       # clean 비활성화
    mixed: false               # SQL만 사용
    locations: classpath:db/migration

management:
  endpoints:
    web:
      exposure:
        include: flyway  # /actuator/flyway 활성화
```

## 마이그레이션 작성 가이드

### Forward-Only 전략
프로덕션에서는 항상 **forward-only** 마이그레이션:
- 데이터 삭제 필요 시 → 새 V 파일로 DROP
- 컬럼 수정 필요 시 → 새 V 파일로 ALTER
- 롤백 필요 시 → 새 V 파일로 복구

### 예시: 컬럼 nullable 변경
```sql
-- ❌ 잘못된 방법: V23 파일 수정
-- V23__Make_guardian_user_id_nullable.sql 수정 (금지!)

-- ✅ 올바른 방법: 새 V 파일 생성
-- V27__Revert_guardian_user_id_not_null.sql
ALTER TABLE guardians
MODIFY COLUMN guardian_user_id BIGINT NOT NULL;
```

## 모니터링

### Actuator 엔드포인트
```bash
# 마이그레이션 상태 확인
curl http://localhost:8080/actuator/flyway

# 응답 예시
{
  "contexts": {
    "application": {
      "flywayBeans": {
        "flyway": {
          "migrations": [
            {
              "type": "SQL",
              "checksum": 1234567890,
              "version": "23",
              "description": "Make guardian user id nullable",
              "script": "V23__Make_guardian_user_id_nullable.sql",
              "state": "SUCCESS",
              "installedOn": "2025-10-20T16:46:05.562Z"
            }
          ]
        }
      }
    }
  }
}
```

## 트러블슈팅

### Checksum 불일치
```
FlywayException: Validate failed: Migration checksum mismatch
```
**해결:**
1. 마이그레이션 파일을 원복
2. 새로운 V 파일로 수정 내용 추가

### 마이그레이션 실행 안 됨
**체크리스트:**
- [ ] `flyway.enabled=true` 확인
- [ ] 마이그레이션 파일이 `src/main/resources/db/migration/`에 있는지
- [ ] 파일명 형식이 `V{숫자}__{설명}.sql` 맞는지
- [ ] 애플리케이션 재시작

### JPA와 DB 스키마 불일치
```
javax.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory
```
**해결:**
1. Flyway 마이그레이션 먼저 실행
2. `ddl-auto=validate`로 검증
3. 불일치 있으면 새 마이그레이션 작성

## 배포 체크리스트

### 마이그레이션 배포 전
- [ ] 로컬에서 마이그레이션 테스트 완료
- [ ] Staging 환경에서 검증 완료
- [ ] 프로덕션 DB 백업 생성
- [ ] 롤백 계획 수립

### 배포 중
- [ ] 애플리케이션 시작 로그 모니터링
- [ ] Flyway 마이그레이션 로그 확인
- [ ] `/actuator/flyway` 엔드포인트 확인

### 배포 후
- [ ] 마이그레이션 성공 여부 확인
- [ ] 애플리케이션 정상 동작 확인
- [ ] 데이터 무결성 검증

## 참고 자료
- [Flyway Official Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Flyway Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
- [Flyway Best Practices 2025](https://rieckpil.de/howto-best-practices-for-flyway-and-hibernate-with-spring-boot/)
