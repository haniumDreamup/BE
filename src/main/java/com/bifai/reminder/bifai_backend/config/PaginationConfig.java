package com.bifai.reminder.bifai_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 페이지네이션 최적화 설정
 */
@Configuration
public class PaginationConfig implements WebMvcConfigurer {
  
  /**
   * 기본 페이지 설정
   */
  @Bean
  public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
    return p -> {
      p.setFallbackPageable(PageRequest.of(0, 20)); // 기본 페이지 크기 20
      p.setMaxPageSize(100); // 최대 페이지 크기 100
      p.setOneIndexedParameters(true); // 1부터 시작하는 페이지 번호
      p.setPageParameterName("page");
      p.setSizeParameterName("size");
      p.setPrefix("");
      p.setQualifierDelimiter("_");
    };
  }
  
  /**
   * 페이지 핸들러 추가
   */
  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
    resolver.setFallbackPageable(PageRequest.of(0, 20));
    resolver.setMaxPageSize(100);
    resolvers.add(resolver);
  }
}