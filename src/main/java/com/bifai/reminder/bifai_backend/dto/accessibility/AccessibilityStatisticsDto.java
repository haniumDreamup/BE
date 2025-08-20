package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Data;

import java.util.Map;

/**
 * 접근성 통계 DTO
 */
@Data
public class AccessibilityStatisticsDto {
  
  private Long totalUsers;
  
  private Map<String, Long> readingLevelDistribution;
  
  private Map<String, Long> colorSchemeDistribution;
  
  private Double voiceGuidanceUsageRate;
  
  private Double simplifiedUiUsageRate;
  
  private Double highContrastUsageRate;
  
  private Map<String, Long> profileTypeDistribution;
}