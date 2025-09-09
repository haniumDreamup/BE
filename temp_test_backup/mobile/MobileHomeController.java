package com.bifai.reminder.bifai_backend.controller.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileDashboardResponse;
import com.bifai.reminder.bifai_backend.dto.response.MobileApiResponse;
import com.bifai.reminder.bifai_backend.service.mobile.MobileHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 모바일 홈 API 컨트롤러
 * 
 * BIF 사용자를 위한 간단한 홈 대시보드 정보를 제공합니다.
 * 오늘의 할 일, 약물 복용, 간단한 통계 등을 포함합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/home")
@RequiredArgsConstructor
@Tag(name = "Mobile Home", description = "모바일 홈 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
public class MobileHomeController {
  
  private final MobileHomeService mobileHomeService;
  
  /**
   * 홈 대시보드 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 대시보드 정보
   */
  @GetMapping("/dashboard")
  @Operation(
      summary = "홈 대시보드 조회",
      description = "사용자의 오늘 일정, 약물 복용, 알림 등 홈 화면에 표시될 정보를 가져옵니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "대시보드 정보 조회 성공",
          content = @Content(schema = @Schema(implementation = MobileDashboardResponse.class))
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 필요",
          content = @Content(schema = @Schema(implementation = MobileApiResponse.class))
      )
  })
  public ResponseEntity<MobileApiResponse<MobileDashboardResponse>> getDashboard(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("홈 대시보드 조회 요청: user={}", userDetails.getUsername());
    
    try {
      MobileDashboardResponse dashboard = mobileHomeService.getDashboard(userDetails.getUsername());
      
      return ResponseEntity.ok(
          MobileApiResponse.success(dashboard, "오늘도 좋은 하루 보내세요!")
      );
      
    } catch (Exception e) {
      log.error("대시보드 조회 실패: {}", e.getMessage());
      
      return ResponseEntity.status(500).body(
          MobileApiResponse.error(
              "HOME_001",
              "정보를 불러올 수 없어요",
              "잠시 후 다시 시도해주세요"
          )
      );
    }
  }
  
  /**
   * 간단한 인사말 조회
   * 
   * @param userDetails 인증된 사용자 정보
   * @return 인사말
   */
  @GetMapping("/greeting")
  @Operation(
      summary = "인사말 조회",
      description = "시간대와 사용자 상태에 맞는 간단한 인사말을 제공합니다."
  )
  public ResponseEntity<MobileApiResponse<String>> getGreeting(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    String greeting = mobileHomeService.getGreeting(userDetails.getUsername());
    
    return ResponseEntity.ok(
        MobileApiResponse.success(greeting, null)
    );
  }
}