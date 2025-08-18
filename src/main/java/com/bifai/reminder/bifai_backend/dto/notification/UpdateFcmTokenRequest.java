package com.bifai.reminder.bifai_backend.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFcmTokenRequest {
  
  @NotBlank(message = "디바이스 ID는 필수입니다")
  private String deviceId;
  
  @NotBlank(message = "FCM 토큰은 필수입니다")
  private String fcmToken;
}