package com.bifai.reminder.bifai_backend.dto.guardian;

import com.bifai.reminder.bifai_backend.entity.GuardianRelationship.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * 보호자 초대 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianInvitationRequest {
  
  @NotNull(message = "사용자 ID는 필수입니다")
  private Long userId;
  
  @NotBlank(message = "보호자 이름은 필수입니다")
  @Size(max = 100)
  private String guardianName;
  
  @NotBlank(message = "보호자 이메일은 필수입니다")
  @Email(message = "유효한 이메일 형식이어야 합니다")
  private String guardianEmail;
  
  @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다")
  private String guardianPhone;
  
  @NotNull(message = "관계 유형은 필수입니다")
  private RelationshipType relationshipType;
  
  @NotNull(message = "권한 레벨은 필수입니다")
  private PermissionLevel permissionLevel;
  
  private Map<String, Boolean> permissionSettings;
  
  @Min(1)
  @Max(10)
  private Integer emergencyPriority;
  
  @Size(max = 500)
  private String notes;
}