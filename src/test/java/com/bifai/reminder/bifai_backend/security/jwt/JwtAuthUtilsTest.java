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
    when(request.getHeader("Authorization")).thenReturn(TEST_TOKEN);
    when(jwtTokenProvider.getUserId(RAW_TOKEN)).thenReturn(TEST_USER_ID);
    
    RequestContextHolder.setRequestAttributes(requestAttributes);
    
    // When
    Long userId = jwtAuthUtils.getCurrentUserId();
    
    // Then
    assertNotNull(userId);
    assertEquals(TEST_USER_ID, userId);
    verify(jwtTokenProvider).getUserId(RAW_TOKEN);
  }
  
  @Test
  void getCurrentUserId_WithNoToken_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(request.getHeader("Authorization")).thenReturn(null);
    
    RequestContextHolder.setRequestAttributes(requestAttributes);
    
    // When
    Long userId = jwtAuthUtils.getCurrentUserId();
    
    // Then
    assertNull(userId);
    verify(jwtTokenProvider, never()).getUserId(anyString());
  }
  
  @Test
  void getCurrentUserId_WithInvalidToken_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(request.getHeader("Authorization")).thenReturn("InvalidToken");
    
    RequestContextHolder.setRequestAttributes(requestAttributes);
    
    // When
    Long userId = jwtAuthUtils.getCurrentUserId();
    
    // Then
    assertNull(userId);
  }
  
  @Test
  void getCurrentUsername_WithValidAuthentication_ReturnsUsername() {
    // Given
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn(TEST_USERNAME);
    
    SecurityContextHolder.setContext(securityContext);
    
    // When
    String username = jwtAuthUtils.getCurrentUsername();
    
    // Then
    assertNotNull(username);
    assertEquals(TEST_USERNAME, username);
  }
  
  @Test
  void getCurrentUsername_WithNoAuthentication_ReturnsNull() {
    // Given
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);
    
    // When
    String username = jwtAuthUtils.getCurrentUsername();
    
    // Then
    assertNull(username);
  }
  
  @Test
  void getCurrentToken_WithBearerToken_ReturnsRawToken() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(request.getHeader("Authorization")).thenReturn(TEST_TOKEN);
    
    RequestContextHolder.setRequestAttributes(requestAttributes);
    
    // When
    String token = jwtAuthUtils.getCurrentToken();
    
    // Then
    assertNotNull(token);
    assertEquals(RAW_TOKEN, token);
  }
  
  @Test
  void getCurrentToken_WithoutBearerPrefix_ReturnsNull() {
    // Given
    when(requestAttributes.getRequest()).thenReturn(request);
    when(request.getHeader("Authorization")).thenReturn(RAW_TOKEN);
    
    RequestContextHolder.setRequestAttributes(requestAttributes);
    
    // When
    String token = jwtAuthUtils.getCurrentToken();
    
    // Then
    assertNull(token);
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
    when(request.getHeader("Authorization")).thenReturn(TEST_TOKEN);
    when(jwtTokenProvider.getUserId(RAW_TOKEN)).thenThrow(new RuntimeException("Token parsing error"));
    
    RequestContextHolder.setRequestAttributes(requestAttributes);
    
    // When
    Long userId = jwtAuthUtils.getCurrentUserId();
    
    // Then
    assertNull(userId);
  }
}