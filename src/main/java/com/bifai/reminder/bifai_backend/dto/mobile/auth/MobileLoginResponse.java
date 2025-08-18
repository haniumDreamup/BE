package com.bifai.reminder.bifai_backend.dto.mobile.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileLoginResponse {
  private String accessToken;
  private String refreshToken;
  private MobileUserInfo user;
  private Long expiresIn;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MobileUserInfo {
    private Long id;
    private String name;
    private String profileImage;
    private String cognitiveLevel;
  }
}