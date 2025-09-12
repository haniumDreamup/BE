package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.common.BaseController;
import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.emergency.*;
import com.bifai.reminder.bifai_backend.service.EmergencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 긴급 상황 관리 컨트롤러
 * 
 * <p>BIF 사용자의 긴급 상황 처리 및 모니터링을 담당합니다.
 * 낙상 감지, 긴급 호출, 안전 구역 이탈 등의 상황을 관리합니다.</p>
 * 
 * @author BIF-AI 개발팀
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class EmergencyController extends BaseController {

  private final EmergencyService emergencyService;

  /**
   * 긴급 상황 발생 신고
   * 
   * <p>BIF 사용자가 수동으로 긴급 상황을 신고합니다.
   * 보호자에게 즉시 알림이 전송됩니다.</p>
   * 
   * @param request 긴급 상황 정보 (유형, 위치, 설명 등)
   * @return 생성된 긴급 상황 정보
   */
  @PostMapping("/alert")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<ApiResponse<EmergencyResponse>> createEmergencyAlert(
      @Valid @RequestBody EmergencyRequest request) {
    log.warn("긴급 상황 신고 요청: type={}", request.getType());
    
    try {
      EmergencyResponse response = emergencyService.createEmergency(request);
      return createCreatedResponse(response, "긴급 상황이 신고되었습니다. 보호자에게 알림을 전송했습니다");
    } catch (Exception e) {
      return handleException("긴급 상황 신고 실패", e, "긴급 상황 신고 중 오류가 발생했습니다");
    }
  }

  /**
   * 낙상 감지 알림
   * 
   * <p>AI 또는 디바이스에서 낙상을 감지했을 때 호출됩니다.
   * 신뢰도에 따라 심각도가 자동으로 결정됩니다.</p>
   * 
   * @param request 낙상 감지 정보 (신뢰도, 위치, 이미지 등)
   * @return 생성된 긴급 상황 정보
   */
  @PostMapping("/fall-detection")
  @PreAuthorize("hasRole('USER') or hasRole('DEVICE')")
  public ResponseEntity<ApiResponse<EmergencyResponse>> reportFallDetection(
      @Valid @RequestBody FallDetectionRequest request) {
    log.error("낙상 감지 알림: confidence={}", request.getConfidence());
    
    try {
      EmergencyResponse response = emergencyService.handleFallDetection(request);
      return createCreatedResponse(response, "낙상이 감지되어 긴급 상황이 등록되었습니다");
    } catch (Exception e) {
      return handleException("낙상 감지 처리 실패", e, "낙상 감지 처리 중 오류가 발생했습니다");
    }
  }

  /**
   * 긴급 상황 상태 조회
   * 
   * <p>특정 긴급 상황의 현재 상태와 상세 정보를 조회합니다.
   * 본인, 보호자, 관리자만 조회 가능합니다.</p>
   * 
   * @param emergencyId 긴급 상황 ID
   * @return 긴급 상황 상세 정보
   */
  @GetMapping("/status/{emergencyId}")
  @PreAuthorize("@emergencyService.isOwnEmergency(#emergencyId) or " +
                "@emergencyService.isGuardianOfEmergency(#emergencyId) or " +
                "hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<EmergencyResponse>> getEmergencyStatus(
      @PathVariable Long emergencyId) {
    log.info("긴급 상황 상태 조회: emergencyId={}", emergencyId);
    
    try {
      EmergencyResponse response = emergencyService.getEmergencyStatus(emergencyId);
      return createSuccessResponse(response, "긴급 상황 정보를 가져왔습니다");
    } catch (Exception e) {
      return handleNotFoundException("긴급 상황 조회 실패: emergencyId=" + emergencyId, e, 
          "긴급 상황을 찾을 수 없습니다");
    }
  }

  /**
   * 사용자의 긴급 상황 이력 조회
   * 
   * <p>특정 사용자의 과거 긴급 상황 발생 이력을 페이지 단위로 조회합니다.
   * 최신 순으로 정렬되어 반환됩니다.</p>
   * 
   * @param userId 사용자 ID
   * @param pageable 페이지 정보 (기본값: size=20)
   * @return 긴급 상황 이력 페이지
   */
  @GetMapping("/history/{userId}")
  @PreAuthorize("#userId == authentication.principal.userId or " +
                "@guardianService.isGuardianOf(#userId) or " +
                "hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Page<EmergencyResponse>>> getUserEmergencyHistory(
      @PathVariable Long userId,
      @PageableDefault(size = 20) Pageable pageable) {
    log.info("긴급 상황 이력 조회: userId={}", userId);
    
    try {
      Page<EmergencyResponse> history = emergencyService.getUserEmergencyHistory(userId, pageable);
      return createSuccessResponse(history, "긴급 상황 이력을 가져왔습니다");
    } catch (Exception e) {
      return handleException("긴급 상황 이력 조회 실패: userId=" + userId, e, 
          "이력 조회 중 오류가 발생했습니다");
    }
  }

  /**
   * 활성 긴급 상황 목록 조회 (관리자/보호자)
   * 
   * <p>현재 활성화된(해결되지 않은) 모든 긴급 상황을 조회합니다.
   * 보호자와 관리자만 접근 가능합니다.</p>
   * 
   * @return 활성 긴급 상황 목록
   */
  @GetMapping("/active")
  @PreAuthorize("hasRole('GUARDIAN') or hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<List<EmergencyResponse>>> getActiveEmergencies() {
    log.info("활성 긴급 상황 목록 조회");
    
    try {
      List<EmergencyResponse> activeEmergencies = emergencyService.getActiveEmergencies();
      return createSuccessResponse(activeEmergencies, "활성 긴급 상황 목록을 가져왔습니다");
    } catch (Exception e) {
      return handleException("활성 긴급 상황 조회 실패", e, 
          "긴급 상황 조회 중 오류가 발생했습니다");
    }
  }

  /**
   * 긴급 상황 해결 처리
   * 
   * <p>활성화된 긴급 상황을 해결 완료로 처리합니다.
   * 해결자 정보와 메모를 기록할 수 있습니다.</p>
   * 
   * @param emergencyId 긴급 상황 ID
   * @param resolvedBy 해결한 사람 이름
   * @param notes 해결 관련 메모 (선택사항)
   * @return 업데이트된 긴급 상황 정보
   */
  @PutMapping("/{emergencyId}/resolve")
  @PreAuthorize("@emergencyService.isGuardianOfEmergency(#emergencyId) or hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<EmergencyResponse>> resolveEmergency(
      @PathVariable Long emergencyId,
      @RequestParam String resolvedBy,
      @RequestParam(required = false) String notes) {
    log.info("긴급 상황 해결 처리: emergencyId={}, resolvedBy={}", emergencyId, resolvedBy);
    
    try {
      EmergencyResponse response = emergencyService.resolveEmergency(emergencyId, resolvedBy, notes);
      return createSuccessResponse(response, "긴급 상황이 해결 처리되었습니다");
    } catch (Exception e) {
      return handleException("긴급 상황 해결 처리 실패: emergencyId=" + emergencyId, e, 
          "해결 처리 중 오류가 발생했습니다");
    }
  }
}