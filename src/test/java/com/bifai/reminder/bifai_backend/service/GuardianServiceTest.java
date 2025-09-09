package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.guardian.GuardianRequest;
import com.bifai.reminder.bifai_backend.dto.guardian.GuardianPermissionRequest;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GuardianService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianService 단위 테스트")
class GuardianServiceTest {

    @Mock
    private GuardianRepository guardianRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private GuardianService guardianService;

    private User testUser;
    private User guardianUser;
    private Guardian testGuardian;
    private GuardianRequest guardianRequest;
    private Role guardianRole;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .name("테스트사용자")
                .cognitiveLevel(User.CognitiveLevel.MILD)
                .isActive(true)
                .roles(new HashSet<>())
                .build();
        
        // 보호자 사용자 생성
        guardianUser = User.builder()
                .userId(2L)
                .username("guardian")
                .email("guardian@example.com")
                .name("보호자")
                .cognitiveLevel(User.CognitiveLevel.MODERATE)
                .isActive(true)
                .roles(new HashSet<>())
                .build();
        
        // 보호자 역할 생성
        guardianRole = Role.builder()
                .id(1L)
                .name("ROLE_GUARDIAN")
                .build();
        
        // 보호자 관계 생성
        testGuardian = Guardian.builder()
                .id(1L)
                .user(testUser)
                .guardianUser(guardianUser)
                .name("보호자")
                .relationship("가족")
                .primaryPhone("010-1234-5678")
                .email("guardian@example.com")
                .canViewLocation(true)
                .canModifySettings(false)
                .canReceiveAlerts(true)
                .isPrimary(false)
                .approvalStatus(Guardian.ApprovalStatus.PENDING)
                .isActive(true)
                .build();
        
        // 보호자 요청 생성
        guardianRequest = new GuardianRequest();
        guardianRequest.setGuardianEmail("guardian@example.com");
        guardianRequest.setGuardianName("보호자");
        guardianRequest.setRelationship("가족");
        guardianRequest.setPrimaryPhone("010-1234-5678");
        guardianRequest.setCanViewLocation(true);
        guardianRequest.setCanModifySettings(false);
        guardianRequest.setCanReceiveAlerts(true);
        
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("현재 사용자의 보호자 목록 조회 성공")
    void getMyGuardians_Success() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(guardianRepository.findByUserAndIsActiveTrue(testUser))
                .thenReturn(Arrays.asList(testGuardian));

