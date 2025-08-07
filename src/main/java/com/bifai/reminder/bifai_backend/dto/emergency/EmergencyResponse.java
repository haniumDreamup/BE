package com.bifai.reminder.bifai_backend.dto.emergency;

import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 긴급 상황 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyResponse {

  private Long id;
  private Long userId;
  private String userName;
  private EmergencyType type;
  private String typeDescription;
  private EmergencyStatus status;
  private String statusDescription;
  private Double latitude;
  private Double longitude;
  private String address;
  private String description;
  private EmergencySeverity severity;
  private String severityDescription;
  private TriggerSource triggeredBy;
  private Double fallConfidence;
  private String imageUrl;
  private LocalDateTime createdAt;
  private LocalDateTime resolvedAt;
  private String resolvedBy;
  private String resolutionNotes;
  private Integer responseTimeSeconds;
  private String[] notifiedGuardians;
  private boolean notificationSent;
  private Integer responderCount;

  /**
   * Entity를 Response DTO로 변환
   */
  public static EmergencyResponse from(Emergency emergency) {
    return EmergencyResponse.builder()
        .id(emergency.getId())
        .userId(emergency.getUser().getId())
        .userName(emergency.getUser().getFullName())
        .type(emergency.getType())
        .typeDescription(emergency.getType().getDescription())
        .status(emergency.getStatus())
        .statusDescription(emergency.getStatus().getDescription())
        .latitude(emergency.getLatitude())
        .longitude(emergency.getLongitude())
        .address(emergency.getAddress())
        .description(emergency.getDescription())
        .severity(emergency.getSeverity())
        .severityDescription(emergency.getSeverity() != null ? emergency.getSeverity().getDescription() : null)
        .triggeredBy(emergency.getTriggeredBy())
        .fallConfidence(emergency.getFallConfidence())
        .imageUrl(emergency.getImageUrl())
        .createdAt(emergency.getCreatedAt())
        .resolvedAt(emergency.getResolvedAt())
        .resolvedBy(emergency.getResolvedBy())
        .resolutionNotes(emergency.getResolutionNotes())
        .responseTimeSeconds(emergency.getResponseTimeSeconds())
        .notifiedGuardians(emergency.getNotifiedGuardians() != null ? 
            emergency.getNotifiedGuardians().split(",") : new String[0])
        .notificationSent(emergency.isNotificationSent())
        .responderCount(emergency.getResponderCount())
        .build();
  }
}