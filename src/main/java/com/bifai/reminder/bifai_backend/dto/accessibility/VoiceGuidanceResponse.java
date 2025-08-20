package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

/**
 * 음성 안내 응답 DTO
 */
@Data
@Builder
public class VoiceGuidanceResponse {
  
  private String text;
  
  private String context;
  
  private String language;
  
  private Float voiceSpeed;
  
  private Float voicePitch;
}