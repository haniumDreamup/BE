package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService;
import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService.VisionAnalysisResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 이미지 분석 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vision")
@RequiredArgsConstructor
@Tag(name = "Vision API", description = "이미지 분석 관련 API")
public class VisionController {
  
  private final GoogleVisionService googleVisionService;
  
  @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "이미지 분석", description = "이미지를 분석하여 BIF 사용자를 위한 설명을 생성합니다")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<ApiResponse<VisionAnalysisResult>> analyzeImage(
      @Parameter(description = "분석할 이미지 파일", required = true)
      @RequestParam("image") @NotNull MultipartFile imageFile) {
    
    log.info("이미지 분석 요청 - 파일명: {}, 크기: {} bytes", 
        imageFile.getOriginalFilename(), imageFile.getSize());
    
    try {
      // 파일 검증
      validateImageFile(imageFile);
      
      // 이미지 분석
      VisionAnalysisResult result = googleVisionService.analyzeImage(imageFile);
      
      log.info("이미지 분석 완료 - 객체: {}개, 텍스트: {}", 
          result.getObjects().size(), 
          result.getText() != null ? "있음" : "없음");
      
      return ResponseEntity.ok(ApiResponse.success(result, "이미지 분석이 완료되었습니다"));
      
    } catch (IllegalArgumentException e) {
      log.warn("이미지 검증 실패: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(ApiResponse.error(
              "IMAGE_VALIDATION_ERROR",
              e.getMessage(),
              "올바른 이미지 파일을 선택해주세요"
          ));
    } catch (IOException e) {
      log.error("이미지 분석 오류", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error(
              "IMAGE_ANALYSIS_ERROR",
              "이미지 분석 중 오류가 발생했습니다",
              "잠시 후 다시 시도해주세요"
          ));
    }
  }
  
  @PostMapping(value = "/detect-danger", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "위험 요소 감지", description = "이미지에서 위험 요소를 감지합니다")
  @PreAuthorize("hasRole('USER')")
  public ResponseEntity<ApiResponse<DangerDetectionResult>> detectDanger(
      @Parameter(description = "검사할 이미지 파일", required = true)
      @RequestParam("image") @NotNull MultipartFile imageFile) {
    
    log.info("위험 요소 감지 요청 - 파일명: {}", imageFile.getOriginalFilename());
    
    try {
      validateImageFile(imageFile);
      
      // 이미지 분석
      VisionAnalysisResult analysis = googleVisionService.analyzeImage(imageFile);
      
      // 위험 요소 판단
      DangerDetectionResult dangerResult = analyzeDanger(analysis);
      
      return ResponseEntity.ok(ApiResponse.success(dangerResult, "위험 요소 감지가 완료되었습니다"));
      
    } catch (Exception e) {
      log.error("위험 요소 감지 오류", e);
      return ResponseEntity.internalServerError()
          .body(ApiResponse.error(
              "DANGER_DETECTION_ERROR",
              "위험 요소 감지 중 오류가 발생했습니다",
              "잠시 후 다시 시도해주세요"
          ));
    }
  }
  
  /**
   * 이미지 파일 검증
   */
  private void validateImageFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("이미지 파일이 비어있습니다");
    }
    
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다");
    }
    
    // 파일 크기 제한 (10MB)
    long maxSize = 10 * 1024 * 1024;
    if (file.getSize() > maxSize) {
      throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다");
    }
  }
  
  /**
   * 위험 요소 분석
   */
  private DangerDetectionResult analyzeDanger(VisionAnalysisResult analysis) {
    DangerDetectionResult result = new DangerDetectionResult();
    
    // 위험 키워드 체크
    String[] dangerKeywords = {"fire", "불", "knife", "칼", "weapon", "무기", "blood", "피"};
    
    // 객체에서 위험 요소 찾기
    for (var obj : analysis.getObjects()) {
      for (String keyword : dangerKeywords) {
        if (obj.getName().toLowerCase().contains(keyword)) {
          result.addDanger("위험한 물건이 있어요: " + obj.getName());
        }
      }
    }
    
    // 안전성 정보 체크
    if (analysis.getSafetyInfo() != null) {
      var safety = analysis.getSafetyInfo();
      if (!safety.getViolence().equals("VERY_UNLIKELY") && 
          !safety.getViolence().equals("UNLIKELY")) {
        result.addDanger("폭력적인 내용이 포함되어 있을 수 있어요");
      }
    }
    
    // 간단한 조언 생성
    if (result.isDangerous()) {
      result.setAdvice("주의가 필요해요. 안전한 곳으로 이동하거나 도움을 요청하세요.");
    } else {
      result.setAdvice("안전해 보여요. 걱정하지 마세요.");
    }
    
    return result;
  }
  
  /**
   * 위험 감지 결과 DTO
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