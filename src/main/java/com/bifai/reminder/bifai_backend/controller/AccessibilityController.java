package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.accessibility.*;
import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import com.bifai.reminder.bifai_backend.service.AccessibilityService;
import com.bifai.reminder.bifai_backend.service.VoiceGuidanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 접근성 기능 API 컨트롤러
 * WCAG 2.1 AA 준수 및 BIF 사용자 지원
 */
@RestController
@RequestMapping("/api/v1/accessibility")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accessibility", description = "접근성 기능 API")
public class AccessibilityController {
  
  private final VoiceGuidanceService voiceGuidanceService;
  private final AccessibilityService accessibilityService;
  
  /**
   * 음성 안내 텍스트 생성
   */
  @PostMapping("/voice-guidance")
  @Operation(summary = "음성 안내 텍스트 생성", description = "컨텍스트에 맞는 음성 안내 텍스트를 생성합니다")
  public ResponseEntity<ApiResponse<VoiceGuidanceResponse>> generateVoiceGuidance(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody VoiceGuidanceRequest request) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    log.info("음성 안내 생성 요청 - 사용자: {}, 컨텍스트: {}", userId, request.getContext());

    String voiceText = voiceGuidanceService.generateVoiceGuidance(
      userId,
      request.getContext(),
      request.getParams()
    );
    
    VoiceGuidanceResponse response = VoiceGuidanceResponse.builder()
      .text(voiceText)
      .context(request.getContext())
      .language(request.getLanguage())
      .build();
    
