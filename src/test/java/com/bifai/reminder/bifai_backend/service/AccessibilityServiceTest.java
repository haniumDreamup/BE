package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.accessibility.*;
import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.AccessibilitySettingsRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * AccessibilityService 테스트
 * 100% 커버리지 목표
 */
@ExtendWith(MockitoExtension.class)
class AccessibilityServiceTest {
  
  @Mock
  private AccessibilitySettingsRepository accessibilitySettingsRepository;
  
  @Mock
  private UserRepository userRepository;
  
  @Mock
  private SimpMessagingTemplate messagingTemplate;
  
  @InjectMocks
  private AccessibilityService accessibilityService;
  
  private User testUser;
  private AccessibilitySettings testSettings;
  
  @BeforeEach
  void setUp() {
    testUser = User.builder()
      .userId(1L)
      .username("testuser")
      .email("test@test.com")
      .isActive(true)
      .build();
    
    testSettings = AccessibilitySettings.builder()
      .settingsId(1L)
      .user(testUser)
      .highContrastEnabled(false)
      .colorScheme("default")
      .fontSize("medium")
      .voiceGuidanceEnabled(true)
      .voiceSpeed(1.0f)
      .voicePitch(1.0f)
      .voiceLanguage("ko-KR")
      .simplifiedUiEnabled(true)
      .largeTouchTargets(true)
      .simpleLanguageEnabled(true)
      .readingLevel("grade5")
      .useEmojis(true)
      .syncEnabled(true)
      .build();
  }
  
