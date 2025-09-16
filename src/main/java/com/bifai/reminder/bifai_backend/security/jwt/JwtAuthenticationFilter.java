package com.bifai.reminder.bifai_backend.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 - 요청마다 JWT 토큰 검증
 * BIF 사용자의 안전한 API 접근을 위한 토큰 기반 인증
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, 
                                 @Qualifier("bifUserDetailsService") UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 헬스체크 및 공개 엔드포인트에 대해서는 로그 레벨 조정
        boolean isPublicEndpoint = isPublicEndpoint(requestURI);

        if (!isPublicEndpoint) {
            log.info("🔍 JWT Filter 실행 - {} {}", method, requestURI);
        } else {
            log.debug("🔍 JWT Filter 실행 (공개 엔드포인트) - {} {}", method, requestURI);
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (!isPublicEndpoint && log.isDebugEnabled()) {
                log.debug("🔍 Authorization 헤더: {}", request.getHeader("Authorization"));
                log.debug("🔍 추출된 JWT: {}", jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null");
            }

            if (StringUtils.hasText(jwt)) {
                if (!isPublicEndpoint) {
                    log.info("🔍 토큰 검증 시작...");
                }

                boolean isValid = tokenProvider.validateToken(jwt);

                if (!isPublicEndpoint) {
                    log.info("🔍 토큰 검증 결과: {}", isValid);
                }

                if (isValid) {
                    String username = tokenProvider.getUsernameFromToken(jwt);

                    if (!isPublicEndpoint) {
                        log.info("🔍 토큰에서 추출된 사용자명: {}", username);
                    }

                    // 토큰 타입 확인 (access token만 허용)
                    String tokenType = tokenProvider.getTokenType(jwt);

                    if (!isPublicEndpoint) {
                        log.info("🔍 토큰 타입: {}", tokenType);
                    }

                    if (!"access".equals(tokenType)) {
                        log.warn("❌ 잘못된 토큰 타입 사용: {} (URI: {})", tokenType, requestURI);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    if (!isPublicEndpoint) {
                        log.info("🔍 UserDetailsService로 사용자 정보 로드 시도...");
                    }

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (!isPublicEndpoint) {
                        log.info("🔍 사용자 정보 로드 성공: {}, 권한: {}", userDetails.getUsername(), userDetails.getAuthorities());
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    if (!isPublicEndpoint) {
                        log.info("✅ 사용자 '{}' 인증 성공, SecurityContext에 설정 완료", username);
                    }
                } else {
                    log.warn("❌ JWT 토큰 검증 실패 (URI: {})", requestURI);
                }
            } else {
                if (!isPublicEndpoint) {
                    log.info("ℹ️ JWT 토큰이 없음 - 인증되지 않은 요청으로 진행 (URI: {})", requestURI);
                } else {
                    log.debug("ℹ️ JWT 토큰이 없음 - 공개 엔드포인트 접근 (URI: {})", requestURI);
                }
            }
        } catch (Exception ex) {
            log.error("❌ 사용자 인증 중 오류 발생 (URI: {}): {}", requestURI, ex.getMessage(), ex);
            // 인증 실패 시에도 필터 체인 계속 실행
            // Spring Security가 적절한 응답 처리
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 공개 엔드포인트 여부 확인
     * 헬스체크, 인증, actuator 등 JWT 토큰이 필요하지 않은 엔드포인트
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/health") ||
               requestURI.startsWith("/health") ||
               requestURI.startsWith("/actuator") ||
               requestURI.startsWith("/api/auth") ||
               requestURI.startsWith("/api/oauth") ||
               requestURI.startsWith("/error") ||
               requestURI.startsWith("/favicon.ico");
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     * Authorization 헤더: "Bearer {token}" 형식
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
} 