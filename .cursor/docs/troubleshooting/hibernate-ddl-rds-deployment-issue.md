# Hibernate DDL RDS 배포 트러블슈팅

## 문제 요약
AWS RDS 재생성 후 Spring Boot 애플리케이션 배포 시 Hibernate가 `ddl-auto: create` 설정에도 불구하고 테이블을 생성하지 않는 문제

## 환경
- **Spring Boot**: 3.5.3
- **Hibernate**: 6.x
- **Database**: AWS RDS MySQL 8.0.43
- **배포 환경**: EC2 (Docker Compose)

---

## 문제 발생 타임라인

### 1단계: 초기 문제 발견
**증상**:
- CI/CD는 성공하지만 애플리케이션이 정상 작동하지 않음
- RDS에 테이블이 하나도 생성되지 않음
- 로그에 "create table" 구문이 없음

**로그 확인**:
```bash
docker logs bifai-backend 2>&1 | grep 'create table'
# 결과: (없음)
```

**Hibernate 로그**:
```
o.h.t.s.s.SchemaManagementToolCoordinator - No schema actions specified for contributor `orm`; doing nothing
o.h.t.s.s.SchemaManagementToolCoordinator - No actions found; doing nothing
```

---

## 트러블슈팅 과정

### 시도 1: DEBUG 로깅 활성화 (실패)
**목적**: Hibernate 내부 동작 파악

**변경 사항** (`application-prod.yml`):
```yaml
logging:
  level:
    org.hibernate: DEBUG
    org.hibernate.tool.schema: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**결과**:
- ❌ 여전히 "No schema actions specified" 로그 발생
- DDL 실행 흔적 없음

---

### 시도 2: Docker MySQL 제거 및 RDS 직접 연결 (부분 성공)
**문제 발견**:
- 로컬 MySQL 컨테이너가 EC2에서 실행 중
- RDS 학습 후 RDS를 사용해야 함을 인지

**변경 사항** (`docker-compose.prod.yml`):
```yaml
# MySQL 컨테이너 완전 제거
services:
  # mysql:  # 삭제됨
  #   image: mysql:8.0

  app:
    environment:
      DB_HOST: ${DB_HOST:-bifai-db-prod.cncwewgskk3u.ap-northeast-2.rds.amazonaws.com}
      DB_USER: ${DB_USER:-admin}
      DB_PASSWORD: ${DB_PASSWORD}
```

**결과**:
- ✅ RDS 연결 성공
- ❌ 여전히 테이블 생성 안 됨

---

### 시도 3: 명시적 DDL 설정 추가 (실패)
**목적**: Spring과 JPA 양쪽 모두에 DDL 설정 명시

**변경 사항** (`application-prod.yml`):
```yaml
jpa:
  hibernate:
    ddl-auto: create
  properties:
    hibernate:
      hbm2ddl:
        auto: create  # 추가
    jakarta:
      persistence:
        schema-generation:
          database:
            action: create  # 추가
```

**결과**:
- ❌ 변화 없음
- 여전히 "No schema actions specified"

---

### 시도 4: DatabaseConfig.java 문제 발견 (✅ 해결)

**근본 원인 발견**:
[DatabaseConfig.java:130](../../src/main/java/com/bifai/reminder/bifai_backend/config/DatabaseConfig.java#L130)

```java
// 문제 코드
jpaProperties.setProperty("hibernate.hbm2ddl.auto",
    "${spring.jpa.hibernate.ddl-auto:validate}");
```

**문제 분석**:
- `Properties` 객체에 **플레이스홀더 문자열이 그대로** 전달됨
- Spring이 `${...}` 구문을 해석하지 못함
- Hibernate는 `"${spring.jpa.hibernate.ddl-auto:validate}"` 라는 **문자열 그대로** 받음
- 유효하지 않은 값이므로 DDL 액션을 무시함

**해결 방법**:
```java
// 필드 추가
@Value("${spring.jpa.hibernate.ddl-auto:validate}")
private String ddlAuto;

@Value("${spring.jpa.properties.hibernate.dialect:org.hibernate.dialect.H2Dialect}")
private String hibernateDialect;

@Value("${spring.jpa.show-sql:false}")
private String showSql;

@Value("${spring.jpa.properties.hibernate.format_sql:true}")
private String formatSql;

