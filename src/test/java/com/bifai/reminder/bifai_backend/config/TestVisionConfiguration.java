package com.bifai.reminder.bifai_backend.config;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Vision API 설정
 */
@TestConfiguration
@Profile("test")
public class TestVisionConfiguration {

  @Bean
  @Primary
  public ImageAnnotatorClient mockImageAnnotatorClient() {
    return mock(ImageAnnotatorClient.class);
  }
}