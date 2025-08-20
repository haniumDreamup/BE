package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 빠른 실행 항목 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickActionDto {
  
  private String id;
  
  private String label;
  
  private String icon;
  
  private String type; // primary, success, warning, danger
  
  private String action;
  
  public QuickActionDto(String id, String label, String icon, String type) {
    this.id = id;
    this.label = label;
    this.icon = icon;
    this.type = type;
    this.action = "quick_action_" + id;
  }
}