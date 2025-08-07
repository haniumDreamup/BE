package com.bifai.reminder.bifai_backend.dto.pose;

import com.bifai.reminder.bifai_backend.entity.FallEvent;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 낙상 상태 조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallStatusDto {
  
  private Long userId;
  private LocalDateTime lastChecked;
  private List<FallEventDto> recentFallEvents;
  private Boolean isMonitoring;
  private Boolean sessionActive;
  private String currentSessionId;
  
  /**
   * 낙상 이벤트 간단 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FallEventDto {
    private Long eventId;
    private LocalDateTime detectedAt;
    private FallEvent.FallSeverity severity;
    private Float confidenceScore;
    private FallEvent.EventStatus status;
    private Boolean falsePositive;
  }
}