        // When
        List<Guardian> result = guardianService.getMyGuardians();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testGuardian);
    }

    @Test
    @DisplayName("현재 보호자가 보호 중인 사용자 목록 조회 성공")
    void getProtectedUsers_Success() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(2L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guardianUser));
        when(guardianRepository.findByGuardianUserAndIsActiveTrue(guardianUser))
                .thenReturn(Arrays.asList(testGuardian));

        // When
        List<Guardian> result = guardianService.getProtectedUsers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testGuardian);
    }

    @Test
    @DisplayName("보호자 등록 요청 성공")
    void requestGuardian_Success() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("guardian@example.com")).thenReturn(Optional.of(guardianUser));
        when(guardianRepository.existsByUserAndGuardianUser(testUser, guardianUser)).thenReturn(false);
        when(roleRepository.findByName("ROLE_GUARDIAN")).thenReturn(Optional.of(guardianRole));
        when(guardianRepository.save(any(Guardian.class))).thenReturn(testGuardian);
        when(userRepository.save(guardianUser)).thenReturn(guardianUser);

        // When
        Guardian result = guardianService.requestGuardian(guardianRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getGuardianUser()).isEqualTo(guardianUser);
        assertThat(result.getApprovalStatus()).isEqualTo(Guardian.ApprovalStatus.PENDING);
        verify(guardianRepository).save(any(Guardian.class));
        verify(userRepository).save(guardianUser);
    }

    @Test
    @DisplayName("보호자 등록 요청 실패 - 존재하지 않는 이메일")
    void requestGuardian_EmailNotFound_ThrowsException() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("guardian@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> guardianService.requestGuardian(guardianRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 이메일의 사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("보호자 등록 요청 실패 - 이미 등록된 보호자")
    void requestGuardian_AlreadyExists_ThrowsException() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("guardian@example.com")).thenReturn(Optional.of(guardianUser));
        when(guardianRepository.existsByUserAndGuardianUser(testUser, guardianUser)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> guardianService.requestGuardian(guardianRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 보호자입니다");
    }

    @Test
    @DisplayName("보호자 등록 요청 실패 - 자기 자신을 보호자로 등록")
    void requestGuardian_SelfGuardian_ThrowsException() {
        // Given
        guardianRequest.setGuardianEmail("test@example.com"); // 같은 이메일
        
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(guardianRepository.existsByUserAndGuardianUser(testUser, testUser)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> guardianService.requestGuardian(guardianRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신을 보호자로 등록할 수 없습니다");
    }

    @Test
    @DisplayName("보호자 요청 승인 성공")
    void approveGuardian_Success() {
        // Given
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));
        when(guardianRepository.save(testGuardian)).thenReturn(testGuardian);

        // When
        Guardian result = guardianService.approveGuardian(1L);

        // Then
        assertThat(result.getApprovalStatus()).isEqualTo(Guardian.ApprovalStatus.APPROVED);
        assertThat(result.getApprovedAt()).isNotNull();
        verify(guardianRepository).save(testGuardian);
    }

    @Test
    @DisplayName("보호자 요청 승인 실패 - 이미 처리된 요청")
    void approveGuardian_AlreadyProcessed_ThrowsException() {
        // Given
        testGuardian.setApprovalStatus(Guardian.ApprovalStatus.APPROVED);
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));

        // When & Then
        assertThatThrownBy(() -> guardianService.approveGuardian(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 처리된 요청입니다");
    }

    @Test
    @DisplayName("보호자 요청 거절 성공")
    void rejectGuardian_Success() {
        // Given
        String reason = "부적절한 요청";
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));
        when(guardianRepository.save(testGuardian)).thenReturn(testGuardian);

        // When
        guardianService.rejectGuardian(1L, reason);

        // Then
        assertThat(testGuardian.getApprovalStatus()).isEqualTo(Guardian.ApprovalStatus.REJECTED);
        assertThat(testGuardian.getRejectionReason()).isEqualTo(reason);
        assertThat(testGuardian.getIsActive()).isFalse();
        verify(guardianRepository).save(testGuardian);
    }

    @Test
    @DisplayName("보호자 권한 수정 성공")
    void updatePermissions_Success() {
        // Given
        GuardianPermissionRequest request = new GuardianPermissionRequest();
        request.setCanViewLocation(false);
        request.setCanModifySettings(true);
        request.setCanReceiveAlerts(false);
        request.setEmergencyPriority(1);
        
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));
        when(guardianRepository.save(testGuardian)).thenReturn(testGuardian);

        // When
        Guardian result = guardianService.updatePermissions(1L, request);

        // Then
        assertThat(result.getCanViewLocation()).isFalse();
        assertThat(result.getCanModifySettings()).isTrue();
        assertThat(result.getCanReceiveAlerts()).isFalse();
        assertThat(result.getEmergencyPriority()).isEqualTo(1);
        verify(guardianRepository).save(testGuardian);
    }

    @Test
    @DisplayName("보호자 삭제 성공")
    void removeGuardian_Success() {
        // Given
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));
        when(guardianRepository.save(testGuardian)).thenReturn(testGuardian);

        // When
        guardianService.removeGuardian(1L);

        // Then
        assertThat(testGuardian.getIsActive()).isFalse();
        assertThat(testGuardian.getTerminatedAt()).isNotNull();
        verify(guardianRepository).save(testGuardian);
    }

    @Test
    @DisplayName("보호 관계 해제 성공")
    void removeRelationship_Success() {
        // Given
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));
        when(guardianRepository.save(testGuardian)).thenReturn(testGuardian);

        // When
        guardianService.removeRelationship(1L);

        // Then
        assertThat(testGuardian.getIsActive()).isFalse();
        assertThat(testGuardian.getTerminatedAt()).isNotNull();
        assertThat(testGuardian.getTerminatedBy()).isEqualTo("GUARDIAN");
        verify(guardianRepository).save(testGuardian);
    }

    @Test
    @DisplayName("특정 사용자의 보호자인지 확인 - 참")
    void isGuardianOf_True() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(2L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guardianUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(guardianRepository.existsByUserAndGuardianUserAndIsActiveTrueAndApprovalStatus(
                testUser, guardianUser, Guardian.ApprovalStatus.APPROVED)).thenReturn(true);

        // When
        boolean result = guardianService.isGuardianOf(1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("보호자 요청 승인 권한 확인 - 참")
    void canApproveGuardian_True() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(2L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guardianUser));
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));

        // When
        boolean result = guardianService.canApproveGuardian(1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("보호자 요청 승인 권한 확인 - 거짓 (존재하지 않는 보호자)")
    void canApproveGuardian_False_NotFound() {
        // Given
        when(guardianRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        boolean result = guardianService.canApproveGuardian(1L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("나의 보호자인지 확인 - 참")
    void isMyGuardian_True() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));

        // When
        boolean result = guardianService.isMyGuardian(1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("보호 관계 해제 권한 확인 - 참")
    void canRemoveRelationship_True() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(2L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(2L)).thenReturn(Optional.of(guardianUser));
        when(guardianRepository.findById(1L)).thenReturn(Optional.of(testGuardian));

        // When
        boolean result = guardianService.canRemoveRelationship(1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 보호자 조회 시 예외 발생")
    void guardianNotFound_ThrowsException() {
        // Given
        when(guardianRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> guardianService.approveGuardian(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("보호자 관계를 찾을 수 없습니다");
    }
}