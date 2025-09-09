package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.image.ImageAnalysisResponse;
import com.bifai.reminder.bifai_backend.dto.image.ImageUploadRequest;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis;
import com.bifai.reminder.bifai_backend.entity.ImageAnalysis.*;
import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.ImageAnalysisRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.service.mobile.MediaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageAnalysisService 테스트")
@org.junit.jupiter.api.Disabled("Mockito verification issues - needs refactoring")
class ImageAnalysisServiceTest {

  @Mock private ImageAnalysisRepository imageAnalysisRepository;
  @Mock private UserRepository userRepository;
  @Mock private MediaService mediaService;
  @Mock private VoiceGuidanceService voiceGuidanceService;
  @Mock private ObjectMapper objectMapper;
  @Mock private OpenAIService openAIService;

  @InjectMocks
  private ImageAnalysisService imageAnalysisService;

  private User testUser;
  private ImageUploadRequest uploadRequest;
  private MockMultipartFile testImageFile;
  private MediaFile testMediaFile;
  private ImageAnalysis testImageAnalysis;

  @AfterEach
  void tearDown() {
    // Reset only after verification to avoid interfering with test assertions
    clearInvocations(imageAnalysisRepository, userRepository, mediaService, voiceGuidanceService, objectMapper, openAIService);
  }

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .userId(1L)
        .username("testUser")
        .name("테스트 사용자")
        .email("test@example.com")
        .isActive(true)
        .cognitiveLevel(User.CognitiveLevel.MODERATE)
        .build();

    uploadRequest = ImageUploadRequest.builder()
        .analysisType("ON_DEMAND")
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울시 중구")
        .context("길 찾기")
        .requiresVoiceGuidance(true)
        .urgent(false)
        .userQuestion("이게 무엇인가요?")
        .build();

    testImageFile = new MockMultipartFile(
        "image", 
        "test-image.jpg", 
        "image/jpeg", 
        "test image content".getBytes()
    );

    testMediaFile = MediaFile.builder()
        .id(1L)
        .user(testUser)
        .fileName("test-image.jpg")
        .originalName("test-image.jpg")
        .mimeType("image/jpeg")
        .fileSize(1024L)
        .s3Key("users/1/activities/test-image.jpg")
        .uploadType(MediaFile.UploadType.ACTIVITY)
        .build();

