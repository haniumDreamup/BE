# JavaDoc 및 Swagger 문서화 모범 사례

## 1. JavaDoc 작성 가이드

### 1.1 클래스 레벨 JavaDoc
```java
/**
 * 사용자 엔티티 - 경계선 지능 장애인을 위한 리마인더 시스템 사용자 정보
 * 
 * <p>BIF(Borderline Intellectual Functioning) 사용자의 정보를 관리하는 핵심 엔티티입니다.
 * IQ 70-85 범위의 사용자를 지원하며, 인지 수준에 맞춘 서비스를 제공합니다.</p>
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>사용자 기본 정보 관리</li>
 *   <li>인지 수준 분류 및 추적</li>
 *   <li>보호자 관계 관리</li>
 *   <li>OAuth2 소셜 로그인 지원</li>
 * </ul>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 * @see Guardian
 * @see UserPreference
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    // ...
}
```

### 1.2 메소드 레벨 JavaDoc
```java
/**
 * 사용자의 인지 수준을 업데이트합니다.
 * 
 * <p>이 메소드는 사용자의 인지 능력 평가 후 수준을 업데이트할 때 사용됩니다.
 * 인지 수준 변경은 시스템 전반의 UI/UX 복잡도에 영향을 미칩니다.</p>
 * 
 * @param cognitiveLevel 새로운 인지 수준 (MILD, MODERATE, SEVERE, UNKNOWN)
 * @throws IllegalArgumentException cognitiveLevel이 null인 경우
 * @since 1.0
 */
public void updateCognitiveLevel(CognitiveLevel cognitiveLevel) {
    if (cognitiveLevel == null) {
        throw new IllegalArgumentException("인지 수준은 null일 수 없습니다");
    }
    this.cognitiveLevel = cognitiveLevel;
}
```

### 1.3 필드 레벨 JavaDoc
```java
/**
 * 사용자의 인지 수준
 * 
 * <p>BIF 사용자의 인지 능력을 분류하여 적절한 수준의 지원을 제공합니다:</p>
 * <ul>
 *   <li>MILD: 독립적 수행 가능, 최소한의 지원</li>
 *   <li>MODERATE: 중등도 지원 필요 (기본값)</li>
 *   <li>SEVERE: 지속적인 지원 필요</li>
 *   <li>UNKNOWN: 평가 필요</li>
 * </ul>
 */
@Column(name = "cognitive_level", length = 20)
@Enumerated(EnumType.STRING)
@Builder.Default
private CognitiveLevel cognitiveLevel = CognitiveLevel.MODERATE;
```

## 2. Swagger/OpenAPI 문서화 가이드

### 2.1 컨트롤러 레벨 문서화
```java
@Tag(name = "인증 API", description = "BIF 사용자 인증 및 토큰 관리")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    // ...
}
```

### 2.2 API 엔드포인트 문서화
```java
@Operation(
    summary = "사용자 로그인",
    description = "BIF 사용자가 아이디/이메일과 비밀번호로 로그인합니다. " +
                 "성공 시 JWT 액세스 토큰과 리프레시 토큰을 반환합니다.",
    responses = {
        @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "로그인 성공 응답",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "tokenType": "Bearer",
                            "expiresIn": 3600
                        },
                        "message": "로그인이 완료되었습니다",
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "error": {
                            "code": "AUTH_FAILED",
                            "message": "아이디 또는 비밀번호가 올바르지 않습니다",
                            "userAction": "아이디와 비밀번호를 다시 확인해주세요"
                        },
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """
                )
            )
        )
    }
)
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(
        @RequestBody @Valid LoginRequest request) {
    // ...
}
```

### 2.3 DTO 문서화
```java
@Schema(description = "로그인 요청 정보")
public class LoginRequest {
    
    @Schema(
        description = "사용자명 또는 이메일",
        example = "bifuser123",
        required = true
    )
    @NotBlank(message = "아이디 또는 이메일을 입력해주세요")
    private String usernameOrEmail;
    
    @Schema(
        description = "비밀번호",
        example = "password123!",
        required = true,
        minLength = 8
    )
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;
}
```

### 2.4 Enum 문서화
```java
@Schema(description = "사용자 인지 수준")
public enum CognitiveLevel {
    @Schema(description = "경미 - 독립적 수행 가능")
    MILD,
    
    @Schema(description = "중등도 - 최소한의 지원 필요")
    MODERATE,
    
    @Schema(description = "심각 - 지속적인 지원 필요")
    SEVERE,
    
    @Schema(description = "미정 - 평가 필요")
    UNKNOWN
}
```

## 3. 모범 사례

### 3.1 BIF 사용자를 위한 문서화
- **간단명료한 설명**: 5학년 수준의 읽기 능력에 맞춘 설명
- **구체적인 예시**: 실제 사용 시나리오 포함
- **시각적 설명**: 가능한 경우 다이어그램이나 플로우차트 참조
- **긍정적인 언어**: 오류 메시지도 격려하는 톤으로 작성

### 3.2 API 문서화 체크리스트
- [ ] 모든 public 클래스에 JavaDoc 작성
- [ ] 모든 public 메소드에 설명 추가
- [ ] 파라미터와 반환값 명확히 문서화
- [ ] 예외 상황 문서화
- [ ] Swagger 어노테이션으로 API 스펙 보강
- [ ] 요청/응답 예시 제공
- [ ] 에러 응답 형식 문서화

### 3.3 Swagger UI 커스터마이징
```java
@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("BIF-AI API")
                .description("경계선 지능 장애인을 위한 인지 지원 시스템")
                .version("1.0")
                .contact(new Contact()
                    .name("BIF-AI 지원팀")
                    .email("support@bifai.com"))
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요")
                )
            );
    }
}
```

## 4. 도구 및 플러그인

### 4.1 IntelliJ IDEA 설정
- JavaDoc 자동 생성: `/**` 입력 후 Enter
- Live Templates 설정으로 반복 패턴 자동화
- CheckStyle 플러그인으로 JavaDoc 검증

### 4.2 Gradle 태스크
```groovy
// JavaDoc 생성
task generateJavadoc(type: Javadoc) {
    source = sourceSets.main.allJava
    classpath = configurations.compileClasspath
    destinationDir = file("$buildDir/docs/javadoc")
    options.encoding = 'UTF-8'
    options.charSet = 'UTF-8'
    options.author = true
    options.version = true
    options.use = true
}

// Swagger 문서 생성
springdoc {
    apiDocs {
        enabled = true
    }
    swaggerUi {
        enabled = true
        path = "/swagger-ui.html"
    }
}
```

## 5. 지속적인 개선

### 5.1 문서화 리뷰 프로세스
1. 코드 리뷰 시 문서화 품질 확인
2. 사용자 피드백 반영
3. API 변경 시 문서 즉시 업데이트
4. 정기적인 문서화 감사

### 5.2 측정 지표
- JavaDoc 커버리지 80% 이상 유지
- API 문서 완성도 100%
- 사용자 문서 이해도 평가
- 문서 업데이트 주기