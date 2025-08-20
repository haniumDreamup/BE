package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.AccessibilitySettingsRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * VoiceGuidanceService 테스트
 * 100% 커버리지 목표
 */
@ExtendWith(MockitoExtension.class)
class VoiceGuidanceServiceTest {
  
  @Mock
  private UserRepository userRepository;
  
  @Mock
  private AccessibilitySettingsRepository accessibilitySettingsRepository;
  
  @InjectMocks
  private VoiceGuidanceService voiceGuidanceService;
  
  private User testUser;
  private AccessibilitySettings testSettings;
  
  @BeforeEach
  void setUp() {
    testUser = User.builder()
      .userId(1L)
      .username("testuser")
      .email("test@test.com")
      .build();
    
    testSettings = AccessibilitySettings.builder()
      .settingsId(1L)
      .user(testUser)
      .voiceLanguage("ko-KR")
      .readingLevel("grade5")
      .useEmojis(true)
      .simplifiedUiEnabled(true)
      .build();
  }
  
  @Test
  @DisplayName("한국어 음성 안내 텍스트 생성 - 버튼 클릭 컨텍스트")
  void generateVoiceGuidance_KoreanButtonClick() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    Map<String, Object> params = new HashMap<>();
    params.put("buttonName", "확인");
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "button_click", params);
    
    // Then
    assertThat(result).contains("확인");
    assertThat(result).contains("버튼");
    assertThat(result).contains("두 번 탭하세요");
    verify(accessibilitySettingsRepository).findByUserId(1L);
  }
  
  @Test
  @DisplayName("영어 음성 안내 텍스트 생성")
  void generateVoiceGuidance_English() {
    // Given
    testSettings.setVoiceLanguage("en-US");
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    Map<String, Object> params = new HashMap<>();
    params.put("buttonName", "Submit");
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "button_click", params);
    
    // Then
    assertThat(result).contains("Submit");
    assertThat(result).contains("button");
    assertThat(result).contains("Double tap");
  }
  
  @Test
  @DisplayName("이모지 추가 - 복약 알림")
  void generateVoiceGuidance_WithEmoji() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    Map<String, Object> params = new HashMap<>();
    params.put("medication", "아스피린");
    params.put("time", "오전 9시");
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "medication_reminder", params);
    
    // Then
    assertThat(result).startsWith("💊");
    assertThat(result).contains("복약 시간");
    assertThat(result).contains("아스피린");
  }
  
  @Test
  @DisplayName("3학년 수준 텍스트 간소화")
  void generateVoiceGuidance_Grade3Simplification() {
    // Given
    testSettings.setReadingLevel("grade3");
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    Map<String, Object> params = new HashMap<>();
    params.put("action", "설정");
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "button_click", params);
    
    // Then
    assertThat(result).doesNotContain("실행하려면");
    assertThat(result).contains("하려면");
  }
  
  @Test
  @DisplayName("설정이 없는 경우 기본값 사용")
  void generateVoiceGuidance_NoSettings() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.empty());
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "button_click", new HashMap<>());
    
    // Then
    assertThat(result).isNotEmpty();
    assertThat(result).contains("버튼");
  }
  
  @Test
  @DisplayName("예외 발생 시 기본 텍스트 반환")
  void generateVoiceGuidance_ExceptionHandling() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(anyLong()))
      .thenThrow(new RuntimeException("DB 오류"));
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "button_click", null);
    
    // Then
    assertThat(result).isEqualTo("button click");
  }
  
  @Test
  @DisplayName("ARIA 라벨 생성 - 버튼")
  void generateAriaLabel_Button() {
    // Given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("disabled", true);
    
    // When
    String label = voiceGuidanceService.generateAriaLabel("button", "저장", attributes);
    
    // Then
    assertThat(label).contains("저장");
    assertThat(label).contains("버튼");
    assertThat(label).contains("비활성화됨");
  }
  
  @Test
  @DisplayName("ARIA 라벨 생성 - 비밀번호 입력")
  void generateAriaLabel_PasswordInput() {
    // Given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("type", "password");
    attributes.put("required", true);
    
    // When
    String label = voiceGuidanceService.generateAriaLabel("input", "비밀번호", attributes);
    
    // Then
    assertThat(label).contains("비밀번호");
    assertThat(label).contains("비밀번호 입력");
    assertThat(label).contains("필수");
  }
  
  @Test
  @DisplayName("ARIA 라벨 생성 - 체크박스")
  void generateAriaLabel_Checkbox() {
    // Given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("checked", true);
    
    // When
    String label = voiceGuidanceService.generateAriaLabel("checkbox", "동의", attributes);
    
    // Then
    assertThat(label).contains("동의");
    assertThat(label).contains("체크박스");
    assertThat(label).contains("선택됨");
  }
  
  @Test
  @DisplayName("ARIA 라벨 생성 - 이미지")
  void generateAriaLabel_Image() {
    // Given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("alt", "프로필 사진");
    
    // When
    String label = voiceGuidanceService.generateAriaLabel("image", null, attributes);
    
    // Then
    assertThat(label).contains("이미지");
    assertThat(label).contains("프로필 사진");
  }
  
  @Test
  @DisplayName("ARIA 라벨 생성 - 목록")
  void generateAriaLabel_List() {
    // Given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("itemCount", 5);
    
    // When
    String label = voiceGuidanceService.generateAriaLabel("list", "메뉴", attributes);
    
    // Then
    assertThat(label).contains("메뉴");
    assertThat(label).contains("목록");
    assertThat(label).contains("5개 항목");
  }
  
  @Test
  @DisplayName("스크린 리더 힌트 생성 - 탭 동작")
  void generateScreenReaderHint_Tap() {
    // When
    String hint = voiceGuidanceService.generateScreenReaderHint("tap", "설정 메뉴");
    
    // Then
    assertThat(hint).contains("설정 메뉴");
    assertThat(hint).contains("선택하려면 두 번 탭하세요");
  }
  
  @Test
  @DisplayName("스크린 리더 힌트 생성 - 스와이프")
  void generateScreenReaderHint_Swipe() {
    // When
    String hintLeft = voiceGuidanceService.generateScreenReaderHint("swipe_left", "");
    String hintRight = voiceGuidanceService.generateScreenReaderHint("swipe_right", "");
    
    // Then
    assertThat(hintLeft).contains("이전 항목");
    assertThat(hintLeft).contains("왼쪽으로 쓸어넘기세요");
    assertThat(hintRight).contains("다음 항목");
    assertThat(hintRight).contains("오른쪽으로 쓸어넘기세요");
  }
  
  @Test
  @DisplayName("스크린 리더 힌트 생성 - 길게 누르기")
  void generateScreenReaderHint_LongPress() {
    // When
    String hint = voiceGuidanceService.generateScreenReaderHint("long_press", "아이템");
    
    // Then
    assertThat(hint).contains("아이템");
    assertThat(hint).contains("추가 옵션");
    assertThat(hint).contains("길게 누르세요");
  }
  
  @Test
  @DisplayName("스크린 리더 힌트 생성 - 알 수 없는 동작")
  void generateScreenReaderHint_UnknownAction() {
    // When
    String hint = voiceGuidanceService.generateScreenReaderHint("unknown_action", "test");
    
    // Then
    assertThat(hint).isEmpty();
  }
  
  @Test
  @DisplayName("템플릿이 없는 컨텍스트 처리")
  void generateVoiceGuidance_NoTemplate() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "unknown_context", null);
    
    // Then
    assertThat(result).isEqualTo("unknown_context");
  }
  
  @Test
  @DisplayName("지원하지 않는 언어 처리")
  void generateVoiceGuidance_UnsupportedLanguage() {
    // Given
    testSettings.setVoiceLanguage("fr-FR"); // 프랑스어 (미지원)
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    Map<String, Object> params = new HashMap<>();
    params.put("buttonName", "Test");
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "button_click", params);
    
    // Then
    assertThat(result).contains("버튼"); // 한국어 기본값 사용
  }
  
  @Test
  @DisplayName("긴 텍스트 자르기 - 3학년 수준")
  void generateVoiceGuidance_TruncateLongText_Grade3() {
    // Given
    testSettings.setReadingLevel("grade3");
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    Map<String, Object> params = new HashMap<>();
    params.put("text", "이것은 매우 긴 문장입니다. 3학년 읽기 수준에서는 30자를 초과하면 잘려야 합니다.");
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "loading", params);
    
    // Then
    assertThat(result.length()).isLessThanOrEqualTo(33); // 30 + "..."
  }
  
  @Test
  @DisplayName("모든 이모지 컨텍스트 테스트")
  void generateVoiceGuidance_AllEmojiContexts() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    String[] contexts = {
      "emergency", "success_message", "error_message", 
      "battery_low", "location_update", "time_announcement"
    };
    
    String[] expectedEmojis = {"🚨", "✅", "❌", "🔋", "📍", "⏰"};
    
    for (int i = 0; i < contexts.length; i++) {
      // When
      String result = voiceGuidanceService.generateVoiceGuidance(1L, contexts[i], new HashMap<>());
      
      // Then
      assertThat(result).startsWith(expectedEmojis[i]);
    }
  }
  
  @Test
  @DisplayName("이모지 비활성화 설정")
  void generateVoiceGuidance_DisabledEmoji() {
    // Given
    testSettings.setUseEmojis(false);
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    String result = voiceGuidanceService.generateVoiceGuidance(1L, "emergency", new HashMap<>());
    
    // Then
    assertThat(result).doesNotContain("🚨");
    assertThat(result).contains("긴급");
  }
  
  @Test
  @DisplayName("ARIA 라벨 - 라디오 버튼")
  void generateAriaLabel_RadioButton() {
    // Given
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("checked", false);
    
    // When
    String label = voiceGuidanceService.generateAriaLabel("radio", "옵션1", attributes);
    
    // Then
    assertThat(label).contains("옵션1");
    assertThat(label).contains("라디오 버튼");
    assertThat(label).doesNotContain("선택됨");
  }
  
  @Test
  @DisplayName("ARIA 라벨 - 링크")
  void generateAriaLabel_Link() {
    // When
    String label = voiceGuidanceService.generateAriaLabel("link", "홈으로", new HashMap<>());
    
    // Then
    assertThat(label).contains("홈으로");
    assertThat(label).contains("링크");
  }
  
  @Test
  @DisplayName("ARIA 라벨 - 기타 입력 타입")
  void generateAriaLabel_OtherInputTypes() {
    // Given
    Map<String, Object> emailAttrs = new HashMap<>();
    emailAttrs.put("type", "email");
    
    Map<String, Object> numberAttrs = new HashMap<>();
    numberAttrs.put("type", "number");
    
    // When
    String emailLabel = voiceGuidanceService.generateAriaLabel("input", "이메일", emailAttrs);
    String numberLabel = voiceGuidanceService.generateAriaLabel("input", "나이", numberAttrs);
    
    // Then
    assertThat(emailLabel).contains("이메일 입력");
    assertThat(numberLabel).contains("숫자 입력");
  }
}