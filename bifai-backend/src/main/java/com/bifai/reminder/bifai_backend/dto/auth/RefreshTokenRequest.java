package com.bifai.reminder.bifai_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 토큰 갱신 요청 DTO
 * Access Token 만료 시 Refresh Token을 사용한 토큰 갱신
 */
@Data
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh Token이 필요합니다")
    private String refreshToken;
} 