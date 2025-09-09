package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.auth.*;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import com.bifai.reminder.bifai_backend.service.cache.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private GuardianRepository guardianRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private BifUserDetailsService userDetailsService;
    
    @Mock
    private RefreshTokenService refreshTokenService;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .name("테스트 사용자")
                .fullName("테스트 사용자")
                .isActive(true)
                .emailVerified(false)
                .build();
        
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        registerRequest.setFullName("새로운 사용자");
        registerRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        registerRequest.setAgreeToTerms(true);
        registerRequest.setAgreeToPrivacyPolicy(true);
        
        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
    }
    
    @Test
    @DisplayName("회원가입 성공")
    void register_Success() {
        // given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        
        // when
        AuthResponse response = authService.register(registerRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).saveRefreshToken(eq(1L), eq("refreshToken"), eq(604800000L));
    }
    
    @Test
    @DisplayName("회원가입 실패 - 중복된 사용자명")
    void register_Fail_DuplicateUsername() {
        // given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 사용자명입니다");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void register_Fail_DuplicateEmail() {
        // given
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 이메일입니다");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("회원가입 실패 - 비밀번호 불일치")
    void register_Fail_PasswordMismatch() {
        // given
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password456"); // 불일치
        registerRequest.setFullName("새로운 사용자");
        registerRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        registerRequest.setAgreeToTerms(true);
        registerRequest.setAgreeToPrivacyPolicy(true);
        
        // when & then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다");
    }
    
    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        Authentication authentication = mock(Authentication.class);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        
        // when
        AuthResponse response = authService.login(loginRequest);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        verify(userRepository).save(testUser); // 마지막 로그인 시간 업데이트
        verify(refreshTokenService).saveRefreshToken(eq(1L), eq("refreshToken"), eq(604800000L));
    }
    
    @Test
    @DisplayName("로그인 실패 - 잘못된 인증 정보")
    void login_Fail_BadCredentials() {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_Success() {
        // given
        String oldRefreshToken = "oldRefreshToken";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(oldRefreshToken);
        
        when(jwtTokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(oldRefreshToken)).thenReturn("refresh");
        when(refreshTokenService.validateRefreshToken(oldRefreshToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("newRefreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);
        when(jwtTokenProvider.getRefreshTokenExpirationMs()).thenReturn(604800000L);
        
        // when
        AuthResponse response = authService.refreshToken(request);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        verify(refreshTokenService).rotateRefreshToken(
                eq(oldRefreshToken), 
                eq("newRefreshToken"), 
                eq(1L), 
                eq(604800000L)
        );
    }
    
    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
    void refreshToken_Fail_InvalidToken() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalidToken");
        
        when(jwtTokenProvider.validateToken("invalidToken")).thenReturn(false);
        
        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 Refresh Token입니다");
    }
    
    @Test
    @DisplayName("토큰 갱신 실패 - Access Token 사용")
    void refreshToken_Fail_WrongTokenType() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("accessToken");
        
        when(jwtTokenProvider.validateToken("accessToken")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("accessToken")).thenReturn("access");
        
        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token이 아닙니다");
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockAuth.getName()).thenReturn("testuser");
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(testUser));
        
        // SecurityContext 모킹
        try (var contextHolderMock = mockStatic(org.springframework.security.core.context.SecurityContextHolder.class)) {
            org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(mockAuth);
            contextHolderMock.when(org.springframework.security.core.context.SecurityContextHolder::getContext).thenReturn(securityContext);
            
            // when
            authService.logout();
            
            // then
            verify(refreshTokenService).deleteRefreshToken(1L);
            contextHolderMock.verify(org.springframework.security.core.context.SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("로그아웃 - 인증 정보 없음")
    void logout_NoAuthentication() {
        // given
        try (var contextHolderMock = mockStatic(org.springframework.security.core.context.SecurityContextHolder.class)) {
            org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            contextHolderMock.when(org.springframework.security.core.context.SecurityContextHolder::getContext).thenReturn(securityContext);
            
            // when
            authService.logout();
            
            // then
            verify(refreshTokenService, never()).deleteRefreshToken(anyLong());
            contextHolderMock.verify(org.springframework.security.core.context.SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("회원가입 실패 - 이용약관 미동의")
    void register_Fail_TermsNotAgreed() {
        // given
        registerRequest.setAgreeToTerms(false);
        
        // when & then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이용약관에 동의해주세요");
    }

    @Test
    @DisplayName("회원가입 실패 - 개인정보처리방침 미동의")
    void register_Fail_PrivacyPolicyNotAgreed() {
        // given
        registerRequest.setAgreeToPrivacyPolicy(false);
        
        // when & then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("개인정보 처리방침에 동의해주세요");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에서 토큰 검증 실패")
    void refreshToken_Fail_TokenNotInRedis() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validToken");
        
        when(jwtTokenProvider.validateToken("validToken")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("validToken")).thenReturn("refresh");
        when(refreshTokenService.validateRefreshToken("validToken")).thenReturn(null);
        
        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 Refresh Token입니다");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 비활성화된 사용자")
    void refreshToken_Fail_InactiveUser() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validToken");
        User inactiveUser = User.builder()
                .userId(1L)
                .username("testuser")
                .email("test@example.com")
                .isActive(false) // 비활성화
                .build();
        
        when(jwtTokenProvider.validateToken("validToken")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("validToken")).thenReturn("refresh");
        when(refreshTokenService.validateRefreshToken("validToken")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveUser));
        
        // when & then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("로그인 실패 - 사용자를 찾을 수 없음")
    void login_Fail_UserNotFound() {
        // given
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail()))
                .thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다");
        
        verify(refreshTokenService, never()).saveRefreshToken(anyLong(), anyString(), anyLong());
    }
}