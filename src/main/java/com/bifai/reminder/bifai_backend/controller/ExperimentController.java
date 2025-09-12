package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.entity.Experiment;
import com.bifai.reminder.bifai_backend.entity.TestGroupAssignment;
import com.bifai.reminder.bifai_backend.service.ABTestService;
import com.bifai.reminder.bifai_backend.service.ExperimentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A/B 테스트 실험 관리 API
 */
@RestController
@RequestMapping("/api/experiments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Experiment", description = "A/B 테스트 실험 관리 API")
public class ExperimentController {
  
  private final ABTestService abTestService;
  private final ExperimentManagementService experimentManagementService;
  
  /**
   * 실험 생성
   */
  @PostMapping
  @Operation(summary = "실험 생성", description = "새로운 A/B 테스트 실험을 생성합니다")
  public ResponseEntity<ApiResponse<Experiment>> createExperiment(
      @Valid @RequestBody ExperimentCreateRequest request) {
    
    log.info("실험 생성 요청: {}", request.getName());
    
    Experiment experiment = experimentManagementService.createExperiment(
      request.getName(),
      request.getDescription(),
      request.getExperimentKey(),
      request.getExperimentType(),
      request.getTargetCriteria(),
      request.getSampleSizeTarget(),
      request.getStartDate(),
      request.getEndDate()
    );
    
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(ApiResponse.success(experiment, "실험이 생성되었습니다"));
  }
  
  /**
   * 실험 목록 조회
   */
  @GetMapping
  @Operation(summary = "실험 목록 조회", description = "모든 실험 목록을 페이징하여 조회합니다")
  public ResponseEntity<ApiResponse<Page<Experiment>>> getExperiments(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String type,
      Pageable pageable) {
    
    Page<Experiment> experiments = experimentManagementService.getExperiments(
      status, type, pageable);
    
    return ResponseEntity.ok(ApiResponse.success(experiments));
  }
  
  /**
   * 실험 상세 조회
   */
  @GetMapping("/{experimentKey}")
  @Operation(summary = "실험 상세 조회", description = "특정 실험의 상세 정보를 조회합니다")
  public ResponseEntity<ApiResponse<Experiment>> getExperiment(
      @PathVariable String experimentKey) {
    
    Experiment experiment = experimentManagementService.getExperiment(experimentKey);
    return ResponseEntity.ok(ApiResponse.success(experiment));
  }
  
  /**
   * 실험 수정
   */
  @PutMapping("/{experimentKey}")
  @Operation(summary = "실험 수정", description = "실험 설정을 수정합니다")
  public ResponseEntity<ApiResponse<Experiment>> updateExperiment(
      @PathVariable String experimentKey,
      @Valid @RequestBody ExperimentUpdateRequest request) {
    
    log.info("실험 수정 요청: {}", experimentKey);
    
    Experiment experiment = experimentManagementService.updateExperiment(
      experimentKey, request);
    
    return ResponseEntity.ok(ApiResponse.success(experiment, "실험이 수정되었습니다"));
  }
  
  /**
   * 실험 시작
   */
  @PostMapping("/{experimentKey}/start")
  @Operation(summary = "실험 시작", description = "예정된 실험을 시작합니다")
  public ResponseEntity<ApiResponse<Experiment>> startExperiment(
      @PathVariable String experimentKey) {
    
    log.info("실험 시작: {}", experimentKey);
    
    Experiment experiment = experimentManagementService.startExperiment(experimentKey);
    return ResponseEntity.ok(ApiResponse.success(experiment, "실험이 시작되었습니다"));
  }
  
  /**
   * 실험 일시 중지
   */
  @PostMapping("/{experimentKey}/pause")
  @Operation(summary = "실험 일시 중지", description = "진행 중인 실험을 일시 중지합니다")
  public ResponseEntity<ApiResponse<Experiment>> pauseExperiment(
      @PathVariable String experimentKey) {
    
    log.info("실험 일시 중지: {}", experimentKey);
    
    Experiment experiment = experimentManagementService.pauseExperiment(experimentKey);
    return ResponseEntity.ok(ApiResponse.success(experiment, "실험이 일시 중지되었습니다"));
  }
  
  /**
   * 실험 재개
   */
  @PostMapping("/{experimentKey}/resume")
  @Operation(summary = "실험 재개", description = "일시 중지된 실험을 재개합니다")
  public ResponseEntity<ApiResponse<Experiment>> resumeExperiment(
      @PathVariable String experimentKey) {
    
    log.info("실험 재개: {}", experimentKey);
    
    Experiment experiment = experimentManagementService.resumeExperiment(experimentKey);
    return ResponseEntity.ok(ApiResponse.success(experiment, "실험이 재개되었습니다"));
  }
  
  /**
   * 실험 종료
   */
  @PostMapping("/{experimentKey}/complete")
  @Operation(summary = "실험 종료", description = "진행 중인 실험을 종료합니다")
  public ResponseEntity<ApiResponse<Experiment>> completeExperiment(
      @PathVariable String experimentKey,
      @RequestParam(required = false) String winnerId) {
    
    log.info("실험 종료: {}, 승자: {}", experimentKey, winnerId);
    
    Experiment experiment = experimentManagementService.completeExperiment(
      experimentKey, winnerId);
    
    return ResponseEntity.ok(ApiResponse.success(experiment, "실험이 종료되었습니다"));
  }
  
