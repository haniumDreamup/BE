package com.bifai.reminder.bifai_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증 응답 DTO
 * 로그인 성공 시 클라이언트에게 전달되는 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * JWT Access Token
     */
    private String accessToken;
    
    /**
     * JWT Refresh Token
     */
    private String refreshToken;
    
    /**
     * 토큰 타입 (Bearer)
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * Access Token 만료 시간 (밀리초)
     */
    private Long accessTokenExpiresIn;
    
    /**
     * Refresh Token 만료 시간 (밀리초)
     */
    private Long refreshTokenExpiresIn;
    
    /**
     * 사용자 기본 정보
     */
    private UserInfo user;
    
    /**
     * 로그인 시간
     */
    private LocalDateTime loginTime;
    
    /**
     * 사용자 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String username;
        private String email;
        private String fullName;
    }
} 