package com.bifai.reminder.bifai_backend.dto.sos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOS 응답 DTO
 * 긴급 도움 요청 처리 결과
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosResponse {

  private Long emergencyId; // 긴급 상황 ID

  private String status; // TRIGGERED, NOTIFIED, RESPONDED

  private String message; // 사용자에게 표시할 메시지

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime triggeredAt; // SOS 발동 시간

  private Integer notifiedContacts; // 알림 받은 연락처 수

  private List<String> notifiedContactNames; // 알림 받은 사람 이름

  private Boolean locationShared; // 위치 공유 여부

  private String estimatedResponseTime; // 예상 도착 시간

  private String emergencyNumber; // 긴급 연락처 (112, 119 등)

  private String simpleInstruction; // 간단한 행동 지침

  @Builder.Default
  private Boolean success = true;
}