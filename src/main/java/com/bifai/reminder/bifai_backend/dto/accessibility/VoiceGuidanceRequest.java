package com.bifai.reminder.bifai_backend.dto.accessibility;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 음성 안내 생성 요청 DTO
 */
@Data
public class VoiceGuidanceRequest {
  
  @NotBlank(message = "컨텍스트는 필수입니다")
  private String context;
  
  private Map<String, Object> params = new HashMap<>();
  
  private String language = "ko-KR";
}