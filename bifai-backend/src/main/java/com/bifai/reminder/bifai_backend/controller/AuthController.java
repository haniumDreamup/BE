package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.auth.*;
import com.bifai.reminder.bifai_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * 인증 컨트롤러
 * BIF 사용자를 위한 회원가입, 로그인, 토큰 관리 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 사용자 회원가입
     * POST /api/v1/auth/register
     */
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
    @PostMapping("/logout")
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
     * 헬스 체크 - 인증 서버 상태 확인
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok()
                .body(ApiResponse.success("인증 서비스가 정상 작동 중입니다", "Auth Service Health Check"));
    }
} 