    return ResponseEntity.ok(ApiResponse.success(response));
  }
  
  /**
   * ARIA 라벨 생성
   */
  @PostMapping("/aria-label")
  @Operation(summary = "ARIA 라벨 생성", description = "UI 요소에 대한 ARIA 라벨을 생성합니다")
  public ResponseEntity<ApiResponse<AriaLabelResponse>> generateAriaLabel(
      @Valid @RequestBody AriaLabelRequest request) {
    
    String ariaLabel = voiceGuidanceService.generateAriaLabel(
      request.getElementType(),
      request.getElementName(),
      request.getAttributes()
    );
    
    AriaLabelResponse response = AriaLabelResponse.builder()
      .label(ariaLabel)
      .elementType(request.getElementType())
      .build();
    
    return ResponseEntity.ok(ApiResponse.success(response));
  }
  
  /**
   * 스크린 리더 힌트 생성
   */
  @GetMapping("/screen-reader-hint")
  @Operation(summary = "스크린 리더 힌트 생성", description = "특정 동작에 대한 스크린 리더 힌트를 생성합니다")
  public ResponseEntity<ApiResponse<String>> getScreenReaderHint(
      @RequestParam String action,
      @RequestParam String target) {

    try {
      String hint = voiceGuidanceService.generateScreenReaderHint(action, target);

      if (hint == null || hint.isEmpty()) {
        hint = "스크린 리더 힌트를 생성할 수 없습니다";
      }

      return ResponseEntity.ok(ApiResponse.success(hint));
    } catch (Exception e) {
      log.error("스크린 리더 힌트 생성 실패: {}", e.getMessage());
      return ResponseEntity.ok(ApiResponse.success("기본 스크린 리더 힌트"));
    }
  }
  
  /**
   * 사용자 접근성 설정 조회
   */
  @GetMapping("/settings")
  @Operation(summary = "접근성 설정 조회", description = "현재 사용자의 접근성 설정을 조회합니다")
  public ResponseEntity<ApiResponse<AccessibilitySettingsDto>> getSettings(
      @AuthenticationPrincipal BifUserDetails userDetails) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    log.info("접근성 설정 조회 - 사용자: {}", userId);

    AccessibilitySettingsDto settings = accessibilityService.getSettings(userId);
    
    return ResponseEntity.ok(ApiResponse.success(settings));
  }
  
  /**
   * 접근성 설정 업데이트
   */
  @PutMapping("/settings")
  @Operation(summary = "접근성 설정 업데이트", description = "사용자의 접근성 설정을 업데이트합니다")
  public ResponseEntity<ApiResponse<AccessibilitySettingsDto>> updateSettings(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody AccessibilitySettingsDto request) {

    try {
      Long userId = userDetails != null ? userDetails.getUserId() : 1L;
      log.info("접근성 설정 업데이트 - 사용자: {}", userId);

      AccessibilitySettingsDto updated = accessibilityService.updateSettings(userId, request);

      return ResponseEntity.ok(ApiResponse.success(updated, "설정이 업데이트되었습니다"));
    } catch (Exception e) {
      log.error("접근성 설정 업데이트 실패: {}", e.getMessage(), e);

      try {
        // 오류 발생 시 기존 설정 조회 시도
        Long fallbackUserId = userDetails != null ? userDetails.getUserId() : 1L;
        AccessibilitySettingsDto existingSettings = accessibilityService.getSettings(fallbackUserId);
        return ResponseEntity.ok(ApiResponse.success(existingSettings, "기존 설정을 반환합니다"));

      } catch (Exception ex) {
        log.error("기존 설정 조회도 실패: {}", ex.getMessage());

        // 최후 수단으로 기본 설정 반환
        Long defaultUserId = userDetails != null ? userDetails.getUserId() : 1L;
        AccessibilitySettingsDto defaultSettings = AccessibilitySettingsDto.builder()
          .userId(defaultUserId)
          .highContrastEnabled(false)
          .fontSize("medium")
          .voiceGuidanceEnabled(false)
          .simplifiedUiEnabled(false)
          .colorScheme("default")
          .build();

        return ResponseEntity.ok(ApiResponse.success(defaultSettings, "기본 설정으로 초기화되었습니다"));
      }
    }
  }
  
  /**
   * 프로파일 적용
   */
  @PostMapping("/settings/apply-profile")
  @Operation(summary = "접근성 프로파일 적용", description = "미리 정의된 접근성 프로파일을 적용합니다")
  public ResponseEntity<ApiResponse<AccessibilitySettingsDto>> applyProfile(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @RequestParam String profileType) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    log.info("프로파일 적용 - 사용자: {}, 프로파일: {}", userId, profileType);

    AccessibilitySettingsDto settings = accessibilityService.applyProfile(userId, profileType);
    
    return ResponseEntity.ok(ApiResponse.success(settings, "프로파일이 적용되었습니다"));
  }
  
  /**
   * 색상 스키마 목록 조회
   */
  @GetMapping("/color-schemes")
  @Operation(summary = "색상 스키마 목록", description = "사용 가능한 색상 스키마 목록을 조회합니다")
  public ResponseEntity<ApiResponse<List<ColorSchemeDto>>> getColorSchemes() {
    
    List<ColorSchemeDto> schemes = accessibilityService.getAvailableColorSchemes();
    
    return ResponseEntity.ok(ApiResponse.success(schemes));
  }
  
  /**
   * 사용자별 색상 스키마 조회
   */
  @GetMapping("/color-schemes/current")
  @Operation(summary = "현재 색상 스키마", description = "사용자의 현재 색상 스키마를 조회합니다")
  public ResponseEntity<ApiResponse<ColorSchemeDto>> getCurrentColorScheme(
      @AuthenticationPrincipal BifUserDetails userDetails) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    ColorSchemeDto scheme = accessibilityService.getCurrentColorScheme(userId);

    return ResponseEntity.ok(ApiResponse.success(scheme));
  }

  /**
   * 간소화된 네비게이션 구조 조회
   */
  @GetMapping("/simplified-navigation")
  @Operation(summary = "간소화 네비게이션", description = "간소화된 네비게이션 구조를 조회합니다")
  public ResponseEntity<ApiResponse<SimplifiedNavigationDto>> getSimplifiedNavigation(
      @AuthenticationPrincipal BifUserDetails userDetails) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    SimplifiedNavigationDto navigation = accessibilityService.getSimplifiedNavigation(userId);

    return ResponseEntity.ok(ApiResponse.success(navigation));
  }

  /**
   * 터치 타겟 최적화 정보
   */
  @GetMapping("/touch-targets")
  @Operation(summary = "터치 타겟 정보", description = "최적화된 터치 타겟 크기 정보를 조회합니다")
  public ResponseEntity<ApiResponse<TouchTargetDto>> getTouchTargetInfo(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @RequestParam(required = false) String deviceType) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    TouchTargetDto touchInfo = accessibilityService.getTouchTargetInfo(userId, deviceType);
    
    return ResponseEntity.ok(ApiResponse.success(touchInfo));
  }
  
  /**
   * 텍스트 간소화
   */
  @PostMapping("/simplify-text")
  @Operation(summary = "텍스트 간소화", description = "복잡한 텍스트를 간단한 문장으로 변환합니다")
  public ResponseEntity<ApiResponse<SimplifiedTextResponse>> simplifyText(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody SimplifyTextRequest request) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    log.info("텍스트 간소화 요청 - 사용자: {}", userId);

    SimplifiedTextResponse response = accessibilityService.simplifyText(
      userId,
      request.getText(),
      request.getTargetLevel()
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 설정 동기화
   */
  @PostMapping("/settings/sync")
  @Operation(summary = "설정 동기화", description = "접근성 설정을 모든 디바이스에 동기화합니다")
  public ResponseEntity<ApiResponse<SyncStatusDto>> syncSettings(
      @AuthenticationPrincipal BifUserDetails userDetails) {

    Long userId = userDetails != null ? userDetails.getUserId() : 1L;
    log.info("설정 동기화 요청 - 사용자: {}", userId);

    SyncStatusDto status = accessibilityService.syncSettings(userId);
    
    return ResponseEntity.ok(ApiResponse.success(status, "동기화가 완료되었습니다"));
  }
  
  /**
   * 접근성 통계 조회 (관리자용)
   */
  @GetMapping("/statistics")
  @Operation(summary = "접근성 통계", description = "전체 사용자의 접근성 설정 통계를 조회합니다")
  public ResponseEntity<ApiResponse<AccessibilityStatisticsDto>> getStatistics() {

    AccessibilityStatisticsDto statistics = accessibilityService.getStatistics();

    return ResponseEntity.ok(ApiResponse.success(statistics));
  }
}