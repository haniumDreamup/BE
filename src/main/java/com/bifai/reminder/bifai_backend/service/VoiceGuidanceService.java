package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.AccessibilitySettingsRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 음성 안내 서비스
 * TTS(Text-To-Speech)를 통한 음성 안내 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceGuidanceService {
  
  private final UserRepository userRepository;
  private final AccessibilitySettingsRepository accessibilitySettingsRepository;
  
  @Autowired(required = false)
  private GoogleTtsService googleTtsService;
  
  // 한국어 템플릿
  private static final Map<String, String> KOREAN_TEMPLATES = new HashMap<>();
  // 영어 템플릿
  private static final Map<String, String> ENGLISH_TEMPLATES = new HashMap<>();
  
  static {
    // 한국어 템플릿 초기화
    KOREAN_TEMPLATES.put("button_click", "{buttonName} 버튼. 실행하려면 두 번 탭하세요");
    KOREAN_TEMPLATES.put("loading", "로딩 중... 잠시만 기다려주세요");
    KOREAN_TEMPLATES.put("success_message", "성공적으로 완료되었습니다");
    KOREAN_TEMPLATES.put("error_message", "오류가 발생했습니다. 다시 시도해주세요");
    KOREAN_TEMPLATES.put("medication_reminder", "복약 시간입니다. {medication}을 {time}에 복용하세요");
    KOREAN_TEMPLATES.put("emergency", "긴급 상황! 도움이 필요합니다");
    KOREAN_TEMPLATES.put("battery_low", "배터리가 부족합니다. 충전이 필요합니다");
    KOREAN_TEMPLATES.put("location_update", "현재 위치: {location}");
    KOREAN_TEMPLATES.put("time_announcement", "현재 시간은 {time}입니다");
    
    // 영어 템플릿 초기화
    ENGLISH_TEMPLATES.put("button_click", "{buttonName} button. Double tap to activate");
    ENGLISH_TEMPLATES.put("loading", "Loading... Please wait");
    ENGLISH_TEMPLATES.put("success_message", "Successfully completed");
    ENGLISH_TEMPLATES.put("error_message", "An error occurred. Please try again");
    ENGLISH_TEMPLATES.put("medication_reminder", "Time for medication. Take {medication} at {time}");
    ENGLISH_TEMPLATES.put("emergency", "Emergency! Help needed");
    ENGLISH_TEMPLATES.put("battery_low", "Battery low. Charging needed");
    ENGLISH_TEMPLATES.put("location_update", "Current location: {location}");
    ENGLISH_TEMPLATES.put("time_announcement", "The current time is {time}");
  }
  
  /**
   * 음성 안내 텍스트 생성
   */
  @Cacheable(value = "voiceGuidance", key = "#userId + ':' + #context + ':' + #params.hashCode()")
  public String generateVoiceGuidance(Long userId, String context, Map<String, Object> params) {
    try {
      Optional<AccessibilitySettings> settingsOpt = accessibilitySettingsRepository.findByUserId(userId);
      
      String language = "ko-KR";
      String readingLevel = "grade5";
      boolean useEmojis = true;
      
      if (settingsOpt.isPresent()) {
        AccessibilitySettings settings = settingsOpt.get();
        language = settings.getVoiceLanguage();
        readingLevel = settings.getReadingLevel();
        useEmojis = settings.getUseEmojis();
      }
      
      // 템플릿 선택
      Map<String, String> templates = language.startsWith("en") ? ENGLISH_TEMPLATES : KOREAN_TEMPLATES;
      String template = templates.getOrDefault(context, context);
      
      // 파라미터 치환
      String result = template;
      if (params != null) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
          result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
      }
      
      // 읽기 수준에 따른 간소화
      result = simplifyByReadingLevel(result, readingLevel);
      
      // 이모지 추가
      if (useEmojis) {
        result = addEmoji(context, result);
      }
      
      return result;
      
    } catch (Exception e) {
      log.error("음성 안내 생성 실패: {}", e.getMessage());
      return context.replace("_", " ");
    }
  }
  
  /**
   * ARIA 라벨 생성
   */
  public String generateAriaLabel(String elementType, String label, Map<String, Object> attributes) {
    StringBuilder ariaLabel = new StringBuilder();
    
    if (label != null && !label.isEmpty()) {
      ariaLabel.append(label).append(" ");
    }
    
    switch (elementType) {
      case "button":
        ariaLabel.append("버튼");
        if (attributes != null && Boolean.TRUE.equals(attributes.get("disabled"))) {
          ariaLabel.append(", 비활성화됨");
        }
        break;
        
      case "input":
        String inputType = attributes != null ? (String) attributes.get("type") : "text";
        if ("password".equals(inputType)) {
          ariaLabel.append("비밀번호 입력");
        } else if ("email".equals(inputType)) {
          ariaLabel.append("이메일 입력");
        } else if ("number".equals(inputType)) {
          ariaLabel.append("숫자 입력");
        } else {
          ariaLabel.append("텍스트 입력");
        }
        if (attributes != null && Boolean.TRUE.equals(attributes.get("required"))) {
          ariaLabel.append(", 필수");
        }
        break;
        
      case "checkbox":
        ariaLabel.append("체크박스");
        if (attributes != null && Boolean.TRUE.equals(attributes.get("checked"))) {
          ariaLabel.append(", 선택됨");
        }
        break;
        
      case "radio":
        ariaLabel.append("라디오 버튼");
        if (attributes != null && Boolean.TRUE.equals(attributes.get("checked"))) {
          ariaLabel.append(", 선택됨");
        }
        break;
        
      case "link":
        ariaLabel.append("링크");
        break;
        
      case "image":
        ariaLabel.append("이미지");
        if (attributes != null && attributes.get("alt") != null) {
          ariaLabel.append(": ").append(attributes.get("alt"));
        }
        break;
        
      case "list":
        ariaLabel.append("목록");
        if (attributes != null && attributes.get("itemCount") != null) {
          ariaLabel.append(", ").append(attributes.get("itemCount")).append("개 항목");
        }
        break;
        
      default:
        ariaLabel.append(elementType);
    }
    
    return ariaLabel.toString();
  }
  
  /**
   * 스크린 리더 힌트 생성
   */
  public String generateScreenReaderHint(String action, String target) {
    try {
      StringBuilder hint = new StringBuilder();

      if (target != null && !target.isEmpty()) {
        hint.append(target).append("를 ");
      }

      switch (action) {
        case "tap":
        case "click":
          hint.append("선택하려면 두 번 탭하세요");
          break;
        case "swipe_left":
          hint.append("이전 항목으로 이동하려면 왼쪽으로 쓸어넘기세요");
          break;
        case "swipe_right":
          hint.append("다음 항목으로 이동하려면 오른쪽으로 쓸어넘기세요");
          break;
        case "long_press":
          hint.append("추가 옵션을 보려면 길게 누르세요");
          break;
        default:
          // 알 수 없는 액션의 경우 기본 힌트 제공
          hint.append("접근하려면 두 번 탭하세요");
          log.debug("알 수 없는 액션: {}, 기본 힌트 제공", action);
          break;
      }

      return hint.toString();

    } catch (Exception e) {
      log.error("스크린 리더 힌트 생성 실패: {}", e.getMessage());
      return "기본 스크린 리더 힌트";
    }
  }
  
  /**
   * 읽기 수준에 따른 텍스트 간소화
   */
  private String simplifyByReadingLevel(String text, String readingLevel) {
    if ("grade3".equals(readingLevel)) {
      // 3학년 수준 - 매우 간단하게
      text = text.replace("실행하려면", "하려면");
      text = text.replace("성공적으로", "잘");
      text = text.replace("완료되었습니다", "됐어요");
      
      // 긴 텍스트 자르기
      if (text.length() > 30) {
        text = text.substring(0, 30) + "...";
      }
    } else if ("grade5".equals(readingLevel)) {
      // 5학년 수준 - 적당히 간단하게
      text = text.replace("성공적으로 완료되었습니다", "완료했어요");
      
      // 긴 텍스트 자르기
      if (text.length() > 50) {
        text = text.substring(0, 50) + "...";
      }
    }
    
    // 느낌표 줄이기
    text = text.replaceAll("!+", "!");
    
    return text;
  }
  
  /**
   * 컨텍스트에 따른 이모지 추가
   */
  private String addEmoji(String context, String text) {
    switch (context) {
      case "medication_reminder":
        return "💊 " + text;
      case "emergency":
        return "🚨 " + text;
      case "success_message":
        return "✅ " + text;
      case "error_message":
        return "❌ " + text;
      case "battery_low":
        return "🔋 " + text;
      case "location_update":
        return "📍 " + text;
      case "time_announcement":
        return "⏰ " + text;
      default:
        return text;
    }
  }

  /**
   * 텍스트를 음성으로 변환하여 재생
   */
  public void speak(String text, String language) {
    log.info("음성 안내: [{}] {}", language, text);
    
    if (googleTtsService == null) {
      log.warn("GoogleTtsService가 사용 불가능합니다. 폴백 모드로 실행합니다.");
      log.info("폴백 음성 안내: [{}] {}", language, text);
      return;
    }
    
    try {
      // BIF 사용자를 위한 텍스트 전처리
      String processedText = googleTtsService.preprocessTextForBIF(text);
      
      // Google TTS로 음성 합성
      byte[] audioData = googleTtsService.synthesizeText(processedText, language);
      
      // 실제 환경에서는 오디오 재생 로직 필요
      // 현재는 로깅으로 대체
      log.info("TTS 음성 생성 완료 - 크기: {} bytes", audioData.length);
      
      // TODO: 실제 오디오 재생을 위한 플레이어 구현 또는 클라이언트로 전송
      // audioPlayer.play(audioData);
      // 또는 WebSocket으로 클라이언트에 전송
      
    } catch (Exception e) {
      log.error("TTS 음성 생성 실패: {}", e.getMessage());
      // 폴백: 로그만 출력
      log.info("폴백 음성 안내: [{}] {}", language, text);
    }
  }

  /**
   * 긴급 음성 안내
   */
  public void speakEmergency(String text, String language) {
    log.warn("긴급 음성 안내: [{}] {}", language, text);
    
    if (googleTtsService == null) {
      log.warn("GoogleTtsService가 사용 불가능합니다. 폴백 모드로 실행합니다.");
      log.warn("폴백 긴급 음성 안내: [{}] {}", language, text);
      return;
    }
    
    try {
      // BIF 사용자를 위한 텍스트 전처리
      String processedText = googleTtsService.preprocessTextForBIF(text);
      
      // 긴급 상황용 더 크고 명확한 음성으로 합성
      byte[] audioData = googleTtsService.synthesizeEmergencyText(processedText, language);
      
      log.warn("긴급 TTS 음성 생성 완료 - 크기: {} bytes", audioData.length);
      
      // TODO: 긴급 상황용 오디오 재생 (더 크고 반복적으로)
      
    } catch (Exception e) {
      log.error("긴급 TTS 음성 생성 실패: {}", e.getMessage());
      // 폴백: 로그만 출력
      log.warn("폴백 긴급 음성 안내: [{}] {}", language, text);
    }
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