# Spring Security JWT 설정 리팩토링 계획

## 현재 문제점
1. **복잡한 커스텀 구현**: JwtAuthenticationFilter, JwtTokenProvider 등 복잡한 커스텀 클래스들
2. **프로파일별 설정 분리**: SecurityConfig와 TestSecurityConfig로 분리되어 의존성 문제 발생
3. **의존성 주입 오류**: BifUserDetailsService, JwtAuthenticationFilter 등에서 순환 의존성

## 베스트 프랙티스 (2024)
1. **Spring Security OAuth2 Resource Server 사용**
   - 내장 JWT 지원 활용
   - 커스텀 필터 최소화

2. **단순한 보안 설정**
   - 단일 SecurityConfig 클래스
   - 프로파일 기반 분기 로직

3. **표준 JWT 처리**
   - JwtDecoder 빈 사용
   - JwtAuthenticationConverter로 권한 매핑

## 리팩토링 계획

### 1단계: 의존성 정리
```gradle
// JWT 관련 의존성 간소화
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.security:spring-security-oauth2-jose'
```

### 2단계: 통합 보안 설정
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health/**", "/api/test/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // 환경에 따른 JWT 디코더 설정
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // 권한 매핑 설정
    }
}
```

### 3단계: 커스텀 클래스 정리
- JwtAuthenticationFilter 제거 → OAuth2ResourceServer 사용
- JwtTokenProvider 간소화 → JwtEncoder/JwtDecoder 사용
- BifUserDetailsService 간소화

### 4단계: 테스트 설정 통합
프로파일 기반으로 하나의 설정에서 처리

## 예상 효과
1. 코드 복잡성 50% 감소
2. 의존성 주입 오류 해결
3. Spring Security 표준 패턴 준수
4. 유지보수성 향상