  /**
   * 사용자를 실험에 할당
   */
  @PostMapping("/{experimentKey}/assign")
  @Operation(summary = "사용자 실험 할당", description = "현재 사용자를 실험 그룹에 할당합니다")
  public ResponseEntity<ApiResponse<TestGroupAssignment>> assignUser(
      @PathVariable String experimentKey,
      @AuthenticationPrincipal Long userId) {
    
    log.info("사용자 {} 실험 {} 할당", userId, experimentKey);
    
    TestGroupAssignment assignment = abTestService.assignUserToExperiment(
      userId, experimentKey);
    
    if (assignment == null) {
      return ResponseEntity.ok(ApiResponse.success(null, "실험 대상이 아닙니다"));
    }
    
    return ResponseEntity.ok(ApiResponse.success(assignment, "실험 그룹에 할당되었습니다"));
  }
  
  /**
   * 전환 기록
   */
  @PostMapping("/{experimentKey}/convert")
  @Operation(summary = "전환 기록", description = "실험 전환(목표 달성)을 기록합니다")
  public ResponseEntity<ApiResponse<Void>> recordConversion(
      @PathVariable String experimentKey,
      @AuthenticationPrincipal Long userId,
      @RequestParam(required = false) Double value) {
    
    log.info("사용자 {} 실험 {} 전환 기록: {}", userId, experimentKey, value);
    
    abTestService.recordConversion(userId, experimentKey, value);
    
    return ResponseEntity.ok(ApiResponse.success(null, "전환이 기록되었습니다"));
  }
  
  /**
   * 실험 분석 결과 조회
   */
  @GetMapping("/{experimentKey}/analysis")
  @Operation(summary = "실험 분석", description = "실험 결과를 분석하여 조회합니다")
  public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeExperiment(
      @PathVariable String experimentKey) {
    
    log.info("실험 분석: {}", experimentKey);
    
    Map<String, Object> analysis = abTestService.analyzeExperiment(experimentKey);
    
    return ResponseEntity.ok(ApiResponse.success(analysis));
  }
  
  /**
   * 사용자의 실험 참여 목록
   */
  @GetMapping("/my-experiments")
  @Operation(summary = "내 실험 목록", description = "현재 사용자가 참여 중인 실험 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<TestGroupAssignment>>> getMyExperiments(
      @AuthenticationPrincipal Long userId) {
    
    List<TestGroupAssignment> assignments = abTestService.getUserExperiments(userId);
    
    return ResponseEntity.ok(ApiResponse.success(assignments));
  }
  
  /**
   * Feature Flag 조회
   */
  @GetMapping("/feature-flags/{flagKey}")
  @Operation(summary = "Feature Flag 조회", description = "사용자의 Feature Flag 값을 조회합니다")
  public ResponseEntity<ApiResponse<Object>> getFeatureFlag(
      @PathVariable String flagKey,
      @AuthenticationPrincipal Long userId) {
    
    Object flagValue = abTestService.getFeatureFlag(userId, flagKey);
    
    return ResponseEntity.ok(ApiResponse.success(flagValue));
  }
  
  /**
   * 실험에서 제외
   */
  @PostMapping("/{experimentKey}/opt-out")
  @Operation(summary = "실험 제외", description = "현재 사용자를 실험에서 제외합니다")
  public ResponseEntity<ApiResponse<Void>> optOut(
      @PathVariable String experimentKey,
      @AuthenticationPrincipal Long userId) {
    
    log.info("사용자 {} 실험 {} 제외", userId, experimentKey);
    
    abTestService.optOutUser(userId, experimentKey);
    
    return ResponseEntity.ok(ApiResponse.success(null, "실험에서 제외되었습니다"));
  }
  
  /**
   * 테스트 그룹 설정
   */
  @PostMapping("/{experimentKey}/groups")
  @Operation(summary = "테스트 그룹 설정", description = "실험의 테스트 그룹을 설정합니다")
  public ResponseEntity<ApiResponse<Experiment>> configureGroups(
      @PathVariable String experimentKey,
      @Valid @RequestBody GroupConfigRequest request) {
    
    log.info("실험 {} 그룹 설정", experimentKey);
    
    Experiment experiment = experimentManagementService.configureTestGroups(
      experimentKey, request.getGroups());
    
    return ResponseEntity.ok(ApiResponse.success(experiment, "테스트 그룹이 설정되었습니다"));
  }
  
  /**
   * 변형(Variant) 설정
   */
  @PostMapping("/{experimentKey}/variants")
  @Operation(summary = "변형 설정", description = "실험의 변형을 설정합니다")
  public ResponseEntity<ApiResponse<Experiment>> configureVariants(
      @PathVariable String experimentKey,
      @Valid @RequestBody VariantConfigRequest request) {
    
    log.info("실험 {} 변형 설정", experimentKey);
    
    Experiment experiment = experimentManagementService.configureVariants(
      experimentKey, request.getVariants());
    
    return ResponseEntity.ok(ApiResponse.success(experiment, "변형이 설정되었습니다"));
  }
}