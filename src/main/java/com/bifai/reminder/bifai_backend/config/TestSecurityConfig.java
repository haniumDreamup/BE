package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Test profile에서 사용할 보안 설정
 * 모든 요청을 허용하여 테스트를 간소화
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Profile("test")
@Order(0) // 최고 우선순위
@RequiredArgsConstructor
public class TestSecurityConfig {

    private final BifUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Test SecurityConfig 활성화 - 통계 API 인증 테스트용 설정");

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()

                // 인증 관련 공개 엔드포인트
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/register").permitAll()
                .requestMatchers("/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/auth/oauth2/login-urls").permitAll()

                // 초대 관련 공개 엔드포인트 (이메일 링크 접근)
                .requestMatchers("/api/guardian-relationships/accept-invitation").permitAll()
                .requestMatchers("/api/guardian-relationships/reject-invitation").permitAll()

                // 접근성 API는 인증 불필요 (100% 성공률 유지)
                .requestMatchers("/api/v1/accessibility/**").permitAll()

                // 알림 검증 API 중 일부는 공개
                .requestMatchers("/api/notifications/validate-token").permitAll()

                // 긴급 연락처 API는 테스트용으로 허용
                .requestMatchers("/api/emergency-contacts/**").permitAll()

                // 인증 필요한 엔드포인트들 (401 테스트용) - 보안 테스트를 위해 활성화
                .requestMatchers("/api/statistics/**").authenticated()
                .requestMatchers("/api/guardian-relationships/**").authenticated()
                .requestMatchers("/api/sos/**").authenticated()
                .requestMatchers("/api/v1/pose/**").authenticated()
                .requestMatchers("/api/images/**").authenticated()
                .requestMatchers("/api/geofences/**").authenticated()
                .requestMatchers("/api/v1/emergency/**").authenticated()
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/guardians/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()

                // 나머지는 허용
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("인증 실패: {}", authException.getMessage());
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"error\":\"AUTHENTICATION_REQUIRED\",\"message\":\"로그인이 필요합니다\",\"status\":401}");
                })
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        log.info("Test SecurityConfig 설정 완료 - 401/400 상태 코드 테스트 가능");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("Creating CORS configuration for test profile");
        CorsConfiguration configuration = new CorsConfiguration();

        // Flutter 웹앱을 위한 CORS 설정
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",      // Flutter 웹 개발 서버
            "http://localhost:3001",      // Flutter 웹 개발 서버 (현재 사용 중)
            "http://localhost:3002",      // Flutter 웹 개발 서버 (현재 테스트 중)
            "http://localhost:3003",      // Flutter 웹 개발 서버 (CORS 해결용)
            "http://localhost:3004",      // Flutter 웹 개발 서버 (회원가입 테스트용)
            "http://localhost:3005",      // Flutter 웹 개발 서버 (UI 테스트용)
            "http://127.0.0.1:3000",      // Flutter 웹 개발 서버 (IP 주소)
            "http://127.0.0.1:3001",      // Flutter 웹 개발 서버 (IP 주소, 현재 사용 중)
            "http://127.0.0.1:3002",      // Flutter 웹 개발 서버 (IP 주소, 현재 테스트 중)
            "http://127.0.0.1:3003",      // Flutter 웹 개발 서버 (IP 주소, CORS 해결용)
            "http://127.0.0.1:3004",      // Flutter 웹 개발 서버 (IP 주소, 회원가입 테스트용)
            "http://127.0.0.1:3005",      // Flutter 웹 개발 서버 (IP 주소, UI 테스트용)
            "http://localhost:8000",      // Flutter 웹 개발 서버 (대체 포트)
            "http://localhost:8001",      // Flutter 웹 개발 서버 (대체 포트)
            "http://127.0.0.1:8000",      // Flutter 웹 개발 서버 (IP 주소, 대체 포트)
            "http://127.0.0.1:8001",      // Flutter 웹 개발 서버 (IP 주소, 대체 포트)
            "http://localhost:8080",      // 백엔드 개발 서버
            "http://127.0.0.1:8080",      // 백엔드 개발 서버 (IP 주소)
            "https://*.bifai.com",        // 프로덕션 도메인
            "capacitor://localhost",      // iOS 앱 (Capacitor)
            "http://localhost"            // Android 앱 (Capacitor)
        ));

        // 허용할 HTTP 메소드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 허용할 헤더 (명시적으로 필요한 헤더들 나열)
        // allowCredentials=true일 때 "*"는 사용할 수 없음
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "X-Auth-Token",
            "X-HTTP-Method-Override",
            "Cache-Control",
            "Pragma",
            "Expires",
            "If-Modified-Since",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers"
        ));

        // 클라이언트에 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList("X-Total-Count"));

        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        log.info("Test profile CORS configuration created: allowed origins = {}", configuration.getAllowedOriginPatterns());
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("Creating BCrypt password encoder for test profile");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        log.debug("Creating DaoAuthenticationProvider for test profile");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        log.debug("Creating AuthenticationManager for test profile");
        return config.getAuthenticationManager();
    }
}