package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 간소화된 네비게이션 DTO
 */
@Data
@Builder
public class SimplifiedNavigationDto {
  
  private Boolean simplified;
  
  private Integer maxDepth;
  
  private List<NavigationItemDto> mainMenuItems;
  
  private List<QuickActionDto> quickActions;
  
  private Boolean breadcrumbsEnabled;
  
  private String navigationStyle; // bottom-nav, side-drawer, tab-bar
}