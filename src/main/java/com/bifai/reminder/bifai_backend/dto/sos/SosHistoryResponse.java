package com.bifai.reminder.bifai_backend.dto.sos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SOS 이력 응답 DTO
 * 김영한 방식: 엔티티 직접 반환 대신 필요한 데이터만 포함한 DTO 사용
 * LazyInitializationException 방지를 위해 엔티티의 연관관계 필드는 제외
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosHistoryResponse {

  private Long emergencyId;

  private String emergencyType;

  private String status;

  private Double latitude;

  private Double longitude;

  private String address;

  private String description;

  private String severity;

  private String triggeredBy;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime triggeredAt;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime resolvedAt;

  private String resolvedBy;

  private String resolutionNotes;

  private Integer responseTimeSeconds;

  private boolean notificationSent;

  private Integer notificationCount;

  /**
   * Emergency 엔티티를 DTO로 변환하는 정적 팩토리 메서드
   * 김영한 방식: 엔티티 -> DTO 변환 로직을 DTO 내부에 캡슐화
   */
  public static SosHistoryResponse from(Emergency emergency) {
    return SosHistoryResponse.builder()
        .emergencyId(emergency.getId())
        .emergencyType(emergency.getType() != null ? emergency.getType().name() : null)
        .status(emergency.getStatus() != null ? emergency.getStatus().name() : null)
        .latitude(emergency.getLatitude())
        .longitude(emergency.getLongitude())
        .address(emergency.getAddress())
        .description(emergency.getDescription())
        .severity(emergency.getSeverity() != null ? emergency.getSeverity().name() : null)
        .triggeredBy(emergency.getTriggeredBy() != null ? emergency.getTriggeredBy().name() : null)
        .triggeredAt(emergency.getTriggeredAt() != null ? emergency.getTriggeredAt() : emergency.getCreatedAt())
        .resolvedAt(emergency.getResolvedAt())
        .resolvedBy(emergency.getResolvedBy())
        .resolutionNotes(emergency.getResolutionNotes())
        .responseTimeSeconds(emergency.getResponseTimeSeconds())
        .notificationSent(emergency.isNotificationSent())
        .notificationCount(emergency.getNotificationCount())
        .build();
  }
}