package com.bifai.reminder.bifai_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * 날씨 정보 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherInfoRequest {
  
  @NotBlank(message = "기온은 필수입니다")
  private String temperature;
  
  @NotBlank(message = "날씨 상태는 필수입니다")
  private String weather;
  
  private String rainProbability;
  
  private String fineDust;
}