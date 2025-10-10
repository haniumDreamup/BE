# Spring Boot + AWS RDS + Hibernate Best Practices

## 핵심 문제 및 해결책

### 문제: Hibernate DDL 자동 생성 실패
**증상**: `ddl-auto: create` 설정에도 불구하고 테이블이 생성되지 않음

**원인**:
1. **ApplicationReadyEvent 타이밍 이슈**: `@EventListener(ApplicationReadyEvent.class)`가 Hibernate DDL 실행 전에 데이터베이스 쿼리를 시도
2. **Early Bean Initialization**: Eager fetch나 startup 시 실행되는 서비스가 EntityManager를 먼저 초기화
3. **CacheWarmingService** 같은 startup task가 repository 쿼리 실행

**해결책**:
```java
// 1. Startup에서 DB 접근하는 서비스 비활성화
@Service
@Profile("!prod")  // 프로덕션 환경에서 비활성화
public class CacheWarmingService {
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCacheOnStartup() {
        // startup 시 DB 쿼리 실행
    }
}

// 2. 또는 지연 실행
@EventListener(ApplicationReadyEvent.class)
@Async
public void warmUpCacheOnStartup() {
    try {
        Thread.sleep(5000); // DDL 실행 대기
        // 캐시 워밍 로직
    } catch (Exception e) {
        log.warn("Cache warming failed", e);
    }
}
```

## Hibernate DDL 설정 베스트 프랙티스

### 1. 프로파일별 DDL 전략

```yaml
# application-dev.yml (로컬 개발)
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # 시작 시 create, 종료 시 drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

# application-test.yml (테스트)
spring:
  jpa:
    hibernate:
      ddl-auto: create  # 테스트 시작 시마다 새로 생성

# application-prod.yml (프로덕션)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 스키마 검증만, 변경 불가
  flyway:
    enabled: true  # Flyway로 마이그레이션 관리
```

### 2. DDL-Auto 옵션 설명

| 옵션 | 설명 | 사용 시나리오 |
|-----|------|-------------|
| `none` | 아무 동작 안함 | 수동 스키마 관리 |
| `validate` | 스키마 검증만 | **프로덕션 권장** |
| `update` | 스키마 업데이트 (삭제 안함) | 개발 중 (주의 필요) |
| `create` | 시작 시 drop → create | 로컬 개발, 첫 배포 |
| `create-drop` | 시작 시 create, 종료 시 drop | 테스트 |

### 3. Schema Generation Script 생성

```yaml
# application-dev.yml
spring:
  jpa:
    properties:
      jakarta.persistence.schema-generation:
        scripts:
          action: create
          create-target: /tmp/schema_dev.sql
```

## AWS RDS 연결 설정

### 1. application-prod.yml 예시

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: BifHikariCP-Prod

  jpa:
    hibernate:
      ddl-auto: validate  # 프로덕션은 validate만!
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: false
        jdbc:
          batch_size: 25
          time_zone: UTC
        order_inserts: true
        order_updates: true
```

### 2. 환경 변수 설정 (docker-compose.prod.yml)

```yaml
environment:
  DB_HOST: bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com
  DB_PORT: 3306
  DB_NAME: bifai_db
  DB_USER: admin
  DB_PASSWORD: ${DB_PASSWORD}  # .env 파일에서 로드
```

## 배치 처리 최적화

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25  # 배치 크기
          batch_versioned_data: true  # @Version 엔티티도 배치 처리
        order_inserts: true  # INSERT 순서 최적화
        order_updates: true  # UPDATE 순서 최적화
        generate_statistics: false  # 프로덕션에서는 비활성화
```

### MySQL 배치 최적화

```yaml
spring:
  datasource:
    url: jdbc:mysql://...?rewriteBatchedStatements=true&cachePrepStmts=true&useServerPrepStmts=true
```

## 초기 배포 시 스키마 생성 전략

### 방법 1: DDL Auto Create (첫 배포만)

