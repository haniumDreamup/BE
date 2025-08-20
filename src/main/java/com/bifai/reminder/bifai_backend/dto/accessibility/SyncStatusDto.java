package com.bifai.reminder.bifai_backend.dto.accessibility;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 동기화 상태 DTO
 */
@Data
@Builder
public class SyncStatusDto {
  
  private Long userId;
  
  private LocalDateTime syncedAt;
  
  private Boolean success;
  
  private Integer syncedDevices;
  
  private String message;
}