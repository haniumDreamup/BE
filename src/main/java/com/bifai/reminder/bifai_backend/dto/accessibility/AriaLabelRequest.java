package com.bifai.reminder.bifai_backend.dto.accessibility;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * ARIA 라벨 생성 요청 DTO
 */
@Data
public class AriaLabelRequest {
  
  @NotBlank(message = "요소 타입은 필수입니다")
  private String elementType;
  
  private String elementName;
  
  private Map<String, Object> attributes = new HashMap<>();
}