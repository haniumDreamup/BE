package com.bifai.reminder.bifai_backend.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * JWT 인증 유틸리티
 * 컨트롤러에서 JWT 토큰으로부터 사용자 정보를 추출하는 편의 메서드 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthUtils {
  
  private final JwtTokenProvider jwtTokenProvider;
  
  /**
   * 현재 요청에서 사용자 ID를 추출
   * @return 토큰에서 추출된 사용자 ID, 없으면 null
   */
  public Long getCurrentUserId() {
    try {
      String token = getCurrentToken();
      if (token == null) {
        log.warn("No JWT token found in current request");
        return null;
      }
      
      return jwtTokenProvider.getUserId(token);
    } catch (Exception e) {
      log.error("Failed to extract user ID from current request", e);
      return null;
    }
  }
  
  /**
   * 현재 요청에서 사용자명(이메일)을 추출
   * @return 토큰에서 추출된 사용자명, 없으면 null
   */
  public String getCurrentUsername() {
    try {
      String token = getCurrentToken();
      if (token == null) {
        log.warn("No JWT token found in current request");
        return null;
      }
      
      return jwtTokenProvider.getUsernameFromToken(token);
    } catch (Exception e) {
      log.error("Failed to extract username from current request", e);
      return null;
    }
  }
  
  /**
   * 현재 요청에서 JWT 토큰을 추출
   * @return JWT 토큰 문자열, 없으면 null
   */
  public String getCurrentToken() {
    try {
      ServletRequestAttributes attributes = 
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      
      if (attributes == null) {
        log.warn("No request attributes found");
        return null;
      }
      
      HttpServletRequest request = attributes.getRequest();
      return jwtTokenProvider.resolveToken(request);
    } catch (Exception e) {
      log.error("Failed to extract JWT token from current request", e);
      return null;
    }
  }
  
  /**
   * 지정된 토큰에서 사용자 ID 추출
   * @param token JWT 토큰
   * @return 사용자 ID, 없으면 null
   */
  public Long getUserIdFromToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return null;
    }
    return jwtTokenProvider.getUserId(token);
  }
  
  /**
   * 지정된 토큰에서 사용자명 추출
   * @param token JWT 토큰
   * @return 사용자명, 없으면 null
   */
  public String getUsernameFromToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return null;
    }
    return jwtTokenProvider.getUsernameFromToken(token);
  }
  
  /**
   * 현재 사용자가 인증되었는지 확인
   * @return 인증되었으면 true, 아니면 false
   */
  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && 
           authentication.isAuthenticated() && 
           !"anonymousUser".equals(authentication.getPrincipal());
  }
  
  /**
   * 토큰의 유효성을 검증
   * @param token JWT 토큰
   * @return 유효하면 true, 아니면 false
   */
  public boolean validateToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return false;
    }
    return jwtTokenProvider.validateToken(token);
  }
}