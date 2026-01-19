package com.bifai.reminder.bifai_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

  @NotBlank(message = "현재 비밀번호를 입력해주세요")
  private String currentPassword;

  @NotBlank(message = "새 비밀번호를 입력해주세요")
  @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
  @Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]+$",
    message = "비밀번호는 영문자와 숫자를 포함해야 합니다"
  )
  private String newPassword;
}
