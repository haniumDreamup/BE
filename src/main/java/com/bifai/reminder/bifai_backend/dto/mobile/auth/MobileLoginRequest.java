package com.bifai.reminder.bifai_backend.dto.mobile.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileLoginRequest {
  
  @NotBlank(message = "이메일을 입력해주세요")
  private String username;
  
  @NotBlank(message = "비밀번호를 입력해주세요")
  private String password;
  
  @NotBlank(message = "디바이스 ID가 필요해요")
  private String deviceId;
  
  @NotBlank(message = "디바이스 타입이 필요해요")
  private String deviceType; // ios, android
}