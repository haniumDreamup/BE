# Spring Boot + Flyway 마이그레이션 전략 (2025 Best Practices)

## 현재 문제 상황

### 지금까지 발생한 문제들
1. **JPA ddl-auto vs RDS 스키마 불일치**: 개발 환경에서 JPA가 자동으로 생성한 스키마와 프로덕션 RDS의 실제 스키마가 다름
2. **필드 제거 방식의 한계**: 계속해서 엔티티 필드를 제거하는 것은 기존 코드에 영향을 미칠 수 있음
3. **데이터베이스 상태 불일치**: 개발/테스트/프로덕션 환경 간 스키마 차이

### 왜 이런 문제가 발생했나?
- `spring.jpa.hibernate.ddl-auto=update/create` 설정으로 인해 JPA가 자동으로 스키마를 변경
- 프로덕션 RDS는 수동으로 생성되어 JPA 엔티티와 일치하지 않음
- 환경별로 다른 데이터베이스 초기화 방식 사용

## 2025년 베스트 프랙티스: Flyway 마이그레이션

### 1. 기본 원칙

#### ❌ 하지 말아야 할 것
```yaml
# 프로덕션에서 절대 사용 금지
spring:
  jpa:
    hibernate:
      ddl-auto: update    # ❌ 위험
      ddl-auto: create    # ❌ 데이터 손실
      ddl-auto: create-drop # ❌ 데이터 손실
```

#### ✅ 권장 설정
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ 스키마 검증만 수행
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### 2. Flyway 설정 방법

#### build.gradle 의존성 추가
```gradle
dependencies {
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'
}
```

#### 마이그레이션 파일 위치
```
src/main/resources/db/migration/
├── V1__Create_initial_schema.sql
├── V2__Add_user_table.sql
├── V3__Add_device_table.sql
└── V4__Add_indexes.sql
```

### 3. 마이그레이션 전략

#### 단계 1: 현재 RDS 스키마 백업
```sql
-- 현재 프로덕션 스키마 구조 추출
mysqldump -h bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com \
  -u admin -p --no-data --routines --triggers bifai_db > current_schema.sql
```

#### 단계 2: 기본 마이그레이션 생성
```sql
-- V1__Create_baseline_schema.sql
-- 현재 RDS의 실제 스키마를 기반으로 작성
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    nickname VARCHAR(50),
    phone_number VARCHAR(20),
    cognitive_level VARCHAR(20) DEFAULT 'MODERATE',
    profile_image_url VARCHAR(500),
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    language_preference VARCHAR(10) DEFAULT 'ko',
    is_active BOOLEAN DEFAULT TRUE,
    emergency_mode_enabled BOOLEAN DEFAULT FALSE,
    last_login_at DATETIME,
    last_activity_at DATETIME,
    password_reset_token VARCHAR(255),
    password_reset_expires_at DATETIME,
    provider VARCHAR(20),
    provider_id VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 단계 3: 점진적 필드 추가
필요한 필드들을 마이그레이션으로 추가:
```sql
-- V2__Add_audit_fields.sql (필요시)
ALTER TABLE users
ADD COLUMN created_by VARCHAR(100),
ADD COLUMN updated_by VARCHAR(100);

-- V3__Add_additional_user_fields.sql (필요시)
ALTER TABLE users
ADD COLUMN date_of_birth DATE,
ADD COLUMN gender VARCHAR(10),
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE;
```

### 4. 환경별 설정

#### application-dev.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: true
    clean-disabled: false  # 개발환경에서만 허용
```

#### application-prod.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: false  # 프로덕션에서는 수동 관리
    clean-disabled: true        # 보안상 비활성화
```

### 5. 마이그레이션 실행 방법

#### 로컬/개발 환경
```bash
./gradlew flywayMigrate
./gradlew bootRun
```

#### 프로덕션 환경
```bash
# 1. 백업 먼저 수행
mysqldump -h [RDS_HOST] -u admin -p bifai_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 마이그레이션 실행
./gradlew flywayMigrate -Dspring.profiles.active=prod

# 3. 애플리케이션 시작
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 6. 안전장치

#### 체크섬 검증
Flyway는 각 마이그레이션 파일의 체크섬을 저장하여 무단 변경을 감지:
```sql
SELECT * FROM flyway_schema_history;
```

#### 롤백 전략
```sql
-- 필요시 수동 롤백 스크립트 준비
-- V2__Add_audit_fields_rollback.sql
ALTER TABLE users
DROP COLUMN created_by,
DROP COLUMN updated_by;
```

### 7. 팀 협업 규칙

1. **마이그레이션 파일은 절대 수정 금지**: 한번 적용된 파일은 변경하지 않음
2. **새로운 변경사항은 새 마이그레이션으로**: V5, V6... 순서대로 추가
3. **코드 리뷰 필수**: 마이그레이션 스크립트는 반드시 리뷰 후 머지
4. **테스트 환경 먼저**: 개발 → 테스트 → 프로덕션 순서로 적용

## 결론

현재 필드 제거 방식보다 Flyway를 사용하는 것이 훨씬 안전하고 관리하기 쉽습니다:

### 장점
- ✅ 모든 환경에서 일관된 스키마
- ✅ 변경 이력 추적 가능
- ✅ 팀원 간 스키마 변경 공유 용이
- ✅ 프로덕션 배포 시 안전성 보장
- ✅ 필요시 특정 버전으로 롤백 가능

### 다음 단계
1. 현재 RDS 스키마 분석 및 백업
2. Flyway 설정 및 기본 마이그레이션 작성
3. 개발 환경에서 테스트
4. 프로덕션 적용