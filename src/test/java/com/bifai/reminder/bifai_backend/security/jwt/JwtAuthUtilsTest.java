package com.bifai.reminder.bifai_backend.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthUtilsTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;
  
  @Mock
  private HttpServletRequest request;
  
  @Mock
  private ServletRequestAttributes requestAttributes;
  
  @Mock
  private SecurityContext securityContext;
  
  @Mock
  private Authentication authentication;
  
  @InjectMocks
  private JwtAuthUtils jwtAuthUtils;
  
  private final String TEST_TOKEN = "Bearer eyJhbGciOiJIUzUxMiJ9.test.token";
  private final String RAW_TOKEN = "eyJhbGciOiJIUzUxMiJ9.test.token";
  private final Long TEST_USER_ID = 123L;
  private final String TEST_USERNAME = "testuser@example.com";
  
  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }
  
  @Test
  void getCurrentUserId_WithValidToken_ReturnsUserId() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(RAW_TOKEN);
    when(jwtTokenProvider.getUserId(RAW_TOKEN)).thenReturn(TEST_USER_ID);

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    Long userId = jwtAuthUtils.getCurrentUserId();

    // Then
    assertNotNull(userId);
    assertEquals(TEST_USER_ID, userId);
    verify(jwtTokenProvider).resolveToken(request);
    verify(jwtTokenProvider).getUserId(RAW_TOKEN);
  }
  
  @Test
  void getCurrentUserId_WithNoToken_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    Long userId = jwtAuthUtils.getCurrentUserId();

    // Then
    assertNull(userId);
    verify(jwtTokenProvider).resolveToken(request);
    verify(jwtTokenProvider, never()).getUserId(anyString());
  }
  
  @Test
  void getCurrentUserId_WithInvalidToken_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn("InvalidToken");
    when(jwtTokenProvider.getUserId("InvalidToken")).thenThrow(new RuntimeException("Invalid token"));

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    Long userId = jwtAuthUtils.getCurrentUserId();

    // Then
    assertNull(userId);
    verify(jwtTokenProvider).resolveToken(request);
    verify(jwtTokenProvider).getUserId("InvalidToken");
  }
  
  @Test
  void getCurrentUsername_WithValidToken_ReturnsUsername() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(RAW_TOKEN);
    when(jwtTokenProvider.getUsernameFromToken(RAW_TOKEN)).thenReturn(TEST_USERNAME);

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    String username = jwtAuthUtils.getCurrentUsername();

    // Then
    assertNotNull(username);
    assertEquals(TEST_USERNAME, username);
    verify(jwtTokenProvider).resolveToken(request);
    verify(jwtTokenProvider).getUsernameFromToken(RAW_TOKEN);
  }
  
  @Test
  void getCurrentUsername_WithNoToken_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    String username = jwtAuthUtils.getCurrentUsername();

    // Then
    assertNull(username);
    verify(jwtTokenProvider).resolveToken(request);
    verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
  }
  
  @Test
  void getCurrentToken_WithBearerToken_ReturnsRawToken() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(RAW_TOKEN);

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    String token = jwtAuthUtils.getCurrentToken();

    // Then
    assertNotNull(token);
    assertEquals(RAW_TOKEN, token);
    verify(jwtTokenProvider).resolveToken(request);
  }
  
  @Test
  void getCurrentToken_WithoutBearerPrefix_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(null);

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    String token = jwtAuthUtils.getCurrentToken();

    // Then
    assertNull(token);
    verify(jwtTokenProvider).resolveToken(request);
  }
  
  @Test
  void getCurrentToken_WithNoRequestContext_ReturnsNull() {
    // Given
    RequestContextHolder.setRequestAttributes(null);
    
    // When
    String token = jwtAuthUtils.getCurrentToken();
    
    // Then
    assertNull(token);
  }
  
  @Test
  void getCurrentUserId_WithExceptionThrown_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(jwtTokenProvider.resolveToken(request)).thenReturn(RAW_TOKEN);
    when(jwtTokenProvider.getUserId(RAW_TOKEN)).thenThrow(new RuntimeException("Token parsing error"));

    RequestContextHolder.setRequestAttributes(requestAttributes);

    // When
    Long userId = jwtAuthUtils.getCurrentUserId();

    // Then
    assertNull(userId);
    verify(jwtTokenProvider).resolveToken(request);
    verify(jwtTokenProvider).getUserId(RAW_TOKEN);
  }
}