// Properties 설정 수정
Properties jpaProperties = new Properties();
jpaProperties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
jpaProperties.setProperty("hibernate.dialect", hibernateDialect);
jpaProperties.setProperty("hibernate.show_sql", showSql);
jpaProperties.setProperty("hibernate.format_sql", formatSql);

log.info("Hibernate 설정 - DDL Auto: {}, Dialect: {}", ddlAuto, hibernateDialect);
```

**결과**:
- ✅ Hibernate가 `ddl-auto: create` 인식
- ✅ 49개 테이블 모두 생성 성공
- ✅ 애플리케이션 정상 작동

---

## 최종 해결책 요약

### 1. 플레이스홀더 해결 필요성
Spring의 `@Configuration` 클래스에서 `Properties` 객체를 직접 설정할 때는:
- ❌ `"${property.name}"` 플레이스홀더 사용 불가
- ✅ `@Value` 어노테이션으로 주입받아 사용

### 2. 커스텀 DataSource 사용 시 주의사항
`LocalContainerEntityManagerFactoryBean`을 직접 구성할 때:
- `application.yml`의 `spring.jpa.*` 설정이 **자동 적용되지 않음**
- 모든 JPA 프로퍼티를 **수동으로 설정**해야 함
- 환경 변수보다 **코드 설정이 우선**

### 3. 검증 로그 추가
```java
log.info("Hibernate 설정 - DDL Auto: {}, Dialect: {}", ddlAuto, hibernateDialect);
```
- 애플리케이션 시작 시 설정값 확인 가능
- 배포 후 즉시 문제 파악 가능

---

## 배포 결과

### 생성된 테이블 (49개)
```sql
mysql> SHOW TABLES;
+---------------------------+
| Tables_in_bifai_db        |
+---------------------------+
| accessibility_settings    |
| activity_logs             |
| activity_metadata         |
| analysis_results          |
| battery_history           |
| captured_images           |
| connectivity_logs         |
| content_metadata          |
| devices                   |
| emergencies               |
| emergency_contacts        |
| experiments               |
| fall_events               |
| geofence_events           |
| geofences                 |
| guardian_relationships    |
| guardians                 |
| health_metrics            |
| image_analyses            |
| interaction_patterns      |
| location_history          |
| locations                 |
| media_files               |
| medication_adherence      |
| medication_times          |
| medications               |
| movement_patterns         |
| notification_channels     |
| notification_deliveries   |
| notification_history      |
| notification_templates    |
| notifications             |
| pose_data                 |
| pose_sessions             |
| reminder_templates        |
| reminders                 |
| roles                     |
| safe_routes               |
| schedule_days             |
| schedules                 |
| template_channels         |
| test_group_assignments    |
| test_groups               |
| test_variants             |
| user_behavior_logs        |
| user_preferences          |
| user_roles                |
| users                     |
| wandering_detections      |
+---------------------------+
49 rows in set (0.02 sec)
```

### Health Check 결과
```bash
curl http://43.200.49.171:8080/api/health
{
  "s": true,
  "d": {
    "message": "Application is running",
    "status": "UP"
  },
  "t": [2025, 10, 10, 11, 6, 43, 197482096]
}
```

---

## 교훈

### 1. 커스텀 Configuration 사용 시 주의점
- Spring Boot Auto Configuration을 우회할 때는 모든 설정을 명시적으로 관리
- 플레이스홀더는 `@Value`로만 해석 가능

### 2. 디버깅 전략
1. **로그 레벨 조정**: DEBUG로 내부 동작 확인
2. **환경 변수 확인**: 컨테이너 내부 실제 값 검증
3. **코드 추적**: 설정이 어디서 오버라이드되는지 확인
4. **검증 로그**: 핵심 설정값을 로그로 출력

### 3. RDS 배포 Best Practice
- 로컬 MySQL 컨테이너 제거
- RDS 엔드포인트 직접 사용
- HikariCP 최적화 설정 적용
- DDL-auto는 초기 배포 후 `validate`로 변경 필요

---

## 관련 문서
- [AWS RDS Production Deployment Guide](../rules/aws-rds-production-deployment-guide.md)
- [Spring Boot RDS Best Practices](../rules/spring-boot-rds-best-practices.md)

## 작성 정보
- **작성일**: 2025-10-10
- **소요 시간**: 약 1.5시간
- **최종 커밋**: `9b6db2a - fix: Inject Hibernate DDL settings via @Value instead of unresolved placeholders`
