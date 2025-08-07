package com.bifai.reminder.bifai_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 음성 안내 서비스
 * TTS(Text-To-Speech)를 통한 음성 안내 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceGuidanceService {

  /**
   * 텍스트를 음성으로 변환하여 재생
   */
  public void speak(String text, String language) {
    log.info("음성 안내: [{}] {}", language, text);
    // TODO: AWS Polly 또는 Google TTS API 연동 구현
  }

  /**
   * 긴급 음성 안내
   */
  public void speakEmergency(String text, String language) {
    log.warn("긴급 음성 안내: [{}] {}", language, text);
    // TODO: 더 크고 명확한 음성으로 재생
  }

  /**
   * 반복 음성 안내
   */
  public void repeatSpeak(String text, String language, int times) {
    for (int i = 0; i < times; i++) {
      speak(text, language);
      try {
        Thread.sleep(2000); // 2초 대기
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}