```yaml
# 첫 배포 시에만 사용
spring:
  jpa:
    hibernate:
      ddl-auto: create
  flyway:
    enabled: false
```

**주의사항**:
- 배포 후 반드시 `validate`로 변경
- 데이터 손실 가능성 - 신중하게 사용

### 방법 2: Flyway Migration (권장)

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

```sql
-- src/main/resources/db/migration/V1__initial_schema.sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  -- ...
);
```

### 방법 3: Schema Export + Manual Execution

```bash
# 1. 로컬에서 스키마 생성
./gradlew bootRun --args='--spring.profiles.active=dev'

# 2. 생성된 SQL 확인
cat /tmp/schema_dev.sql

# 3. RDS에 수동 실행
mysql -h bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com \
      -u admin -p bifai_db < /tmp/schema_dev.sql
```

## Startup Task 관리

### 잘못된 예시 ❌

```java
@Service
public class CacheWarmingService {

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCacheOnStartup() {
        // DDL 실행 전에 쿼리 시도 → 테이블 없음 에러
        List<User> users = userRepository.findActiveUsersForCaching();
    }
}
```

### 올바른 예시 ✅

```java
@Service
@Profile("!prod")  // 프로덕션 제외
public class CacheWarmingService {

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCacheOnStartup() {
        try {
            // DDL 실행 완료 대기
            Thread.sleep(5000);

            List<User> users = userRepository.findActiveUsersForCaching();
            // 캐시 워밍...
        } catch (Exception e) {
            log.error("Cache warming failed", e);
        }
    }
}
```

## RDS vs Docker MySQL 비교

| 항목 | AWS RDS | Docker MySQL |
|-----|---------|--------------|
| **관리** | 완전 관리형 | 수동 관리 필요 |
| **백업** | 자동 백업 | 수동 설정 |
| **확장** | 자동 스케일링 | 수동 스케일링 |
| **비용** | 높음 | 낮음 (EC2 비용만) |
| **성능** | 최적화됨 | 설정 필요 |
| **배포 속도** | 느림 (5-10분) | 빠름 (1-2분) |

### 사용 시나리오

**RDS 선택**:
- 프로덕션 환경
- 고가용성 필요
- 자동 백업/복구 필요
- 팀 규모가 크고 DB 관리 리소스 부족

**Docker MySQL 선택**:
- 개발/스테이징 환경
- 빠른 배포 필요
- 비용 절감
- 완전한 컨트롤 필요

## 트러블슈팅

### 1. Table doesn't exist 에러

```bash
# 원인 확인
docker logs bifai-backend | grep -i "create table"

# 해결책
1. Startup task 비활성화 (@Profile 사용)
2. ddl-auto 설정 확인
3. 데이터베이스 연결 확인
```

### 2. DDL이 실행되지 않음

```yaml
# 로깅 활성화로 확인
logging:
  level:
    org.hibernate.tool.schema: DEBUG
    org.hibernate.SQL: DEBUG
```

### 3. 스키마 불일치

```bash
# 현재 스키마 확인
mysql -h <host> -u <user> -p<password> <database> -e "SHOW TABLES;"

# Flyway로 마이그레이션
./gradlew flywayMigrate
```

## 체크리스트

### 첫 배포 전
- [ ] `ddl-auto: create` 설정 (첫 배포만)
- [ ] Startup task 비활성화 확인
- [ ] 환경 변수 설정 확인
- [ ] RDS 보안 그룹 설정

### 첫 배포 후
- [ ] 테이블 생성 확인
- [ ] `ddl-auto: validate`로 변경
- [ ] Flyway 활성화 (선택)
- [ ] 배포 완료 확인

### 프로덕션 체크
- [ ] `ddl-auto: validate` 확인
- [ ] Startup task 필요 여부 검토
- [ ] Connection pool 설정 최적화
- [ ] 배치 처리 활성화
- [ ] 로깅 레벨 조정 (show-sql: false)
