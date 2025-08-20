package com.bifai.reminder.bifai_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 입력 검증 설정
 * SQL Injection, XSS 등 악의적인 입력 차단
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class InputValidationConfig implements Filter {
  
  // SQL Injection 패턴
  private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
    ".*(;|--|'|\"|\\*|xp_|sp_|exec|execute|union|select|insert|update|delete|drop|create|alter|grant|revoke).*",
    Pattern.CASE_INSENSITIVE
  );
  
  // XSS 패턴
  private static final Pattern XSS_PATTERN = Pattern.compile(
    ".*((<|%3C).*script.*(>|%3E)|(<|%3C).*iframe.*(>|%3E)|javascript:|onerror=|onload=|alert\\(|prompt\\(|confirm\\().*",
    Pattern.CASE_INSENSITIVE
  );
  
  // 경로 탐색 패턴
  private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
    ".*(\\.\\./|\\.\\.|%2e%2e|%252e%252e).*",
    Pattern.CASE_INSENSITIVE
  );
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    
    // 요청 래퍼 생성
    SanitizedRequestWrapper sanitizedRequest = new SanitizedRequestWrapper(httpRequest);
    
    // 위험한 패턴 검사
    if (containsDangerousPattern(httpRequest)) {
      log.warn("위험한 입력 감지 - IP: {}, URI: {}", 
        httpRequest.getRemoteAddr(), 
        httpRequest.getRequestURI());
      
      // 400 Bad Request 반환
      response.getWriter().write("잘못된 요청입니다");
      return;
    }
    
    chain.doFilter(sanitizedRequest, response);
  }
  
  /**
   * 위험한 패턴 검사
   */
  private boolean containsDangerousPattern(HttpServletRequest request) {
    // URL 검사
    String uri = request.getRequestURI();
    if (PATH_TRAVERSAL_PATTERN.matcher(uri).matches()) {
      return true;
    }
    
    // 파라미터 검사
    var parameterMap = request.getParameterMap();
    for (var entry : parameterMap.entrySet()) {
      for (String value : entry.getValue()) {
        if (SQL_INJECTION_PATTERN.matcher(value).matches() ||
            XSS_PATTERN.matcher(value).matches()) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  /**
   * 요청 래퍼 클래스
   * 입력값 정제
   */
  private static class SanitizedRequestWrapper extends HttpServletRequestWrapper {
    
    public SanitizedRequestWrapper(HttpServletRequest request) {
      super(request);
    }
    
    @Override
    public String getParameter(String name) {
      String value = super.getParameter(name);
      return sanitize(value);
    }
    
    @Override
    public String[] getParameterValues(String name) {
      String[] values = super.getParameterValues(name);
      if (values == null) {
        return null;
      }
      
      String[] sanitized = new String[values.length];
      for (int i = 0; i < values.length; i++) {
        sanitized[i] = sanitize(values[i]);
      }
      return sanitized;
    }
    
    /**
     * 입력값 정제
     */
    private String sanitize(String value) {
      if (value == null) {
        return null;
      }
      
      // HTML 특수문자 이스케이프
      value = value.replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;");
      
      return value;
    }
  }
}