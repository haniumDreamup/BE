package com.bifai.reminder.bifai_backend.dto.sos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * SOS 요청 DTO
 * 긴급 도움 요청 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosRequest {

  @NotNull(message = "위도는 필수입니다")
  private Double latitude;

  @NotNull(message = "경도는 필수입니다")
  private Double longitude;

  private String address; // 현재 주소

  private String message; // 긴급 메시지

  private String emergencyType; // FALL, PANIC, LOST, MEDICAL, OTHER

  @Builder.Default
  private Boolean notifyAllContacts = true; // 모든 긴급 연락처에 알림

  @Builder.Default
  private Boolean requestAmbulance = false; // 구급차 요청 여부

  @Builder.Default
  private Boolean shareLocation = true; // 위치 공유 여부

  private String additionalInfo; // 추가 정보
}