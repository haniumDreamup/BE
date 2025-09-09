package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileLoginRequest;
import com.bifai.reminder.bifai_backend.dto.mobile.MobileLoginResponse;
import com.bifai.reminder.bifai_backend.dto.mobile.MobileRefreshRequest;
import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.service.mobile.MobileAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 모바일 인증 API 컨트롤러
 * 
 * BIF 사용자를 위한 간단한 인증 프로세스를 제공합니다.
 * JWT 기반 인증과 디바이스 관리를 포함합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/auth")
@RequiredArgsConstructor
@Tag(name = "Mobile Auth", description = "모바일 인증 API")
public class MobileAuthController {
  
  private final MobileAuthService mobileAuthService;
  
  /**
   * 모바일 로그인
   * 
   * @param request 로그인 요청 정보
   * @return 로그인 응답 (토큰 포함)
   */
  @PostMapping("/login")
  @Operation(
      summary = "모바일 로그인",
      description = "사용자명/이메일과 비밀번호로 로그인합니다. 디바이스 정보를 함께 등록합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그인 성공",
          content = @Content(schema = @Schema(implementation = MobileLoginResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 실패",
          content = @Content(schema = @Schema(implementation = MobileApiResponse.class))
      )
  })
  public ResponseEntity<MobileApiResponse<MobileLoginResponse>> login(
      @Valid @RequestBody MobileLoginRequest request) {
    
    log.info("모바일 로그인 시도: username={}, deviceId={}", 
        request.getUsername(), request.getDeviceId());
    
    try {
      MobileLoginResponse response = mobileAuthService.login(request);
      
      log.info("모바일 로그인 성공: userId={}", response.getUser().getId());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(response, "로그인 성공!")
      );
      
    } catch (Exception e) {
      log.error("모바일 로그인 실패: {}", e.getMessage());
      
      return ResponseEntity.status(401).body(
          MobileApiResponse.error(
              "AUTH_001",
              "아이디나 비밀번호를 확인해주세요",
              "다시 입력해보세요"
          )
      );
    }
  }
  
  /**
   * 토큰 갱신
   * 
   * @param request 리프레시 토큰 요청
   * @return 새로운 액세스 토큰
   */
  @PostMapping("/refresh")
  @Operation(
      summary = "토큰 갱신",
      description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "토큰 갱신 성공"
      ),
      @ApiResponse(
          responseCode = "401",
          description = "리프레시 토큰 만료 또는 유효하지 않음"
      )
  })
  public ResponseEntity<MobileApiResponse<MobileLoginResponse>> refresh(
      @Valid @RequestBody MobileRefreshRequest request) {
    
    log.info("토큰 갱신 요청");
    
    try {
      MobileLoginResponse response = mobileAuthService.refresh(request);
      
      log.info("토큰 갱신 성공: userId={}", response.getUser().getId());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(response, "토큰이 갱신되었어요")
      );
      
    } catch (Exception e) {
      log.error("토큰 갱신 실패: {}", e.getMessage());
      
      return ResponseEntity.status(401).body(
          MobileApiResponse.error(
              "AUTH_002",
              "다시 로그인해 주세요",
              "앱을 다시 열어주세요"
          )
      );
    }
  }
  
  /**
   * 로그아웃
   * 
   * @param deviceId 디바이스 ID (헤더)
   * @return 로그아웃 결과
   */
  @PostMapping("/logout")
  @Operation(
      summary = "로그아웃",
      description = "현재 세션을 종료하고 디바이스 토큰을 제거합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그아웃 성공"
      )
  })
  public ResponseEntity<MobileApiResponse<Void>> logout(
      @Parameter(description = "디바이스 ID", required = true)
      @RequestHeader("X-Device-Id") String deviceId) {
    
    log.info("로그아웃 요청: deviceId={}", deviceId);
    
    try {
      mobileAuthService.logout(deviceId);
      
      return ResponseEntity.ok(
          MobileApiResponse.success(null, "안전하게 로그아웃 되었어요")
      );
      
    } catch (Exception e) {
      log.error("로그아웃 처리 중 오류: {}", e.getMessage());
      // 로그아웃은 항상 성공으로 처리
      return ResponseEntity.ok(
          MobileApiResponse.success(null, "로그아웃 되었어요")
      );
    }
  }
  
  /**
   * 자동 로그인 체크
   * 
   * @param deviceId 디바이스 ID
   * @return 자동 로그인 가능 여부
   */
  @GetMapping("/check")
  @Operation(
      summary = "자동 로그인 체크",
      description = "저장된 리프레시 토큰으로 자동 로그인이 가능한지 확인합니다."
  )
  public ResponseEntity<MobileApiResponse<Boolean>> checkAutoLogin(
      @Parameter(description = "디바이스 ID", required = true)
      @RequestHeader("X-Device-Id") String deviceId) {
    
    log.info("자동 로그인 체크: deviceId={}", deviceId);
    
    boolean canAutoLogin = mobileAuthService.canAutoLogin(deviceId);
    
    return ResponseEntity.ok(
        MobileApiResponse.success(
            canAutoLogin,
            canAutoLogin ? "자동 로그인 가능해요" : "다시 로그인해주세요"
        )
    );
  }
}