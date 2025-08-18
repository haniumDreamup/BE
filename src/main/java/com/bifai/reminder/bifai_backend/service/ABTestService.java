package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentStatus;
import com.bifai.reminder.bifai_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A/B 테스트 서비스
 * 실험 관리 및 사용자 그룹 할당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ABTestService {
  
  private final ExperimentRepository experimentRepository;
  private final TestGroupRepository testGroupRepository;
  private final TestVariantRepository testVariantRepository;
  private final TestGroupAssignmentRepository assignmentRepository;
  private final UserRepository userRepository;
  
  /**
   * 사용자를 실험 그룹에 할당
   * 해시 기반 균등 분배 알고리즘 사용
   */
  @Transactional
  public TestGroupAssignment assignUserToExperiment(Long userId, String experimentKey) {
    // 사용자 조회
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    // 실험 조회
    Experiment experiment = experimentRepository.findByExperimentKeyAndStatus(
      experimentKey, ExperimentStatus.ACTIVE)
      .orElseThrow(() -> new IllegalArgumentException("활성 실험을 찾을 수 없습니다: " + experimentKey));
    
    // 이미 할당된 경우 기존 할당 반환
    Optional<TestGroupAssignment> existing = assignmentRepository
      .findByUserAndExperiment(user, experiment);
    
    if (existing.isPresent()) {
      log.debug("사용자 {}는 이미 실험 {}에 할당되어 있습니다", userId, experimentKey);
      existing.get().recordExposure();
      return assignmentRepository.save(existing.get());
    }
    
    // 대상 조건 확인
    if (!isUserEligible(user, experiment)) {
      log.info("사용자 {}는 실험 {} 대상이 아닙니다", userId, experimentKey);
      return null;
    }
    
    // 해시 기반 그룹 할당
    TestGroup assignedGroup = assignGroupByHash(user, experiment);
    if (assignedGroup == null) {
      log.warn("사용자 {}를 실험 {}에 할당할 수 없습니다", userId, experimentKey);
      return null;
    }
    
    // 변형(Variant) 선택
    TestVariant variant = selectVariant(experiment, assignedGroup);
    
    // 할당 생성
    TestGroupAssignment assignment = TestGroupAssignment.builder()
      .user(user)
      .testGroup(assignedGroup)
      .experiment(experiment)
      .variant(variant)
      .assignmentHash(generateHash(userId, experimentKey))
      .assignmentReason("hash_based")
      .build();
    
    assignment.recordExposure();
    
    // 그룹 크기 업데이트
    assignedGroup.addUser();
    testGroupRepository.save(assignedGroup);
    
    // 실험 참여자 수 업데이트
    experiment.incrementParticipants();
    experimentRepository.save(experiment);
    
    log.info("사용자 {}를 실험 {} 그룹 {}에 할당했습니다", 
            userId, experimentKey, assignedGroup.getGroupName());
    
    return assignmentRepository.save(assignment);
  }
  
  /**
   * 해시 기반 그룹 할당 알고리즘
   */
  private TestGroup assignGroupByHash(User user, Experiment experiment) {
    String hash = generateHash(user.getUserId(), experiment.getExperimentKey());
    int hashValue = Math.abs(hash.hashCode());
    
    // 트래픽 할당 비율에 따라 그룹 선택
    Map<String, Integer> allocation = experiment.getTrafficAllocation();
    int totalAllocation = allocation.values().stream().mapToInt(Integer::intValue).sum();
    
    if (totalAllocation == 0) {
      log.error("실험 {}의 트래픽 할당이 설정되지 않았습니다", experiment.getExperimentKey());
      return null;
    }
    
    int bucket = hashValue % totalAllocation;
    int cumulativeAllocation = 0;
    
    // 그룹 목록 조회
    List<TestGroup> groups = testGroupRepository.findByExperimentAndIsActiveTrue(experiment);
    
    for (TestGroup group : groups) {
      Integer groupAllocation = allocation.get(group.getGroupName());
      if (groupAllocation == null) continue;
      
      cumulativeAllocation += groupAllocation;
      if (bucket < cumulativeAllocation) {
        // 그룹이 가득 찼는지 확인
        if (!group.isFull()) {
          return group;
        }
      }
    }
    
    // 기본적으로 대조군 반환
    return groups.stream()
      .filter(TestGroup::getIsControl)
      .findFirst()
      .orElse(null);
  }
  
  /**
   * 변형(Variant) 선택
   */
  private TestVariant selectVariant(Experiment experiment, TestGroup group) {
    List<TestVariant> variants = testVariantRepository
      .findByExperimentAndIsActiveTrue(experiment);
    
    // 그룹에 매핑된 변형 찾기
    return variants.stream()
      .filter(v -> v.getVariantKey().equals(group.getGroupName()))
      .findFirst()
      .orElse(variants.stream()
        .filter(TestVariant::getIsControl)
        .findFirst()
        .orElse(null));
  }
  
  /**
   * 사용자 실험 대상 여부 확인
   */
  private boolean isUserEligible(User user, Experiment experiment) {
    Map<String, Object> criteria = experiment.getTargetCriteria();
    
    if (criteria == null || criteria.isEmpty()) {
      return true; // 조건이 없으면 모든 사용자 대상
    }
    
    // 인지 수준 확인
    if (criteria.containsKey("cognitiveLevel")) {
      String requiredLevel = (String) criteria.get("cognitiveLevel");
      if (!user.getCognitiveLevel().name().equals(requiredLevel)) {
        return false;
      }
    }
    
    // 가입일 확인 (createdAt이 없으므로 다른 조건으로 대체)
    // 실제로는 User 엔티티에 createdAt 필드가 필요할 수 있음
    // 현재는 이 조건을 건너뜀
    /*
    if (criteria.containsKey("minDaysSinceSignup")) {
      Integer minDays = (Integer) criteria.get("minDaysSinceSignup");
      // User 엔티티에 createdAt 필드가 없으므로 패스
      // 추후 필요시 추가 구현
    }
    */
    
    // 활성 사용자 확인
    if (criteria.containsKey("requireActive")) {
      Boolean requireActive = (Boolean) criteria.get("requireActive");
      if (requireActive && !user.getIsActive()) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * Feature Flag 값 조회
   */
  @Cacheable(value = "featureFlags", key = "#userId + ':' + #flagKey")
  public Object getFeatureFlag(Long userId, String flagKey) {
    // 사용자의 활성 실험 할당 조회
    List<TestGroupAssignment> assignments = assignmentRepository
      .findActiveAssignmentsByUserId(userId);
    
    for (TestGroupAssignment assignment : assignments) {
      if (assignment.getVariant() != null) {
        Object flagValue = assignment.getVariant().getFeatureFlag(flagKey);
        if (flagValue != null) {
          // 노출 기록
          assignment.recordExposure();
          assignmentRepository.save(assignment);
          return flagValue;
        }
      }
    }
    
    // 기본값 반환
    return getDefaultFeatureFlag(flagKey);
  }
  
  /**
   * 기본 Feature Flag 값
   */
  private Object getDefaultFeatureFlag(String flagKey) {
    Map<String, Object> defaults = new HashMap<>();
    defaults.put("show_new_ui", false);
    defaults.put("enable_voice_guide", true);
    defaults.put("large_font_size", false);
    defaults.put("simplified_navigation", true);
    defaults.put("auto_reminder", true);
    
    return defaults.get(flagKey);
  }
  
  /**
   * 전환 기록
   */
  @Transactional
  public void recordConversion(Long userId, String experimentKey, Double value) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    TestGroupAssignment assignment = assignmentRepository
      .findByUserAndExperiment(user, experiment)
      .orElse(null);
    
    if (assignment != null && !assignment.getConversionAchieved()) {
      assignment.recordConversion(value);
      assignmentRepository.save(assignment);
      
      // 변형 성과 업데이트
      if (assignment.getVariant() != null) {
        TestVariant variant = assignment.getVariant();
        variant.incrementParticipants();
        testVariantRepository.save(variant);
      }
      
      log.info("사용자 {} 실험 {} 전환 기록: {}", userId, experimentKey, value);
    }
  }
  
  /**
   * 실험 결과 분석
   */
  @Transactional(readOnly = true)
  public Map<String, Object> analyzeExperiment(String experimentKey) {
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    Map<String, Object> analysis = new HashMap<>();
    analysis.put("experimentKey", experimentKey);
    analysis.put("status", experiment.getStatus());
    analysis.put("participants", experiment.getCurrentParticipants());
    analysis.put("progress", experiment.getProgress());
    
    // 그룹별 성과
    List<Map<String, Object>> groupResults = new ArrayList<>();
    List<TestGroup> groups = testGroupRepository.findByExperiment(experiment);
    
    for (TestGroup group : groups) {
      Map<String, Object> groupResult = new HashMap<>();
      groupResult.put("groupName", group.getGroupName());
      groupResult.put("groupType", group.getGroupType());
      groupResult.put("size", group.getCurrentSize());
      
      // 전환율 계산
      long conversions = assignmentRepository.countConversionsByTestGroup(group);
      double conversionRate = group.getCurrentSize() > 0 
        ? (double) conversions / group.getCurrentSize() * 100 : 0;
      groupResult.put("conversionRate", conversionRate);
      
      groupResults.add(groupResult);
    }
    
    analysis.put("groupResults", groupResults);
    
    // 통계적 유의성 계산 (간단한 버전)
    if (groups.size() == 2) {
      TestGroup control = groups.stream()
        .filter(TestGroup::getIsControl)
        .findFirst().orElse(null);
      TestGroup variant = groups.stream()
        .filter(g -> !g.getIsControl())
        .findFirst().orElse(null);
      
      if (control != null && variant != null) {
        double pValue = calculatePValue(control, variant);
        analysis.put("pValue", pValue);
        analysis.put("isSignificant", pValue < 0.05);
      }
    }
    
    return analysis;
  }
  
  /**
   * P-value 계산 (간단한 구현)
   */
  private double calculatePValue(TestGroup control, TestGroup variant) {
    // 실제로는 통계 라이브러리 사용 필요
    // 여기서는 간단한 근사치만 계산
    long controlConversions = assignmentRepository.countConversionsByTestGroup(control);
    long variantConversions = assignmentRepository.countConversionsByTestGroup(variant);
    
    double controlRate = (double) controlConversions / control.getCurrentSize();
    double variantRate = (double) variantConversions / variant.getCurrentSize();
    
    double difference = Math.abs(controlRate - variantRate);
    
    // 매우 간단한 근사
    if (difference > 0.1) return 0.01;
    if (difference > 0.05) return 0.03;
    if (difference > 0.02) return 0.05;
    return 0.10;
  }
  
  /**
   * 해시 생성
   */
  private String generateHash(Long userId, String experimentKey) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String input = userId + ":" + experimentKey;
      byte[] hashBytes = md.digest(input.getBytes());
      
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
      
    } catch (NoSuchAlgorithmException e) {
      // Fallback to simple hash
      return String.valueOf((userId + experimentKey).hashCode());
    }
  }
  
  /**
   * 사용자의 모든 활성 실험 조회
   */
  @Transactional(readOnly = true)
  public List<TestGroupAssignment> getUserExperiments(Long userId) {
    return assignmentRepository.findActiveAssignmentsByUserId(userId);
  }
  
  /**
   * 실험에서 사용자 제외
   */
  @Transactional
  public void optOutUser(Long userId, String experimentKey) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    Experiment experiment = experimentRepository.findByExperimentKey(experimentKey)
      .orElseThrow(() -> new IllegalArgumentException("실험을 찾을 수 없습니다: " + experimentKey));
    
    TestGroupAssignment assignment = assignmentRepository
      .findByUserAndExperiment(user, experiment)
      .orElse(null);
    
    if (assignment != null) {
      assignment.optOut();
      assignmentRepository.save(assignment);
      log.info("사용자 {}가 실험 {}에서 제외되었습니다", userId, experimentKey);
    }
  }
}