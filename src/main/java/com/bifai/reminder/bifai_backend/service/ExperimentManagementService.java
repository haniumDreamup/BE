package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.controller.ExperimentUpdateRequest;
import com.bifai.reminder.bifai_backend.controller.GroupConfigRequest;
import com.bifai.reminder.bifai_backend.controller.VariantConfigRequest;
import com.bifai.reminder.bifai_backend.entity.Experiment;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentStatus;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentType;
import com.bifai.reminder.bifai_backend.entity.TestGroup;
import com.bifai.reminder.bifai_backend.entity.TestGroup.GroupType;
import com.bifai.reminder.bifai_backend.entity.TestVariant;
import com.bifai.reminder.bifai_backend.repository.ExperimentRepository;
import com.bifai.reminder.bifai_backend.repository.TestGroupRepository;
import com.bifai.reminder.bifai_backend.repository.TestVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 실험 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentManagementService {
  
  private final ExperimentRepository experimentRepository;
  private final TestGroupRepository testGroupRepository;
  private final TestVariantRepository testVariantRepository;
  
  /**
   * 실험 생성
   */
  @Transactional
  public Experiment createExperiment(
      String name,
      String description,
      String experimentKey,
      ExperimentType experimentType,
      Map<String, Object> targetCriteria,
      Integer sampleSizeTarget,
      LocalDateTime startDate,
      LocalDateTime endDate) {
    
    // 실험 키 중복 확인
    if (experimentRepository.findByExperimentKey(experimentKey).isPresent()) {
      throw new IllegalArgumentException("이미 존재하는 실험 키입니다: " + experimentKey);
    }
    
    // 실험 생성
    Experiment experiment = Experiment.builder()
      .name(name)
      .description(description)
      .experimentKey(experimentKey)
      .experimentType(experimentType)
      .targetCriteria(targetCriteria != null ? targetCriteria : new HashMap<>())
      .sampleSizeTarget(sampleSizeTarget)
      .startDate(startDate)
      .endDate(endDate)
      .status(ExperimentStatus.DRAFT)
      .isActive(false)
      .trafficAllocation(new HashMap<>())
      .metadata(new HashMap<>())
      .createdBy("system") // 실제로는 현재 사용자 ID
      .build();
    
    experiment = experimentRepository.save(experiment);
    
    // 기본 그룹 생성 (대조군, 실험군)
    createDefaultGroups(experiment);
    
    // 기본 변형 생성
    createDefaultVariants(experiment);
    
    log.info("실험 생성 완료: {}", experimentKey);
    
    return experiment;
  }
  
  /**
   * 기본 그룹 생성
   */
  private void createDefaultGroups(Experiment experiment) {
    // 대조군
    TestGroup controlGroup = TestGroup.builder()
      .experiment(experiment)
      .groupName("control")
      .groupType(GroupType.CONTROL)
      .isControl(true)
      .isActive(true)
      .currentSize(0)
      .description("대조군")
      .build();
    
    testGroupRepository.save(controlGroup);
    
    // 실험군
    TestGroup treatmentGroup = TestGroup.builder()
      .experiment(experiment)
      .groupName("treatment")
      .groupType(GroupType.TREATMENT)
      .isControl(false)
      .isActive(true)
      .currentSize(0)
      .description("실험군")
      .build();
    
    testGroupRepository.save(treatmentGroup);
    
    // 트래픽 할당 설정 (50:50)
    Map<String, Integer> allocation = new HashMap<>();
    allocation.put("control", 50);
    allocation.put("treatment", 50);
    experiment.setTrafficAllocation(allocation);
    experimentRepository.save(experiment);
  }
  
  /**
   * 기본 변형 생성
   */
  private void createDefaultVariants(Experiment experiment) {
    // 대조군 변형
    TestVariant controlVariant = TestVariant.builder()
      .experiment(experiment)
      .variantKey("control")
      .variantName("대조군")
      .isControl(true)
      .isActive(true)
      .config(new HashMap<>())
      .featureFlags(new HashMap<>())
      .build();
    
    testVariantRepository.save(controlVariant);
    
    // 실험군 변형
    TestVariant treatmentVariant = TestVariant.builder()
      .experiment(experiment)
      .variantKey("treatment")
      .variantName("실험군")
      .isControl(false)
      .isActive(true)
      .config(new HashMap<>())
      .featureFlags(new HashMap<>())
      .build();
    
    testVariantRepository.save(treatmentVariant);
  }
  
  /**
   * 실험 목록 조회
   */
  @Transactional(readOnly = true)
  public Page<Experiment> getExperiments(String status, String type, Pageable pageable) {
    if (status != null && type != null) {
      ExperimentStatus experimentStatus = ExperimentStatus.valueOf(status.toUpperCase());
      ExperimentType experimentType = ExperimentType.valueOf(type.toUpperCase());
      return experimentRepository.findAll(pageable); // 실제로는 상태와 타입으로 필터링 필요
    } else if (status != null) {
      ExperimentStatus experimentStatus = ExperimentStatus.valueOf(status.toUpperCase());
      return experimentRepository.findAll(pageable); // 실제로는 상태로 필터링 필요
    } else if (type != null) {
      ExperimentType experimentType = ExperimentType.valueOf(type.toUpperCase());
      return experimentRepository.findAll(pageable); // 실제로는 타입으로 필터링 필요
    }
    
    return experimentRepository.findAll(pageable);
  }
  
  /**
   * 실험 상세 조회
   */
  @Transactional(readOnly = true)
  public Experiment getExperiment(String experimentKey) {
    return experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
  }
  
  /**
   * 실험 수정
   */
  @Transactional
  public Experiment updateExperiment(String experimentKey, ExperimentUpdateRequest request) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    // DRAFT 또는 SCHEDULED 상태에서만 수정 가능
    if (experiment.getStatus() != ExperimentStatus.DRAFT && 
        experiment.getStatus() != ExperimentStatus.SCHEDULED) {
      throw new IllegalStateException("진행 중이거나 완료된 실험은 수정할 수 없습니다");
    }
    
    if (request.getName() != null) {
      experiment.setName(request.getName());
    }
    
    if (request.getDescription() != null) {
      experiment.setDescription(request.getDescription());
    }
    
    if (request.getTargetCriteria() != null) {
      experiment.setTargetCriteria(request.getTargetCriteria());
    }
    
    if (request.getTrafficAllocation() != null) {
      experiment.setTrafficAllocation(request.getTrafficAllocation());
    }
    
    if (request.getSampleSizeTarget() != null) {
      experiment.setSampleSizeTarget(request.getSampleSizeTarget());
    }
    
    if (request.getEndDate() != null) {
      experiment.setEndDate(request.getEndDate());
    }
    
    if (request.getMetadata() != null) {
      experiment.setMetadata(request.getMetadata());
    }
    
    experiment.setUpdatedAt(LocalDateTime.now());
    
    return experimentRepository.save(experiment);
  }
  
  /**
   * 실험 시작
   */
  @Transactional
  public Experiment startExperiment(String experimentKey) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    if (experiment.getStatus() != ExperimentStatus.DRAFT && 
        experiment.getStatus() != ExperimentStatus.SCHEDULED) {
      throw new IllegalStateException("시작할 수 없는 상태입니다: " + experiment.getStatus());
    }
    
    experiment.setStatus(ExperimentStatus.ACTIVE);
    experiment.setIsActive(true);
    experiment.setActualStartDate(LocalDateTime.now());
    
    log.info("실험 시작: {}", experimentKey);
    
    return experimentRepository.save(experiment);
  }
  
  /**
   * 실험 일시 중지
   */
  @Transactional
  public Experiment pauseExperiment(String experimentKey) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    if (experiment.getStatus() != ExperimentStatus.ACTIVE) {
      throw new IllegalStateException("활성 상태가 아닙니다: " + experiment.getStatus());
    }
    
    experiment.setStatus(ExperimentStatus.PAUSED);
    experiment.setIsActive(false);
    
    log.info("실험 일시 중지: {}", experimentKey);
    
    return experimentRepository.save(experiment);
  }
  
  /**
   * 실험 재개
   */
  @Transactional
  public Experiment resumeExperiment(String experimentKey) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    if (experiment.getStatus() != ExperimentStatus.PAUSED) {
      throw new IllegalStateException("일시 중지 상태가 아닙니다: " + experiment.getStatus());
    }
    
    experiment.setStatus(ExperimentStatus.ACTIVE);
    experiment.setIsActive(true);
    
    log.info("실험 재개: {}", experimentKey);
    
    return experimentRepository.save(experiment);
  }
  
  /**
   * 실험 종료
   */
  @Transactional
  public Experiment completeExperiment(String experimentKey, String winnerId) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    if (experiment.getStatus() != ExperimentStatus.ACTIVE && 
        experiment.getStatus() != ExperimentStatus.PAUSED) {
      throw new IllegalStateException("종료할 수 없는 상태입니다: " + experiment.getStatus());
    }
    
    experiment.setStatus(ExperimentStatus.COMPLETED);
    experiment.setIsActive(false);
    experiment.setActualEndDate(LocalDateTime.now());
    
    // 승자 변형 설정
    if (winnerId != null) {
      TestVariant winner = testVariantRepository.findById(Long.parseLong(winnerId))
        .orElse(null);
      
      if (winner != null && winner.getExperiment().getExperimentId().equals(experiment.getExperimentId())) {
        winner.setIsWinner(true);
        testVariantRepository.save(winner);
        
        // 메타데이터에 승자 정보 저장
        experiment.getMetadata().put("winnerId", winnerId);
        experiment.getMetadata().put("winnerKey", winner.getVariantKey());
      }
    }
    
    log.info("실험 종료: {}, 승자: {}", experimentKey, winnerId);
    
    return experimentRepository.save(experiment);
  }
  
  /**
   * 테스트 그룹 설정
   */
  @Transactional
  public Experiment configureTestGroups(String experimentKey, List<GroupConfigRequest.TestGroupConfig> groupConfigs) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    if (experiment.getStatus() != ExperimentStatus.DRAFT) {
      throw new IllegalStateException("DRAFT 상태에서만 그룹을 설정할 수 있습니다");
    }
    
    // 기존 그룹 삭제
    List<TestGroup> existingGroups = testGroupRepository.findByExperiment(experiment);
    testGroupRepository.deleteAll(existingGroups);
    
    // 새 그룹 생성
    Map<String, Integer> trafficAllocation = new HashMap<>();
    
    for (GroupConfigRequest.TestGroupConfig config : groupConfigs) {
      TestGroup group = TestGroup.builder()
        .experiment(experiment)
        .groupName(config.getGroupName())
        .groupType(GroupType.valueOf(config.getGroupType().toUpperCase()))
        .isControl(config.getIsControl() != null ? config.getIsControl() : false)
        .isActive(true)
        .minSampleSize(config.getMinSampleSize())
        .maxSampleSize(config.getMaxSampleSize())
        .currentSize(0)
        .description(config.getDescription())
        .build();
      
      testGroupRepository.save(group);
      
      if (config.getTrafficAllocation() != null) {
        trafficAllocation.put(config.getGroupName(), config.getTrafficAllocation());
      }
    }
    
    // 트래픽 할당 업데이트
    experiment.setTrafficAllocation(trafficAllocation);
    
    return experimentRepository.save(experiment);
  }
  
  /**
   * 변형 설정
   */
  @Transactional
  public Experiment configureVariants(String experimentKey, List<VariantConfigRequest.VariantConfig> variantConfigs) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    if (experiment.getStatus() != ExperimentStatus.DRAFT) {
      throw new IllegalStateException("DRAFT 상태에서만 변형을 설정할 수 있습니다");
    }
    
    // 기존 변형 삭제
    List<TestVariant> existingVariants = testVariantRepository.findByExperiment(experiment);
    testVariantRepository.deleteAll(existingVariants);
    
    // 새 변형 생성
    for (VariantConfigRequest.VariantConfig config : variantConfigs) {
      TestVariant variant = TestVariant.builder()
        .experiment(experiment)
        .variantKey(config.getVariantKey())
        .variantName(config.getVariantName())
        .isControl(config.getIsControl() != null ? config.getIsControl() : false)
        .isActive(true)
        .config(config.getConfig() != null ? config.getConfig() : new HashMap<>())
        .featureFlags(config.getFeatureFlags() != null ? config.getFeatureFlags() : new HashMap<>())
        .description(config.getDescription())
        .build();
      
      testVariantRepository.save(variant);
    }
    
    return experiment;
  }
}