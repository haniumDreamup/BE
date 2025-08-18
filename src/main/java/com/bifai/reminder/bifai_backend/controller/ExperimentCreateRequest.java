package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 실험 생성 요청 DTO
 */
@Data
public class ExperimentCreateRequest {
  
  @NotBlank(message = "실험명은 필수입니다")
  private String name;
  
  @NotBlank(message = "실험 설명은 필수입니다")
  private String description;
  
  @NotBlank(message = "실험 키는 필수입니다")
  private String experimentKey;
  
  @NotNull(message = "실험 타입은 필수입니다")
  private ExperimentType experimentType;
  
  private Map<String, Object> targetCriteria;
  
  @Positive(message = "목표 샘플 크기는 양수여야 합니다")
  private Integer sampleSizeTarget;
  
  @NotNull(message = "시작일은 필수입니다")
  private LocalDateTime startDate;
  
  @NotNull(message = "종료일은 필수입니다")
  @Future(message = "종료일은 미래여야 합니다")
  private LocalDateTime endDate;
}