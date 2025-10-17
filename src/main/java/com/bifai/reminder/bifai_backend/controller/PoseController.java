package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseResponseDto;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import com.bifai.reminder.bifai_backend.service.pose.PoseDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pose 데이터 처리 컨트롤러
 * MediaPipe Pose 데이터를 수신하고 낙상 감지를 수행
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pose")
@RequiredArgsConstructor
@Tag(name = "Pose API", description = "MediaPipe Pose 데이터 처리 및 낙상 감지")
public class PoseController {

  private final PoseDataService poseDataService;
  private final JwtAuthUtils jwtAuthUtils;
  
  @PostMapping("/data")
  @Operation(summary = "Pose 데이터 전송", description = "MediaPipe에서 추출한 pose 데이터를 전송합니다")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<ApiResponse<PoseResponseDto>> receivePoseData(
      @Valid @RequestBody PoseDataDto poseData) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 Pose 데이터 전송 시도");
      return ResponseEntity.status(401)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    log.debug("Pose 데이터 수신 - userId: {}, timestamp: {}",
        poseData.getUserId(), poseData.getTimestamp());

    try {
      // Pose 데이터 처리
      PoseResponseDto result = poseDataService.processPoseData(poseData);

      return ResponseEntity.ok(ApiResponse.success(
          result,
          "포즈 데이터가 처리되었습니다"
      ));

    } catch (Exception e) {
      log.error("Pose 데이터 처리 중 오류", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("OPERATION_FAILED", "포즈 데이터 처리에 실패했습니다"));
    }
  }
  
  @PostMapping("/data/batch")
  @Operation(summary = "Pose 데이터 일괄 전송",
      description = "여러 프레임의 pose 데이터를 한 번에 전송합니다")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<ApiResponse<List<PoseResponseDto>>> receivePoseDataBatch(
      @Valid @RequestBody List<@Valid PoseDataDto> poseDataList) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 Pose 데이터 일괄 전송 시도");
      return ResponseEntity.status(401)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // 빈 리스트 검증 - 비즈니스 로직상 빈 배열은 400 에러
    if (poseDataList == null || poseDataList.isEmpty()) {
      log.warn("빈 Pose 데이터 리스트 전송 시도");
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("VALIDATION_ERROR", "포즈 데이터가 비어있습니다"));
    }

    log.debug("Pose 데이터 일괄 수신 - 프레임 수: {}", poseDataList.size());

    try {
      // 일괄 처리
      List<PoseResponseDto> result = poseDataService.processPoseDataBatch(poseDataList);

      return ResponseEntity.ok(ApiResponse.success(
          result,
          poseDataList.size() + "개의 포즈 데이터가 처리되었습니다"
      ));

    } catch (Exception e) {
      log.error("Pose 데이터 일괄 처리 중 오류", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("OPERATION_FAILED", "포즈 데이터 일괄 처리에 실패했습니다"));
    }
  }
  
  @GetMapping("/fall-status/{userId}")
  @Operation(summary = "낙상 감지 상태 조회",
      description = "특정 사용자의 낙상 감지 상태와 최근 이벤트를 조회합니다")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  public ResponseEntity<ApiResponse<FallStatusDto>> getFallStatus(
      @PathVariable Long userId) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 낙상 상태 조회 시도");
      return ResponseEntity.status(401)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // userId 유효성 검증
    if (userId == null || userId <= 0) {
      log.warn("잘못된 사용자 ID로 낙상 상태 조회 시도: {}", userId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("VALIDATION_ERROR", "올바른 사용자 ID를 입력해주세요"));
    }

    log.debug("낙상 상태 조회 - userId: {}", userId);

    try {
      FallStatusDto status = poseDataService.getFallStatus(userId);

      return ResponseEntity.ok(ApiResponse.success(
          status,
          "낙상 상태를 조회했습니다"
      ));

    } catch (Exception e) {
      log.error("낙상 상태 조회 중 오류", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("OPERATION_FAILED", "낙상 상태 조회에 실패했습니다"));
    }
  }
  
  @PostMapping("/fall-event/{eventId}/feedback")
  @Operation(summary = "낙상 이벤트 피드백",
      description = "낙상 감지 결과에 대한 피드백을 제출합니다 (오탐지 등)")
  @PreAuthorize("hasRole('USER') or hasRole('GUARDIAN')")
  public ResponseEntity<ApiResponse<Void>> submitFallFeedback(
      @PathVariable Long eventId,
      @RequestBody Map<String, Object> feedback) {

    // 인증 확인
    Long currentUserId = jwtAuthUtils.getCurrentUserId();
    if (currentUserId == null) {
      log.warn("인증되지 않은 사용자의 낙상 피드백 제출 시도");
      return ResponseEntity.status(401)
          .body(ApiResponse.error("UNAUTHORIZED", "로그인이 필요합니다"));
    }

    // eventId 유효성 검증
    if (eventId == null || eventId <= 0) {
      log.warn("잘못된 이벤트 ID로 낙상 피드백 제출 시도: {}", eventId);
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("VALIDATION_ERROR", "올바른 이벤트 ID를 입력해주세요"));
    }

    log.debug("낙상 피드백 제출 - eventId: {}", eventId);

    try {
      Boolean isFalsePositive = (Boolean) feedback.get("isFalsePositive");
      String userComment = (String) feedback.get("userComment");

      poseDataService.submitFallFeedback(eventId, isFalsePositive, userComment);

      return ResponseEntity.ok(ApiResponse.success(
          null,
          "피드백이 성공적으로 제출되었습니다"
      ));

    } catch (Exception e) {
      log.error("낙상 피드백 제출 중 오류", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error("OPERATION_FAILED", "피드백 제출에 실패했습니다"));
    }
  }
}