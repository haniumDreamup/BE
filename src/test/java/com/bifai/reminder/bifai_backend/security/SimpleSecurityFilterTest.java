package com.bifai.reminder.bifai_backend.security;

import com.bifai.reminder.bifai_backend.config.InputValidationConfig;
import com.bifai.reminder.bifai_backend.config.SecurityHeaderConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 보안 필터 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("보안 필터 단위 테스트")
class SimpleSecurityFilterTest {
  
  @Mock
  private HttpServletRequest request;
  
  @Mock
  private HttpServletResponse response;
  
  @Mock
  private FilterChain filterChain;
  
  private SecurityHeaderConfig.SecurityHeaderFilter securityHeaderFilter;
  private InputValidationConfig inputValidationFilter;
  
  @BeforeEach
  void setUp() {
    securityHeaderFilter = new SecurityHeaderConfig.SecurityHeaderFilter();
    inputValidationFilter = new InputValidationConfig();
  }
  
  @Test
  @DisplayName("보안 헤더가 올바르게 설정되는지 확인")
  void testSecurityHeaders() throws Exception {
    // when
    securityHeaderFilter.doFilter(request, response, filterChain);
    
    // then
    verify(response).setHeader("X-XSS-Protection", "1; mode=block");
    verify(response).setHeader("X-Content-Type-Options", "nosniff");
    verify(response).setHeader("X-Frame-Options", "DENY");
    verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
    verify(response).setHeader(eq("Content-Security-Policy"), anyString());
    verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    verify(response).setHeader(eq("Permissions-Policy"), anyString());
    verify(filterChain).doFilter(request, response);
  }
  
  @Test
  @DisplayName("SQL Injection 패턴이 차단되는지 확인")
  void testSqlInjectionPrevention() throws Exception {
    // given
    when(request.getRequestURI()).thenReturn("/api/users");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    Map<String, String[]> params = new HashMap<>();
    params.put("name", new String[]{"admin'; DROP TABLE users; --"});
    when(request.getParameterMap()).thenReturn(params);
    
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
    
    // when
    inputValidationFilter.doFilter(request, response, filterChain);
    
    // then
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json;charset=UTF-8");
    verify(filterChain, never()).doFilter(any(), any());
  }
  
  @Test
  @DisplayName("XSS 공격 패턴이 차단되는지 확인")
  void testXssPrevention() throws Exception {
    // given
    when(request.getRequestURI()).thenReturn("/api/comments");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    Map<String, String[]> params = new HashMap<>();
    params.put("text", new String[]{"<script>alert('XSS')</script>"});
    when(request.getParameterMap()).thenReturn(params);
    
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
    
    // when
    inputValidationFilter.doFilter(request, response, filterChain);
    
    // then
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json;charset=UTF-8");
    verify(filterChain, never()).doFilter(any(), any());
  }
  
  @Test
  @DisplayName("Path Traversal 공격이 차단되는지 확인")
  void testPathTraversalPrevention() throws Exception {
    // given
    when(request.getRequestURI()).thenReturn("/api/files/../../../etc/passwd");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getParameterMap()).thenReturn(new HashMap<>());
    
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);
    
    // when
    inputValidationFilter.doFilter(request, response, filterChain);
    
    // then
    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    verify(response).setContentType("application/json;charset=UTF-8");
    verify(filterChain, never()).doFilter(any(), any());
  }
  
  @Test
  @DisplayName("정상적인 요청은 통과하는지 확인")
  void testNormalRequestPassesThrough() throws Exception {
    // given
    when(request.getRequestURI()).thenReturn("/api/users");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    
    Map<String, String[]> params = new HashMap<>();
    params.put("name", new String[]{"홍길동"});
    when(request.getParameterMap()).thenReturn(params);
    
    // when
    inputValidationFilter.doFilter(request, response, filterChain);
    
    // then
    verify(filterChain).doFilter(any(), eq(response));
    verify(response, never()).setStatus(HttpServletResponse.SC_BAD_REQUEST);
  }
}