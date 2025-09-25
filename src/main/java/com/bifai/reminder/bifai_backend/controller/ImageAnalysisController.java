package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.image.ImageAnalysisResponse;
import com.bifai.reminder.bifai_backend.dto.image.ImageUploadRequest;
import com.bifai.reminder.bifai_backend.dto.response.BifApiResponse;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthUtils;
import com.bifai.reminder.bifai_backend.service.ImageAnalysisService;
import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService;
import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService.VisionAnalysisResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 이미지 분석 컨트롤러
 * 웨어러블 카메라 이미지 분석 API
 */
@RestController
@RequestMapping("/api/images")
@Slf4j
@Tag(name = "Image Analysis", description = "이미지 분석 API")
public class ImageAnalysisController {

  private final ImageAnalysisService imageAnalysisService;
  private final JwtAuthUtils jwtAuthUtils;
  private GoogleVisionService googleVisionService;

  public ImageAnalysisController(ImageAnalysisService imageAnalysisService,
                                JwtAuthUtils jwtAuthUtils,
                                @Autowired(required = false) GoogleVisionService googleVisionService) {
    this.imageAnalysisService = imageAnalysisService;
    this.jwtAuthUtils = jwtAuthUtils;
    this.googleVisionService = googleVisionService;
  }

