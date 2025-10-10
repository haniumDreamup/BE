# AWS RDS MySQL 프로덕션 배포 완벽 가이드

> **작성일**: 2025-01-10
> **대상**: Spring Boot 3.5 + MySQL 8.0 + AWS RDS 프로덕션 환경

## 목차
1. [RDS 인스턴스 생성 및 설정](#1-rds-인스턴스-생성-및-설정)
2. [Parameter Group 최적화](#2-parameter-group-최적화)
3. [Connection Pool 설정](#3-connection-pool-설정)
4. [Hibernate DDL 전략](#4-hibernate-ddl-전략)
5. [초기 배포 시 스키마 생성](#5-초기-배포-시-스키마-생성)
6. [모니터링 및 성능 최적화](#6-모니터링-및-성능-최적화)
7. [트러블슈팅](#7-트러블슈팅)

---

## 1. RDS 인스턴스 생성 및 설정

### 1.1 RDS 생성 (AWS CLI)

```bash
# RDS MySQL 8.0 인스턴스 생성
aws rds create-db-instance \
  --db-instance-identifier bifai-db-prod \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0.42 \
  --master-username admin \
  --master-user-password 'BifaiSecure2025' \
  --allocated-storage 20 \
  --storage-type gp3 \
  --storage-encrypted \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --db-parameter-group-name bifai-mysql8-params \
  --vpc-security-group-ids sg-0275e2106f2a99744 \
  --db-subnet-group-name default \
  --publicly-accessible \
  --enable-cloudwatch-logs-exports '["error","general","slowquery"]' \
  --tags Key=Environment,Value=production Key=Project,Value=bifai
```

### 1.2 보안 그룹 설정

```bash
# Spring Boot 서버에서 RDS 접근 허용
aws ec2 authorize-security-group-ingress \
  --group-id sg-0275e2106f2a99744 \
  --protocol tcp \
  --port 3306 \
  --source-group <EC2-SECURITY-GROUP-ID>

# 로컬 개발 환경에서 접근 (임시, 프로덕션에서는 제거)
aws ec2 authorize-security-group-ingress \
  --group-id sg-0275e2106f2a99744 \
  --protocol tcp \
  --port 3306 \
  --cidr <YOUR-IP>/32
```

### 1.3 RDS 상태 확인

```bash
# 생성 완료 대기 (약 5-10분)
aws rds wait db-instance-available \
  --db-instance-identifier bifai-db-prod

# 엔드포인트 확인
aws rds describe-db-instances \
  --db-instance-identifier bifai-db-prod \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

---

## 2. Parameter Group 최적화

### 2.1 Custom Parameter Group 생성

```bash
# MySQL 8.0용 커스텀 파라미터 그룹 생성
aws rds create-db-parameter-group \
  --db-parameter-group-name bifai-mysql8-params \
  --db-parameter-group-family mysql8.0 \
  --description "Optimized parameters for BIF-AI production"
```

### 2.2 핵심 성능 파라미터 설정

```bash
# innodb_buffer_pool_size (가용 메모리의 70-80%)
# db.t3.micro = 1GB RAM → 700MB
aws rds modify-db-parameter-group \
  --db-parameter-group-name bifai-mysql8-params \
  --parameters "ParameterName=innodb_buffer_pool_size,ParameterValue=734003200,ApplyMethod=pending-reboot"

# Connection 설정
aws rds modify-db-parameter-group \
  --db-parameter-group-name bifai-mysql8-params \
  --parameters \
    "ParameterName=max_connections,ParameterValue=200,ApplyMethod=immediate" \
    "ParameterName=interactive_timeout,ParameterValue=28800,ApplyMethod=immediate" \
    "ParameterName=wait_timeout,ParameterValue=28800,ApplyMethod=immediate"

# Query Cache (MySQL 8.0에서는 제거됨, 무시해도 됨)
# innodb_adaptive_hash_index 활성화
aws rds modify-db-parameter-group \
  --db-parameter-group-name bifai-mysql8-params \
  --parameters "ParameterName=innodb_adaptive_hash_index,ParameterValue=1,ApplyMethod=immediate"

# 로깅 최적화
aws rds modify-db-parameter-group \
  --db-parameter-group-name bifai-mysql8-params \
  --parameters \
    "ParameterName=slow_query_log,ParameterValue=1,ApplyMethod=immediate" \
    "ParameterName=long_query_time,ParameterValue=2,ApplyMethod=immediate" \
    "ParameterName=log_queries_not_using_indexes,ParameterValue=1,ApplyMethod=immediate"
```

### 2.3 권장 Parameter Group 설정 (전체)

| 파라미터 | 값 | 설명 |
|---------|-----|------|
| `innodb_buffer_pool_size` | 700MB (70% of RAM) | InnoDB 버퍼 풀 크기 |
| `max_connections` | 200 | 최대 동시 연결 수 |
| `interactive_timeout` | 28800 (8시간) | 대화형 연결 타임아웃 |
| `wait_timeout` | 28800 | 비대화형 연결 타임아웃 |
| `slow_query_log` | 1 (ON) | 슬로우 쿼리 로깅 활성화 |
| `long_query_time` | 2 (초) | 슬로우 쿼리 기준 시간 |
| `innodb_sync_array_size` | 4 | 동시성 개선 (기본 1) |
| `table_definition_cache` | 2000 | 테이블 정의 캐시 크기 |

---

## 3. Connection Pool 설정

### 3.1 HikariCP 설정 (권장)

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      # Pool 크기 계산: connections = ((core_count * 2) + effective_spindle_count)
      # t3.micro (1 vCPU) = 2 * 2 + 1 = 5-10
      maximum-pool-size: 10
      minimum-idle: 2

      # Timeout 설정
      connection-timeout: 30000  # 30초
      idle-timeout: 600000       # 10분
      max-lifetime: 1800000      # 30분

      # 연결 테스트
      connection-test-query: SELECT 1
      pool-name: BifHikariCP-Prod

      # 누수 감지
      leak-detection-threshold: 60000  # 1분
```

### 3.2 MySQL JDBC URL 최적화

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048&useServerPrepStmts=true&useLocalSessionState=true&useLocalTransactionState=true&cacheResultSetMetadata=true&cacheServerConfiguration=true&elideSetAutoCommits=true&maintainTimeStats=false
```

**주요 파라미터 설명**:
- `rewriteBatchedStatements=true`: 배치 INSERT/UPDATE 최적화
- `cachePrepStmts=true`: PreparedStatement 캐싱
- `prepStmtCacheSize=250`: 캐시할 statement 수
- `useServerPrepStmts=true`: 서버 사이드 prepared statements 사용
- `cacheServerConfiguration=true`: 서버 설정 캐싱으로 왕복 감소

### 3.3 Connection Pool 크기 계산

```
공식: Pool Size = ((core_count × 2) + effective_spindle_count)

예시:
- db.t3.micro (1 vCPU, EBS): (1 × 2) + 1 = 3-5
- db.t3.small (2 vCPU, EBS): (2 × 2) + 1 = 5-10
- db.t3.medium (2 vCPU, EBS): (2 × 2) + 1 = 5-10

애플리케이션 인스턴스 수 고려:
- 3개 인스턴스 × 10 connections = 30 connections
- RDS max_connections = 200이면 충분
```

---

## 4. Hibernate DDL 전략

### 4.1 환경별 DDL 설정

```yaml
# application-dev.yml (로컬 개발)
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # 시작 시 생성, 종료 시 삭제
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

# application-test.yml (테스트)
spring:
  jpa:
    hibernate:
      ddl-auto: create  # 매번 새로 생성

# application-prod.yml (프로덕션)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ⭐ 스키마 검증만, 변경 불가
    show-sql: false
  flyway:
    enabled: true  # Flyway로 마이그레이션 관리
```

### 4.2 DDL-Auto 옵션 비교

| 옵션 | 동작 | 권장 환경 | 위험도 |
|-----|------|----------|--------|
| `none` | 아무 작업 안함 | 수동 관리 | ⭐ 안전 |
| `validate` | 스키마 검증만 | **프로덕션** | ⭐ 안전 |
| `update` | 스키마 업데이트 (삭제 안함) | 개발 중 | ⚠️ 주의 |
| `create` | Drop → Create | 첫 배포, 로컬 | ❌ 위험 |
| `create-drop` | Create → Drop | 테스트 | ❌ 위험 |

### 4.3 Startup Task 관리 (중요!)

**❌ 잘못된 예시** - DDL 실행 전 쿼리 시도:
```java
@Service
public class CacheWarmingService {

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCacheOnStartup() {
        // ❌ DDL 실행 전에 쿼리 → 테이블 없음 에러
        List<User> users = userRepository.findAll();
    }
}
```

**✅ 올바른 예시 1** - 프로덕션 비활성화:
```java
@Service
@Profile("!prod")  // 프로덕션 제외
public class CacheWarmingService {

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCacheOnStartup() {
        try {
            Thread.sleep(5000);  // DDL 완료 대기
            List<User> users = userRepository.findAll();
        } catch (Exception e) {
            log.error("Cache warming failed", e);
        }
    }
}
```

**✅ 올바른 예시 2** - 조건부 실행:
```java
@Service
public class CacheWarmingService {

    @Value("${cache.warming.enabled:false}")
    private boolean warmingEnabled;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCacheOnStartup() {
        if (!warmingEnabled) {
            log.info("Cache warming disabled");
            return;
        }
        // 캐시 워밍 로직
    }
}
```

---

## 5. 초기 배포 시 스키마 생성

### 방법 1: Flyway Migration (✅ 권장)

#### Step 1: Flyway 설정

```yaml
# application-prod.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway가 스키마 관리
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    baseline-version: 0
    baseline-description: "Initial baseline"
```

#### Step 2: 초기 마이그레이션 생성

```sql
-- src/main/resources/db/migration/V1__initial_schema.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 추가 테이블들...
```

#### Step 3: 로컬에서 검증

```bash
# 로컬 MySQL에서 테스트
./gradlew flywayMigrate -Dflyway.url=jdbc:mysql://localhost:3306/bifai_db

# 마이그레이션 히스토리 확인
./gradlew flywayInfo
```

#### Step 4: 프로덕션 배포

```bash
# 배포 시 자동으로 Flyway 실행
# application-prod.yml의 spring.flyway.enabled=true로 인해
# 애플리케이션 시작 시 자동 마이그레이션
```

### 방법 2: Hibernate Schema Export (첫 배포만)

#### Step 1: 스키마 생성 활성화

```yaml
# application-prod.yml (첫 배포 시에만!)
spring:
  jpa:
    hibernate:
      ddl-auto: create  # ⚠️ 첫 배포만!
    properties:
      jakarta.persistence.schema-generation:
        scripts:
          action: create
          create-target: /tmp/schema.sql
  flyway:
    enabled: false  # 첫 배포 시 비활성화
```

#### Step 2: 배포 및 검증

```bash
# 1. 배포
# 2. 애플리케이션 시작 → 테이블 자동 생성
# 3. 테이블 확인
mysql -h <RDS_ENDPOINT> -u admin -p -e "SHOW TABLES FROM bifai_db;"
```

#### Step 3: 설정 변경 (중요!)

```yaml
# application-prod.yml (배포 완료 후 즉시 변경!)
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # ✅ validate로 변경
  flyway:
    enabled: true  # Flyway 활성화
```

#### Step 4: 재배포

```bash
git add application-prod.yml
git commit -m "feat: ddl-auto를 validate로 변경, Flyway 활성화"
git push
```

### 방법 3: 수동 DDL 실행

#### Step 1: 로컬에서 스키마 생성

```bash
# 로컬에서 앱 실행하여 DDL 생성
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun

# 생성된 SQL 확인
cat /tmp/schema_dev.sql
```

#### Step 2: RDS에 수동 실행

```bash
# RDS에 직접 연결하여 DDL 실행
mysql -h bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com \
      -u admin -p bifai_db < /tmp/schema_dev.sql
```

#### Step 3: 검증

```bash
# 테이블 생성 확인
mysql -h bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com \
      -u admin -p -e "
USE bifai_db;
SHOW TABLES;
DESC users;
"
```

---

## 6. 모니터링 및 성능 최적화

### 6.1 CloudWatch 로그 활성화

```bash
# RDS 로그 CloudWatch로 전송
aws rds modify-db-instance \
  --db-instance-identifier bifai-db-prod \
  --cloudwatch-logs-export-configuration \
    'EnableLogTypes=["error","general","slowquery"]' \
  --apply-immediately
```

### 6.2 Performance Insights 활성화

```bash
# Performance Insights 활성화 (7일 무료)
aws rds modify-db-instance \
  --db-instance-identifier bifai-db-prod \
  --enable-performance-insights \
  --performance-insights-retention-period 7 \
  --apply-immediately
```

### 6.3 주요 모니터링 지표

| 지표 | 임계값 | 설명 |
|-----|-------|------|
| `CPUUtilization` | < 80% | CPU 사용률 |
| `DatabaseConnections` | < 160 (80% of 200) | 활성 연결 수 |
| `FreeableMemory` | > 200MB (20%) | 여유 메모리 |
| `ReadLatency` | < 20ms | 읽기 지연 시간 |
| `WriteLatency` | < 20ms | 쓰기 지연 시간 |
| `DiskQueueDepth` | < 10 | 디스크 대기 큐 |

### 6.4 슬로우 쿼리 분석

```sql
-- 슬로우 쿼리 확인 (RDS 콘솔 > 로그)
SELECT * FROM mysql.slow_log
ORDER BY query_time DESC
LIMIT 10;

-- 인덱스 없는 쿼리
SELECT * FROM mysql.slow_log
WHERE sql_text LIKE '%JOIN%'
  AND NOT sql_text LIKE '%USE INDEX%'
LIMIT 10;
```

---

## 7. 트러블슈팅

### 7.1 "Table doesn't exist" 에러

**증상**:
```
Table 'bifai_db.users' doesn't exist
```

**원인**:
1. Hibernate `ddl-auto: create`가 실행되지 않음
2. Startup task가 DDL 전에 쿼리 실행
3. 데이터베이스 연결 실패

**해결**:
```bash
# 1. Startup task 확인
grep -r "@EventListener(ApplicationReadyEvent" src/

# 2. 로깅 활성화하여 DDL 실행 확인
# application-prod.yml
logging:
  level:
    org.hibernate.tool.schema: DEBUG
    org.hibernate.SQL: DEBUG

# 3. 수동으로 테이블 생성
mysql -h <RDS_ENDPOINT> -u admin -p bifai_db < schema.sql
```

### 7.2 Connection Pool Exhaustion

**증상**:
```
HikariPool - Connection is not available, request timed out after 30000ms
```

**원인**:
- Connection leak (연결 닫지 않음)
- Pool 크기 부족
- 슬로우 쿼리로 인한 연결 고갈

**해결**:
```yaml
# 1. Pool 크기 증가
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # 10 → 20
      leak-detection-threshold: 30000  # 누수 감지

# 2. 타임아웃 조정
spring:
  datasource:
    hikari:
      connection-timeout: 60000  # 30s → 60s

# 3. Connection 사용 패턴 확인
@Transactional  # 자동으로 connection 관리
public void someMethod() {
    // Repository 작업
}
```

### 7.3 Failover 후 연결 실패

**증상**:
```
Communications link failure: The last packet sent successfully to the server was 0 milliseconds ago
```

**AWS Advanced JDBC Wrapper 사용**:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>software.amazon.jdbc</groupId>
    <artifactId>aws-advanced-jdbc-wrapper</artifactId>
    <version>2.3.1</version>
</dependency>
```

```yaml
# application-prod.yml
spring:
  datasource:
    driver-class-name: software.amazon.jdbc.Driver
    url: jdbc:aws-wrapper:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    hikari:
      data-source-properties:
        enableClusterAwareFailover: true
        failoverTimeoutMs: 180000
```

### 7.4 스키마 불일치

**증상**:
```
Schema validation failed: Table 'users' has wrong column 'email'
```

**해결**:
```bash
# 1. 현재 스키마 확인
mysql -h <RDS_ENDPOINT> -u admin -p bifai_db -e "DESC users;"

# 2. Entity와 비교
cat src/main/java/com/bifai/reminder/bifai_backend/entity/User.java

# 3. Flyway로 마이그레이션 생성
# src/main/resources/db/migration/V2__add_email_column.sql
ALTER TABLE users ADD COLUMN email VARCHAR(255);

# 4. 마이그레이션 실행
./gradlew flywayMigrate
```

---

## 배포 체크리스트

### 초기 배포 전
- [ ] RDS 인스턴스 생성 완료
- [ ] Parameter Group 최적화
- [ ] 보안 그룹 설정 (EC2 → RDS)
- [ ] Startup task 비활성화 또는 지연 실행
- [ ] `ddl-auto: create` 설정 (첫 배포만)
- [ ] 환경 변수 설정 (`DB_HOST`, `DB_PASSWORD` 등)

### 초기 배포 후
- [ ] 테이블 생성 확인
- [ ] `ddl-auto: validate`로 변경
- [ ] Flyway 활성화
- [ ] CloudWatch 로그 확인
- [ ] Performance Insights 확인
- [ ] 슬로우 쿼리 모니터링 설정

### 프로덕션 운영
- [ ] 정기 백업 확인 (일일)
- [ ] Connection pool 지표 모니터링
- [ ] CPU/Memory 사용률 < 80%
- [ ] 슬로우 쿼리 주간 리뷰
- [ ] RDS 마이너 버전 업그레이드 계획

---

## 참고 자료

### AWS 공식 문서
- [Best Practices for Amazon RDS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)
- [RDS for MySQL Parameters](https://aws.amazon.com/blogs/database/best-practices-for-configuring-parameters-for-amazon-rds-for-mysql-part-1-parameters-related-to-performance/)
- [Performance Insights](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_PerfInsights.html)

### Spring Boot / Hibernate
- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)

### 성능 최적화
- [Hibernate Performance Tuning](https://vladmihalcea.com/tutorials/hibernate/)
- [MySQL Performance Tuning](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [AWS Advanced JDBC Wrapper](https://github.com/awslabs/aws-advanced-jdbc-wrapper)
