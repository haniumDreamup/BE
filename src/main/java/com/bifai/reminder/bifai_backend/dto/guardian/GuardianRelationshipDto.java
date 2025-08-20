package com.bifai.reminder.bifai_backend.dto.guardian;

import com.bifai.reminder.bifai_backend.entity.GuardianRelationship.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 보호자 관계 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRelationshipDto {
  
  private Long relationshipId;
  
  private Long guardianId;
  private String guardianName;
  private String guardianEmail;
  
  private Long userId;
  private String userName;
  
  private RelationshipType relationshipType;
  private PermissionLevel permissionLevel;
  private RelationshipStatus status;
  
  private Map<String, Boolean> permissionSettings;
  
  private Integer emergencyPriority;
  private String notes;
  
  private LocalDateTime lastActiveAt;
  private LocalDateTime createdAt;
  private LocalDateTime approvedAt;
}