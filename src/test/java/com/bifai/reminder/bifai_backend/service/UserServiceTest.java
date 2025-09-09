package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .name("테스트사용자")
                .cognitiveLevel(User.CognitiveLevel.MILD)
                .isActive(true)
                .build();
        
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("현재 사용자 조회 성공")
    void getCurrentUser_Success() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(1L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getCurrentUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("인증 정보가 없는 경우 예외 발생")
    void getCurrentUser_NoAuthentication_ThrowsException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(SecurityException.class)
                .hasMessage("인증 정보가 없습니다");
    }

    @Test
    @DisplayName("Principal이 null인 경우 예외 발생")
    void getCurrentUser_NullPrincipal_ThrowsException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(SecurityException.class)
                .hasMessage("인증 정보가 올바르지 않습니다");
    }

    @Test
    @DisplayName("Principal이 BifUserDetails가 아닌 경우 예외 발생")
    void getCurrentUser_WrongPrincipalType_ThrowsException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("testuser");

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(SecurityException.class)
                .hasMessage("올바르지 않은 인증 타입입니다");
    }

    @Test
    @DisplayName("UserId가 null인 경우 예외 발생")
    void getCurrentUser_NullUserId_ThrowsException() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(null);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(SecurityException.class)
                .hasMessage("사용자 ID를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("사용자를 찾을 수 없는 경우 예외 발생")
    void getCurrentUser_UserNotFound_ThrowsException() {
        // Given
        BifUserDetails userDetails = mock(BifUserDetails.class);
        when(userDetails.getUserId()).thenReturn(999L);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("이메일로 사용자 찾기 성공")
    void findByEmail_Success() {
        // Given
        String email = "TEST@EXAMPLE.COM";
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(testUser.getEmail());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("이메일이 null인 경우 빈 Optional 반환")
    void findByEmail_NullEmail_ReturnsEmpty() {
        // When
        Optional<User> result = userService.findByEmail(null);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("이메일이 빈 문자열인 경우 빈 Optional 반환")
    void findByEmail_EmptyEmail_ReturnsEmpty() {
        // When
        Optional<User> result = userService.findByEmail("   ");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void getUserById_Success() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("사용자 ID가 null인 경우 예외 발생")
    void getUserById_NullId_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> userService.getUserById(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("User ID cannot be null");
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 시 예외 발생")
    void getUserById_NotFound_ThrowsException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("전체 사용자 목록 조회 성공")
    void getAllUsers_Success() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<User> result = userService.getAllUsers(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testUser);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자 비활성화 성공")
    void deactivateUser_Success() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.deactivateUser(userId);

        // Then
        assertThat(testUser.getIsActive()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("사용자 활성화 성공")
    void activateUser_Success() {
        // Given
        Long userId = 1L;
        testUser.setIsActive(false); // 비활성화 상태로 설정
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        userService.activateUser(userId);

        // Then
        assertThat(testUser.getIsActive()).isTrue();
        verify(userRepository).save(testUser);
    }
}