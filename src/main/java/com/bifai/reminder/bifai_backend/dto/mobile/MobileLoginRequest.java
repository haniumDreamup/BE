package com.bifai.reminder.bifai_backend.dto.mobile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 모바일 로그인 요청 DTO
 * 
 * 디바이스 정보를 포함한 로그인 요청 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "모바일 로그인 요청")
public class MobileLoginRequest {
  
  @NotBlank(message = "아이디를 입력해주세요")
  @Size(min = 3, max = 50, message = "아이디는 3자 이상 50자 이하여야 해요")
  @Schema(description = "사용자명 또는 이메일", example = "user@example.com")
  private String username;
  
  @NotBlank(message = "비밀번호를 입력해주세요")
  @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 해요")
  @Schema(description = "비밀번호", example = "password123")
  private String password;
  
  @NotBlank(message = "디바이스 정보가 필요해요")
  @Schema(description = "디바이스 고유 ID", example = "device-uuid-123")
  private String deviceId;
  
  @NotBlank(message = "디바이스 종류를 알려주세요")
  @Pattern(regexp = "ios|android", message = "ios 또는 android만 가능해요")
  @Schema(description = "디바이스 종류", example = "ios", allowableValues = {"ios", "android"})
  private String deviceType;
  
  @Schema(description = "디바이스 모델명", example = "iPhone 14")
  private String deviceModel;
  
  @Schema(description = "OS 버전", example = "16.0")
  private String osVersion;
  
  @Schema(description = "앱 버전", example = "1.0.0")
  private String appVersion;
  
  /**
   * FCM 푸시 토큰 (선택사항)
   */
  @Schema(description = "FCM 푸시 알림 토큰", required = false)
  private String pushToken;
}