# JPA Entity 스캔 문제 최종 해결 방안

## 문제 진단
Spring Boot 애플리케이션이 시작될 때 `Not a managed type: class com.bifai.reminder.bifai_backend.entity.User` 오류가 발생합니다.

## 근본 원인
1. **엔티티 스캔 타이밍 문제**: Spring Security 필터가 먼저 초기화되면서 UserRepository를 필요로 하는데, 이 시점에 엔티티가 아직 스캔되지 않음
2. **중복된 JPA 설정**: BifaiBackendApplication과 JpaConfig에서 중복 설정
3. **의존성 순서 문제**: Security → UserDetailsService → UserRepository → Entity 순서로 의존

## 해결 방안

### 즉시 적용 가능한 해결책

1. **Security를 Lazy 로딩으로 변경**
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       
       @Bean
       @Lazy
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           // 설정...
       }
   }
   ```

2. **JPA 설정 통합**
   - JpaConfig의 @EntityScan, @EnableJpaRepositories 제거
   - BifaiBackendApplication에서만 관리

3. **프로파일별 실행**
   - 개발: `--spring.profiles.active=dev,no-security`
   - 테스트: `--spring.profiles.active=test`
   - 운영: `--spring.profiles.active=prod`

### 장기적 해결책

1. **모듈 분리**
   - bifai-backend-core: 엔티티, 리포지토리
   - bifai-backend-security: 보안 설정
   - bifai-backend-web: 컨트롤러

2. **의존성 역전**
   - UserDetailsService를 인터페이스로 분리
   - 구현체에서 UserRepository 주입

## 현재 상황에서의 최선의 방법

1. JpaConfig에서 중복 어노테이션 제거 (완료)
2. Security 설정을 @Lazy로 변경
3. 테스트용 프로파일로 실행하여 동작 확인

## API 테스트 방법 (JPA 문제 해결 후)

```bash
# Health Check
curl -X GET http://localhost:8080/api/v1/health

# OAuth2 로그인 URL 조회
curl -X GET http://localhost:8080/api/v1/auth/oauth2/login-urls

# Swagger UI 접속
http://localhost:8080/swagger-ui.html
```

## 결론
현재 JPA 엔티티 스캔 문제는 Spring Security와 JPA 초기화 순서 문제입니다. Security를 Lazy 로딩으로 변경하거나, Security 없이 먼저 테스트하는 것이 가장 빠른 해결책입니다.