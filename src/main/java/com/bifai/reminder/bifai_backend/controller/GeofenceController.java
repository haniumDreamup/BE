package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceRequest;
import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceResponse;
import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import com.bifai.reminder.bifai_backend.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 지오펜스(안전 구역) 관리 API
 * BIF 사용자의 안전 구역 설정 및 모니터링
 */
@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Geofence", description = "안전 구역 관리 API")
public class GeofenceController {

  private final GeofenceService geofenceService;

  /**
   * 안전 구역 생성
   */
  @PostMapping
  @Operation(summary = "안전 구역 생성", description = "새로운 안전 구역을 만들어요")
  public ResponseEntity<ApiResponse> createGeofence(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody GeofenceRequest request) {
    
    log.info("Creating geofence for user: {}", userDetails.getUserId());
    
    GeofenceResponse response = geofenceService.createGeofence(
        userDetails.getUserId(), request);
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "안전 구역이 만들어졌어요"));
  }

  /**
   * 안전 구역 수정
   */
  @PutMapping("/{geofenceId}")
  @Operation(summary = "안전 구역 수정", description = "안전 구역 정보를 수정해요")
  public ResponseEntity<ApiResponse> updateGeofence(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable Long geofenceId,
      @Valid @RequestBody GeofenceRequest request) {
    
    log.info("Updating geofence {} for user: {}", geofenceId, userDetails.getUserId());
    
    GeofenceResponse response = geofenceService.updateGeofence(
        userDetails.getUserId(), geofenceId, request);
    
    return ResponseEntity.ok(ApiResponse.success(response, "안전 구역이 수정되었어요"));
  }

  /**
   * 안전 구역 삭제
   */
  @DeleteMapping("/{geofenceId}")
  @Operation(summary = "안전 구역 삭제", description = "안전 구역을 삭제해요")
  public ResponseEntity<ApiResponse> deleteGeofence(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable Long geofenceId) {
    
    log.info("Deleting geofence {} for user: {}", geofenceId, userDetails.getUserId());
    
    geofenceService.deleteGeofence(userDetails.getUserId(), geofenceId);
    
    return ResponseEntity.ok(ApiResponse.success(null, "안전 구역이 삭제되었어요"));
  }

  /**
   * 안전 구역 단일 조회
   */
  @GetMapping("/{geofenceId}")
  @Operation(summary = "안전 구역 조회", description = "특정 안전 구역 정보를 조회해요")
  public ResponseEntity<ApiResponse> getGeofence(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable Long geofenceId) {
    
    log.debug("Getting geofence {} for user: {}", geofenceId, userDetails.getUserId());
    
    GeofenceResponse response = geofenceService.getGeofence(
        userDetails.getUserId(), geofenceId);
    
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 내 안전 구역 목록 조회
   */
  @GetMapping
  @Operation(summary = "내 안전 구역 목록", description = "내가 설정한 모든 안전 구역을 조회해요")
  public ResponseEntity<ApiResponse> getMyGeofences(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @RequestParam(required = false) Boolean activeOnly) {
    
    log.debug("Getting geofences for user: {}, activeOnly: {}", 
             userDetails.getUserId(), activeOnly);
    
    List<GeofenceResponse> response;
    if (Boolean.TRUE.equals(activeOnly)) {
      response = geofenceService.getActiveGeofences(userDetails.getUserId());
    } else {
      response = geofenceService.getUserGeofences(userDetails.getUserId());
    }
    
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 내 안전 구역 페이징 조회
   */
  @GetMapping("/paged")
  @Operation(summary = "안전 구역 페이징 조회", description = "안전 구역을 페이지 단위로 조회해요")
  public ResponseEntity<ApiResponse> getMyGeofencesPaged(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PageableDefault(size = 10, sort = "priority", direction = Sort.Direction.DESC) 
      Pageable pageable) {
    
    log.debug("Getting paged geofences for user: {}", userDetails.getUserId());
    
    Page<GeofenceResponse> response = geofenceService.getUserGeofencesPaged(
        userDetails.getUserId(), pageable);
    
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 타입별 안전 구역 조회
   */
  @GetMapping("/type/{type}")
  @Operation(summary = "타입별 안전 구역 조회", description = "특정 타입의 안전 구역만 조회해요")
  public ResponseEntity<ApiResponse> getGeofencesByType(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable @Parameter(description = "구역 타입 (HOME, WORK, SCHOOL 등)") 
      Geofence.GeofenceType type) {
    
    log.debug("Getting {} type geofences for user: {}", type, userDetails.getUserId());
    
    List<GeofenceResponse> response = geofenceService.getGeofencesByType(
        userDetails.getUserId(), type);
    
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 안전 구역 활성화/비활성화 토글
   */
  @PatchMapping("/{geofenceId}/toggle")
  @Operation(summary = "안전 구역 활성화 토글", description = "안전 구역을 켜거나 꺼요")
  public ResponseEntity<ApiResponse> toggleGeofence(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable Long geofenceId) {
    
    log.info("Toggling geofence {} for user: {}", geofenceId, userDetails.getUserId());
    
    GeofenceResponse response = geofenceService.toggleGeofenceActive(
        userDetails.getUserId(), geofenceId);
    
    String message = response.getIsActive() ? "안전 구역이 켜졌어요" : "안전 구역이 꺼졌어요";
    return ResponseEntity.ok(ApiResponse.success(response, message));
  }

  /**
   * 안전 구역 우선순위 변경
   */
  @PutMapping("/priorities")
  @Operation(summary = "우선순위 변경", description = "안전 구역들의 우선순위를 변경해요")
  public ResponseEntity<ApiResponse> updatePriorities(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @RequestBody List<Long> geofenceIds) {
    
    log.info("Updating priorities for user: {}", userDetails.getUserId());
    
    geofenceService.updateGeofencePriorities(userDetails.getUserId(), geofenceIds);
    
    return ResponseEntity.ok(ApiResponse.success(null, "우선순위가 변경되었어요"));
  }

  /**
   * 안전 구역 통계
   */
  @GetMapping("/stats")
  @Operation(summary = "안전 구역 통계", description = "내 안전 구역 사용 통계를 확인해요")
  public ResponseEntity<ApiResponse> getGeofenceStats(
      @AuthenticationPrincipal BifUserDetails userDetails) {
    
    log.debug("Getting geofence stats for user: {}", userDetails.getUserId());
    
    // TODO: 통계 서비스 구현
    return ResponseEntity.ok(ApiResponse.success(null, "통계 기능은 준비중이에요"));
  }
}