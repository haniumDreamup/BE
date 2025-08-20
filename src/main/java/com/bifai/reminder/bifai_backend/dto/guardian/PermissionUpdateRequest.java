package com.bifai.reminder.bifai_backend.dto.guardian;

import com.bifai.reminder.bifai_backend.entity.GuardianRelationship.PermissionLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * 권한 업데이트 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionUpdateRequest {
  
  private PermissionLevel permissionLevel;
  
  private Map<String, Boolean> permissionSettings;
  
  private Integer emergencyPriority;
  
  private Map<String, Boolean> notificationPreferences;
}