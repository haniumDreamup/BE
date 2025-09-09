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
  private final OpenAIService openAIService;

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
    
    // 비동기로 AI 분석 시작
    processImageAsync(analysis, request);
    
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
    try {
      analysis.startProcessing();
      imageAnalysisRepository.save(analysis);
      
      // 1. 객체 인식 (YOLOv8)
      List<Map<String, Object>> objects = detectObjects(analysis.getImageUrl());
      analysis.setDetectedObjects(objectMapper.writeValueAsString(objects));
      analysis.setObjectCount(objects.size());
      
      // 2. 텍스트 추출 (OCR)
      String extractedText = extractText(analysis.getImageUrl());
      analysis.setExtractedText(extractedText);
      
      // 3. 상황 해석 (OpenAI)
      Map<String, String> interpretation = openAIService.interpretSituation(
          objects, 
          extractedText, 
          request.getUserQuestion(),
          request.getContext()
      );
      
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
   * 객체 인식 (YOLOv8)
   */
  private List<Map<String, Object>> detectObjects(String imageUrl) {
    // TODO: 실제 YOLOv8 API 호출
    // return yoloService.detect(imageUrl);
    
    // 임시 데이터
    List<Map<String, Object>> objects = new ArrayList<>();
    Map<String, Object> obj = new HashMap<>();
    obj.put("label", "사람");
    obj.put("englishLabel", "person");
    obj.put("confidence", 0.95f);
    obj.put("bbox", Map.of("x", 100, "y", 100, "width", 200, "height", 300));
    objects.add(obj);
    return objects;
  }

  /**
   * 텍스트 추출 (OCR)
   */
  private String extractText(String imageUrl) {
    // TODO: 실제 OCR API 호출
    // return ocrService.extractText(imageUrl);
    
    // 임시 데이터
    return "출구";
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
}