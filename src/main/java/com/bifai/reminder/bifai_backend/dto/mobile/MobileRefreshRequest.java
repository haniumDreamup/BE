package com.bifai.reminder.bifai_backend.dto.mobile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 모바일 토큰 갱신 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 갱신 요청")
public class MobileRefreshRequest {
  
  @NotBlank(message = "리프레시 토큰이 필요해요")
  @Schema(description = "JWT 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIs...")
  private String refreshToken;
  
  @Schema(description = "디바이스 ID", example = "device-uuid-123")
  private String deviceId;
}