package com.bifai.reminder.bifai_backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 변형 설정 요청 DTO
 */
@Data
public class VariantConfigRequest {
  
  @NotEmpty(message = "변형 목록은 필수입니다")
  @Valid
  private List<VariantConfig> variants;
  
  /**
   * 변형 설정
   */
  @Data
  public static class VariantConfig {
    private String variantKey;
    private String variantName;
    private Boolean isControl;
    private Map<String, Object> config;
    private Map<String, Object> featureFlags;
    private String description;
  }
}