package com.bifai.reminder.bifai_backend.service.vision;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleVisionServiceTest {
  
  @Mock
  private ImageAnnotatorClient visionClient;
  
  @InjectMocks
  private GoogleVisionService googleVisionService;
  
  private MockMultipartFile testImage;
  
  @BeforeEach
  void setUp() {
    // 테스트 이미지 파일 생성
    testImage = new MockMultipartFile(
        "image",
        "test.jpg",
        "image/jpeg",
        "test image content".getBytes()
    );
    
    // @Value 필드 설정
    ReflectionTestUtils.setField(googleVisionService, "maxResults", 10);
    ReflectionTestUtils.setField(googleVisionService, "confidenceThreshold", 0.7f);
  }
  
  @Test
  @DisplayName("Vision API 클라이언트가 null일 때 빈 결과 반환")
  void analyzeImage_WhenClientIsNull_ReturnsEmptyResult() throws IOException {
    // Given
    googleVisionService = new GoogleVisionService(null);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSimpleDescription()).isEqualTo("이미지를 분석할 수 없습니다");
  }
  
  @Test
  @DisplayName("이미지 분석 성공 - 객체 감지")
  void analyzeImage_WithObjects_Success() throws IOException {
    // Given
    LocalizedObjectAnnotation object1 = LocalizedObjectAnnotation.newBuilder()
        .setName("person")
        .setScore(0.95f)
        .setBoundingPoly(BoundingPoly.newBuilder()
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.1f).setY(0.1f))
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.9f).setY(0.9f)))
        .build();
    
    LocalizedObjectAnnotation object2 = LocalizedObjectAnnotation.newBuilder()
        .setName("car")
        .setScore(0.85f)
        .setBoundingPoly(BoundingPoly.newBuilder()
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.2f).setY(0.2f))
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.8f).setY(0.8f)))
        .build();
    
    AnnotateImageResponse objectResponse = AnnotateImageResponse.newBuilder()
        .addAllLocalizedObjectAnnotations(Arrays.asList(object1, object2))
        .build();
    
    // 다른 응답들은 빈 응답으로 설정
    AnnotateImageResponse emptyResponse = AnnotateImageResponse.newBuilder().build();
    
    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
        .addResponses(objectResponse)  // 객체 감지
        .addResponses(emptyResponse)   // 라벨
        .addResponses(emptyResponse)   // 텍스트
        .addResponses(emptyResponse)   // 안전성
        .addResponses(emptyResponse)   // 얼굴
        .build();
    
    when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getObjects()).hasSize(2);
    assertThat(result.getObjects().get(0).getName()).isEqualTo("사람");
    assertThat(result.getObjects().get(0).getConfidence()).isEqualTo(0.95f);
    assertThat(result.getObjects().get(1).getName()).isEqualTo("자동차");
    assertThat(result.getSimpleDescription()).contains("발견한 것: 사람, 자동차");
  }
  
  @Test
  @DisplayName("이미지 분석 성공 - 텍스트 감지")
  void analyzeImage_WithText_Success() throws IOException {
    // Given
    EntityAnnotation textAnnotation = EntityAnnotation.newBuilder()
        .setDescription("안전 제일")
        .build();
    
    AnnotateImageResponse textResponse = AnnotateImageResponse.newBuilder()
        .addTextAnnotations(textAnnotation)
        .build();
    
    AnnotateImageResponse emptyResponse = AnnotateImageResponse.newBuilder().build();
    
    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
        .addResponses(emptyResponse)   // 객체
        .addResponses(emptyResponse)   // 라벨
        .addResponses(textResponse)    // 텍스트
        .addResponses(emptyResponse)   // 안전성
        .addResponses(emptyResponse)   // 얼굴
        .build();
    
    when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result.getText()).isEqualTo("안전 제일");
    assertThat(result.getSimpleDescription()).contains("글자가 있어요: 안전 제일");
  }
  
  @Test
  @DisplayName("이미지 분석 성공 - 안전성 감지")
  void analyzeImage_WithSafetyInfo_Success() throws IOException {
    // Given
    SafeSearchAnnotation safetyAnnotation = SafeSearchAnnotation.newBuilder()
        .setAdult(Likelihood.VERY_UNLIKELY)
        .setViolence(Likelihood.POSSIBLE)
        .setMedical(Likelihood.UNLIKELY)
        .build();
    
    AnnotateImageResponse safetyResponse = AnnotateImageResponse.newBuilder()
        .setSafeSearchAnnotation(safetyAnnotation)
        .build();
    
    AnnotateImageResponse emptyResponse = AnnotateImageResponse.newBuilder().build();
    
    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
        .addResponses(emptyResponse)   // 객체
        .addResponses(emptyResponse)   // 라벨
        .addResponses(emptyResponse)   // 텍스트
        .addResponses(safetyResponse)  // 안전성
        .addResponses(emptyResponse)   // 얼굴
        .build();
    
    when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result.getSafetyInfo()).isNotNull();
    assertThat(result.getSafetyInfo().getViolence()).isEqualTo("POSSIBLE");
    assertThat(result.getSimpleDescription()).contains("⚠️ 주의가 필요한 내용이 있어요");
  }
  
  @Test
  @DisplayName("이미지 분석 성공 - 얼굴 감지")
  void analyzeImage_WithFaces_Success() throws IOException {
    // Given
    FaceAnnotation face1 = FaceAnnotation.newBuilder()
        .setJoyLikelihood(Likelihood.LIKELY)
        .setSorrowLikelihood(Likelihood.VERY_UNLIKELY)
        .setAngerLikelihood(Likelihood.VERY_UNLIKELY)
        .setSurpriseLikelihood(Likelihood.POSSIBLE)
        .setDetectionConfidence(0.92f)
        .build();
    
    FaceAnnotation face2 = FaceAnnotation.newBuilder()
        .setJoyLikelihood(Likelihood.POSSIBLE)
        .setSorrowLikelihood(Likelihood.UNLIKELY)
        .setAngerLikelihood(Likelihood.VERY_UNLIKELY)
        .setSurpriseLikelihood(Likelihood.UNLIKELY)
        .setDetectionConfidence(0.88f)
        .build();
    
    AnnotateImageResponse faceResponse = AnnotateImageResponse.newBuilder()
        .addAllFaceAnnotations(Arrays.asList(face1, face2))
        .build();
    
    AnnotateImageResponse emptyResponse = AnnotateImageResponse.newBuilder().build();
    
    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
        .addResponses(emptyResponse)   // 객체
        .addResponses(emptyResponse)   // 라벨
        .addResponses(emptyResponse)   // 텍스트
        .addResponses(emptyResponse)   // 안전성
        .addResponses(faceResponse)    // 얼굴
        .build();
    
    when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result.getFaces()).hasSize(2);
    assertThat(result.getFaces().get(0).getJoy()).isEqualTo("LIKELY");
    assertThat(result.getSimpleDescription()).contains("사람 2명이 보여요");
  }
  
  @Test
  @DisplayName("이미지 분석 실패 - API 오류")
  void analyzeImage_WhenApiError_ThrowsIOException() {
    // Given
    when(visionClient.batchAnnotateImages(anyList()))
        .thenThrow(new RuntimeException("API 오류"));
    
    // When & Then
    assertThatThrownBy(() -> googleVisionService.analyzeImage(testImage))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("이미지 분석 실패");
  }
  
  @Test
  @DisplayName("낮은 신뢰도 객체는 필터링")
  void analyzeImage_FiltersLowConfidenceObjects() throws IOException {
    // Given
    LocalizedObjectAnnotation highConfidence = LocalizedObjectAnnotation.newBuilder()
        .setName("person")
        .setScore(0.95f)
        .setBoundingPoly(BoundingPoly.newBuilder()
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.1f).setY(0.1f))
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.9f).setY(0.9f)))
        .build();
    
    LocalizedObjectAnnotation lowConfidence = LocalizedObjectAnnotation.newBuilder()
        .setName("dog")
        .setScore(0.5f)  // 임계값(0.7) 미만
        .setBoundingPoly(BoundingPoly.newBuilder()
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.2f).setY(0.2f))
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.8f).setY(0.8f)))
        .build();
    
    AnnotateImageResponse objectResponse = AnnotateImageResponse.newBuilder()
        .addAllLocalizedObjectAnnotations(Arrays.asList(highConfidence, lowConfidence))
        .build();
    
    AnnotateImageResponse emptyResponse = AnnotateImageResponse.newBuilder().build();
    
    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
        .addResponses(objectResponse)
        .addResponses(emptyResponse)
        .addResponses(emptyResponse)
        .addResponses(emptyResponse)
        .addResponses(emptyResponse)
        .build();
    
    when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result.getObjects()).hasSize(1);
    assertThat(result.getObjects().get(0).getName()).isEqualTo("사람");
  }
  
  @Test
  @DisplayName("복합 분석 - 여러 기능 동시 사용")
  void analyzeImage_WithMultipleFeatures_Success() throws IOException {
    // Given
    // 객체
    LocalizedObjectAnnotation object = LocalizedObjectAnnotation.newBuilder()
        .setName("person")
        .setScore(0.9f)
        .setBoundingPoly(BoundingPoly.newBuilder()
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.1f).setY(0.1f))
            .addNormalizedVertices(NormalizedVertex.newBuilder().setX(0.9f).setY(0.9f)))
        .build();
    
    // 텍스트
    EntityAnnotation text = EntityAnnotation.newBuilder()
        .setDescription("출입금지")
        .build();
    
    // 얼굴
    FaceAnnotation face = FaceAnnotation.newBuilder()
        .setJoyLikelihood(Likelihood.UNLIKELY)
        .setSorrowLikelihood(Likelihood.LIKELY)
        .setAngerLikelihood(Likelihood.VERY_UNLIKELY)
        .setSurpriseLikelihood(Likelihood.VERY_UNLIKELY)
        .setDetectionConfidence(0.85f)
        .build();
    
    BatchAnnotateImagesResponse batchResponse = BatchAnnotateImagesResponse.newBuilder()
        .addResponses(AnnotateImageResponse.newBuilder()
            .addLocalizedObjectAnnotations(object))
        .addResponses(AnnotateImageResponse.newBuilder())  // 라벨
        .addResponses(AnnotateImageResponse.newBuilder()
            .addTextAnnotations(text))
        .addResponses(AnnotateImageResponse.newBuilder())  // 안전성
        .addResponses(AnnotateImageResponse.newBuilder()
            .addFaceAnnotations(face))
        .build();
    
    when(visionClient.batchAnnotateImages(anyList())).thenReturn(batchResponse);
    
    // When
    var result = googleVisionService.analyzeImage(testImage);
    
    // Then
    assertThat(result.getObjects()).hasSize(1);
    assertThat(result.getText()).isEqualTo("출입금지");
    assertThat(result.getFaces()).hasSize(1);
    assertThat(result.getSimpleDescription())
        .contains("발견한 것: 사람")
        .contains("글자가 있어요: 출입금지")
        .contains("사람 1명이 보여요");
  }
}