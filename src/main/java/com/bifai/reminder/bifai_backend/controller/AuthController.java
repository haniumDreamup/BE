package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.auth.*;
import com.bifai.reminder.bifai_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

/**
 * 인증 컨트롤러
 * 
 * <p>BIF 사용자를 위한 회원가입, 로그인, 토큰 관리 엔드포인트를 제공합니다.
 * 모든 응답 메시지는 5학년 수준의 쉽고 친근한 언어로 작성됩니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Tag(name = "인증 API", description = "BIF 사용자 인증 및 토큰 관리")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 사용자 회원가입
     * POST /api/v1/auth/register
     */
    @Operation(
        summary = "사용자 회원가입",
        description = "BIF 사용자가 새로운 계정을 만듭니다. 회원가입 후 자동으로 로그인되며, " +
                     "JWT 토큰을 반환합니다. 모든 입력 필드는 필수입니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "회원가입 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "회원가입 성공 응답",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                            "tokenType": "Bearer",
                            "expiresIn": 3600,
                            "user": {
                                "userId": 1,
                                "username": "bifuser123",
                                "email": "user@example.com",
                                "name": "홍길동"
                            }
                        },
                        "message": "회원가입이 완료되었습니다",
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": false,
                        "error": {
                            "code": "DUPLICATE_USER",
                            "message": "이미 사용 중인 아이디입니다",
                            "userAction": "다른 아이디를 사용해주세요"
                        },
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {

        log.info("회원가입 요청: username={}, email={}", request.getUsername(), request.getEmail());

        // 입력 검증 오류 확인
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorMessage));
        }

        try {
            AuthResponse authResponse = authService.register(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(authResponse, "회원가입이 완료되었습니다"));
                    
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("회원가입 중 오류가 발생했습니다"));
        }
    }

    /**
     * 사용자 로그인
     * POST /api/v1/auth/login
     */
    @Operation(
        summary = "사용자 로그인",
        description = "BIF 사용자가 아이디/이메일과 비밀번호로 로그인합니다. " +
                     "성공 시 JWT 액세스 토큰과 리프레시 토큰을 반환합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
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
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult) {

        log.info("로그인 요청: {}", request.getUsernameOrEmail());

        // 입력 검증 오류 확인
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorMessage));
        }

        try {
            AuthResponse authResponse = authService.login(request);
            
            return ResponseEntity.ok()
                    .body(ApiResponse.success(authResponse, "로그인이 완료되었습니다"));
                    
        } catch (Exception e) {
            log.warn("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("아이디 또는 비밀번호가 올바르지 않습니다"));
        }
    }

    /**
     * 토큰 갱신
     * POST /api/v1/auth/refresh
     */
    @Operation(
        summary = "토큰 갱신",
        description = "만료된 액세스 토큰을 리프레시 토큰을 사용하여 갱신합니다. " +
                     "새로운 액세스 토큰과 리프레시 토큰을 반환합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "토큰 갱신 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "유효하지 않은 리프레시 토큰",
            content = @Content(
                mediaType = "application/json"
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            BindingResult bindingResult) {

        log.info("토큰 갱신 요청");

        // 입력 검증 오류 확인
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorMessage));
        }

        try {
            AuthResponse authResponse = authService.refreshToken(request);
            
            return ResponseEntity.ok()
                    .body(ApiResponse.success(authResponse, "토큰이 갱신되었습니다"));
                    
        } catch (IllegalArgumentException e) {
            log.warn("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("토큰 갱신 중 오류가 발생했습니다"));
        }
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     */
    @Operation(
        summary = "로그아웃",
        description = "현재 로그인된 사용자를 로그아웃합니다. " +
                     "서버에 저장된 리프레시 토큰이 삭제됩니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "success": true,
                        "data": null,
                        "message": "로그아웃이 완료되었습니다",
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("로그아웃 요청");

        try {
            authService.logout();
            
            return ResponseEntity.ok()
                    .body(ApiResponse.success(null, "로그아웃이 완료되었습니다"));
                    
        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그아웃 중 오류가 발생했습니다"));
        }
    }

    /**
     * OAuth2 로그인 URL 조회
     * GET /api/v1/auth/oauth2/login-urls
     */
    @Operation(
        summary = "OAuth2 로그인 URL 조회",
        description = "소셜 로그인을 위한 OAuth2 제공자별 로그인 URL을 반환합니다. " +
                     "현재 지원하는 제공자: 카카오, 네이버, 구글"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "OAuth2 로그인 URL 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "OAuth2 로그인 URL 응답",
                    value = """
                    {
                        "success": true,
                        "data": {
                            "kakao": "/oauth2/authorization/kakao",
                            "naver": "/oauth2/authorization/naver",
                            "google": "/oauth2/authorization/google"
                        },
                        "message": "소셜 로그인 주소를 가져왔습니다",
                        "timestamp": "2024-01-01T00:00:00Z"
                    }
                    """
                )
            )
        )
    })
    @SecurityRequirements
    @GetMapping("/oauth2/login-urls")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOAuth2LoginUrls() {
        log.info("OAuth2 로그인 URL 요청");

        Map<String, String> loginUrls = new HashMap<>();
        loginUrls.put("kakao", "/oauth2/authorization/kakao");
        loginUrls.put("naver", "/oauth2/authorization/naver");
        loginUrls.put("google", "/oauth2/authorization/google");

        return ResponseEntity.ok()
                .body(ApiResponse.success(loginUrls, "소셜 로그인 주소를 가져왔습니다"));
    }

} 