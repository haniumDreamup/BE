package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 네비게이션 항목 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {
  
  private String id;
  
  private String label;
  
  private String icon;
  
  private Integer order;
  
  private String path;
  
  private Boolean visible = true;
  
  public NavigationItemDto(String id, String label, String icon, Integer order) {
    this.id = id;
    this.label = label;
    this.icon = icon;
    this.order = order;
    this.path = "/" + id;
    this.visible = true;
  }
}