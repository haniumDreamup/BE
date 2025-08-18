package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.image.ImageAnalysisResponse;
import com.bifai.reminder.bifai_backend.dto.image.ImageUploadRequest;
import com.bifai.reminder.bifai_backend.dto.response.BifApiResponse;
import com.bifai.reminder.bifai_backend.service.ImageAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 이미지 분석 컨트롤러
 * 웨어러블 카메라 이미지 분석 API
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Image Analysis", description = "이미지 분석 API")
public class ImageAnalysisController {

  private final ImageAnalysisService imageAnalysisService;

  /**
   * 이미지 업로드 및 분석
   */
  @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "이미지 분석", description = "이미지를 업로드하고 AI 분석을 시작합니다")
  public ResponseEntity<BifApiResponse<ImageAnalysisResponse>> analyzeImage(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestPart("image") MultipartFile imageFile,
      @RequestPart(value = "request", required = false) @Valid ImageUploadRequest request) {
    
    log.info("이미지 분석 요청: 사용자 {}, 파일명 {}", 
        userDetails.getUsername(), imageFile.getOriginalFilename());
    
    // 파일 검증
    if (imageFile.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(BifApiResponse.error(
              "FILE_EMPTY",
              "이미지 파일이 비어있습니다",
              "이미지 파일을 선택해주세요"
          ));
    }
    
    // 파일 크기 제한 (10MB)
    if (imageFile.getSize() > 10 * 1024 * 1024) {
      return ResponseEntity.badRequest()
          .body(BifApiResponse.error(
              "FILE_TOO_LARGE",
              "파일 크기는 10MB를 초과할 수 없습니다",
              "더 작은 크기의 이미지를 선택해주세요"
          ));
    }
    
    // 기본 요청 객체 생성
    if (request == null) {
      request = ImageUploadRequest.builder()
          .analysisType("ON_DEMAND")
          .requiresVoiceGuidance(true)
          .build();
    }
    
    try {
      Long userId = 1L; // TODO: UserDetails에서 userId 추출
      
      ImageAnalysisResponse response = imageAnalysisService
          .uploadAndAnalyze(userId, imageFile, request);
      
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(BifApiResponse.success(
              response,
              "이미지 분석이 시작되었습니다"
          ));
      
    } catch (IOException e) {
      log.error("이미지 업로드 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "UPLOAD_FAILED",
              "이미지 업로드에 실패했습니다",
              "다시 시도해주세요"
          ));
    }
  }

  /**
   * 분석 결과 조회
   */
  @GetMapping("/analysis/{analysisId}")
  @Operation(summary = "분석 결과 조회", description = "이미지 분석 결과를 조회합니다")
  public ResponseEntity<BifApiResponse<ImageAnalysisResponse>> getAnalysisResult(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long analysisId) {
    
    log.info("분석 결과 조회: ID {}", analysisId);
    
    try {
      ImageAnalysisResponse response = imageAnalysisService
          .getAnalysisResult(analysisId);
      
      String message = response.getAnalysisStatus().equals("COMPLETED") 
          ? "분석이 완료되었습니다"
          : "분석 중입니다";
      
      return ResponseEntity.ok(
          BifApiResponse.success(response, message)
      );
      
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(BifApiResponse.error(
              "NOT_FOUND",
              "분석 결과를 찾을 수 없습니다",
              "분석 ID를 확인해주세요"
          ));
    }
  }

  /**
   * 빠른 이미지 분석 (긴급 상황)
   */
  @PostMapping(value = "/quick-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "빠른 분석", description = "긴급 상황에서 빠른 이미지 분석")
  public ResponseEntity<BifApiResponse<ImageAnalysisResponse>> quickAnalyze(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestPart("image") MultipartFile imageFile,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {
    
    log.warn("빠른 분석 요청: 사용자 {}", userDetails.getUsername());
    
    // 긴급 분석 요청 생성
    ImageUploadRequest quickRequest = ImageUploadRequest.builder()
        .analysisType("EMERGENCY")
        .latitude(latitude)
        .longitude(longitude)
        .urgent(true)
        .requiresVoiceGuidance(true)
        .build();
    
    try {
      Long userId = 1L; // TODO: UserDetails에서 userId 추출
      
      ImageAnalysisResponse response = imageAnalysisService
          .uploadAndAnalyze(userId, imageFile, quickRequest);
      
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(BifApiResponse.success(
              response,
              "긴급 분석이 시작되었습니다"
          ));
      
    } catch (IOException e) {
      log.error("빠른 분석 실패: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "ANALYSIS_FAILED",
              "이미지 처리에 실패했습니다",
              "다시 시도해주세요"
          ));
    }
  }
}