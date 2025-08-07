package com.bifai.reminder.bifai_backend.security.oauth2;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private Authentication authentication;

  @Mock
  private OAuth2UserPrincipal principal;

  @Mock
  private RedirectStrategy redirectStrategy;

  @InjectMocks
  private OAuth2AuthenticationSuccessHandler successHandler;

  private User user;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(successHandler, "redirectUri", "http://localhost:3000/auth/callback");
    successHandler.setRedirectStrategy(redirectStrategy);
    
    user = User.builder()
      .userId(1L)
      .email("test@example.com")
      .name("Test User")
      .isActive(true)
      .build();
  }

  @Test
  @DisplayName("OAuth2 로그인 성공 시 토큰 발급 및 리다이렉트")
  void testOnAuthenticationSuccess() throws Exception {
    // Given
    String accessToken = "test-access-token";
    String refreshToken = "test-refresh-token";
    
    when(authentication.getPrincipal()).thenReturn(principal);
    when(principal.getUser()).thenReturn(user);
    when(tokenProvider.createAccessToken(user)).thenReturn(accessToken);
    when(tokenProvider.createRefreshToken(user)).thenReturn(refreshToken);

    // When
    successHandler.onAuthenticationSuccess(request, response, authentication);

    // Then
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());
    
    String redirectUrl = urlCaptor.getValue();
    assertThat(redirectUrl).contains("http://localhost:3000/auth/callback");
    assertThat(redirectUrl).contains("accessToken=" + accessToken);
    assertThat(redirectUrl).contains("refreshToken=" + refreshToken);
    
    verify(tokenProvider).createAccessToken(user);
    verify(tokenProvider).createRefreshToken(user);
  }
}