package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.util.concurrent.TimeUnit;

/**
 * 웹 성능 최적화 설정
 * 압축, 캐싱, 리소스 최적화
 */
@Configuration
@EnableWebMvc
@Slf4j
public class WebPerformanceConfig implements WebMvcConfigurer {
  
  /**
   * CORS 설정 (성능 고려)
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOriginPatterns("*")
      .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true)
      .maxAge(3600); // 1시간 동안 preflight 캐시
  }
  
  /**
   * 정적 리소스 캐싱 설정
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/static/**")
      .addResourceLocations("classpath:/static/")
      .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
      .resourceChain(true)
      .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
    
    registry.addResourceHandler("/images/**")
      .addResourceLocations("classpath:/images/")
      .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
  }
  
  /**
   * 비동기 요청 처리 설정
   */
  @Override
  public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setDefaultTimeout(30000); // 30초
    configurer.setTaskExecutor(asyncTaskExecutor());
  }
  
  /**
   * 비동기 작업 실행기
   */
  @Bean
  public org.springframework.core.task.AsyncTaskExecutor asyncTaskExecutor() {
    org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = 
      new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(50);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("async-");
    executor.initialize();
    return executor;
  }
  
  /**
   * ETag 필터 (HTTP 캐싱)
   */
  @Bean
  public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean = 
      new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
    filterRegistrationBean.addUrlPatterns("/api/*");
    filterRegistrationBean.setName("etagFilter");
    filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return filterRegistrationBean;
  }
  
  /**
   * 요청 압축 필터
   */
  @Bean
  public FilterRegistrationBean<CompressionFilter> compressionFilter() {
    FilterRegistrationBean<CompressionFilter> filterRegistrationBean = 
      new FilterRegistrationBean<>(new CompressionFilter());
    filterRegistrationBean.addUrlPatterns("/api/*");
    filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return filterRegistrationBean;
  }
  
  /**
   * Tomcat 서버 최적화
   */
  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
    return factory -> {
      factory.addConnectorCustomizers(connector -> {
        // 압축 활성화
        connector.setProperty("compression", "on");
        connector.setProperty("compressionMinSize", "1024");
        connector.setProperty("compressibleMimeType", 
          "text/html,text/xml,text/plain,text/css,text/javascript," +
          "application/javascript,application/json,application/xml");
        
        // 연결 설정
        connector.setProperty("maxThreads", "200");
        connector.setProperty("minSpareThreads", "10");
        connector.setProperty("maxConnections", "8192");
        connector.setProperty("acceptCount", "100");
        
        // Keep-Alive 설정
        connector.setProperty("maxKeepAliveRequests", "100");
        connector.setProperty("keepAliveTimeout", "60000");
        
        log.info("Tomcat 성능 최적화 설정 완료");
      });
    };
  }
  
  /**
   * 인터셉터 추가
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new PerformanceInterceptor())
      .addPathPatterns("/api/**");
  }
  
  /**
   * 성능 모니터링 인터셉터
   */
  public static class PerformanceInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
    
    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, 
                           jakarta.servlet.http.HttpServletResponse response, 
                           Object handler) {
      request.setAttribute("startTime", System.currentTimeMillis());
      return true;
    }
    
    @Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, 
                               jakarta.servlet.http.HttpServletResponse response, 
                               Object handler, Exception ex) {
      Long startTime = (Long) request.getAttribute("startTime");
      if (startTime != null) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > 1000) { // 1초 이상 걸린 요청 로깅
          log.warn("Slow API: {} {}ms", request.getRequestURI(), duration);
        }
      }
    }
  }
  
  /**
   * 압축 필터 구현
   */
  public static class CompressionFilter implements jakarta.servlet.Filter {
    
    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, 
                        jakarta.servlet.ServletResponse response, 
                        jakarta.servlet.FilterChain chain) 
        throws java.io.IOException, jakarta.servlet.ServletException {
      
      jakarta.servlet.http.HttpServletRequest httpRequest = 
        (jakarta.servlet.http.HttpServletRequest) request;
      jakarta.servlet.http.HttpServletResponse httpResponse = 
        (jakarta.servlet.http.HttpServletResponse) response;
      
      String acceptEncoding = httpRequest.getHeader("Accept-Encoding");
      
      if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
        httpResponse.setHeader("Content-Encoding", "gzip");
        httpResponse.setHeader("Vary", "Accept-Encoding");
      }
      
      chain.doFilter(request, response);
    }
  }
}