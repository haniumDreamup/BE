package com.bifai.reminder.bifai_backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 테스트 그룹 설정 요청 DTO
 */
@Data
public class GroupConfigRequest {
  
  @NotEmpty(message = "그룹 목록은 필수입니다")
  @Valid
  private List<TestGroupConfig> groups;
  
  /**
   * 테스트 그룹 설정
   */
  @Data
  public static class TestGroupConfig {
    private String groupName;
    private String groupType;
    private Boolean isControl;
    private Integer trafficAllocation;
    private Integer minSampleSize;
    private Integer maxSampleSize;
    private String description;
  }
}