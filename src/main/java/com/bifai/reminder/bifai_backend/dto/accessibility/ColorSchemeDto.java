package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

/**
 * 색상 스키마 DTO
 */
@Data
@Builder
public class ColorSchemeDto {
  
  private String id;
  
  private String name;
  
  private String description;
  
  private String primaryColor;
  
  private String secondaryColor;
  
  private String backgroundColor;
  
  private String textColor;
  
  private String errorColor;
  
  private String successColor;
  
  private String warningColor;
  
  private Double contrastRatio;
  
  private Boolean wcagAACompliant;
  
  private Boolean wcagAAACompliant;
}