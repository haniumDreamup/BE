package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.auth.AuthResponse;
import com.bifai.reminder.bifai_backend.dto.auth.LoginRequest;
import com.bifai.reminder.bifai_backend.dto.auth.RegisterRequest;
import com.bifai.reminder.bifai_backend.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 단독 테스트 (Spring Security 없이)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("인증 컨트롤러 단독 테스트")
class AuthControllerStandaloneTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthController authController;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules(); // Java 8 time module 등록
  }

  @Test
  @DisplayName("회원가입 성공")
  void register_Success() throws Exception {
    // given
    RegisterRequest request = new RegisterRequest();
    request.setUsername("testuser");
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setConfirmPassword("password123");
    request.setFullName("테스트 사용자");
    request.setBirthDate(LocalDate.of(1990, 1, 1));
    request.setAgreeToTerms(true);
    request.setAgreeToPrivacyPolicy(true);

    AuthResponse response = AuthResponse.builder()
        .accessToken("access-token")
        .refreshToken("refresh-token")
        .tokenType("Bearer")
        .accessTokenExpiresIn(900000L)
        .refreshTokenExpiresIn(604800000L)
        .user(AuthResponse.UserInfo.builder()
            .userId(1L)
            .username("testuser")
            .email("test@example.com")
            .fullName("테스트 사용자")
            .build())
        .loginTime(LocalDateTime.now())
        .build();

    when(authService.register(any(RegisterRequest.class))).thenReturn(response);

    // when & then
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"))
        .andExpect(jsonPath("$.data.accessToken").value("access-token"));
  }

  @Test
  @DisplayName("로그인 성공")
  void login_Success() throws Exception {
    // given
    LoginRequest request = new LoginRequest();
    request.setUsernameOrEmail("testuser");
    request.setPassword("password123");

    AuthResponse response = AuthResponse.builder()
        .accessToken("access-token")
        .refreshToken("refresh-token")
        .tokenType("Bearer")
        .accessTokenExpiresIn(900000L)
        .refreshTokenExpiresIn(604800000L)
        .user(AuthResponse.UserInfo.builder()
            .userId(1L)
            .username("testuser")
            .email("test@example.com")
            .fullName("테스트 사용자")
            .build())
        .loginTime(LocalDateTime.now())
        .build();

    when(authService.login(any(LoginRequest.class))).thenReturn(response);

    // when & then
    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("로그인이 완료되었습니다"));
  }

  @Test
  @DisplayName("회원가입 실패 - 중복된 사용자명")
  void register_DuplicateUsername() throws Exception {
    // given
    RegisterRequest request = new RegisterRequest();
    request.setUsername("testuser");
    request.setEmail("test@example.com");
    request.setPassword("password123");
    request.setConfirmPassword("password123");
    request.setFullName("테스트 사용자");
    request.setBirthDate(LocalDate.of(1990, 1, 1));
    request.setAgreeToTerms(true);
    request.setAgreeToPrivacyPolicy(true);

    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(new IllegalArgumentException("이미 사용 중인 사용자명입니다"));

    // when & then
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.message").value("이미 사용 중인 사용자명입니다"));
  }
}