  @Test
  @DisplayName("접근성 설정 조회 - 기존 설정")
  void getSettings_ExistingSettings() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    AccessibilitySettingsDto result = accessibilityService.getSettings(1L);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1L);
    assertThat(result.getVoiceGuidanceEnabled()).isTrue();
    assertThat(result.getSimplifiedUiEnabled()).isTrue();
    assertThat(result.getReadingLevel()).isEqualTo("grade5");
  }
  
  @Test
  @DisplayName("접근성 설정 조회 - 설정 없는 경우 기본값 생성")
  void getSettings_CreateDefault() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.empty());
    when(userRepository.findById(1L))
      .thenReturn(Optional.of(testUser));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenAnswer(invocation -> {
        AccessibilitySettings saved = invocation.getArgument(0);
        saved.setSettingsId(2L);
        return saved;
      });
    
    // When
    AccessibilitySettingsDto result = accessibilityService.getSettings(1L);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSimplifiedUiEnabled()).isTrue();
    assertThat(result.getSimpleLanguageEnabled()).isTrue();
    assertThat(result.getLargeTouchTargets()).isTrue();
    assertThat(result.getVoiceGuidanceEnabled()).isTrue();
    verify(accessibilitySettingsRepository).save(any(AccessibilitySettings.class));
  }
  
  @Test
  @DisplayName("접근성 설정 업데이트")
  void updateSettings_Success() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    AccessibilitySettingsDto updateDto = AccessibilitySettingsDto.builder()
      .highContrastEnabled(true)
      .fontSize("large")
      .voiceSpeed(1.5f)
      .build();
    
    // When
    AccessibilitySettingsDto result = accessibilityService.updateSettings(1L, updateDto);
    
    // Then
    assertThat(result).isNotNull();
    ArgumentCaptor<AccessibilitySettings> captor = ArgumentCaptor.forClass(AccessibilitySettings.class);
    verify(accessibilitySettingsRepository).save(captor.capture());
    
    AccessibilitySettings saved = captor.getValue();
    assertThat(saved.getHighContrastEnabled()).isTrue();
    assertThat(saved.getFontSize()).isEqualTo("large");
    assertThat(saved.getVoiceSpeed()).isEqualTo(1.5f);
  }
  
  @Test
  @DisplayName("접근성 설정 업데이트 - 동기화 활성화")
  void updateSettings_WithSync() {
    // Given
    testSettings.setSyncEnabled(true);
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    AccessibilitySettingsDto updateDto = AccessibilitySettingsDto.builder()
      .highContrastEnabled(true)
      .build();
    
    // When
    accessibilityService.updateSettings(1L, updateDto);
    
    // Then
    verify(messagingTemplate).convertAndSend(
      eq("/user/1/accessibility/sync"), 
      any(AccessibilitySettingsDto.class)
    );
  }
  
  @Test
  @DisplayName("프로파일 적용 - 시각 장애")
  void applyProfile_VisualImpaired() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    // When
    AccessibilitySettingsDto result = accessibilityService.applyProfile(1L, "visual-impaired");
    
    // Then
    ArgumentCaptor<AccessibilitySettings> captor = ArgumentCaptor.forClass(AccessibilitySettings.class);
    verify(accessibilitySettingsRepository).save(captor.capture());
    
    AccessibilitySettings saved = captor.getValue();
    assertThat(saved.getHighContrastEnabled()).isTrue();
    assertThat(saved.getFontSize()).isEqualTo("extra-large");
    assertThat(saved.getVoiceGuidanceEnabled()).isTrue();
    assertThat(saved.getShowFocusIndicators()).isTrue();
  }
  
  @Test
  @DisplayName("프로파일 적용 - 인지 장애")
  void applyProfile_Cognitive() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    // When
    accessibilityService.applyProfile(1L, "cognitive");
    
    // Then
    ArgumentCaptor<AccessibilitySettings> captor = ArgumentCaptor.forClass(AccessibilitySettings.class);
    verify(accessibilitySettingsRepository).save(captor.capture());
    
    AccessibilitySettings saved = captor.getValue();
    assertThat(saved.getSimplifiedUiEnabled()).isTrue();
    assertThat(saved.getSimpleLanguageEnabled()).isTrue();
    assertThat(saved.getReadingLevel()).isEqualTo("grade3");
    assertThat(saved.getUseEmojis()).isTrue();
  }
  
  @Test
  @DisplayName("색상 스키마 목록 조회")
  void getAvailableColorSchemes() {
    // When
    List<ColorSchemeDto> schemes = accessibilityService.getAvailableColorSchemes();
    
    // Then
    assertThat(schemes).isNotEmpty();
    assertThat(schemes).hasSize(4);
    assertThat(schemes.stream().map(ColorSchemeDto::getId))
      .containsExactly("default", "dark", "high-contrast", "color-blind");
  }
  
  @Test
  @DisplayName("현재 색상 스키마 조회 - 설정 있음")
  void getCurrentColorScheme_WithSettings() {
    // Given
    testSettings.setColorScheme("dark");
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    ColorSchemeDto scheme = accessibilityService.getCurrentColorScheme(1L);
    
    // Then
    assertThat(scheme).isNotNull();
    assertThat(scheme.getId()).isEqualTo("dark");
    assertThat(scheme.getName()).isEqualTo("다크 모드");
  }
  
  @Test
  @DisplayName("현재 색상 스키마 조회 - 설정 없음")
  void getCurrentColorScheme_NoSettings() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.empty());
    
    // When
    ColorSchemeDto scheme = accessibilityService.getCurrentColorScheme(1L);
    
    // Then
    assertThat(scheme).isNotNull();
    assertThat(scheme.getId()).isEqualTo("default");
  }
  
  @Test
  @DisplayName("간소화된 네비게이션 조회 - 간소화 모드")
  void getSimplifiedNavigation_Simplified() {
    // Given
    testSettings.setSimplifiedUiEnabled(true);
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    SimplifiedNavigationDto navigation = accessibilityService.getSimplifiedNavigation(1L);
    
    // Then
    assertThat(navigation).isNotNull();
    assertThat(navigation.getSimplified()).isTrue();
    assertThat(navigation.getMaxDepth()).isEqualTo(2);
    assertThat(navigation.getMainMenuItems()).hasSize(4);
    assertThat(navigation.getBreadcrumbsEnabled()).isFalse();
  }
  
  @Test
  @DisplayName("간소화된 네비게이션 조회 - 전체 모드")
  void getSimplifiedNavigation_Full() {
    // Given
    testSettings.setSimplifiedUiEnabled(false);
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    SimplifiedNavigationDto navigation = accessibilityService.getSimplifiedNavigation(1L);
    
    // Then
    assertThat(navigation.getSimplified()).isFalse();
    assertThat(navigation.getMaxDepth()).isEqualTo(3);
    assertThat(navigation.getMainMenuItems()).hasSize(7);
    assertThat(navigation.getBreadcrumbsEnabled()).isTrue();
  }
  
  @Test
  @DisplayName("터치 타겟 정보 조회 - 큰 터치 타겟")
  void getTouchTargetInfo_LargeTargets() {
    // Given
    testSettings.setLargeTouchTargets(true);
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    TouchTargetDto touchInfo = accessibilityService.getTouchTargetInfo(1L, "tablet");
    
    // Then
    assertThat(touchInfo).isNotNull();
    assertThat(touchInfo.getMinSize()).isEqualTo(48);
    assertThat(touchInfo.getRecommendedSize()).isEqualTo(56);
    assertThat(touchInfo.getSpacing()).isEqualTo(12);
    assertThat(touchInfo.getDeviceType()).isEqualTo("tablet");
    assertThat(touchInfo.getWcagCompliant()).isTrue();
  }
  
  @Test
  @DisplayName("터치 타겟 정보 조회 - 일반 터치 타겟")
  void getTouchTargetInfo_NormalTargets() {
    // Given
    testSettings.setLargeTouchTargets(false);
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    // When
    TouchTargetDto touchInfo = accessibilityService.getTouchTargetInfo(1L, null);
    
    // Then
    assertThat(touchInfo.getMinSize()).isEqualTo(44);
    assertThat(touchInfo.getRecommendedSize()).isEqualTo(48);
    assertThat(touchInfo.getSpacing()).isEqualTo(8);
    assertThat(touchInfo.getDeviceType()).isEqualTo("mobile");
  }
  
  @Test
  @DisplayName("텍스트 간소화 - 3학년 수준")
  void simplifyText_Grade3() {
    // Given
    testSettings.setReadingLevel("grade3");
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    String originalText = "이것은 복잡한 문장입니다. 여러 개의 느낌표가 있습니다!!!";
    
    // When
    SimplifiedTextResponse response = accessibilityService.simplifyText(1L, originalText, null);
    
    // Then
    assertThat(response).isNotNull();
    assertThat(response.getOriginalText()).isEqualTo(originalText);
    assertThat(response.getSimplifiedText()).doesNotContain("!!!");
    assertThat(response.getReadingLevel()).isEqualTo("grade3");
  }
  
  @Test
  @DisplayName("텍스트 간소화 - 대상 레벨 지정")
  void simplifyText_WithTargetLevel() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    
    String originalText = "복잡한 텍스트입니다.";
    
    // When
    SimplifiedTextResponse response = accessibilityService.simplifyText(1L, originalText, "grade5");
    
    // Then
    assertThat(response.getReadingLevel()).isEqualTo("grade5");
  }
  
  @Test
  @DisplayName("설정 동기화")
  void syncSettings_Success() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    // When
    SyncStatusDto status = accessibilityService.syncSettings(1L);
    
    // Then
    assertThat(status).isNotNull();
    assertThat(status.getUserId()).isEqualTo(1L);
    assertThat(status.getSuccess()).isTrue();
    assertThat(status.getSyncedDevices()).isEqualTo(2);
    
    verify(messagingTemplate).convertAndSend(anyString(), any(AccessibilitySettingsDto.class));
  }
  
  @Test
  @DisplayName("설정 동기화 - 설정 없음 예외")
  void syncSettings_NoSettings() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> accessibilityService.syncSettings(1L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("설정을 찾을 수 없습니다");
  }
  
  @Test
  @DisplayName("접근성 통계 조회")
  void getStatistics() {
    // Given
    List<Object[]> readingLevelStats = Arrays.asList(
      new Object[]{"grade3", 10L},
      new Object[]{"grade5", 20L},
      new Object[]{"grade7", 5L}
    );
    
    List<Object[]> colorSchemeStats = Arrays.asList(
      new Object[]{"default", 25L},
      new Object[]{"dark", 10L}
    );
    
    when(accessibilitySettingsRepository.countByReadingLevel())
      .thenReturn(readingLevelStats);
    when(accessibilitySettingsRepository.countByColorScheme())
      .thenReturn(colorSchemeStats);
    when(accessibilitySettingsRepository.count()).thenReturn(35L);
    when(accessibilitySettingsRepository.findByVoiceGuidanceEnabledTrue())
      .thenReturn(Arrays.asList(testSettings));
    when(accessibilitySettingsRepository.findBySimplifiedUiEnabledTrue())
      .thenReturn(Arrays.asList(testSettings, testSettings));
    
    // When
    AccessibilityStatisticsDto stats = accessibilityService.getStatistics();
    
    // Then
    assertThat(stats).isNotNull();
    assertThat(stats.getTotalUsers()).isEqualTo(35L);
    assertThat(stats.getReadingLevelDistribution()).hasSize(3);
    assertThat(stats.getColorSchemeDistribution()).hasSize(2);
    assertThat(stats.getVoiceGuidanceUsageRate()).isGreaterThan(0);
    assertThat(stats.getSimplifiedUiUsageRate()).isGreaterThan(0);
  }
  
  @Test
  @DisplayName("기본 설정 생성 - 사용자 없음 예외")
  void createDefaultSettings_UserNotFound() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(999L))
      .thenReturn(Optional.empty());
    when(userRepository.findById(999L))
      .thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> accessibilityService.getSettings(999L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("사용자를 찾을 수 없습니다");
  }
  
  @Test
  @DisplayName("프로파일 적용 - 운동 장애")
  void applyProfile_Motor() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    // When
    accessibilityService.applyProfile(1L, "motor");
    
    // Then
    ArgumentCaptor<AccessibilitySettings> captor = ArgumentCaptor.forClass(AccessibilitySettings.class);
    verify(accessibilitySettingsRepository).save(captor.capture());
    
    AccessibilitySettings saved = captor.getValue();
    assertThat(saved.getLargeTouchTargets()).isTrue();
    assertThat(saved.getStickyKeysEnabled()).isTrue();
    assertThat(saved.getReduceAnimations()).isTrue();
  }
  
  @Test
  @DisplayName("프로파일 적용 - 청각 장애")
  void applyProfile_Hearing() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    // When
    accessibilityService.applyProfile(1L, "hearing");
    
    // Then
    ArgumentCaptor<AccessibilitySettings> captor = ArgumentCaptor.forClass(AccessibilitySettings.class);
    verify(accessibilitySettingsRepository).save(captor.capture());
    
    AccessibilitySettings saved = captor.getValue();
    assertThat(saved.getVisualAlertsEnabled()).isTrue();
    assertThat(saved.getVibrationEnabled()).isTrue();
    assertThat(saved.getAudioAlertsEnabled()).isFalse();
  }
  
  @Test
  @DisplayName("설정 업데이트 - 모든 필드")
  void updateSettings_AllFields() {
    // Given
    when(accessibilitySettingsRepository.findByUserId(1L))
      .thenReturn(Optional.of(testSettings));
    when(accessibilitySettingsRepository.save(any(AccessibilitySettings.class)))
      .thenReturn(testSettings);
    
    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put("custom1", "value1");
    
    AccessibilitySettingsDto updateDto = AccessibilitySettingsDto.builder()
      .highContrastEnabled(true)
      .colorScheme("dark")
      .fontSize("large")
      .fontFamily("dyslexic-friendly")
      .voiceGuidanceEnabled(false)
      .voiceSpeed(1.5f)
      .voicePitch(0.8f)
      .voiceLanguage("en-US")
      .simplifiedUiEnabled(false)
      .largeTouchTargets(false)
      .reduceAnimations(true)
      .showFocusIndicators(false)
      .simpleLanguageEnabled(false)
      .readingLevel("adult")
      .showIcons(false)
      .useEmojis(false)
      .vibrationEnabled(false)
      .visualAlertsEnabled(false)
      .audioAlertsEnabled(false)
      .stickyKeysEnabled(true)
      .keyboardShortcutsEnabled(false)
      .customSettings(customSettings)
      .syncEnabled(false)
      .build();
    
    // When
    accessibilityService.updateSettings(1L, updateDto);
    
    // Then
    ArgumentCaptor<AccessibilitySettings> captor = ArgumentCaptor.forClass(AccessibilitySettings.class);
    verify(accessibilitySettingsRepository).save(captor.capture());
    
    AccessibilitySettings saved = captor.getValue();
    assertThat(saved.getHighContrastEnabled()).isTrue();
    assertThat(saved.getColorScheme()).isEqualTo("dark");
    assertThat(saved.getVoiceSpeed()).isEqualTo(1.5f);
    assertThat(saved.getCustomSettings()).isEqualTo(customSettings);
  }
  
  @Test
  @DisplayName("설정 검증 - 음성 속도 범위 초과")
  void validateSettings_VoiceSpeedOutOfRange() {
    // Given
    testSettings.setVoiceSpeed(3.0f); // 범위 초과
    testSettings.setVoicePitch(0.1f); // 범위 미달
    
    // When
    boolean result = testSettings.validateSettings();
    
    // Then
    assertThat(result).isTrue();
    assertThat(testSettings.getVoiceSpeed()).isEqualTo(1.0f); // 기본값으로 복원
    assertThat(testSettings.getVoicePitch()).isEqualTo(1.0f); // 기본값으로 복원
  }
  
  @Test
  @DisplayName("동기화 시간 업데이트")
  void updateSyncTime() {
    // Given
    LocalDateTime before = testSettings.getLastSyncedAt();
    
    // When
    testSettings.updateSyncTime();
    
    // Then
    assertThat(testSettings.getLastSyncedAt()).isNotEqualTo(before);
    assertThat(testSettings.getLastSyncedAt()).isNotNull();
  }
}