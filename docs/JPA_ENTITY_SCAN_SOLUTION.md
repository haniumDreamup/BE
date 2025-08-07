# JPA Entity Scan 문제 해결 분석

## 문제 상황
- Spring Boot 3.5.0에서 "Not a managed type: class com.bifai.reminder.bifai_backend.entity.User" 오류 발생
- EntityManagerFactory가 User 엔티티를 인식하지 못함
- @Entity 애노테이션과 @EntityScan 설정이 있음에도 불구하고 문제 지속

## 원인 분석

### 1. Spring Boot 3.x의 변경사항
- Jakarta EE 9 도입으로 javax.persistence에서 jakarta.persistence로 변경
- 엔티티는 이미 jakarta.persistence.Entity를 사용 중

### 2. 가능한 원인들
1. **클래스 로딩 순서 문제**: Spring Security가 JPA보다 먼저 초기화
2. **패키지 스캔 설정 충돌**: 여러 곳에서 @EntityScan 중복 설정
3. **Hibernate와 Spring Boot 버전 호환성 문제**
4. **빌드 캐시 문제**: 컴파일된 클래스 파일이 제대로 업데이트되지 않음

## 해결 방법

### 1. 메인 애플리케이션 클래스에 @EntityScan 추가 (권장)
```java
@SpringBootApplication
@EntityScan(basePackages = "com.bifai.reminder.bifai_backend.entity")
public class BifaiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BifaiBackendApplication.class, args);
    }
}
```

### 2. EntityManagerFactory 수동 설정
```java
@Configuration
public class JpaConfig {
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, 
            JpaVendorAdapter jpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bifai.reminder.bifai_backend.entity");
        em.setJpaVendorAdapter(jpaVendorAdapter);
        return em;
    }
}
```

### 3. application.yml에 패키지 스캔 설정 추가
```yaml
spring:
  jpa:
    packages-to-scan: com.bifai.reminder.bifai_backend.entity
```

### 4. 빌드 클린 및 캐시 제거
```bash
./gradlew clean
rm -rf ~/.gradle/caches/
./gradlew build --no-build-cache
```

## 최종 권장사항

1. **JpaConfig에서 모든 JPA 설정 중앙화**
2. **메인 애플리케이션 클래스는 깨끗하게 유지**
3. **빌드 캐시 문제 해결을 위해 정기적인 clean build**
4. **테스트 환경과 실제 환경의 설정 분리**

## 검증 방법

1. EntityManagerFactory 빈이 제대로 생성되는지 확인
2. 엔티티 클래스가 persistence unit에 포함되는지 로그 확인
3. 단위 테스트로 Repository 동작 검증