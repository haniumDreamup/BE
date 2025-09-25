package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test profile에서 사용할 보안 설정
 * 모든 요청을 허용하여 테스트를 간소화
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Profile("test")
@Order(0) // 최고 우선순위
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Test SecurityConfig 활성화 - 통계 API 인증 테스트용 설정");

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()

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
}