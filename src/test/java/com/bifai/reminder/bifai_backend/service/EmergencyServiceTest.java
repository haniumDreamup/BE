package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.EmergencyRepository;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * 긴급 상황 서비스 테스트
 * 
 * <p>EmergencyService의 비즈니스 로직을 테스트합니다.
 * 긴급 상황 생성, 조회, 해결, 보호자 알림 등을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergencyService 테스트")
class EmergencyServiceTest {

  @Mock
  private EmergencyRepository emergencyRepository;

  @Mock
  private GuardianRepository guardianRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private EmergencyService emergencyService;

  private User testUser;
  private Emergency testEmergency;
  private Guardian testGuardian;

  @BeforeEach
  void setUp() {
    // 테스트 사용자 설정
    testUser = User.builder()
        .userId(1L)
        .username("testuser")
        .fullName("테스트 사용자")
        .email("test@example.com")
        .build();

    // 테스트 긴급 상황 설정
    testEmergency = Emergency.builder()
        .id(1L)
        .user(testUser)
        .type(EmergencyType.MANUAL_ALERT)
        .status(EmergencyStatus.ACTIVE)
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울특별시 중구")
        .description("도움이 필요합니다")
        .severity(EmergencySeverity.HIGH)
        .triggeredBy(TriggerSource.USER)
        .createdAt(LocalDateTime.now())
        .build();
    testEmergency.setNotifiedGuardians("guardian@example.com");

    // 테스트 보호자 설정
    User guardianUser = User.builder()
        .userId(2L)
        .username("guardian")
        .fullName("보호자")
        .email("guardian@example.com")
        .build();

    testGuardian = Guardian.builder()
        .id(1L)
        .user(testUser)
        .guardianUser(guardianUser)
        .relationship("어머니")
        .isActive(true)
        .isPrimary(true)
        .build();

    // Security Context 설정
    BifUserDetails userDetails = new BifUserDetails(testUser);
    
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(
        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList())
    );
    SecurityContextHolder.setContext(securityContext);
    