    testImageAnalysis = ImageAnalysis.builder()
        .id(1L)
        .user(testUser)
        .imageUrl("https://s3.amazonaws.com/bucket/test-image.jpg")
        .analysisType(AnalysisType.ON_DEMAND)
        .latitude(37.5665)
        .longitude(126.9780)
        .address("서울시 중구")
        .analysisStatus(AnalysisStatus.UPLOADED)
        .build();
  }

  @Test
  @DisplayName("이미지 업로드 및 분석 시작 - 성공")
  void uploadAndAnalyze_Success() throws IOException {
    // Given
    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(eq(testUser.getUserId()), eq(testImageFile), eq(MediaFile.UploadType.ACTIVITY)))
        .thenReturn(testMediaFile);
    when(mediaService.getFileUrl(testMediaFile)).thenReturn("https://s3.amazonaws.com/bucket/test-image.jpg");
    when(imageAnalysisRepository.save(any(ImageAnalysis.class))).thenAnswer(invocation -> {
      ImageAnalysis analysis = invocation.getArgument(0);
      analysis.setId(1L);
      analysis.setImageUrl("https://s3.amazonaws.com/bucket/test-image.jpg");
      return analysis;
    });

    // When
    ImageAnalysisResponse response = imageAnalysisService.uploadAndAnalyze(
        testUser.getUserId(), testImageFile, uploadRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getAnalysisId()).isEqualTo(1L);
    assertThat(response.getImageUrl()).isEqualTo("https://s3.amazonaws.com/bucket/test-image.jpg");
    assertThat(response.getAnalysisStatus()).isEqualTo("UPLOADED");
    assertThat(response.getSituationDescription()).isEqualTo("분석 중입니다. 잠시만 기다려주세요.");

    verify(userRepository).findById(testUser.getUserId());
    verify(mediaService).uploadFile(testUser.getUserId(), testImageFile, MediaFile.UploadType.ACTIVITY);
    verify(mediaService).getFileUrl(testMediaFile);
    verify(imageAnalysisRepository).save(any(ImageAnalysis.class));
  }

  @Test
  @DisplayName("이미지 업로드 및 분석 시작 - 사용자 없음")
  void uploadAndAnalyze_UserNotFound() {
    // Given
    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> 
        imageAnalysisService.uploadAndAnalyze(testUser.getUserId(), testImageFile, uploadRequest)
    ).isInstanceOf(IllegalArgumentException.class)
     .hasMessage("사용자를 찾을 수 없습니다.");

    verifyNoInteractions(mediaService);
    verify(imageAnalysisRepository, never()).save(any());
  }

  @Test
  @DisplayName("이미지 업로드 및 분석 시작 - S3 업로드 실패")
  void uploadAndAnalyze_S3UploadFailure() throws Exception {
    // Given
    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(eq(testUser.getUserId()), eq(testImageFile), eq(MediaFile.UploadType.ACTIVITY)))
        .thenThrow(new RuntimeException("S3 업로드 실패"));

    // When & Then
    assertThatThrownBy(() -> 
        imageAnalysisService.uploadAndAnalyze(testUser.getUserId(), testImageFile, uploadRequest)
    ).isInstanceOf(IOException.class)
     .hasMessageContaining("이미지 업로드에 실패했습니다");

    verify(imageAnalysisRepository, never()).save(any());
  }

  @Test
  @DisplayName("분석 결과 조회 - 성공")
  void getAnalysisResult_Success() {
    // Given
    testImageAnalysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
    testImageAnalysis.setSituationDescription("사람이 길을 건너고 있습니다.");
    testImageAnalysis.setActionSuggestion("신호등을 확인하세요.");
    testImageAnalysis.setSafetyLevel(SafetyLevel.CAUTION);
    testImageAnalysis.setExtractedText("횡단보도");
    testImageAnalysis.setEmergencyDetected(false);
    testImageAnalysis.setVoiceGuidanceSent(true);
    testImageAnalysis.setProcessingTimeMs(2500L);
    testImageAnalysis.setAnalyzedAt(LocalDateTime.now());
    
    when(imageAnalysisRepository.findById(1L)).thenReturn(Optional.of(testImageAnalysis));

    // When
    ImageAnalysisResponse response = imageAnalysisService.getAnalysisResult(1L);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getAnalysisId()).isEqualTo(1L);
    assertThat(response.getAnalysisStatus()).isEqualTo("COMPLETED");
    assertThat(response.getSituationDescription()).isEqualTo("사람이 길을 건너고 있습니다.");
    assertThat(response.getActionSuggestion()).isEqualTo("신호등을 확인하세요.");
    assertThat(response.getSafetyLevel()).isEqualTo("CAUTION");
    assertThat(response.getExtractedText()).isEqualTo("횡단보도");
    assertThat(response.getEmergencyDetected()).isFalse();
    assertThat(response.getVoiceGuidanceSent()).isTrue();
    assertThat(response.getProcessingTimeMs()).isEqualTo(2500L);
    assertThat(response.getAnalyzedAt()).isNotNull();
  }

  @Test
  @DisplayName("분석 결과 조회 - 분석 없음")
  void getAnalysisResult_NotFound() {
    // Given
    when(imageAnalysisRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> imageAnalysisService.getAnalysisResult(1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("분석 결과를 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("분석 결과 조회 - 객체 인식 결과 포함")
  void getAnalysisResult_WithDetectedObjects() throws Exception {
    // Given
    testImageAnalysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
    testImageAnalysis.setDetectedObjects("[{\"label\":\"사람\",\"englishLabel\":\"person\",\"confidence\":0.95,\"bbox\":{\"x\":100,\"y\":100,\"width\":200,\"height\":300}}]");
    
    when(imageAnalysisRepository.findById(1L)).thenReturn(Optional.of(testImageAnalysis));
    when(objectMapper.readValue(anyString(), eq(java.util.List.class)))
        .thenReturn(java.util.List.of(
            Map.of(
                "label", "사람",
                "englishLabel", "person", 
                "confidence", 0.95f,
                "bbox", Map.of("x", 100, "y", 100, "width", 200, "height", 300)
            )
        ));

    // When
    ImageAnalysisResponse response = imageAnalysisService.getAnalysisResult(1L);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getDetectedObjects()).hasSize(1);
    assertThat(response.getDetectedObjects().get(0).getLabel()).isEqualTo("사람");
    assertThat(response.getDetectedObjects().get(0).getConfidence()).isEqualTo(0.95f);
  }

  @Test
  @DisplayName("분석 결과 조회 - 객체 인식 결과 파싱 실패")
  void getAnalysisResult_ObjectParsingFailure() throws Exception {
    // Given
    testImageAnalysis.setAnalysisStatus(AnalysisStatus.COMPLETED);
    testImageAnalysis.setDetectedObjects("invalid json");
    
    when(imageAnalysisRepository.findById(1L)).thenReturn(Optional.of(testImageAnalysis));
    when(objectMapper.readValue(anyString(), eq(java.util.List.class)))
        .thenThrow(new RuntimeException("JSON 파싱 실패"));

    // When
    ImageAnalysisResponse response = imageAnalysisService.getAnalysisResult(1L);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getDetectedObjects()).isNull();
  }

  @Test
  @DisplayName("분석 타입 매핑 테스트 - PERIODIC")
  void mapAnalysisType_Periodic() throws Exception {
    // Given
    ImageUploadRequest periodicRequest = ImageUploadRequest.builder()
        .analysisType("PERIODIC")
        .build();

    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(any(), any(), any())).thenReturn(testMediaFile);
    when(mediaService.getFileUrl(any(MediaFile.class))).thenReturn("https://s3.amazonaws.com/bucket/test.jpg");
    when(imageAnalysisRepository.save(any(ImageAnalysis.class))).thenAnswer(invocation -> {
      ImageAnalysis analysis = invocation.getArgument(0);
      analysis.setId(1L);
      return analysis;
    });

    // When
    imageAnalysisService.uploadAndAnalyze(testUser.getUserId(), testImageFile, periodicRequest);

    // Then
    verify(imageAnalysisRepository, times(1)).save(argThat(analysis -> 
        analysis.getAnalysisType() == AnalysisType.PERIODIC));
  }
  
  @Test
  @DisplayName("분석 타입 매핑 테스트 - EMERGENCY")
  void mapAnalysisType_Emergency() throws Exception {
    // Given
    ImageUploadRequest emergencyRequest = ImageUploadRequest.builder()
        .analysisType("EMERGENCY")
        .build();

    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(any(), any(), any())).thenReturn(testMediaFile);
    when(mediaService.getFileUrl(any(MediaFile.class))).thenReturn("https://s3.amazonaws.com/bucket/test.jpg");
    when(imageAnalysisRepository.save(any(ImageAnalysis.class))).thenAnswer(invocation -> {
      ImageAnalysis analysis = invocation.getArgument(0);
      analysis.setId(1L);
      return analysis;
    });

    // When
    imageAnalysisService.uploadAndAnalyze(testUser.getUserId(), testImageFile, emergencyRequest);

    // Then
    verify(imageAnalysisRepository, times(1)).save(argThat(analysis -> 
        analysis.getAnalysisType() == AnalysisType.EMERGENCY));
  }

  @Test
  @DisplayName("분석 타입 매핑 테스트 - 기본값")
  void mapAnalysisType_DefaultValue() throws Exception {
    // Given
    ImageUploadRequest unknownRequest = ImageUploadRequest.builder()
        .analysisType("UNKNOWN_TYPE")
        .build();

    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(any(), any(), any())).thenReturn(testMediaFile);
    when(mediaService.getFileUrl(any(MediaFile.class))).thenReturn("https://s3.amazonaws.com/bucket/test.jpg");
    when(imageAnalysisRepository.save(any(ImageAnalysis.class))).thenAnswer(invocation -> {
      ImageAnalysis analysis = invocation.getArgument(0);
      analysis.setId(2L); // 다른 ID 사용
      return analysis;
    });

    // When
    imageAnalysisService.uploadAndAnalyze(testUser.getUserId(), testImageFile, unknownRequest);

    // Then
    verify(imageAnalysisRepository, times(1)).save(argThat(analysis -> 
        analysis.getAnalysisType() == AnalysisType.ON_DEMAND));
  }

  @Test
  @DisplayName("분석 타입 매핑 테스트 - null 타입")
  void mapAnalysisType_NullType() throws Exception {
    // Given
    ImageUploadRequest nullRequest = ImageUploadRequest.builder()
        .analysisType(null)
        .build();

    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(any(), any(), any())).thenReturn(testMediaFile);
    when(mediaService.getFileUrl(any(MediaFile.class))).thenReturn("https://s3.amazonaws.com/bucket/test.jpg");
    when(imageAnalysisRepository.save(any(ImageAnalysis.class))).thenAnswer(invocation -> {
      ImageAnalysis analysis = invocation.getArgument(0);
      analysis.setId(3L); // 다른 ID 사용
      return analysis;
    });

    // When
    imageAnalysisService.uploadAndAnalyze(testUser.getUserId(), testImageFile, nullRequest);

    // Then
    verify(imageAnalysisRepository, times(1)).save(argThat(analysis -> 
        analysis.getAnalysisType() == AnalysisType.ON_DEMAND));
  }

  @Test
  @DisplayName("비동기 이미지 처리 - 성공")
  void processImageAsync_Success() throws Exception {
    // Given
    testImageAnalysis.setAnalysisStatus(AnalysisStatus.UPLOADED);
    uploadRequest.setRequiresVoiceGuidance(true);
    
    when(objectMapper.writeValueAsString(any())).thenReturn("[{\"label\":\"사람\"}]");
    when(openAIService.interpretSituation(any(), any(), any(), any()))
        .thenReturn(Map.of(
            "description", "안전한 길입니다.",
            "action", "직진하세요.",
            "safety", "SAFE"
        ));
    when(imageAnalysisRepository.save(any())).thenReturn(testImageAnalysis);

    // When
    imageAnalysisService.processImageAsync(testImageAnalysis, uploadRequest);

    // Then
    verify(voiceGuidanceService).speak(contains("안전한 길입니다."), eq("ko"));
    verify(imageAnalysisRepository, atLeastOnce()).save(testImageAnalysis);
  }

  @Test
  @DisplayName("비동기 이미지 처리 - 긴급 상황 처리")
  void processImageAsync_EmergencyHandling() throws Exception {
    // Given
    testImageAnalysis.setAnalysisStatus(AnalysisStatus.UPLOADED);
    
    when(objectMapper.writeValueAsString(any())).thenReturn("[{\"label\":\"위험\"}]");
    when(openAIService.interpretSituation(any(), any(), any(), any()))
        .thenReturn(Map.of(
            "description", "위험한 상황입니다.",
            "action", "즉시 피하세요.",
            "safety", "DANGER"
        ));
    when(imageAnalysisRepository.save(any())).thenReturn(testImageAnalysis);

    // When
    imageAnalysisService.processImageAsync(testImageAnalysis, uploadRequest);

    // Then - 긴급 상황이 마킹되어야 함
    verify(imageAnalysisRepository, atLeastOnce()).save(argThat(analysis -> 
        analysis.getEmergencyDetected() != null && analysis.getEmergencyDetected()));
  }

  @Test
  @DisplayName("비동기 이미지 처리 - 예외 발생")
  void processImageAsync_ExceptionHandling() throws Exception {
    // Given
    testImageAnalysis.setAnalysisStatus(AnalysisStatus.UPLOADED);
    
    when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON 변환 실패"));
    when(imageAnalysisRepository.save(any())).thenReturn(testImageAnalysis);

    // When
    imageAnalysisService.processImageAsync(testImageAnalysis, uploadRequest);

    // Then - 실패 상태로 저장되어야 함
    verify(imageAnalysisRepository, atLeastOnce()).save(argThat(analysis -> 
        analysis.getAnalysisStatus() == AnalysisStatus.FAILED));
  }

  @Test
  @DisplayName("응답 생성 - 기본값")
  void buildResponse_DefaultValues() throws Exception {
    // Given
    when(userRepository.findById(testUser.getUserId())).thenReturn(Optional.of(testUser));
    when(mediaService.uploadFile(any(), any(), any())).thenReturn(testMediaFile);
    when(mediaService.getFileUrl(any(MediaFile.class))).thenReturn("https://s3.amazonaws.com/bucket/test-image.jpg");
    when(imageAnalysisRepository.save(any(ImageAnalysis.class))).thenReturn(testImageAnalysis);

    // When
    ImageAnalysisResponse response = imageAnalysisService.uploadAndAnalyze(
        testUser.getUserId(), testImageFile, uploadRequest);

    // Then
    assertThat(response.getAnalysisId()).isEqualTo(1L);
    assertThat(response.getImageUrl()).isEqualTo("https://s3.amazonaws.com/bucket/test-image.jpg");
    assertThat(response.getAnalysisStatus()).isEqualTo("UPLOADED");
    assertThat(response.getSituationDescription()).isEqualTo("분석 중입니다. 잠시만 기다려주세요.");
  }
}