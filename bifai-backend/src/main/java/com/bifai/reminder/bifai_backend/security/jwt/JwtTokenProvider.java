package com.bifai.reminder.bifai_backend.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return createToken(userPrincipal.getUsername(), "access", jwtAccessTokenExpirationMs);
    }
    
    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return createToken(userPrincipal.getUsername(), "refresh", jwtRefreshTokenExpirationMs);
    }
    
    /**
     * JWT 토큰 생성 (내부 메서드)
     */
    private String createToken(String username, String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username) // 토큰 주체 (사용자 식별자)
                .claim("type", tokenType) // 토큰 타입 (access/refresh)
                .setIssuedAt(now) // 토큰 발급 시간
                .setExpiration(expiration) // 토큰 만료 시간
                .signWith(jwtSecret, SignatureAlgorithm.HS512) // 서명
                .compact();
    }
    
    /**
     * 토큰에서 사용자명 추출
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getSubject();
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
} 