    // BaseService의 userRepository 필드 설정
    ReflectionTestUtils.setField(emergencyService, "userRepository", userRepository);
  }

  @Test
  @DisplayName("긴급 상황 생성 - 성공")
  void createEmergency_Success() {
    // given
    EmergencyRequest request = EmergencyRequest.builder()
        .type(EmergencyType.MANUAL_ALERT)
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울특별시 중구")
        .description("도움이 필요합니다")
        .severity(EmergencySeverity.HIGH)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(testEmergency);
    given(guardianRepository.findActiveGuardiansByUserId(1L))
        .willReturn(Arrays.asList(testGuardian));

    // when
    EmergencyResponse response = emergencyService.createEmergency(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getType()).isEqualTo(EmergencyType.MANUAL_ALERT);
    assertThat(response.getStatus()).isEqualTo(EmergencyStatus.NOTIFIED); // 보호자에게 알림 후 NOTIFIED로 변경됨

    verify(emergencyRepository, times(2)).save(any(Emergency.class)); // 생성 시 1번, 알림 후 상태 업데이트 시 1번
    verify(notificationService).sendEmergencyNotification(eq(testGuardian), any(Emergency.class));
  }

  @Test
  @DisplayName("긴급 상황 생성 - 보호자 없음")
  void createEmergency_NoGuardians() {
    // given
    EmergencyRequest request = EmergencyRequest.builder()
        .type(EmergencyType.MANUAL_ALERT)
        .latitude(37.5665)
        .longitude(126.9780)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(testEmergency);
    given(guardianRepository.findActiveGuardiansByUserId(1L))
        .willReturn(Collections.emptyList());

    // when
    EmergencyResponse response = emergencyService.createEmergency(request);

    // then
    assertThat(response).isNotNull();
    verify(notificationService, never()).sendEmergencyNotification(any(), any());
  }

  @Test
  @DisplayName("낙상 감지 처리 - 높은 신뢰도")
  void handleFallDetection_HighConfidence() {
    // given
    FallDetectionRequest request = FallDetectionRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .confidence(95.0)
        .imageUrl("https://example.com/fall.jpg")
        .build();

    Emergency fallEmergency = Emergency.builder()
        .id(2L)
        .user(testUser)
        .type(EmergencyType.FALL_DETECTION)
        .status(EmergencyStatus.ACTIVE)
        .severity(EmergencySeverity.CRITICAL)
        .fallConfidence(95.0)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(fallEmergency);
    given(guardianRepository.findActiveGuardiansByUserId(1L))
        .willReturn(Arrays.asList(testGuardian));

    // when
    EmergencyResponse response = emergencyService.handleFallDetection(request);

    // then
    assertThat(response.getType()).isEqualTo(EmergencyType.FALL_DETECTION);
    assertThat(response.getSeverity()).isEqualTo(EmergencySeverity.CRITICAL);
    verify(emergencyRepository, times(2)).save(argThat(e -> 
        e.getSeverity() == EmergencySeverity.CRITICAL &&
        e.getFallConfidence() == 95.0
    )); // 생성 시 1번, 알림 후 상태 업데이트 시 1번
  }

  @Test
  @DisplayName("긴급 상황 상태 조회 - 성공")
  void getEmergencyStatus_Success() {
    // given
    given(emergencyRepository.findById(1L)).willReturn(Optional.of(testEmergency));

    // when
    EmergencyResponse response = emergencyService.getEmergencyStatus(1L);

    // then
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getStatus()).isEqualTo(EmergencyStatus.ACTIVE);
  }

  @Test
  @DisplayName("긴급 상황 상태 조회 - 존재하지 않음")
  void getEmergencyStatus_NotFound() {
    // given
    given(emergencyRepository.findById(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> emergencyService.getEmergencyStatus(999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("긴급 상황을 찾을 수 없습니다");
  }

  @Test
  @DisplayName("활성 긴급 상황 목록 조회")
  void getActiveEmergencies_Success() {
    // given
    List<Emergency> activeEmergencies = Arrays.asList(testEmergency);
    given(emergencyRepository.findActiveEmergencies(anyList()))
        .willReturn(activeEmergencies);

    // when
    List<EmergencyResponse> responses = emergencyService.getActiveEmergencies();

    // then
    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).getStatus()).isEqualTo(EmergencyStatus.ACTIVE);
  }

  @Test
  @DisplayName("긴급 상황 해결 - 성공")
  void resolveEmergency_Success() {
    // given
    given(emergencyRepository.findById(1L)).willReturn(Optional.of(testEmergency));
    given(emergencyRepository.save(any(Emergency.class))).willAnswer(invocation -> invocation.getArgument(0));

    // when
    EmergencyResponse response = emergencyService.resolveEmergency(1L, "보호자", "안전하게 해결됨");

    // then
    assertThat(response.getStatus()).isEqualTo(EmergencyStatus.RESOLVED);
    verify(emergencyRepository).save(argThat(e -> 
        e.getStatus() == EmergencyStatus.RESOLVED &&
        e.getResolvedBy().equals("보호자") &&
        e.getResolutionNotes().equals("안전하게 해결됨") &&
        e.getResolvedAt() != null &&
        e.getResponseTimeSeconds() != null
    ));
  }

  @Test
  @DisplayName("사용자 긴급 상황 이력 조회")
  void getUserEmergencyHistory_Success() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    Page<Emergency> emergencyPage = new PageImpl<>(Arrays.asList(testEmergency));
    
    given(emergencyRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
        .willReturn(emergencyPage);

    // when
    Page<EmergencyResponse> responses = emergencyService.getUserEmergencyHistory(1L, pageable);

    // then
    assertThat(responses.getTotalElements()).isEqualTo(1);
    assertThat(responses.getContent().get(0).getUserId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("본인 긴급 상황 확인 - 성공")
  void isOwnEmergency_True() {
    // given
    given(emergencyRepository.findById(1L)).willReturn(Optional.of(testEmergency));

    // when
    boolean isOwn = emergencyService.isOwnEmergency(1L);

    // then
    assertThat(isOwn).isTrue();
  }

  @Test
  @DisplayName("보호자의 긴급 상황 확인 - 성공")
  void isGuardianOfEmergency_True() {
    // given
    given(emergencyRepository.findById(1L)).willReturn(Optional.of(testEmergency));
    given(guardianRepository.existsByUserIdAndGuardianUserId(1L, 1L))
        .willReturn(true);

    // when
    boolean isGuardian = emergencyService.isGuardianOfEmergency(1L);

    // then
    assertThat(isGuardian).isTrue();
  }

  @Test
  @DisplayName("낙상 감지 처리 - 중간 신뢰도")
  void handleFallDetection_MediumConfidence() {
    // given
    FallDetectionRequest request = FallDetectionRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .confidence(65.0) // 중간 신뢰도
        .build();

    Emergency fallEmergency = Emergency.builder()
        .id(3L)
        .user(testUser)
        .type(EmergencyType.FALL_DETECTION)
        .status(EmergencyStatus.ACTIVE)
        .severity(EmergencySeverity.MEDIUM)
        .fallConfidence(65.0)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(fallEmergency);
    given(guardianRepository.findActiveGuardiansByUserId(1L))
        .willReturn(Arrays.asList(testGuardian));

    // when
    EmergencyResponse response = emergencyService.handleFallDetection(request);

    // then
    assertThat(response.getSeverity()).isEqualTo(EmergencySeverity.MEDIUM);
  }

  @Test
  @DisplayName("낙상 감지 처리 - 낮은 신뢰도")
  void handleFallDetection_LowConfidence() {
    // given
    FallDetectionRequest request = FallDetectionRequest.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .confidence(30.0) // 낮은 신뢰도
        .build();

    Emergency fallEmergency = Emergency.builder()
        .id(4L)
        .user(testUser)
        .type(EmergencyType.FALL_DETECTION)
        .status(EmergencyStatus.ACTIVE)
        .severity(EmergencySeverity.LOW)
        .fallConfidence(30.0)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(fallEmergency);
    given(guardianRepository.findActiveGuardiansByUserId(1L))
        .willReturn(Arrays.asList(testGuardian));

    // when
    EmergencyResponse response = emergencyService.handleFallDetection(request);

    // then
    assertThat(response.getSeverity()).isEqualTo(EmergencySeverity.LOW);
  }

  @Test
  @DisplayName("긴급 상황 해결 (DTO) - 성공")
  void resolveEmergency_WithDTO_Success() {
    // given
    ResolveEmergencyRequest request = ResolveEmergencyRequest.builder()
        .resolvedBy("보호자")
        .resolutionNotes("안전하게 해결됨")
        .build();
    
    given(emergencyRepository.findById(1L)).willReturn(Optional.of(testEmergency));
    given(emergencyRepository.save(any(Emergency.class))).willAnswer(invocation -> invocation.getArgument(0));

    // when
    EmergencyResponse response = emergencyService.resolveEmergency(1L, request);

    // then
    assertThat(response.getStatus()).isEqualTo(EmergencyStatus.RESOLVED);
    verify(emergencyRepository).save(argThat(e -> 
        e.getStatus() == EmergencyStatus.RESOLVED &&
        e.getResolvedBy().equals("보호자") &&
        e.getResolutionNotes().equals("안전하게 해결됨")
    ));
  }

  @Test
  @DisplayName("본인 긴급 상황 확인 - 실패")
  void isOwnEmergency_False() {
    // given
    User anotherUser = User.builder()
        .userId(2L)
        .username("anotheruser")
        .email("another@example.com")
        .build();
    
    Emergency anotherEmergency = Emergency.builder()
        .id(2L)
        .user(anotherUser)
        .type(EmergencyType.MANUAL_ALERT)
        .status(EmergencyStatus.ACTIVE)
        .build();
    
    given(emergencyRepository.findById(2L)).willReturn(Optional.of(anotherEmergency));

    // when
    boolean isOwn = emergencyService.isOwnEmergency(2L);

    // then
    assertThat(isOwn).isFalse();
  }

  @Test
  @DisplayName("보호자의 긴급 상황 확인 - 실패")
  void isGuardianOfEmergency_False() {
    // given
    given(emergencyRepository.findById(1L)).willReturn(Optional.of(testEmergency));
    given(guardianRepository.existsByUserIdAndGuardianUserId(1L, 1L))
        .willReturn(false);

    // when
    boolean isGuardian = emergencyService.isGuardianOfEmergency(1L);

    // then
    assertThat(isGuardian).isFalse();
  }

  @Test
  @DisplayName("긴급 상황 해결 - 존재하지 않는 긴급상황")
  void resolveEmergency_NotFound() {
    // given
    given(emergencyRepository.findById(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> emergencyService.resolveEmergency(999L, "보호자", "메모"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("긴급 상황을 찾을 수 없습니다");
  }
}