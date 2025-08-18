package com.bifai.reminder.bifai_backend.dto.image;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 이미지 업로드 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadRequest {

  @NotNull(message = "분석 타입은 필수입니다")
  private String analysisType; // PERIODIC, ON_DEMAND, EMERGENCY, NAVIGATION, MEDICATION, TEXT_READING

  private Double latitude;
  
  private Double longitude;
  
  private String address;

  private String context; // 추가 컨텍스트 정보

  @Builder.Default
  private Boolean requiresVoiceGuidance = true; // 음성 안내 필요 여부

  @Builder.Default
  private Boolean urgent = false; // 긴급 처리 여부

  private String userQuestion; // 사용자가 물어본 질문 (예: "이게 무엇인가요?")
}