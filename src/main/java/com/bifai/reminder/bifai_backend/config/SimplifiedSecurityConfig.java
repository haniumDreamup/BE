package com.bifai.reminder.bifai_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 간소화된 Spring Security 설정 (베스트 프랙티스 적용)
 * OAuth2 Resource Server 기반 JWT 인증 사용
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SimplifiedSecurityConfig {

    private final Environment environment;

    public SimplifiedSecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // 공개 엔드포인트 (모든 환경에서 인증 없이 접근 가능)
                auth.requestMatchers("/api/health/**", "/health", "/api/v1/health", "/api/v2/health").permitAll()
                    .requestMatchers("/h2-console/**", "/error").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                    // 인증 관련 공개 엔드포인트 (선택적 허용)
                    .requestMatchers("/api/v1/auth/health", "/api/v1/auth/register",
                                   "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()

                    // 초대 관련 공개 엔드포인트
                    .requestMatchers("/api/guardian-relationships/accept-invitation",
                                   "/api/guardian-relationships/reject-invitation").permitAll()

                    // 접근성 기능은 인증 없이 허용
                    .requestMatchers("/api/v1/accessibility/**").permitAll();

                if (isTestProfile) {
                    // 테스트 환경: 다른 모든 요청은 401 반환
                    auth.anyRequest().authenticated();
                } else {
                    // 프로덕션 환경: API 인증 필요
                    auth.requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll();
                }
            })
            .headers(headers -> headers.frameOptions().sameOrigin());

        // 테스트 환경이 아닌 경우에만 JWT 설정 적용
        if (!isTestProfile) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        } else {
            // 테스트 환경에서 인증 실패 시 401 반환하도록 설정
            http.httpBasic(basic -> basic.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"인증이 필요합니다\"}");
            }));
        }

        return http.build();
    }

    @Bean
    @Profile("!test")
    public JwtDecoder jwtDecoder() {
        String jwtSecret = environment.getProperty("JWT_SECRET",
            "YmlmYWlSZW1pbmRlclNlY3JldEtleUZvckpXVFRva2VuR2VuZXJhdGlvbjIwMjRCb3JkZXJsaW5lSW50ZWxsaWdlbmNlRnVuY3Rpb25pbmdSZW1pbmRlclNlcnZpY2U=");

        SecretKey secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        );

        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}