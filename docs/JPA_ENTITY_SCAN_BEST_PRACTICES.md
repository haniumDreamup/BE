# Spring Boot JPA Entity Scan 설정에 대한 베스트 프랙티스

## 핵심 원칙

### 1. Spring Boot의 기본 동작
Spring Boot는 `@SpringBootApplication`이 위치한 패키지와 그 하위 패키지에서 자동으로 다음을 스캔합니다:
- `@Entity`
- `@Embeddable`
- `@MappedSuperclass`
- Spring Data JPA Repository

### 2. @EntityScan이 필요한 경우
- 엔티티가 메인 애플리케이션 패키지 외부에 있을 때
- 여러 모듈로 분리된 프로젝트에서 엔티티가 다른 패키지에 있을 때
- 테스트에서 특정 엔티티만 선택적으로 로드하고 싶을 때

### 3. 현재 프로젝트 구조 분석
```
com.bifai.reminder.bifai_backend
├── BifaiBackendApplication.java (메인 클래스)
├── entity/
│   └── User.java (@Entity)
└── repository/
    └── UserRepository.java
```

**결론**: 현재 구조에서는 @EntityScan이 불필요합니다. 모든 엔티티가 메인 애플리케이션 패키지의 하위에 있기 때문입니다.

## 권장 설정 방법

### 1. 기본 설정 (권장)
```java
@SpringBootApplication
public class BifaiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BifaiBackendApplication.class, args);
    }
}
```

### 2. JPA 설정이 필요한 경우
```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA Auditing 등 추가 설정
}
```

### 3. 초기화 순서 문제 해결
Spring Security가 JPA보다 먼저 초기화되는 문제가 있을 경우:

```java
@Configuration
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "com.bifai.reminder.bifai_backend.repository")
@EnableJpaAuditing
public class JpaConfig {
}
```

## 문제 해결 체크리스트

1. **패키지 구조 확인**
   - 엔티티가 메인 애플리케이션 패키지 하위에 있는가?
   - Repository가 올바른 패키지에 있는가?

2. **의존성 확인**
   - spring-boot-starter-data-jpa가 포함되어 있는가?
   - 데이터베이스 드라이버가 classpath에 있는가?

3. **프로파일 설정**
   - 활성 프로파일에서 JPA 설정이 비활성화되어 있지 않은가?
   - spring.jpa.repositories.enabled=true (기본값)

4. **엔티티 애노테이션**
   - @Entity 애노테이션이 올바르게 적용되었는가?
   - jakarta.persistence.Entity를 사용하고 있는가? (Spring Boot 3.x)

## 현재 프로젝트에 대한 권장사항

1. **BifaiBackendApplication.java에서 JPA 관련 애노테이션 제거** ✓
2. **별도의 JpaConfig 클래스 사용** ✓
3. **@EntityScan과 @EnableJpaRepositories 제거 고려**
   - 기본 스캔으로 충분할 가능성이 높음
   - 문제가 계속되면 패키지 구조나 다른 설정 확인 필요

## 참고 자료
- Spring Boot 공식 문서: 기본적으로 자동 구성 패키지 내의 모든 엔티티를 스캔
- @EntityScan은 특별한 경우에만 필요
- 대부분의 프로젝트는 기본 설정으로 충분