  /**
   * 이미지 업로드 및 분석
   */
  @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "이미지 분석", description = "이미지를 업로드하고 AI 분석을 시작합니다")
  public ResponseEntity<BifApiResponse<ImageAnalysisResponse>> analyzeImage(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestPart(value = "image", required = false) MultipartFile imageFile,
      @RequestPart(value = "request", required = false) @Valid ImageUploadRequest request) {

    // 인증 확인 - JwtAuthUtils 사용
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.warn("인증되지 않은 사용자의 이미지 분석 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(BifApiResponse.error(
              "UNAUTHORIZED",
              "로그인이 필요합니다",
              "다시 로그인해주세요"
          ));
    }

    String username = userDetails != null ? userDetails.getUsername() : "user:" + userId;
    log.info("이미지 분석 요청: 사용자 {}, 파일명 {}",
        username, imageFile != null ? imageFile.getOriginalFilename() : "없음");

    // 파일 존재 확인
    if (imageFile == null) {
      return ResponseEntity.badRequest()
          .body(BifApiResponse.error(
              "FILE_MISSING",
              "이미지 파일이 필요합니다",
              "이미지 파일을 선택해주세요"
          ));
    }

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
      log.info("이미지 분석 요청: 사용자 ID {}, 파일명 {}",
          userId, imageFile.getOriginalFilename());

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
    } catch (Exception e) {
      log.error("이미지 분석 중 예상치 못한 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "ANALYSIS_ERROR",
              "이미지 분석 중 오류가 발생했습니다",
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

    // 인증 확인 - JwtAuthUtils 사용
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.warn("인증되지 않은 사용자의 분석 결과 조회 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(BifApiResponse.error(
              "UNAUTHORIZED",
              "로그인이 필요합니다",
              "다시 로그인해주세요"
          ));
    }

    // analysisId 유효성 검증
    if (analysisId == null || analysisId <= 0) {
      log.warn("잘못된 분석 ID로 조회 시도: {}", analysisId);
      return ResponseEntity.badRequest()
          .body(BifApiResponse.error(
              "INVALID_ANALYSIS_ID",
              "올바른 분석 ID를 입력해주세요",
              "분석 ID는 양수여야 합니다"
          ));
    }

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
    } catch (Exception e) {
      log.error("분석 결과 조회 중 예상치 못한 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "RESULT_ERROR",
              "분석 결과 조회 중 오류가 발생했습니다",
              "다시 시도해주세요"
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
      @RequestPart(value = "image", required = false) MultipartFile imageFile,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {

    // 인증 확인 - JwtAuthUtils 사용
    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.warn("인증되지 않은 사용자의 빠른 분석 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(BifApiResponse.error(
              "UNAUTHORIZED",
              "로그인이 필요합니다",
              "다시 로그인해주세요"
          ));
    }

    String username = userDetails != null ? userDetails.getUsername() : "user:" + userId;
    log.warn("빠른 분석 요청: 사용자 {}", username);

    // 파일 존재 확인
    if (imageFile == null) {
      return ResponseEntity.badRequest()
          .body(BifApiResponse.error(
              "FILE_MISSING",
              "이미지 파일이 필요합니다",
              "이미지 파일을 선택해주세요"
          ));
    }

    // 긴급 분석 요청 생성
    ImageUploadRequest quickRequest = ImageUploadRequest.builder()
        .analysisType("EMERGENCY")
        .latitude(latitude)
        .longitude(longitude)
        .urgent(true)
        .requiresVoiceGuidance(true)
        .build();

    try {
      log.warn("빠른 분석 요청: 사용자 ID {}, 위치 {},{}",
          userId, latitude, longitude);

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
    } catch (Exception e) {
      log.error("빠른 분석 중 예상치 못한 오류: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "QUICK_ANALYSIS_ERROR",
              "빠른 분석 중 오류가 발생했습니다",
              "다시 시도해주세요"
          ));
    }
  }

  /**
   * Google Vision API를 통한 직접 이미지 분석
   */
  @PostMapping(value = "/vision-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Vision API 이미지 분석", description = "Google Vision API로 직접 이미지를 분석합니다")
  public ResponseEntity<BifApiResponse<VisionAnalysisResult>> visionAnalyze(
      @AuthenticationPrincipal UserDetails userDetails,
      @Parameter(description = "분석할 이미지 파일", required = true)
      @RequestPart("image") @NotNull MultipartFile imageFile) {

    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.warn("인증되지 않은 사용자의 Vision 분석 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(BifApiResponse.error(
              "UNAUTHORIZED",
              "로그인이 필요합니다",
              "다시 로그인해주세요"
          ));
    }

    log.info("Vision API 분석 요청 - 파일명: {}, 크기: {} bytes",
        imageFile.getOriginalFilename(), imageFile.getSize());

    try {
      validateImageFile(imageFile);

      if (googleVisionService == null) {
        log.warn("GoogleVisionService가 사용 불가능합니다 (테스트 환경)");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(BifApiResponse.error(
                "SERVICE_UNAVAILABLE",
                "Vision 분석 서비스를 사용할 수 없습니다",
                "나중에 다시 시도해주세요"
            ));
      }

      VisionAnalysisResult result = googleVisionService.analyzeImage(imageFile);

      log.info("Vision API 분석 완료 - 객체: {}개, 텍스트: {}",
          result.getObjects().size(),
          result.getText() != null ? "있음" : "없음");

      return ResponseEntity.ok(BifApiResponse.success(result, "이미지 분석이 완료되었습니다"));

    } catch (IllegalArgumentException e) {
      log.warn("이미지 검증 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(BifApiResponse.error(
              "IMAGE_VALIDATION_ERROR",
              e.getMessage(),
              "올바른 이미지 파일을 선택해주세요"
          ));
    } catch (IOException e) {
      log.error("Vision API 분석 오류", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "VISION_ANALYSIS_ERROR",
              "이미지 분석 중 오류가 발생했습니다",
              "잠시 후 다시 시도해주세요"
          ));
    }
  }

  /**
   * 위험 요소 감지
   */
  @PostMapping(value = "/detect-danger", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "위험 요소 감지", description = "이미지에서 위험 요소를 감지합니다")
  public ResponseEntity<BifApiResponse<DangerDetectionResult>> detectDanger(
      @AuthenticationPrincipal UserDetails userDetails,
      @Parameter(description = "검사할 이미지 파일", required = true)
      @RequestPart("image") @NotNull MultipartFile imageFile) {

    Long userId = jwtAuthUtils.getCurrentUserId();
    if (userId == null) {
      log.warn("인증되지 않은 사용자의 위험 감지 시도");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(BifApiResponse.error(
              "UNAUTHORIZED",
              "로그인이 필요합니다",
              "다시 로그인해주세요"
          ));
    }

    log.info("위험 요소 감지 요청 - 파일명: {}", imageFile.getOriginalFilename());

    try {
      validateImageFile(imageFile);

      if (googleVisionService == null) {
        log.warn("GoogleVisionService가 사용 불가능합니다 (테스트 환경)");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(BifApiResponse.error(
                "SERVICE_UNAVAILABLE",
                "위험 감지 서비스를 사용할 수 없습니다",
                "나중에 다시 시도해주세요"
            ));
      }

      VisionAnalysisResult analysis = googleVisionService.analyzeImage(imageFile);
      DangerDetectionResult dangerResult = analyzeDanger(analysis);

      return ResponseEntity.ok(BifApiResponse.success(dangerResult, "위험 요소 감지가 완료되었습니다"));

    } catch (Exception e) {
      log.error("위험 요소 감지 오류", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(BifApiResponse.error(
              "DANGER_DETECTION_ERROR",
              "위험 요소 감지 중 오류가 발생했습니다",
              "잠시 후 다시 시도해주세요"
          ));
    }
  }

  /**
   * 이미지 파일 검증 (VisionController에서 가져온 로직)
   */
  private void validateImageFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("이미지 파일이 비어있습니다");
    }

    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다");
    }

    long maxSize = 10 * 1024 * 1024;
    if (file.getSize() > maxSize) {
      throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다");
    }
  }

  /**
   * 위험 요소 분석 (VisionController에서 가져온 로직)
   */
  private DangerDetectionResult analyzeDanger(VisionAnalysisResult analysis) {
    DangerDetectionResult result = new DangerDetectionResult();

    String[] dangerKeywords = {"fire", "불", "knife", "칼", "weapon", "무기", "blood", "피"};

    for (var obj : analysis.getObjects()) {
      for (String keyword : dangerKeywords) {
        if (obj.getName().toLowerCase().contains(keyword)) {
          result.addDanger("위험한 물건이 있어요: " + obj.getName());
        }
      }
    }

    if (analysis.getSafetyInfo() != null) {
      var safety = analysis.getSafetyInfo();
      if (!safety.getViolence().equals("VERY_UNLIKELY") &&
          !safety.getViolence().equals("UNLIKELY")) {
        result.addDanger("폭력적인 내용이 포함되어 있을 수 있어요");
      }
    }

    if (result.isDangerous()) {
      result.setAdvice("주의가 필요해요. 안전한 곳으로 이동하거나 도움을 요청하세요.");
    } else {
      result.setAdvice("안전해 보여요. 걱정하지 마세요.");
    }

    return result;
  }

  /**
   * 위험 감지 결과 DTO (VisionController에서 가져온 클래스)
   */
  @lombok.Data
  public static class DangerDetectionResult {
    private boolean isDangerous = false;
    private List<String> dangers = new ArrayList<>();
    private String advice;

    public void addDanger(String danger) {
      this.isDangerous = true;
      this.dangers.add(danger);
    }
  }
}