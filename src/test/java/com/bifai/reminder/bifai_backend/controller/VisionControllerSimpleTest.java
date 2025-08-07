package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService;
import com.bifai.reminder.bifai_backend.service.vision.GoogleVisionService.VisionAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VisionController 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class VisionControllerSimpleTest {
  
  @Mock
  private GoogleVisionService googleVisionService;
  
  @InjectMocks
  private VisionController visionController;
  
  @Test
  @DisplayName("이미지 분석 성공")
  void analyzeImage_Success() throws IOException {
    // Given
    MockMultipartFile validImageFile = new MockMultipartFile(
        "image",
        "test.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "test image content".getBytes()
    );
    
    VisionAnalysisResult mockResult = VisionAnalysisResult.builder()
        .objects(Arrays.asList(
            GoogleVisionService.DetectedObject.builder()
                .name("사람")
                .confidence(0.95f)
                .build()
        ))
        .text("안전 제일")
        .simpleDescription("발견한 것: 사람\\n글자가 있어요: 안전 제일")
        .build();
    
    when(googleVisionService.analyzeImage(any())).thenReturn(mockResult);
    
    // When
    var response = visionController.analyzeImage(validImageFile);
    
    // Then
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getData()).isNotNull();
    assertThat(response.getBody().getData().getObjects()).hasSize(1);
    assertThat(response.getBody().getData().getText()).isEqualTo("안전 제일");
  }
  
  @Test
  @DisplayName("이미지 분석 - 빈 파일")
  void analyzeImage_EmptyFile() {
    // Given
    MockMultipartFile emptyFile = new MockMultipartFile(
        "image",
        "empty.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        new byte[0]
    );
    
    // When
    var response = visionController.analyzeImage(emptyFile);
    
    // Then
    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isFalse();
    assertThat(response.getBody().getError().getCode()).isEqualTo("IMAGE_VALIDATION_ERROR");
  }
  
  @Test
  @DisplayName("위험 감지 성공 - 위험 요소 발견")
  void detectDanger_WithDanger() throws Exception {
    // Given
    MockMultipartFile validImageFile = new MockMultipartFile(
        "image",
        "test.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "test image content".getBytes()
    );
    
    VisionAnalysisResult mockResult = VisionAnalysisResult.builder()
        .objects(Arrays.asList(
            GoogleVisionService.DetectedObject.builder()
                .name("칼")
                .confidence(0.9f)
                .build()
        ))
        .safetyInfo(GoogleVisionService.SafetyInfo.builder()
            .violence("LIKELY")
            .adult("VERY_UNLIKELY")
            .medical("UNLIKELY")
            .build())
        .simpleDescription("위험한 물건이 발견되었습니다")
        .build();
    
    when(googleVisionService.analyzeImage(any())).thenReturn(mockResult);
    
    // When
    var response = visionController.detectDanger(validImageFile);
    
    // Then
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getData()).isNotNull();
    assertThat(response.getBody().getData().isDangerous()).isTrue();
    assertThat(response.getBody().getData().getDangers()).isNotEmpty();
  }
}