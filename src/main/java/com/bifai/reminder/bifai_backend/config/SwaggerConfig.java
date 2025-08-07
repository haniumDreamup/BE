package com.bifai.reminder.bifai_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3.0 설정 클래스
 * 
 * <p>BIF-AI API 문서를 자동으로 생성하고 관리합니다.
 * Springdoc-OpenAPI를 사용하여 Swagger UI를 제공합니다.</p>
 * 
 * <p>접속 URL:</p>
 * <ul>
 *   <li>Swagger UI: http://localhost:8080/swagger-ui/index.html</li>
 *   <li>API Docs (JSON): http://localhost:8080/v3/api-docs</li>
 *   <li>API Docs (YAML): http://localhost:8080/v3/api-docs.yaml</li>
 * </ul>
 * 
 * <p>BIF 사용자를 위한 특징:</p>
 * <ul>
 *   <li>한국어 설명 포함</li>
 *   <li>명확한 예시 제공</li>
 *   <li>인증 방법 안내</li>
 *   <li>쉽고 친근한 설명</li>
 * </ul>
 * 
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String appVersion;
    
    @Value("${app.description}")
    private String appDescription;
    
    /**
     * OpenAPI 기본 설정
     * 
     * <p>API 문서의 기본 정보와 보안 설정을 정의합니다.</p>
     * 
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        // JWT 보안 스키마 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT 토큰을 입력하세요 (Bearer 접두사는 자동으로 추가됩니다)");
        
        // 보안 요구사항 - 모든 API에 기본으로 적용
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");
        
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                            .url("/api/v1")
                            .description("기본 API 서버 (context-path: /api/v1)")
                ))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme));
    }
    
    /**
     * API 문서 기본 정보 설정
     * 
     * <p>Swagger UI에 표시될 API 문서의 기본 정보를 설정합니다.</p>
     * 
     * @return API 정보 객체
     */
    private Info apiInfo() {
        return new Info()
                .title(appName + " API Documentation")
                .description(appDescription + "\n\n" +
                        "## 🌟 주요 특징\n" +
                        "- **경계선 지능 장애인(BIF)**을 위한 특화 시스템\n" +
                        "- **5학년 수준**의 쉬운 응답 메시지\n" +
                        "- **실시간 위치 추적** 및 안전 관리\n" +
                        "- **AI 기반** 상황 인지 지원\n\n" +
                        "## 🔐 인증 방법\n" +
                        "### JWT 토큰 인증\n" +
                        "1. `/api/v1/auth/login` 또는 OAuth2 로그인으로 토큰 획득\n" +
                        "2. `Authorization: Bearer {token}` 헤더에 토큰 포함\n" +
                        "3. 토큰 만료 시 리프레시 토큰으로 갱신\n\n" +
                        "### OAuth2 소셜 로그인\n" +
                        "- **카카오**: `/oauth2/authorization/kakao`\n" +
                        "- **네이버**: `/oauth2/authorization/naver`\n" +
                        "- **구글**: `/oauth2/authorization/google`\n\n" +
                        "## 📄 응답 형식\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"success\": true,\n" +
                        "  \"data\": {},\n" +
                        "  \"message\": \"성공 메시지\",\n" +
                        "  \"timestamp\": \"2024-01-01T00:00:00Z\"\n" +
                        "}\n" +
                        "```\n\n" +
                        "## 👥 API 그룹\n" +
                        "- **Auth**: 인증 및 회원가입\n" +
                        "- **User**: 사용자 정보 관리\n" +
                        "- **Guardian**: 보호자 관리\n" +
                        "- **Schedule**: 일정 관리\n" +
                        "- **Notification**: 알림 관리\n" +
                        "- **Location**: 위치 추적\n" +
                        "- **Medication**: 복약 관리")
                .version(appVersion)
                .contact(new Contact()
                        .name("BIF-AI 개발팀")
                        .email("support@bifai.com")
                        .url("https://bifai.com"))
                .license(new License()
                        .name("Private License")
                        .url("https://bifai.com/license"));
    }
}