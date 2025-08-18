package com.bifai.reminder.bifai_backend.dto.mobile;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 모바일 로그인 응답 DTO
 * 
 * JWT 토큰과 사용자 기본 정보를 포함한 로그인 응답
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "모바일 로그인 응답")
public class MobileLoginResponse {
  
  @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIs...")
  private String accessToken;
  
  @Schema(description = "JWT 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIs...")
  private String refreshToken;
  
  @Schema(description = "토큰 만료 시간 (초)", example = "3600")
  private Long expiresIn;
  
  @Schema(description = "사용자 정보")
  private UserInfo user;
  
  /**
   * 사용자 기본 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "사용자 기본 정보")
  public static class UserInfo {
    
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
    
    @Schema(description = "프로필 이미지 URL", example = "https://cdn.bifai.com/profiles/user1.jpg")
    private String profileImage;
    
    @Schema(description = "인지 수준", example = "MODERATE", allowableValues = {"MILD", "MODERATE", "SEVERE", "UNKNOWN"})
    private String cognitiveLevel;
    
    @Schema(description = "선호 언어", example = "ko")
    private String language;
    
    @Schema(description = "응급 모드 활성화 여부", example = "false")
    private Boolean emergencyMode;
    
    @Schema(description = "주 보호자 연락처", example = "010-****-5678")
    private String guardianContact;
  }
}