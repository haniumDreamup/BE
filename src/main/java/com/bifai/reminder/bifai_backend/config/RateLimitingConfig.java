package com.bifai.reminder.bifai_backend.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Rate Limiting 설정
 * IP 기반 요청 제한
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingConfig implements Filter {
  
  private final RateLimiterRegistry rateLimiterRegistry;
  private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();
  
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    
    String clientIp = getClientIp(httpRequest);
    String path = httpRequest.getRequestURI();
    
    // 특정 경로는 Rate Limiting 제외
    if (isExcludedPath(path)) {
      chain.doFilter(request, response);
      return;
    }
    
    // IP별 Rate Limiter 가져오기 또는 생성
    RateLimiter limiter = limiters.computeIfAbsent(clientIp, 
      ip -> rateLimiterRegistry.rateLimiter("ip-" + ip, "api-limiter"));
    
    // Rate Limit 체크
    if (limiter.acquirePermission()) {
      chain.doFilter(request, response);
    } else {
      log.warn("Rate limit 초과 - IP: {}, Path: {}", clientIp, path);
      
      // 429 Too Many Requests
      httpResponse.setStatus(429);
      httpResponse.setHeader("X-RateLimit-Retry-After", "1");
      httpResponse.setContentType("application/json;charset=UTF-8");
      httpResponse.getWriter().write(
        "{\"error\":\"너무 많은 요청입니다. 잠시 후 다시 시도해주세요.\"}"
      );
    }
  }
  
  /**
   * 클라이언트 IP 추출
   */
  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    
    // 여러 IP가 있는 경우 첫 번째 IP 사용
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    
    return ip;
  }
  
  /**
   * Rate Limiting 제외 경로 확인
   */
  private boolean isExcludedPath(String path) {
    return path.startsWith("/api/health") ||
           path.startsWith("/api/docs") ||
           path.startsWith("/swagger-ui") ||
           path.startsWith("/actuator");
  }
}