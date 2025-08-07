# Spring Boot 3.x JPA Configuration Best Practices

## 핵심 변경사항
- Spring Boot 3.x는 Jakarta EE 9+를 사용 (javax → jakarta)
- Hibernate 6.x와 통합
- 자동 구성 개선

## 권장 설정 방법

### 1. 메인 애플리케이션 클래스 - 최소화
```java
@SpringBootApplication
public class BifaiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BifaiBackendApplication.class, args);
    }
}
```

### 2. JPA 설정 클래스 - 전용 설정
```java
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.bifai.reminder.bifai_backend.repository")
public class JpaConfig {
    // JPA Auditing 설정
}
```

### 3. 엔티티 패키지 구조
- 엔티티는 `@SpringBootApplication`이 있는 패키지의 하위에 위치해야 함
- 명시적 `@EntityScan`은 일반적으로 불필요

### 4. application.yml 설정
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect
    show-sql: false
    open-in-view: false
```

## 문제 해결 체크리스트

### "Not a managed type" 오류 시
1. **엔티티 클래스 확인**
   - `@Entity` 어노테이션 존재 여부
   - `jakarta.persistence.*` 패키지 사용 여부
   - 컴파일 오류 없는지 확인

2. **패키지 구조 확인**
   - 엔티티가 `@SpringBootApplication`의 하위 패키지에 있는지
   - 다른 패키지라면 `@EntityScan` 필요

3. **의존성 확인**
   - `spring-boot-starter-data-jpa` 포함 여부
   - Hibernate와 JPA 버전 호환성

4. **빌드 캐시 문제**
   ```bash
   ./gradlew clean build --no-build-cache
   ```

5. **프로파일별 설정**
   - 테스트 환경과 실행 환경의 설정 차이 확인
   - H2와 MySQL 설정 분리

## Spring Boot 3.x 특징

### 자동 구성
- ComponentScan이 기본적으로 엔티티도 스캔
- `@EntityScan`은 특별한 경우에만 필요:
  - 엔티티가 다른 패키지에 있을 때
  - 멀티 모듈 프로젝트
  - 외부 라이브러리의 엔티티 사용

### 권장사항
1. **단순하게 유지**: 기본 자동 구성 활용
2. **명시적 설정 최소화**: 필요한 경우에만 추가
3. **프로파일 활용**: 환경별 설정 분리
4. **로깅 활성화**: 문제 진단을 위한 상세 로그

## 현재 프로젝트 적용 방안

1. `@EntityScan` 제거 (메인 클래스에서)
2. JpaConfig에서 repository 설정만 유지
3. 엔티티 패키지가 올바른 위치에 있는지 확인
4. 빌드 캐시 정리 후 재시작