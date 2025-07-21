package com.bifai.reminder.bifai_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 설정
 * BIF-AI API 문서화를 위한 설정
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @Value("${app.description}")
    private String appDescription;
    
    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 스키마 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");
        
        // 보안 요구사항
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");
        
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("/api/v1").description("기본 API 서버")
                ))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme));
    }
    
    private Info apiInfo() {
        return new Info()
                .title(appName + " API Documentation")
                .description(appDescription + "\n\n" +
                        "## 주요 특징\n" +
                        "- 경계선 지능 장애인(BIF)을 위한 특화 시스템\n" +
                        "- 5학년 수준의 쉬운 응답 메시지\n" +
                        "- 실시간 위치 추적 및 안전 관리\n" +
                        "- AI 기반 상황 인지 지원\n\n" +
                        "## 인증\n" +
                        "대부분의 API는 JWT 토큰이 필요합니다. 로그인 후 받은 토큰을 `Authorization: Bearer {token}` 형식으로 사용하세요.")
                .version(appVersion)
                .contact(new Contact()
                        .name("BIF-AI 개발팀")
                        .email("support@bifai.com"));
    }
}