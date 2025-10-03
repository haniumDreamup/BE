package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.image.ImageAnalysisResponse;
import com.bifai.reminder.bifai_backend.dto.image.ImageUploadRequest;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis.AnalysisStatus;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis.AnalysisType;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis.SafetyLevel;
import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.ImageAnalysisRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.mobile.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 이미지 분석 서비스
 * 이미지 업로드, AI 분석, 상황 설명 생성
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ImageAnalysisService {

  private final ImageAnalysisRepository imageAnalysisRepository;
  private final UserRepository userRepository;
  private final MediaService mediaService;
  private final VoiceGuidanceService voiceGuidanceService;
  private final ObjectMapper objectMapper;

  @Autowired(required = false)
  private OpenAIService openAIService;

  @Autowired(required = false)
  private com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService googleVisionService;

  /**
   * 이미지 업로드 및 분석 시작
   */
  public ImageAnalysisResponse uploadAndAnalyze(
      Long userId,
      MultipartFile imageFile,
      ImageUploadRequest request) throws IOException {

    log.info("이미지 분석 시작: 사용자 {}, 타입 {}", userId, request.getAnalysisType());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // S3에 이미지 업로드
    MediaFile mediaFile = uploadToS3(imageFile, userId);
    String imageUrl = mediaService.getFileUrl(mediaFile);

    // 분석 엔티티 생성
    ImageAnalysis analysis = createImageAnalysis(user, mediaFile, imageUrl, request);

    // 비동기로 AI 분석 시작 (MultipartFile 전달)
    processImageAsync(analysis, request, imageFile);

    // 즉시 응답 반환 (분석은 백그라운드에서 진행)
    return buildResponse(analysis);
  }

  /**
   * S3 업로드
   */
  private MediaFile uploadToS3(MultipartFile file, Long userId) throws IOException {
    try {
      MediaFile mediaFile = mediaService.uploadFile(userId, file, MediaFile.UploadType.ACTIVITY);
      log.info("이미지 S3 업로드 완료: 사용자 {}, 미디어파일 ID {}, S3 키 {}", 
          userId, mediaFile.getId(), mediaFile.getS3Key());
      return mediaFile;
    } catch (Exception e) {
      log.error("S3 업로드 실패: 사용자 {}, 파일명 {}", userId, file.getOriginalFilename(), e);
      throw new IOException("이미지 업로드에 실패했습니다: " + e.getMessage(), e);
    }
  }

  /**
   * 이미지 분석 엔티티 생성
   */
  private ImageAnalysis createImageAnalysis(
      User user, 
      MediaFile mediaFile,
      String imageUrl, 
      ImageUploadRequest request) {
    
    AnalysisType type = mapAnalysisType(request.getAnalysisType());
    
    ImageAnalysis analysis = ImageAnalysis.builder()
        .user(user)
        .imageUrl(imageUrl)
        .analysisType(type)
        .latitude(request.getLatitude())
        .longitude(request.getLongitude())
        .address(request.getAddress())
        .analysisStatus(AnalysisStatus.UPLOADED)
        .build();
    
    // MediaFile과의 연관관계 설정 (필요한 경우 ImageAnalysis 엔티티에 mediaFile 필드 추가)
    log.debug("이미지 분석 엔티티 생성: 미디어파일 ID {}, S3키 {}", 
        mediaFile.getId(), mediaFile.getS3Key());
    
    return imageAnalysisRepository.save(analysis);
  }

  /**
   * 비동기 이미지 처리
   */
  @Async
  public void processImageAsync(ImageAnalysis analysis, ImageUploadRequest request) {
    processImageAsync(analysis, request, null);
  }

  /**
   * 비동기 이미지 처리 (MultipartFile 포함)
   */
  @Async
  public void processImageAsync(ImageAnalysis analysis, ImageUploadRequest request, MultipartFile imageFile) {
    try {
      analysis.startProcessing();
      imageAnalysisRepository.save(analysis);

      List<Map<String, Object>> objects = new ArrayList<>();
      String extractedText = "";

      // Google Vision API 사용 가능 여부 확인
      if (googleVisionService != null && imageFile != null) {
        try {
          // 1. Google Vision API로 이미지 분석
          log.info("Google Vision API를 사용하여 이미지 분석 시작");
          var visionResult = googleVisionService.analyzeImage(imageFile);

          // 2. 객체 인식 결과 변환
          objects = convertVisionObjectsToMap(visionResult.getObjects());
          analysis.setDetectedObjects(objectMapper.writeValueAsString(objects));
          analysis.setObjectCount(objects.size());

          // 3. 텍스트 추출 결과
          extractedText = visionResult.getText() != null ? visionResult.getText() : "";
          analysis.setExtractedText(extractedText);

          log.info("Google Vision API 분석 완료: 객체 {}개, 텍스트 {}자",
              objects.size(), extractedText.length());

        } catch (Exception e) {
          log.error("Google Vision API 분석 실패, 폴백 사용: {}", e.getMessage());
          objects = createFallbackObjects();
          analysis.setDetectedObjects(objectMapper.writeValueAsString(objects));
          analysis.setObjectCount(objects.size());
        }
      } else {
        // Google Vision API 사용 불가 시 폴백
        log.warn("Google Vision API를 사용할 수 없습니다. 폴백 데이터를 사용합니다.");
        objects = createFallbackObjects();
        analysis.setDetectedObjects(objectMapper.writeValueAsString(objects));
        analysis.setObjectCount(objects.size());
      }
      
      // 3. 상황 해석 (OpenAI)
      Map<String, String> interpretation;
      if (openAIService != null) {
        interpretation = openAIService.interpretSituation(
            objects, 
            extractedText, 
            request.getUserQuestion(),
            request.getContext()
        );
      } else {
        log.warn("OpenAI 서비스를 사용할 수 없습니다. 기본 해석을 제공합니다.");
        interpretation = createDefaultInterpretation(objects, extractedText, request.getUserQuestion());
      }
      
      analysis.setSituationDescription(interpretation.get("description"));
      analysis.setActionSuggestion(interpretation.get("action"));
      analysis.setSafetyLevel(determineSafetyLevel(interpretation.get("safety")));
      
      // 4. 완료 처리
      analysis.completeAnalysis();
      analysis.setAiConfidenceScore(0.85f); // 임시 값
      
      // 5. 음성 안내
      if (Boolean.TRUE.equals(request.getRequiresVoiceGuidance())) {
        provideVoiceGuidance(analysis);
      }
      
      // 6. 긴급 상황 처리
      if (analysis.getSafetyLevel() == SafetyLevel.DANGER) {
        handleEmergency(analysis);
      }
      
      imageAnalysisRepository.save(analysis);
      log.info("이미지 분석 완료: ID {}", analysis.getId());
      
    } catch (Exception e) {
      log.error("이미지 분석 실패: {}", e.getMessage());
      analysis.failAnalysis(e.getMessage());
      imageAnalysisRepository.save(analysis);
    }
  }

  /**
   * 객체 인식 (Google Vision API)
   */
  private List<Map<String, Object>> detectObjects(String imageUrl) {
    if (googleVisionService == null) {
      log.warn("Google Vision Service를 사용할 수 없습니다. 기본 응답을 반환합니다.");
      return createFallbackObjects();
    }

    try {
      // Google Vision API에서 이미지 URL 기반 분석은 지원하지만
      // 여기서는 S3 URL을 사용하므로 MediaFile을 다시 가져와야 함
      // 간단히 처리하기 위해 빈 결과 반환 (추후 개선 필요)
      log.info("Google Vision API를 통한 객체 인식 시작: imageUrl={}", imageUrl);

      // 실제 구현에서는 S3에서 이미지를 다운로드하거나
      // Vision API에 URL을 직접 전달해야 함
      return createFallbackObjects();

    } catch (Exception e) {
      log.error("Google Vision API 호출 실패: {}", e.getMessage());
      return createFallbackObjects();
    }
  }

  /**
   * 텍스트 추출 (OCR)
   */
  private String extractText(String imageUrl) {
    if (googleVisionService == null) {
      log.warn("Google Vision Service를 사용할 수 없습니다. 기본 응답을 반환합니다.");
      return "";
    }

    try {
      log.info("Google Vision API를 통한 텍스트 추출 시작: imageUrl={}", imageUrl);
      // 실제 구현에서는 S3에서 이미지를 다운로드하거나
      // Vision API에 URL을 직접 전달해야 함
      return "";

    } catch (Exception e) {
      log.error("Google Vision API OCR 실패: {}", e.getMessage());
      return "";
    }
  }

  /**
   * 폴백 객체 데이터 생성
   */
  private List<Map<String, Object>> createFallbackObjects() {
    List<Map<String, Object>> objects = new ArrayList<>();
    Map<String, Object> obj = new HashMap<>();
    obj.put("label", "분석 대기");
    obj.put("englishLabel", "pending");
    obj.put("confidence", 0.0f);
    obj.put("bbox", Map.of("x", 0, "y", 0, "width", 0, "height", 0));
    objects.add(obj);
    return objects;
  }

  /**
   * Google Vision 객체를 Map으로 변환
   */
  private List<Map<String, Object>> convertVisionObjectsToMap(
      List<com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService.DetectedObject> visionObjects) {

    List<Map<String, Object>> objects = new ArrayList<>();

    if (visionObjects == null || visionObjects.isEmpty()) {
      return objects;
    }

    for (var visionObj : visionObjects) {
      Map<String, Object> obj = new HashMap<>();
      obj.put("label", visionObj.getName());
      obj.put("englishLabel", visionObj.getName());
      obj.put("confidence", visionObj.getConfidence());

      // BoundingBox 변환
      if (visionObj.getBoundingBox() != null) {
        var bbox = visionObj.getBoundingBox();
        obj.put("bbox", Map.of(
            "x", bbox.getX1(),
            "y", bbox.getY1(),
            "width", bbox.getX2() - bbox.getX1(),
            "height", bbox.getY2() - bbox.getY1()
        ));
      } else {
        obj.put("bbox", Map.of("x", 0, "y", 0, "width", 0, "height", 0));
      }

      objects.add(obj);
    }

    return objects;
  }


  /**
   * 음성 안내 제공
   */
  private void provideVoiceGuidance(ImageAnalysis analysis) {
    String message = analysis.getSituationDescription();
    if (analysis.getActionSuggestion() != null) {
      message += " " + analysis.getActionSuggestion();
    }
    
    voiceGuidanceService.speak(message, "ko");
    analysis.setVoiceGuidanceSent(true);
  }

  /**
   * 긴급 상황 처리
   */
  private void handleEmergency(ImageAnalysis analysis) {
    analysis.markAsEmergency();
    // TODO: 보호자 알림, 긴급 서비스 호출
    log.warn("긴급 상황 감지: 분석 ID {}", analysis.getId());
  }

  /**
   * 분석 타입 매핑
   */
  private AnalysisType mapAnalysisType(String type) {
    if (type == null) {
      return AnalysisType.ON_DEMAND;
    }
    
    return switch (type.toUpperCase()) {
      case "PERIODIC" -> AnalysisType.PERIODIC;
      case "EMERGENCY" -> AnalysisType.EMERGENCY;
      case "NAVIGATION" -> AnalysisType.NAVIGATION;
      case "MEDICATION" -> AnalysisType.MEDICATION;
      case "TEXT_READING" -> AnalysisType.TEXT_READING;
      default -> AnalysisType.ON_DEMAND;
    };
  }

  /**
   * 안전 수준 결정
   */
  private SafetyLevel determineSafetyLevel(String level) {
    if (level == null) {
      return SafetyLevel.SAFE;
    }
    
    return switch (level.toUpperCase()) {
      case "DANGER" -> SafetyLevel.DANGER;
      case "WARNING" -> SafetyLevel.WARNING;
      case "CAUTION" -> SafetyLevel.CAUTION;
      default -> SafetyLevel.SAFE;
    };
  }

  /**
   * 응답 생성
   */
  private ImageAnalysisResponse buildResponse(ImageAnalysis analysis) {
    return ImageAnalysisResponse.builder()
        .analysisId(analysis.getId())
        .imageUrl(analysis.getImageUrl())
        .analysisStatus(analysis.getAnalysisStatus().name())
        .situationDescription("분석 중입니다. 잠시만 기다려주세요.")
        .build();
  }

  /**
   * 분석 결과 조회
   */
  public ImageAnalysisResponse getAnalysisResult(Long analysisId) {
    ImageAnalysis analysis = imageAnalysisRepository.findById(analysisId)
        .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다."));
    
    ImageAnalysisResponse response = ImageAnalysisResponse.builder()
        .analysisId(analysis.getId())
        .imageUrl(analysis.getImageUrl())
        .analysisStatus(analysis.getAnalysisStatus().name())
        .situationDescription(analysis.getSituationDescription())
        .actionSuggestion(analysis.getActionSuggestion())
        .safetyLevel(analysis.getSafetyLevel() != null ? 
            analysis.getSafetyLevel().name() : null)
        .extractedText(analysis.getExtractedText())
        .emergencyDetected(analysis.getEmergencyDetected())
        .voiceGuidanceSent(analysis.getVoiceGuidanceSent())
        .processingTimeMs(analysis.getProcessingTimeMs())
        .analyzedAt(analysis.getAnalyzedAt())
        .build();
    
    // 객체 인식 결과 파싱
    if (analysis.getDetectedObjects() != null) {
      try {
        List<Map<String, Object>> objects = objectMapper.readValue(
            analysis.getDetectedObjects(), 
            List.class
        );
        response.setDetectedObjects(convertToDetectedObjects(objects));
      } catch (Exception e) {
        log.error("객체 인식 결과 파싱 실패: {}", e.getMessage());
      }
    }
    
    return response;
  }

  /**
   * 객체 인식 결과 변환
   */
  private List<ImageAnalysisResponse.DetectedObject> convertToDetectedObjects(
      List<Map<String, Object>> objects) {
    
    List<ImageAnalysisResponse.DetectedObject> result = new ArrayList<>();
    
    for (Map<String, Object> obj : objects) {
      ImageAnalysisResponse.DetectedObject detected = 
          ImageAnalysisResponse.DetectedObject.builder()
              .label((String) obj.get("label"))
              .englishLabel((String) obj.get("englishLabel"))
              .confidence(((Number) obj.get("confidence")).floatValue())
              .build();
      
      if (obj.get("bbox") instanceof Map) {
        Map<String, Object> bbox = (Map<String, Object>) obj.get("bbox");
        detected.setBbox(ImageAnalysisResponse.BoundingBox.builder()
            .x((Integer) bbox.get("x"))
            .y((Integer) bbox.get("y"))
            .width((Integer) bbox.get("width"))
            .height((Integer) bbox.get("height"))
            .build());
      }
      
      result.add(detected);
    }
    
    return result;
  }
  
  /**
   * OpenAI 서비스가 없을 때 기본 해석 제공
   */
  private Map<String, String> createDefaultInterpretation(List<Map<String, Object>> objects, String extractedText, String userQuestion) {
    Map<String, String> interpretation = new HashMap<>();
    
    StringBuilder description = new StringBuilder("이미지 분석 결과: ");
    if (!objects.isEmpty()) {
      description.append(objects.size()).append("개의 객체를 발견했습니다.");
    } else {
      description.append("특별한 객체를 발견하지 못했습니다.");
    }
    
    if (extractedText != null && !extractedText.trim().isEmpty()) {
      description.append(" 텍스트도 발견되었습니다: ").append(extractedText.substring(0, Math.min(50, extractedText.length())));
      if (extractedText.length() > 50) {
        description.append("...");
      }
    }
    
    interpretation.put("description", description.toString());
    interpretation.put("action", "상세한 분석을 위해서는 AI 서비스 설정이 필요합니다.");
    interpretation.put("safety", "MEDIUM");
    
    return interpretation;
  }
}