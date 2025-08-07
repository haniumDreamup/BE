package com.bifai.reminder.bifai_backend.dto.ai;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상황 분석 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SituationAnalysisResponse {
  
  private String situationType;
  
  private String currentSituation; // 현재 상황 요약 (1문장)
  
  private List<String> actionSteps; // 해야 할 일 목록
  
  private String caution; // 주의사항
  
  private String fullResponse; // 전체 AI 응답
  
  private LocalDateTime analyzedAt;
  
  private Long responseTimeMs; // 응답 시간 (밀리초)
  
  private Integer tokensUsed; // 사용된 토큰 수
}