package com.bifai.reminder.bifai_backend.service;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * Google Cloud TTS 서비스
 * BIF 사용자를 위한 한국어 최적화 음성 합성
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!test")
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(com.google.cloud.texttospeech.v1.TextToSpeechClient.class)
public class GoogleTtsService {
  
  private final TextToSpeechClient textToSpeechClient;
  
  @Value("${google.cloud.tts.voice.name:ko-KR-Wavenet-A}")
  private String voiceName;
  
  @Value("${google.cloud.tts.voice.language-code:ko-KR}")
  private String languageCode;
  
  @Value("${google.cloud.tts.speaking-rate:1.0}")
  private double speakingRate;
  
  @Value("${google.cloud.tts.pitch:0.0}")
  private double pitch;
  
  @Value("${google.cloud.tts.cache-dir:/tmp/tts-cache}")
  private String cacheDir;
  
  /**
   * 텍스트를 음성으로 변환 (동기)
   */
  @Cacheable(value = "ttsAudio", key = "#text + ':' + #language")
  public byte[] synthesizeText(String text, String language) throws IOException {
    if (textToSpeechClient == null) {
      log.error("Google TTS 클라이언트가 초기화되지 않았습니다");
      throw new IllegalStateException("TTS 서비스를 사용할 수 없습니다");
    }
    
    if (text == null || text.trim().isEmpty()) {
      log.warn("Empty text provided for TTS");
      throw new IllegalArgumentException("텍스트가 필요합니다");
    }
    
    if (text.length() > 5000) {
      log.warn("Text too long for TTS: {} characters", text.length());
      text = text.substring(0, 5000) + "...";
    }
    
    try {
      log.info("TTS 합성 시작 - 텍스트: {}, 언어: {}", text.substring(0, Math.min(50, text.length())), language);
      
      // 언어별 음성 설정
      String voiceNameForLang = getVoiceForLanguage(language);
      String langCode = getLanguageCode(language);
      
      // TTS 요청 구성
      SynthesisInput input = SynthesisInput.newBuilder()
          .setText(text)
          .build();
      
      VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
          .setLanguageCode(langCode)
          .setName(voiceNameForLang)
          .setSsmlGender(SsmlVoiceGender.FEMALE) // BIF 사용자에게 친근한 여성 목소리
          .build();
      
      AudioConfig audioConfig = AudioConfig.newBuilder()
          .setAudioEncoding(AudioEncoding.MP3)
          .setSpeakingRate(speakingRate) // 조금 느리게 (BIF 사용자 배려)
          .setPitch(pitch)
          .setVolumeGainDb(2.0) // 음량 조금 높게
          .build();
      
      // TTS API 호출
      SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
          input, voice, audioConfig);
      
      ByteString audioContents = response.getAudioContent();
      byte[] audioBytes = audioContents.toByteArray();
      
      log.info("TTS 합성 완료 - 오디오 크기: {} bytes", audioBytes.length);
      
      return audioBytes;
      
    } catch (Exception e) {
      log.error("TTS 합성 실패: {}", e.getMessage());
      throw new IOException("음성 합성 실패: " + e.getMessage());
    }
  }
  
  /**
   * 텍스트를 음성 파일로 저장
   */
  public String saveToFile(String text, String language, String filename) throws IOException {
    byte[] audioBytes = synthesizeText(text, language);
    
    // 캐시 디렉토리 생성
    Path cachePath = Paths.get(cacheDir);
    if (!Files.exists(cachePath)) {
      Files.createDirectories(cachePath);
    }
    
    // 파일 저장
    String filePath = Paths.get(cacheDir, filename + ".mp3").toString();
    try (FileOutputStream out = new FileOutputStream(filePath)) {
      out.write(audioBytes);
    }
    
    log.info("TTS 파일 저장 완료: {}", filePath);
    return filePath;
  }
  
  /**
   * 비동기 TTS 처리
   */
  public CompletableFuture<byte[]> synthesizeTextAsync(String text, String language) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return synthesizeText(text, language);
      } catch (IOException e) {
        log.error("비동기 TTS 실패", e);
        throw new RuntimeException(e);
      }
    });
  }
  
  /**
   * 긴급 상황용 TTS (더 크고 명확하게)
   */
  public byte[] synthesizeEmergencyText(String text, String language) throws IOException {
    if (text == null || text.trim().isEmpty()) {
      throw new IllegalArgumentException("긴급 텍스트가 필요합니다");
    }
    
    log.warn("긴급 TTS 합성: {}", text);
    
    try {
      String voiceNameForLang = getVoiceForLanguage(language);
      String langCode = getLanguageCode(language);
      
      // 긴급상황용 SSML 사용
      String ssmlText = String.format(
          "<speak><emphasis level=\"strong\"><prosody rate=\"slow\" pitch=\"+2st\" volume=\"+6dB\">%s</prosody></emphasis></speak>",
          text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
      );
      
      SynthesisInput input = SynthesisInput.newBuilder()
          .setSsml(ssmlText)
          .build();
      
      VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
          .setLanguageCode(langCode)
          .setName(voiceNameForLang)
          .setSsmlGender(SsmlVoiceGender.FEMALE)
          .build();
      
      AudioConfig audioConfig = AudioConfig.newBuilder()
          .setAudioEncoding(AudioEncoding.MP3)
          .setSpeakingRate(0.8) // 더 천천히
          .setPitch(2.0) // 더 높은 톤
          .setVolumeGainDb(6.0) // 더 크게
          .build();
      
      SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
          input, voice, audioConfig);
      
      return response.getAudioContent().toByteArray();
      
    } catch (Exception e) {
      log.error("긴급 TTS 합성 실패", e);
      throw new IOException("긴급 음성 합성 실패: " + e.getMessage());
    }
  }
  
  /**
   * 언어별 음성 선택
   */
  private String getVoiceForLanguage(String language) {
    if (language == null) {
      return voiceName;
    }
    
    return switch (language.toLowerCase()) {
      case "ko", "ko-kr", "korean" -> "ko-KR-Wavenet-A"; // 한국어 여성 목소리
      case "en", "en-us", "english" -> "en-US-Wavenet-F"; // 영어 여성 목소리
      case "ja", "ja-jp", "japanese" -> "ja-JP-Wavenet-A"; // 일본어 여성 목소리
      default -> voiceName;
    };
  }
  
  /**
   * 언어별 언어 코드 선택
   */
  private String getLanguageCode(String language) {
    if (language == null) {
      return languageCode;
    }
    
    return switch (language.toLowerCase()) {
      case "ko", "ko-kr", "korean" -> "ko-KR";
      case "en", "en-us", "english" -> "en-US";
      case "ja", "ja-jp", "japanese" -> "ja-JP";
      default -> languageCode;
    };
  }
  
  /**
   * BIF 사용자를 위한 텍스트 전처리
   */
  public String preprocessTextForBIF(String text) {
    if (text == null) {
      return "";
    }
    
    // 어려운 단어를 쉬운 단어로 변경
    text = text.replace("즉시", "지금");
    text = text.replace("신속히", "빨리");
    text = text.replace("대피", "피해");
    text = text.replace("복용", "먹기");
    text = text.replace("확인", "보기");
    text = text.replace("완료", "끝");
    
    // 긴 문장을 짧게 나누기
    if (text.length() > 50) {
      text = text.replaceAll("([.!?])\\s*", "$1 잠깐. ");
    }
    
    // 이모지 텍스트화
    text = text.replace("💊", "알약");
    text = text.replace("🚨", "위험");
    text = text.replace("✅", "완료");
    text = text.replace("❌", "안돼요");
    text = text.replace("🔋", "배터리");
    text = text.replace("📍", "위치");
    text = text.replace("⏰", "시간");
    
    return text.trim();
  }
}