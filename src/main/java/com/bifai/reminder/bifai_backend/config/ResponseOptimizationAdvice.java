package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.dto.response.OptimizedApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * 응답 최적화 어드바이스
 * 모든 API 응답을 자동으로 최적화된 형태로 래핑
 */
@RestControllerAdvice(basePackages = "com.bifai.reminder.bifai_backend.controller")
@Slf4j
public class ResponseOptimizationAdvice implements ResponseBodyAdvice<Object> {
  
  @Override
  public boolean supports(MethodParameter returnType, 
                         Class<? extends HttpMessageConverter<?>> converterType) {
    // 이미 OptimizedApiResponse로 래핑된 경우 제외
    // ApiResponse도 제외하여 이중 래핑 방지
    Class<?> paramType = returnType.getParameterType();
    return !paramType.equals(OptimizedApiResponse.class) && 
           !paramType.equals(com.bifai.reminder.bifai_backend.dto.ApiResponse.class);
  }
  
  @Override
  public Object beforeBodyWrite(Object body, 
                               MethodParameter returnType,
                               MediaType selectedContentType, 
                               Class<? extends HttpMessageConverter<?>> selectedConverterType,
                               ServerHttpRequest request, 
                               ServerHttpResponse response) {
    
    // null 응답 처리
    if (body == null) {
      return OptimizedApiResponse.success(null);
    }
    
    // String 응답은 그대로 반환 (StringHttpMessageConverter 호환성)
    if (body instanceof String) {
      return body;
    }
    
    // 페이징 응답 처리
    if (body instanceof Page) {
      Page<?> page = (Page<?>) body;
      
      // 캐시 헤더 설정 (GET 요청만)
      if ("GET".equals(request.getMethod().name())) {
        response.getHeaders().add("Cache-Control", "public, max-age=60");
      }
      
      return OptimizedApiResponse.success(page);
    }
    
    // 리스트 응답 처리
    if (body instanceof List) {
      List<?> list = (List<?>) body;
      
      // 큰 리스트는 경고 로그
      if (list.size() > 100) {
        log.warn("Large list response: {} items for {}", 
          list.size(), request.getURI());
      }
      
      return OptimizedApiResponse.success(list);
    }
    
    // 일반 객체 응답
    return OptimizedApiResponse.success(body);
  }
}