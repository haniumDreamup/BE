package com.bifai.reminder.bifai_backend.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * 회원가입 요청 DTO
 * BIF 사용자를 위한 포괄적인 회원가입 정보
 */
@Data
public class RegisterRequest {
    
    // 기본 사용자 정보
    @NotBlank(message = "사용자명을 입력해주세요")
    @Size(min = 3, max = 50, message = "사용자명은 3글자 이상 50글자 이하여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다")
    private String username;
    
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식을 입력해주세요")
    @Size(max = 100, message = "이메일은 100글자를 초과할 수 없습니다")
    private String email;
    
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 4, max = 128, message = "비밀번호는 4글자 이상 128글자 이하여야 합니다")
    private String password;
    
    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private String confirmPassword;
    
    @NotBlank(message = "이름을 입력해주세요")
    @Size(max = 100, message = "이름은 100글자를 초과할 수 없습니다")
    private String fullName;

    // 추가 사용자 정보
    private LocalDate birthDate;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별은 MALE, FEMALE, OTHER 중 하나여야 합니다")
    private String gender;

    @Pattern(regexp = "ko|en", message = "언어는 ko 또는 en만 지원됩니다")
    private String languagePreference = "ko";

    @Pattern(regexp = "ko|en", message = "보조 언어는 ko 또는 en만 지원됩니다")
    private String languagePreferenceSecondary = "ko";

    // 보호자 정보 (BIF 사용자를 위한 안전망)
    @Size(max = 100, message = "보호자 이름은 100글자를 초과할 수 없습니다")
    private String guardianName;
    
    @Pattern(regexp = "^[0-9-]*$", message = "전화번호는 숫자와 하이픈(-)만 사용할 수 있습니다")
    @Size(max = 20, message = "전화번호는 20글자를 초과할 수 없습니다")
    private String guardianPhone;
    
    @Email(message = "올바른 보호자 이메일 형식을 입력해주세요")
    @Size(max = 100, message = "보호자 이메일은 100글자를 초과할 수 없습니다")
    private String guardianEmail;
    
    // 약관 동의
    @AssertTrue(message = "이용약관에 동의해주세요")
    private Boolean agreeToTerms;
    
    @AssertTrue(message = "개인정보 처리방침에 동의해주세요")
    private Boolean agreeToPrivacyPolicy;
    
    // 마케팅 동의 (선택)
    private Boolean agreeToMarketing = false;
    
    /**
     * 비밀번호 일치 확인
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
} 