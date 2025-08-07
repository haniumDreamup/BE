package com.bifai.reminder.bifai_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 로그인 요청 DTO
 * 
 * <p>BIF 사용자를 위한 단순하고 명확한 로그인 형식입니다.
 * 사용자명 또는 이메일로 로그인할 수 있습니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Schema(description = "로그인 요청 정보")
@Data
public class LoginRequest {
    
    @Schema(
        description = "사용자명 또는 이메일",
        example = "bifuser123",
        required = true,
        maxLength = 100
    )
    @NotBlank(message = "사용자명 또는 이메일을 입력해주세요")
    @Size(max = 100, message = "사용자명은 100글자를 초과할 수 없습니다")
    private String usernameOrEmail;
    
    @Schema(
        description = "비밀번호",
        example = "password123!",
        required = true,
        minLength = 4,
        maxLength = 128,
        format = "password"
    )
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 4, max = 128, message = "비밀번호는 4글자 이상 128글자 이하여야 합니다")
    private String password;
    
    /**
     * 로그인 상태 유지 옵션
     * 
     * <p>BIF 사용자를 위한 단순한 기억하기 기능입니다.
     * 체크하면 30일간 로그인 상태가 유지됩니다.</p>
     */
    @Schema(
        description = "로그인 상태 유지 여부 (30일간 자동 로그인)",
        example = "false",
        defaultValue = "false"
    )
    private Boolean rememberMe = false;
} 