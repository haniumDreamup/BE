package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.response.BifApiResponse;
import com.bifai.reminder.bifai_backend.dto.sos.SosRequest;
import com.bifai.reminder.bifai_backend.dto.sos.SosResponse;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.service.SosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SOS 컨트롤러
 * 원터치 긴급 도움 요청 API
 */
@RestController
@RequestMapping("/api/v1/sos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SOS", description = "긴급 도움 요청 API")
public class SosController {

  private final SosService sosService;

  /**
   * SOS 발동
   */
  @PostMapping("/trigger")
  @Operation(summary = "SOS 발동", description = "원터치로 긴급 도움을 요청합니다")
  public ResponseEntity<BifApiResponse<SosResponse>> triggerSos(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody SosRequest request) {
    
    log.warn("SOS 요청: 사용자 {}", userDetails.getUsername());
    
    // TODO: UserDetails에서 userId 추출 로직 구현 필요
    Long userId = 1L; // 임시 값
    
    SosResponse response = sosService.triggerSos(userId, request);
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BifApiResponse.success(
            response,
            "도움 요청이 전달되었습니다"
        ));
  }

  /**
   * SOS 취소
   */
  @PutMapping("/{emergencyId}/cancel")
  @Operation(summary = "SOS 취소", description = "발동된 SOS를 취소합니다")
  public ResponseEntity<BifApiResponse<Void>> cancelSos(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long emergencyId) {
    
    log.info("SOS 취소 요청: 사용자 {}, 긴급ID {}", 
        userDetails.getUsername(), emergencyId);
    
    Long userId = 1L; // 임시 값
    sosService.cancelSos(userId, emergencyId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(null, "SOS가 취소되었습니다")
    );
  }

  /**
   * 최근 SOS 이력 조회
   */
  @GetMapping("/history")
  @Operation(summary = "SOS 이력 조회", description = "최근 SOS 발동 이력을 조회합니다")
  public ResponseEntity<BifApiResponse<List<Emergency>>> getSosHistory(
      @AuthenticationPrincipal UserDetails userDetails) {
    
    log.info("SOS 이력 조회: 사용자 {}", userDetails.getUsername());
    
    Long userId = 1L; // 임시 값
    List<Emergency> history = sosService.getRecentSosHistory(userId);
    
    return ResponseEntity.ok(
        BifApiResponse.success(
            history,
            history.size() + "개의 SOS 이력이 있습니다"
        )
    );
  }

  /**
   * 간단한 SOS (최소 정보만 전송)
   */
  @PostMapping("/quick")
  @Operation(summary = "빠른 SOS", description = "최소 정보만으로 빠르게 SOS를 발동합니다")
  public ResponseEntity<BifApiResponse<SosResponse>> quickSos(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam Double latitude,
      @RequestParam Double longitude) {
    
    log.warn("빠른 SOS 요청: 사용자 {}, 위치 {},{}", 
        userDetails.getUsername(), latitude, longitude);
    
    // 최소 정보로 SOS 요청 생성
    SosRequest quickRequest = SosRequest.builder()
        .latitude(latitude)
        .longitude(longitude)
        .emergencyType("PANIC")
        .message("긴급 도움 필요")
        .notifyAllContacts(true)
        .shareLocation(true)
        .build();
    
    Long userId = 1L; // 임시 값
    SosResponse response = sosService.triggerSos(userId, quickRequest);
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(BifApiResponse.success(
            response,
            "긴급 도움 요청이 전달되었습니다"
        ));
  }
}