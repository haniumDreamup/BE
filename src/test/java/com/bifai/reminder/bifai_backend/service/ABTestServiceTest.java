package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentStatus;
import com.bifai.reminder.bifai_backend.entity.Experiment.ExperimentType;
import com.bifai.reminder.bifai_backend.entity.TestGroup.GroupType;
import com.bifai.reminder.bifai_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * A/B 테스트 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class ABTestServiceTest {
  
  @Mock
  private ExperimentRepository experimentRepository;
  
  @Mock
  private TestGroupRepository testGroupRepository;
  
  @Mock
  private TestVariantRepository testVariantRepository;
  
  @Mock
  private TestGroupAssignmentRepository assignmentRepository;
  
  @Mock
  private UserRepository userRepository;
  
  @InjectMocks
  private ABTestService abTestService;
  
  private User testUser;
  private Experiment testExperiment;
  private TestGroup controlGroup;
  private TestGroup treatmentGroup;
  private TestVariant controlVariant;
  private TestVariant treatmentVariant;
  
  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser = User.builder()
      .userId(1L)
      .username("testuser")
      .cognitiveLevel(User.CognitiveLevel.MODERATE)
      .isActive(true)
      .build();
    
    // 테스트 실험 설정
    testExperiment = Experiment.builder()
      .experimentId(1L)
      .name("테스트 실험")
      .experimentKey("test_experiment")
      .experimentType(ExperimentType.AB_TEST)
      .status(ExperimentStatus.ACTIVE)
      .isActive(true)
      .sampleSizeTarget(1000)
      .currentParticipants(0)
      .startDate(LocalDateTime.now().minusDays(1))
      .endDate(LocalDateTime.now().plusDays(30))
      .targetCriteria(new HashMap<>())
      .trafficAllocation(createTrafficAllocation())
      .metadata(new HashMap<>())
      .build();
    
    // 테스트 그룹 설정
    controlGroup = TestGroup.builder()
      .groupId(1L)
      .experiment(testExperiment)
      .groupName("control")
      .groupType(GroupType.CONTROL)
      .isControl(true)
      .isActive(true)
      .currentSize(0)
      .maxSampleSize(500)
      .build();
    
    treatmentGroup = TestGroup.builder()
      .groupId(2L)
      .experiment(testExperiment)
      .groupName("treatment")
      .groupType(GroupType.TREATMENT)
      .isControl(false)
      .isActive(true)
      .currentSize(0)
      .maxSampleSize(500)
      .build();
    
    // 테스트 변형 설정
    controlVariant = TestVariant.builder()
      .variantId(1L)
      .experiment(testExperiment)
      .variantKey("control")
      .variantName("대조군")
      .isControl(true)
      .isActive(true)
      .featureFlags(createControlFeatureFlags())
      .build();
    
    treatmentVariant = TestVariant.builder()
      .variantId(2L)
      .experiment(testExperiment)
      .variantKey("treatment")
      .variantName("실험군")
      .isControl(false)
      .isActive(true)
      .featureFlags(createTreatmentFeatureFlags())
      .build();
  }
  
  private Map<String, Integer> createTrafficAllocation() {
    Map<String, Integer> allocation = new HashMap<>();
    allocation.put("control", 50);
    allocation.put("treatment", 50);
    return allocation;
  }
  
  private Map<String, Object> createControlFeatureFlags() {
    Map<String, Object> flags = new HashMap<>();
    flags.put("show_new_ui", false);
    flags.put("enable_voice_guide", true);
    return flags;
  }
  
  private Map<String, Object> createTreatmentFeatureFlags() {
    Map<String, Object> flags = new HashMap<>();
    flags.put("show_new_ui", true);
    flags.put("enable_voice_guide", true);
    flags.put("large_font_size", true);
    return flags;
  }
  
  @Test
  @DisplayName("사용자를 실험 그룹에 할당할 수 있다")
  void assignUserToExperiment_Success() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(experimentRepository.findByExperimentKeyAndStatus("test_experiment", ExperimentStatus.ACTIVE))
      .thenReturn(Optional.of(testExperiment));
    when(assignmentRepository.findByUserAndExperiment(testUser, testExperiment))
      .thenReturn(Optional.empty());
    when(testGroupRepository.findByExperimentAndIsActiveTrue(testExperiment))
      .thenReturn(Arrays.asList(controlGroup, treatmentGroup));
    when(testVariantRepository.findByExperimentAndIsActiveTrue(testExperiment))
      .thenReturn(Arrays.asList(controlVariant, treatmentVariant));
    when(assignmentRepository.save(any(TestGroupAssignment.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    
    // When
    TestGroupAssignment assignment = abTestService.assignUserToExperiment(1L, "test_experiment");
    
    // Then
    assertThat(assignment).isNotNull();
    assertThat(assignment.getUser()).isEqualTo(testUser);
    assertThat(assignment.getExperiment()).isEqualTo(testExperiment);
    assertThat(assignment.getTestGroup()).isIn(controlGroup, treatmentGroup);
    assertThat(assignment.getVariant()).isIn(controlVariant, treatmentVariant);
    assertThat(assignment.getAssignmentHash()).isNotEmpty();
    
    verify(testGroupRepository).save(any(TestGroup.class));
    verify(experimentRepository).save(testExperiment);
  }
  
  @Test
  @DisplayName("이미 할당된 사용자는 기존 할당을 반환한다")
  void assignUserToExperiment_AlreadyAssigned() {
    // Given
    TestGroupAssignment existingAssignment = TestGroupAssignment.builder()
      .user(testUser)
      .experiment(testExperiment)
      .testGroup(controlGroup)
      .variant(controlVariant)
      .exposureCount(5)
      .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(experimentRepository.findByExperimentKeyAndStatus("test_experiment", ExperimentStatus.ACTIVE))
      .thenReturn(Optional.of(testExperiment));
    when(assignmentRepository.findByUserAndExperiment(testUser, testExperiment))
      .thenReturn(Optional.of(existingAssignment));
    when(assignmentRepository.save(existingAssignment)).thenReturn(existingAssignment);
    
    // When
    TestGroupAssignment assignment = abTestService.assignUserToExperiment(1L, "test_experiment");
    
    // Then
    assertThat(assignment).isEqualTo(existingAssignment);
    assertThat(assignment.getExposureCount()).isEqualTo(6); // 노출 횟수 증가
    
    verify(testGroupRepository, never()).save(any());
    verify(experimentRepository, never()).save(any());
  }
  
  @Test
  @DisplayName("대상 조건에 맞지 않는 사용자는 할당되지 않는다")
  void assignUserToExperiment_NotEligible() {
    // Given
    Map<String, Object> criteria = new HashMap<>();
    criteria.put("cognitiveLevel", "SEVERE");
    testExperiment.setTargetCriteria(criteria);
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(experimentRepository.findByExperimentKeyAndStatus("test_experiment", ExperimentStatus.ACTIVE))
      .thenReturn(Optional.of(testExperiment));
    when(assignmentRepository.findByUserAndExperiment(testUser, testExperiment))
      .thenReturn(Optional.empty());
    
    // When
    TestGroupAssignment assignment = abTestService.assignUserToExperiment(1L, "test_experiment");
    
    // Then
    assertThat(assignment).isNull();
    
    verify(assignmentRepository, never()).save(any());
  }
  
  @Test
  @DisplayName("Feature Flag 값을 조회할 수 있다")
  void getFeatureFlag_Success() {
    // Given
    TestGroupAssignment assignment = TestGroupAssignment.builder()
      .user(testUser)
      .experiment(testExperiment)
      .testGroup(treatmentGroup)
      .variant(treatmentVariant)
      .isActive(true)
      .build();
    
    when(assignmentRepository.findActiveAssignmentsByUserId(1L))
      .thenReturn(Collections.singletonList(assignment));
    when(assignmentRepository.save(assignment)).thenReturn(assignment);
    
    // When
    Object flagValue = abTestService.getFeatureFlag(1L, "show_new_ui");
    
    // Then
    assertThat(flagValue).isEqualTo(true);
    verify(assignmentRepository).save(assignment);
  }
  
  @Test
  @DisplayName("Feature Flag가 없으면 기본값을 반환한다")
  void getFeatureFlag_DefaultValue() {
    // Given
    when(assignmentRepository.findActiveAssignmentsByUserId(1L))
      .thenReturn(Collections.emptyList());
    
    // When
    Object flagValue = abTestService.getFeatureFlag(1L, "show_new_ui");
    
    // Then
    assertThat(flagValue).isEqualTo(false); // 기본값
  }
  
  @Test
  @DisplayName("전환을 기록할 수 있다")
  void recordConversion_Success() {
    // Given
    TestGroupAssignment assignment = TestGroupAssignment.builder()
      .user(testUser)
      .experiment(testExperiment)
      .testGroup(treatmentGroup)
      .variant(treatmentVariant)
      .conversionAchieved(false)
      .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(experimentRepository.findByExperimentKey("test_experiment"))
      .thenReturn(Optional.of(testExperiment));
    when(assignmentRepository.findByUserAndExperiment(testUser, testExperiment))
      .thenReturn(Optional.of(assignment));
    when(assignmentRepository.save(assignment)).thenReturn(assignment);
    when(testVariantRepository.save(treatmentVariant)).thenReturn(treatmentVariant);
    
    // When
    abTestService.recordConversion(1L, "test_experiment", 100.0);
    
    // Then
    assertThat(assignment.getConversionAchieved()).isTrue();
    assertThat(assignment.getConversionValue()).isEqualTo(100.0);
    assertThat(assignment.getConversionAt()).isNotNull();
    
    verify(assignmentRepository).save(assignment);
    verify(testVariantRepository).save(treatmentVariant);
  }
  
  @Test
  @DisplayName("실험 결과를 분석할 수 있다")
  void analyzeExperiment_Success() {
    // Given
    when(experimentRepository.findByExperimentKey("test_experiment"))
      .thenReturn(Optional.of(testExperiment));
    when(testGroupRepository.findByExperiment(testExperiment))
      .thenReturn(Arrays.asList(controlGroup, treatmentGroup));
    when(assignmentRepository.countConversionsByTestGroup(controlGroup))
      .thenReturn(25L);
    when(assignmentRepository.countConversionsByTestGroup(treatmentGroup))
      .thenReturn(40L);
    
    controlGroup.setCurrentSize(100);
    treatmentGroup.setCurrentSize(100);
    
    // When
    Map<String, Object> analysis = abTestService.analyzeExperiment("test_experiment");
    
    // Then
    assertThat(analysis).isNotNull();
    assertThat(analysis.get("experimentKey")).isEqualTo("test_experiment");
    assertThat(analysis.get("status")).isEqualTo(ExperimentStatus.ACTIVE);
    assertThat(analysis.get("participants")).isEqualTo(0);
    
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> groupResults = (List<Map<String, Object>>) analysis.get("groupResults");
    assertThat(groupResults).hasSize(2);
    
    Map<String, Object> controlResult = groupResults.get(0);
    assertThat(controlResult.get("groupName")).isEqualTo("control");
    assertThat(controlResult.get("conversionRate")).isEqualTo(25.0);
    
    Map<String, Object> treatmentResult = groupResults.get(1);
    assertThat(treatmentResult.get("groupName")).isEqualTo("treatment");
    assertThat(treatmentResult.get("conversionRate")).isEqualTo(40.0);
    
    assertThat(analysis.get("pValue")).isNotNull();
    assertThat(analysis.get("isSignificant")).isNotNull();
  }
  
  @Test
  @DisplayName("사용자를 실험에서 제외할 수 있다")
  void optOutUser_Success() {
    // Given
    TestGroupAssignment assignment = TestGroupAssignment.builder()
      .user(testUser)
      .experiment(testExperiment)
      .testGroup(treatmentGroup)
      .variant(treatmentVariant)
      .optedOut(false)
      .build();
    
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(experimentRepository.findByExperimentKey("test_experiment"))
      .thenReturn(Optional.of(testExperiment));
    when(assignmentRepository.findByUserAndExperiment(testUser, testExperiment))
      .thenReturn(Optional.of(assignment));
    when(assignmentRepository.save(assignment)).thenReturn(assignment);
    
    // When
    abTestService.optOutUser(1L, "test_experiment");
    
    // Then
    assertThat(assignment.getOptedOut()).isTrue();
    assertThat(assignment.getIsActive()).isFalse();
    
    verify(assignmentRepository).save(assignment);
  }
  
  @Test
  @DisplayName("존재하지 않는 사용자는 실험에 할당할 수 없다")
  void assignUserToExperiment_UserNotFound() {
    // Given
    when(userRepository.findById(999L)).thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> abTestService.assignUserToExperiment(999L, "test_experiment"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("사용자를 찾을 수 없습니다");
  }
  
  @Test
  @DisplayName("존재하지 않는 실험에는 할당할 수 없다")
  void assignUserToExperiment_ExperimentNotFound() {
    // Given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(experimentRepository.findByExperimentKeyAndStatus("invalid_experiment", ExperimentStatus.ACTIVE))
      .thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> abTestService.assignUserToExperiment(1L, "invalid_experiment"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("활성 실험을 찾을 수 없습니다");
  }
}