package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthenticationFilter;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import com.bifai.reminder.bifai_backend.security.BifAuthenticationEntryPoint;
import com.bifai.reminder.bifai_backend.security.BifAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 설정 - JWT 기반 인증 및 권한 관리
 * BIF 사용자를 위한 보안 설정
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Order(1) // 높은 우선순위로 변경
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
public class SecurityConfig {

    private final BifUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BifAuthenticationEntryPoint bifAuthenticationEntryPoint;
    private final BifAccessDeniedHandler bifAccessDeniedHandler;

    /**
     * Spring Security 필터 체인 설정
     * 
     * <p>BIF-AI 시스템의 보안 정책을 정의합니다:</p>
     * <ul>
     *   <li>JWT 기반 인증 (Stateless)</li>
     *   <li>CORS 설정으로 모바일 앱 지원</li>
     *   <li>공개/보호 엔드포인트 분리</li>
     * </ul>
     * 
     * @param http HttpSecurity 객체
     * @return 설정된 SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security with JWT support");
        
        http
            // CSRF 비활성화 - JWT를 사용하므로 CSRF 토큰 불필요
            .csrf(csrf -> csrf.disable())
            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 세션 사용하지 않음 (JWT 사용)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 엔드포인트별 접근 권한 설정 - 단계별 테스트
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트 (인증 불필요)
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/health/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()

                // OAuth2 관련 공개 엔드포인트
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()

                // 인증 관련 공개 엔드포인트
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/register").permitAll()
                .requestMatchers("/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/auth/oauth2/login-urls").permitAll()

                // 초대 관련 공개 엔드포인트 (이메일 링크 접근)
                .requestMatchers("/api/guardian-relationships/accept-invitation").permitAll()
                .requestMatchers("/api/guardian-relationships/reject-invitation").permitAll()

                // 접근성 기능은 인증 없이 허용 (장애인 접근성 고려)
                .requestMatchers("/api/v1/accessibility/**").permitAll()

                // 모든 API 엔드포인트는 인증 필요
                .requestMatchers("/api/**").authenticated()

                // 나머지는 허용 (정적 리소스 등)
                .anyRequest().permitAll()
            )
            // 커스텀 예외 처리 핸들러 설정
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(bifAuthenticationEntryPoint)
                .accessDeniedHandler(bifAccessDeniedHandler)
            )
            // H2 콘솔을 위한 iframe 허용
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // JWT 인증 필터 추가 - UsernamePasswordAuthenticationFilter 전에 실행
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Spring Security configuration completed successfully with JWT filter");
        return http.build();
    }

    /**
     * 비밀번호 인코더 빈 설정
     * 
     * <p>BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 암호화합니다.
     * work factor 12는 보안성과 성능의 균형을 고려한 값입니다.</p>
     * 
     * <p>BCrypt 특징:</p>
     * <ul>
     *   <li>salt를 자동으로 생성하여 rainbow table 공격 방지</li>
     *   <li>work factor로 연산 비용 조절 가능</li>
     *   <li>단방향 해시로 원본 비밀번호 복구 불가</li>
     * </ul>
     * 
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Creating BCryptPasswordEncoder with work factor 12");
        return new BCryptPasswordEncoder(12);
    }

    /**
     * DaoAuthenticationProvider 설정
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        log.debug("Creating DaoAuthenticationProvider");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * AuthenticationManager 빈 설정
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.debug("Creating AuthenticationManager");
        return config.getAuthenticationManager();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 설정
     * 
     * <p>BIF-AI 모바일 앱과 웹 클라이언트의 API 접근을 허용합니다.
     * 보안을 위해 특정 도메인만 허용하고 있습니다.</p>
     * 
     * <p>허용된 도메인:</p>
     * <ul>
     *   <li>개발 환경: localhost:3000, localhost:8080</li>
     *   <li>프로덕션: *.bifai.com</li>
     *   <li>모바일 앱: capacitor://localhost (iOS), http://localhost (Android)</li>
     * </ul>
     * 
     * @return CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("Creating CORS configuration");
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 도메인 패턴 설정
        // BIF 사용자를 위한 모바일 앱 지원 - 보안을 위해 특정 도메인만 허용
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",      // 프론트엔드 개발 서버
            "http://localhost:3001",      // Flutter 웹 개발 서버
            "http://localhost:3002",      // Flutter 웹 개발 서버
            "http://localhost:3003",      // Flutter 웹 개발 서버
            "http://localhost:3004",      // Flutter 웹 개발 서버
            "http://localhost:3005",      // Flutter 웹 개발 서버
            "http://localhost:3006",      // Flutter 웹 개발 서버
            "http://localhost:8000",      // Flutter 웹 개발 서버
            "http://localhost:8001",      // Flutter 웹 개발 서버
            "http://localhost:8080",      // 백엔드 개발 서버
            "https://*.bifai.com",        // 프로덕션 도메인
            "http://43.200.49.171:*",     // EC2 프로덕션 서버
            "capacitor://localhost",      // iOS 앱 (Capacitor)
            "http://localhost"            // Android 앱 (Capacitor)
        ));
        
        // 허용할 HTTP 메소드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 허용할 헤더 (Flutter 앱 호환성을 위해 확장)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",      // JWT 토큰
            "Content-Type",       // 콘텐츠 타입
            "X-Requested-With",   // AJAX 요청 식별
            "Accept",             // 응답 타입 지정
            "Origin",             // 요청 출처
            "X-Auth-Token",       // 추가 인증 토큰 헤더
            "X-HTTP-Method-Override" // HTTP 메소드 오버라이드
        ));
        
        // 클라이언트에 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count")); // 페이지네이션용
        
        // 인증 정보 포함 허용 (JWT 토큰 전송을 위해 필요)
        configuration.setAllowCredentials(true);
        
        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용
        
        log.info("CORS configuration created: allowed origins = {}", configuration.getAllowedOriginPatterns());
        return source;
    }
}