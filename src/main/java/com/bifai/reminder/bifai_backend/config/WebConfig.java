package com.bifai.reminder.bifai_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 설정 - Flutter 웹 앱 정적 파일 서빙
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 정적 리소스 핸들러 설정
     * Flutter 빌드된 웹 파일들을 서빙
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Flutter 웹 빌드 파일 서빙 (API 경로는 제외)
        registry.addResourceHandler("/assets/**", "/icons/**", "/images/**", "/favicon.ico")
                .addResourceLocations("classpath:/static/", "file:build/web/")
                .setCachePeriod(0);

        registry.addResourceHandler("/")
                .addResourceLocations("classpath:/static/", "file:build/web/")
                .setCachePeriod(0);
    }
}