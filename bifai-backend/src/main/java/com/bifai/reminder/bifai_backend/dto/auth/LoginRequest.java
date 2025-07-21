package com.bifai.reminder.bifai_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 로그인 요청 DTO
 * BIF 사용자를 위한 단순하고 명확한 로그인 형식
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "사용자명 또는 이메일을 입력해주세요")
    @Size(max = 100, message = "사용자명은 100글자를 초과할 수 없습니다")
    private String usernameOrEmail;
    
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 4, max = 128, message = "비밀번호는 4글자 이상 128글자 이하여야 합니다")
    private String password;
    
    /**
     * 로그인 상태 유지 옵션
     * BIF 사용자를 위한 단순한 기억하기 기능
     */
    private Boolean rememberMe = false;
} 