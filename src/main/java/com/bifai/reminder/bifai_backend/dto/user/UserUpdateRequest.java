package com.bifai.reminder.bifai_backend.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 사용자 정보 수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
    private String name;
    
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요")
    private String nickname;
    
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (010-0000-0000)")
    private String phoneNumber;
    
    @Pattern(regexp = "^(남성|여성|기타)$", message = "성별은 '남성', '여성', '기타' 중 하나로 입력해주세요")
    private String gender;
    
    @Size(max = 500, message = "주소는 500자 이하로 입력해주세요")
    private String address;
    
    @Pattern(regexp = "^(ko|en)$", message = "언어 설정은 'ko' 또는 'en'으로 입력해주세요")
    private String languagePreference;
    
    private String profileImageUrl;
}