package com.bifai.reminder.bifai_backend.controller;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 실험 수정 요청 DTO
 */
@Data
public class ExperimentUpdateRequest {
  
  private String name;
  
  private String description;
  
  private Map<String, Object> targetCriteria;
  
  private Map<String, Integer> trafficAllocation;
  
  private Integer sampleSizeTarget;
  
  private LocalDateTime endDate;
  
  private Map<String, Object> metadata;
}