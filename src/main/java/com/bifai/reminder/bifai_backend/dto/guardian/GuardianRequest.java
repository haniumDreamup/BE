package com.bifai.reminder.bifai_backend.dto.guardian;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 보호자 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianRequest {
    
    @NotBlank(message = "보호자 이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식을 입력해주세요")
    private String guardianEmail;
    
    @NotBlank(message = "보호자 이름을 입력해주세요")
    private String guardianName;
    
    @NotBlank(message = "관계를 입력해주세요")
    @Pattern(regexp = "^(부모|자녀|배우자|형제자매|친척|기타)$", 
             message = "관계는 '부모', '자녀', '배우자', '형제자매', '친척', '기타' 중 하나로 입력해주세요")
    private String relationship;
    
    @NotBlank(message = "보호자 전화번호를 입력해주세요")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
    private String primaryPhone;
    
    private String secondaryPhone;
    
    private String message;  // 보호자에게 전달할 메시지
    
    @Builder.Default
    private Boolean canViewLocation = true;
    
    @Builder.Default
    private Boolean canModifySettings = false;
    
    @Builder.Default
    private Boolean canReceiveAlerts = true;
}