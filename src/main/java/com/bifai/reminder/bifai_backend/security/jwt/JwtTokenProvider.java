package com.bifai.reminder.bifai_backend.security.jwt;

import com.bifai.reminder.bifai_backend.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 서비스
 * BIF 사용자를 위한 안전한 인증 토큰 관리
 * 
 * 주요 기능:
 * - JWT Access Token 및 Refresh Token 생성
 * - 토큰 유효성 검증 및 만료 확인
 * - 토큰에서 사용자 정보 추출
 * 
 * 보안 특징:
 * - HS512 서명 알고리즘 사용
 * - 토큰 만료 시간 관리
 * - 상세한 예외 처리 및 로깅
 */
@Slf4j
@Service
public class JwtTokenProvider {
    
    private final SecretKey jwtSecret;
    private final long jwtAccessTokenExpirationMs;
    private final long jwtRefreshTokenExpirationMs;
    
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecretString,
            @Value("${app.jwt.access-token-expiration-ms:900000}") long jwtAccessTokenExpirationMs, // 15분
            @Value("${app.jwt.refresh-token-expiration-ms:604800000}") long jwtRefreshTokenExpirationMs // 7일
    ) {
        // Secret Key가 충분히 긴지 확인 (HS512는 최소 512비트 = 64바이트 필요)
        if (jwtSecretString.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalArgumentException("JWT secret key must be at least 64 bytes for HS512");
        }
        
        this.jwtSecret = Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));
        this.jwtAccessTokenExpirationMs = jwtAccessTokenExpirationMs;
        this.jwtRefreshTokenExpirationMs = jwtRefreshTokenExpirationMs;
        
        log.info("JwtTokenProvider initialized - Access: {}ms, Refresh: {}ms", 
                jwtAccessTokenExpirationMs, jwtRefreshTokenExpirationMs);
    }
    
    /**
     * Access Token 생성
     */
    public String generateAccessToken(Authentication authentication) {
        return createTokenFromAuthentication(authentication, "access", jwtAccessTokenExpirationMs);
    }
    
    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Authentication authentication) {
        return createTokenFromAuthentication(authentication, "refresh", jwtRefreshTokenExpirationMs);
    }
    
    /**
     * User 엔티티로 Access Token 생성 (OAuth2 로그인용)
     */
    public String createAccessToken(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(user.getEmail(), "User email cannot be null");
        Objects.requireNonNull(user.getUserId(), "User ID cannot be null");
        
        if (user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be empty");
        }
        
        return createTokenWithUserId(user.getEmail(), user.getUserId(), "access", jwtAccessTokenExpirationMs);
    }
    
    /**
     * User 엔티티로 Refresh Token 생성 (OAuth2 로그인용)
     */
    public String createRefreshToken(User user) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(user.getEmail(), "User email cannot be null");
        Objects.requireNonNull(user.getUserId(), "User ID cannot be null");
        
        if (user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be empty");
        }
        
        return createTokenWithUserId(user.getEmail(), user.getUserId(), "refresh", jwtRefreshTokenExpirationMs);
    }
    
    /**
     * Authentication으로부터 안전하게 토큰 생성
     */
    private String createTokenFromAuthentication(Authentication authentication, String tokenType, long expirationMs) {
        String username = extractUsernameFromAuthentication(authentication);
        if (username == null) {
            throw new IllegalArgumentException("Cannot extract username from authentication");
        }
        return createToken(username, tokenType, expirationMs);
    }
    
    /**
     * Authentication에서 안전하게 username 추출
     */
    private String extractUsernameFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            log.error("Authentication is null");
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            log.error("Authentication principal is null");
            return null;
        }
        
        // UserDetails 구현체인 경우
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            log.debug("Extracting username from UserDetails: {}", userDetails.getUsername());
            return userDetails.getUsername();
        }
        
        // String인 경우 (일부 인증 시나리오에서 발생할 수 있음)
        if (principal instanceof String) {
            log.debug("Extracting username from String principal: {}", principal);
            return (String) principal;
        }
        
        log.error("Unexpected principal type: {}. Expected UserDetails or String.", 
                principal.getClass().getSimpleName());
        return null;
    }

    /**
     * JWT 토큰 생성 (내부 메서드)
     */
    private String createToken(String username, String tokenType, long expirationMs) {
        return createTokenWithUserId(username, null, tokenType, expirationMs);
    }
    
    /**
     * user_id 클레임을 포함한 JWT 토큰 생성 (내부 메서드)
     */
    private String createTokenWithUserId(String username, Long userId, String tokenType, long expirationMs) {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(tokenType, "Token type cannot be null");
        
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("Expiration time must be positive");
        }
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        JwtBuilder jwtBuilder = Jwts.builder()
                .setSubject(username) // 토큰 주체 (사용자 식별자)
                .claim("type", tokenType) // 토큰 타입 (access/refresh)
                .setIssuedAt(now) // 토큰 발급 시간
                .setExpiration(expiration); // 토큰 만료 시간
        
        // userId가 있으면 클레임에 추가
        if (userId != null) {
            jwtBuilder.claim("user_id", userId);
        }
        
        return jwtBuilder
                .signWith(jwtSecret, SignatureAlgorithm.HS512) // 서명
                .compact();
    }
    
    /**
     * 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token is null or empty");
            return null;
        }
        
        try {
            Claims claims = getClaims(token);
            String subject = claims.getSubject();
            
            if (subject == null || subject.trim().isEmpty()) {
                log.warn("Token subject is null or empty");
                return null;
            }
            
            return subject;
        } catch (Exception e) {
            log.error("토큰에서 사용자명 추출 실패", e);
            return null;
        }
    }
    
    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String authToken) {
        if (authToken == null || authToken.trim().isEmpty()) {
            return false;
        }
        
        try {
            getClaims(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("토큰이 만료되었습니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 토큰입니다: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 토큰입니다: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("토큰 서명이 올바르지 않습니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("토큰이 비어있습니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("토큰 검증 중 오류 발생", e);
        }
        
        return false;
    }
    
    /**
     * 토큰 타입 확인 (access/refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("토큰에서 타입 추출 실패", e);
            return null;
        }
    }
    
    /**
     * 토큰 만료 시간 가져오기
     */
    public Date getExpirationFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("토큰에서 만료 시간 추출 실패", e);
            return null;
        }
    }
    
    /**
     * JWT 토큰이 만료되었는지 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true; // 만료된 토큰
        } catch (Exception e) {
            log.error("토큰 만료 확인 중 오류 발생", e);
            return true; // 오류 발생 시 만료된 것으로 간주
        }
    }
    
    /**
     * JWT 토큰을 파싱하여 Claims 객체 반환
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Access Token 만료 시간 (밀리초)
     */
    public long getAccessTokenExpirationMs() {
        return jwtAccessTokenExpirationMs;
    }
    
    /**
     * Refresh Token 만료 시간 (밀리초)
     */
    public long getRefreshTokenExpirationMs() {
        return jwtRefreshTokenExpirationMs;
    }
    
    /**
     * HTTP 요청에서 JWT 토큰 추출
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    /**
     * 토큰에서 사용자 ID 추출
     * JWT 토큰의 user_id 클레임에서 직접 추출
     */
    public Long getUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token is null or empty when extracting user ID");
            return null;
        }
        
        try {
            Claims claims = getClaims(token);
            Object userIdClaim = claims.get("user_id");
            
            if (userIdClaim == null) {
                log.warn("user_id claim not found in token");
                return null;
            }
            
            // Long 또는 Integer로 저장될 수 있으므로 안전하게 변환
            if (userIdClaim instanceof Number) {
                return ((Number) userIdClaim).longValue();
            }
            
            // String으로 저장된 경우도 처리
            if (userIdClaim instanceof String) {
                try {
                    return Long.parseLong((String) userIdClaim);
                } catch (NumberFormatException e) {
                    log.warn("Invalid user_id format in token: {}", userIdClaim);
                    return null;
                }
            }
            
            log.warn("Unexpected user_id type in token: {}", userIdClaim.getClass());
            return null;
            
        } catch (Exception e) {
            log.error("토큰에서 사용자 ID 추출 실패", e);
            return null;
        }
    }
} 