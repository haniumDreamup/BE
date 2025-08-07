package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.EmergencyRepository;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

/**
 * EmergencyService 단위 테스트
 * Spring 컨텍스트 없이 순수 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmergencyService 단위 테스트")
class EmergencyServiceUnitTest {

  @Mock
  private EmergencyRepository emergencyRepository;

  @Mock
  private GuardianRepository guardianRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationService notificationService;

  private EmergencyService emergencyService;

  private User testUser;
  private Emergency testEmergency;
  private Guardian testGuardian;

  @BeforeEach
  void setUp() {
    // EmergencyService 수동 생성
    emergencyService = new EmergencyService(
        emergencyRepository,
        guardianRepository,
        notificationService
    );
    
    // BaseService의 userRepository 필드 설정
    ReflectionTestUtils.setField(emergencyService, "userRepository", userRepository);
    
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
    assertThat(response.getStatus()).isEqualTo(EmergencyStatus.NOTIFIED);

    verify(emergencyRepository, times(2)).save(any(Emergency.class));
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
        .address("서울특별시 중구")
        .description("도움이 필요합니다")
        .severity(EmergencySeverity.HIGH)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(testEmergency);
    given(guardianRepository.findActiveGuardiansByUserId(1L))
        .willReturn(Collections.emptyList());

    // when
    EmergencyResponse response = emergencyService.createEmergency(request);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    
    verify(emergencyRepository).save(any(Emergency.class));
    verify(notificationService, never()).sendEmergencyNotification(any(), any());
  }

  @Test
  @DisplayName("긴급 상황 해결 - 성공")
  void resolveEmergency_Success() {
    // given
    Long emergencyId = 1L;
    String resolvedBy = "보호자";
    String notes = "안전하게 도착했습니다";

    given(emergencyRepository.findById(emergencyId)).willReturn(Optional.of(testEmergency));
    given(emergencyRepository.save(any(Emergency.class))).willReturn(testEmergency);

    // when
    EmergencyResponse response = emergencyService.resolveEmergency(emergencyId, resolvedBy, notes);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(EmergencyStatus.RESOLVED);
    
    verify(emergencyRepository).findById(emergencyId);
    verify(emergencyRepository).save(argThat(emergency -> 
        emergency.getStatus() == EmergencyStatus.RESOLVED
    ));
  }
}