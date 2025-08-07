package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.emergencycontact.EmergencyContactRequest;
import com.bifai.reminder.bifai_backend.dto.emergencycontact.EmergencyContactResponse;
import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.DuplicateResourceException;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.EmergencyContactRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 긴급 연락처 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("긴급 연락처 서비스 테스트")
class EmergencyContactServiceTest {

  @Mock
  private EmergencyContactRepository emergencyContactRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private EmergencyContactService emergencyContactService;

  private User testUser;
  private EmergencyContactRequest testRequest;
  private EmergencyContact testContact;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .userId(1L)
        .username("testuser")
        .email("test@example.com")
        .build();

    testRequest = EmergencyContactRequest.builder()
        .name("김보호")
        .relationship("아버지")
        .phoneNumber("010-1234-5678")
        .email("father@example.com")
        .contactType(EmergencyContact.ContactType.FAMILY)
        .priority(1)
        .isPrimary(true)
        .canReceiveAlerts(true)
        .build();

    testContact = EmergencyContact.builder()
        .id(1L)
        .user(testUser)
        .name("김보호")
        .relationship("아버지")
        .phoneNumber("010-1234-5678")
        .email("father@example.com")
        .contactType(EmergencyContact.ContactType.FAMILY)
        .priority(1)
        .isPrimary(true)
        .isActive(true)
        .canReceiveAlerts(true)
        .build();
  }

  @Test
  @DisplayName("긴급 연락처 생성 성공")
  void createEmergencyContact_Success() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.countByUser(testUser)).thenReturn(0L);
    when(emergencyContactRepository.existsByUserAndPhoneNumber(testUser, "010-1234-5678"))
        .thenReturn(false);
    when(emergencyContactRepository.save(any(EmergencyContact.class)))
        .thenReturn(testContact);

    // when
    EmergencyContactResponse response = emergencyContactService.createEmergencyContact(1L, testRequest);

    // then
    assertNotNull(response);
    assertEquals("김보호", response.getName());
    assertEquals("아버지", response.getRelationship());
    verify(emergencyContactRepository, times(1)).save(any(EmergencyContact.class));
  }

  @Test
  @DisplayName("중복된 전화번호로 연락처 생성 실패")
  void createEmergencyContact_DuplicatePhoneNumber() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.countByUser(testUser)).thenReturn(1L);
    when(emergencyContactRepository.existsByUserAndPhoneNumber(testUser, "010-1234-5678"))
        .thenReturn(true);

    // when & then
    assertThrows(DuplicateResourceException.class, () -> 
        emergencyContactService.createEmergencyContact(1L, testRequest)
    );
    verify(emergencyContactRepository, never()).save(any());
  }

  @Test
  @DisplayName("최대 연락처 개수 초과 시 생성 실패")
  void createEmergencyContact_MaxContactsExceeded() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.countByUser(testUser)).thenReturn(20L);

    // when & then
    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
        emergencyContactService.createEmergencyContact(1L, testRequest)
    );
    assertTrue(exception.getMessage().contains("최대"));
    verify(emergencyContactRepository, never()).save(any());
  }

  @Test
  @DisplayName("긴급 연락처 수정 성공")
  void updateEmergencyContact_Success() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByIdAndUser(1L, testUser))
        .thenReturn(Optional.of(testContact));
    when(emergencyContactRepository.save(any(EmergencyContact.class)))
        .thenReturn(testContact);

    EmergencyContactRequest updateRequest = EmergencyContactRequest.builder()
        .name("김새보호")
        .relationship("어머니")
        .phoneNumber("010-9876-5432")
        .email("mother@example.com")
        .contactType(EmergencyContact.ContactType.FAMILY)
        .build();

    // when
    EmergencyContactResponse response = emergencyContactService.updateEmergencyContact(1L, 1L, updateRequest);

    // then
    assertNotNull(response);
    verify(emergencyContactRepository, times(1)).save(any(EmergencyContact.class));
  }

  @Test
  @DisplayName("존재하지 않는 연락처 수정 실패")
  void updateEmergencyContact_NotFound() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByIdAndUser(999L, testUser))
        .thenReturn(Optional.empty());

    // when & then
    assertThrows(ResourceNotFoundException.class, () -> 
        emergencyContactService.updateEmergencyContact(1L, 999L, testRequest)
    );
    verify(emergencyContactRepository, never()).save(any());
  }

  @Test
  @DisplayName("긴급 연락처 삭제 성공")
  void deleteEmergencyContact_Success() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByIdAndUser(1L, testUser))
        .thenReturn(Optional.of(testContact));
    when(emergencyContactRepository.findByUserOrderByPriorityAsc(testUser))
        .thenReturn(Arrays.asList());

    // when
    assertDoesNotThrow(() -> 
        emergencyContactService.deleteEmergencyContact(1L, 1L)
    );

    // then
    verify(emergencyContactRepository, times(1)).delete(testContact);
  }

  @Test
  @DisplayName("사용자의 모든 긴급 연락처 조회")
  void getUserEmergencyContacts_Success() {
    // given
    List<EmergencyContact> contacts = Arrays.asList(testContact);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByUserOrderByPriorityAsc(testUser))
        .thenReturn(contacts);

    // when
    List<EmergencyContactResponse> responses = emergencyContactService.getUserEmergencyContacts(1L);

    // then
    assertNotNull(responses);
    assertEquals(1, responses.size());
    assertEquals("김보호", responses.get(0).getName());
  }

  @Test
  @DisplayName("활성화된 연락처만 조회")
  void getActiveContacts_Success() {
    // given
    List<EmergencyContact> activeContacts = Arrays.asList(testContact);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByUserAndIsActiveTrueOrderByPriorityAsc(testUser))
        .thenReturn(activeContacts);

    // when
    List<EmergencyContactResponse> responses = emergencyContactService.getActiveContacts(1L);

    // then
    assertNotNull(responses);
    assertEquals(1, responses.size());
    assertTrue(responses.get(0).getIsActive());
  }

  @Test
  @DisplayName("의료진 연락처 조회")
  void getMedicalContacts_Success() {
    // given
    EmergencyContact medicalContact = EmergencyContact.builder()
        .id(2L)
        .user(testUser)
        .name("이의사")
        .relationship("담당의사")
        .phoneNumber("010-5555-6666")
        .contactType(EmergencyContact.ContactType.DOCTOR)
        .isMedicalProfessional(true)
        .specialization("신경과")
        .hospitalName("서울대병원")
        .isActive(true)
        .canReceiveAlerts(true)
        .build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findMedicalContacts(testUser))
        .thenReturn(Arrays.asList(medicalContact));

    // when
    List<EmergencyContactResponse> responses = emergencyContactService.getMedicalContacts(1L);

    // then
    assertNotNull(responses);
    assertEquals(1, responses.size());
    assertEquals(EmergencyContact.ContactType.DOCTOR, responses.get(0).getContactType());
    assertTrue(responses.get(0).getIsMedicalProfessional());
  }

  @Test
  @DisplayName("연락처 활성화/비활성화 토글")
  void toggleContactActive_Success() {
    // given
    testContact.setIsActive(true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByIdAndUser(1L, testUser))
        .thenReturn(Optional.of(testContact));
    when(emergencyContactRepository.save(any(EmergencyContact.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    EmergencyContactResponse response = emergencyContactService.toggleContactActive(1L, 1L);

    // then
    assertNotNull(response);
    assertFalse(testContact.getIsActive());
    verify(emergencyContactRepository, times(1)).save(testContact);
  }

  @Test
  @DisplayName("연락처 검증 성공")
  void verifyContact_Success() {
    // given
    testContact.setVerificationCode("123456");
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByUserAndVerificationCode(testUser, "123456"))
        .thenReturn(Optional.of(testContact));
    when(emergencyContactRepository.save(any(EmergencyContact.class)))
        .thenReturn(testContact);

    // when
    EmergencyContactResponse response = emergencyContactService.verifyContact(1L, 1L, "123456");

    // then
    assertNotNull(response);
    assertTrue(testContact.getVerified());
    assertNull(testContact.getVerificationCode());
    verify(emergencyContactRepository, times(1)).save(testContact);
  }

  @Test
  @DisplayName("잘못된 검증 코드로 검증 실패")
  void verifyContact_InvalidCode() {
    // given
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(emergencyContactRepository.findByUserAndVerificationCode(testUser, "wrong"))
        .thenReturn(Optional.empty());

    // when & then
    assertThrows(ResourceNotFoundException.class, () -> 
        emergencyContactService.verifyContact(1L, 1L, "wrong")
    );
    verify(emergencyContactRepository, never()).save(any());
  }
}