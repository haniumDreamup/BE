package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.accessibility.*;
import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.AccessibilitySettingsRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 접근성 설정 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccessibilityService {
  
  private final AccessibilitySettingsRepository accessibilitySettingsRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;
  
  /**
   * 사용자 접근성 설정 조회
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "accessibilitySettings", key = "#userId")
  public AccessibilitySettingsDto getSettings(Long userId) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElseGet(() -> createDefaultSettings(userId));
    
    return toDto(settings);
  }
  
  /**
   * 접근성 설정 업데이트
   */
  @Transactional
  @CacheEvict(value = "accessibilitySettings", key = "#userId")
  public AccessibilitySettingsDto updateSettings(Long userId, AccessibilitySettingsDto dto) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElseGet(() -> createDefaultSettings(userId));
    
    // 설정 업데이트
    updateFromDto(settings, dto);
    settings.validateSettings();
    
    AccessibilitySettings saved = accessibilitySettingsRepository.save(settings);
    
    // 다른 디바이스에 동기화 알림
    if (saved.getSyncEnabled()) {
      syncToOtherDevices(userId, saved);
    }
    
    log.info("접근성 설정 업데이트 - 사용자: {}", userId);
    
    return toDto(saved);
  }
  
  /**
   * 프로파일 적용
   */
  @Transactional
  @CacheEvict(value = "accessibilitySettings", key = "#userId")
  public AccessibilitySettingsDto applyProfile(Long userId, String profileType) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElseGet(() -> createDefaultSettings(userId));
    
    settings.applyProfile(profileType);
    AccessibilitySettings saved = accessibilitySettingsRepository.save(settings);
    
    log.info("프로파일 적용 - 사용자: {}, 프로파일: {}", userId, profileType);
    
    return toDto(saved);
  }
  
  /**
   * 사용 가능한 색상 스키마 목록
   */
  @Cacheable(value = "colorSchemes")
  public List<ColorSchemeDto> getAvailableColorSchemes() {
    List<ColorSchemeDto> schemes = new ArrayList<>();
    
    // 기본 스키마
    schemes.add(ColorSchemeDto.builder()
      .id("default")
      .name("기본")
      .description("표준 색상 스키마")
      .primaryColor("#1976D2")
      .secondaryColor("#424242")
      .backgroundColor("#FFFFFF")
      .textColor("#212121")
      .contrastRatio(7.0)
      .build());
    
    // 다크 모드
    schemes.add(ColorSchemeDto.builder()
      .id("dark")
      .name("다크 모드")
      .description("어두운 배경의 고대비 모드")
      .primaryColor("#90CAF9")
      .secondaryColor("#F5F5F5")
      .backgroundColor("#121212")
      .textColor("#FFFFFF")
      .contrastRatio(15.8)
      .build());
    
    // 고대비 모드
    schemes.add(ColorSchemeDto.builder()
      .id("high-contrast")
      .name("고대비")
      .description("WCAG AAA 수준의 고대비")
      .primaryColor("#FFFF00")
      .secondaryColor("#00FFFF")
      .backgroundColor("#000000")
      .textColor("#FFFFFF")
      .contrastRatio(21.0)
      .build());
    
    // 색맹 친화
    schemes.add(ColorSchemeDto.builder()
      .id("color-blind")
      .name("색맹 친화")
      .description("적록색맹을 위한 색상 조합")
      .primaryColor("#0173B2")
      .secondaryColor("#DE8F05")
      .backgroundColor("#FFFFFF")
      .textColor("#333333")
      .contrastRatio(8.5)
      .build());
    
    return schemes;
  }
  
  /**
   * 현재 색상 스키마 조회
   */
  public ColorSchemeDto getCurrentColorScheme(Long userId) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElse(null);
    
    String schemeId = settings != null ? settings.getColorScheme() : "default";
    
    return getAvailableColorSchemes().stream()
      .filter(s -> s.getId().equals(schemeId))
      .findFirst()
      .orElse(getAvailableColorSchemes().get(0));
  }
  
  /**
   * 간소화된 네비게이션 구조
   */
  @Cacheable(value = "simplifiedNavigation", key = "#userId")
  public SimplifiedNavigationDto getSimplifiedNavigation(Long userId) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElse(null);
    
    boolean useSimplified = settings != null && settings.getSimplifiedUiEnabled();
    
    return SimplifiedNavigationDto.builder()
      .simplified(useSimplified)
      .maxDepth(useSimplified ? 2 : 3)
      .mainMenuItems(getMainMenuItems(useSimplified))
      .quickActions(getQuickActions(userId))
      .breadcrumbsEnabled(!useSimplified)
      .build();
  }
  
  /**
   * 터치 타겟 정보
   */
  public TouchTargetDto getTouchTargetInfo(Long userId, String deviceType) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElse(null);
    
    boolean largeTouchTargets = settings != null && settings.getLargeTouchTargets();
    
    return TouchTargetDto.builder()
      .minSize(largeTouchTargets ? 48 : 44) // dp
      .recommendedSize(largeTouchTargets ? 56 : 48)
      .spacing(largeTouchTargets ? 12 : 8)
      .deviceType(deviceType != null ? deviceType : "mobile")
      .wcagCompliant(true)
      .build();
  }
  
  /**
   * 텍스트 간소화
   */
  public SimplifiedTextResponse simplifyText(Long userId, String text, String targetLevel) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElse(null);
    
    String readingLevel = targetLevel != null ? targetLevel : 
      (settings != null ? settings.getReadingLevel() : "grade5");
    
    // 실제로는 OpenAI API나 다른 NLP 서비스를 사용해야 함
    String simplifiedText = simplifyTextForLevel(text, readingLevel);
    
    return SimplifiedTextResponse.builder()
      .originalText(text)
      .simplifiedText(simplifiedText)
      .readingLevel(readingLevel)
      .wordCount(simplifiedText.split("\\s+").length)
      .build();
  }
  
  /**
   * 설정 동기화
   */
  @Transactional
  public SyncStatusDto syncSettings(Long userId) {
    AccessibilitySettings settings = accessibilitySettingsRepository.findByUserId(userId)
      .orElseThrow(() -> new IllegalArgumentException("설정을 찾을 수 없습니다"));
    
    settings.updateSyncTime();
    accessibilitySettingsRepository.save(settings);
    
    // WebSocket으로 다른 디바이스에 알림
    syncToOtherDevices(userId, settings);
    
    return SyncStatusDto.builder()
      .userId(userId)
      .syncedAt(LocalDateTime.now())
      .success(true)
      .syncedDevices(getSyncedDeviceCount(userId))
      .build();
  }
  
  /**
   * 접근성 통계
   */
  @Cacheable(value = "accessibilityStatistics")
  public AccessibilityStatisticsDto getStatistics() {
    AccessibilityStatisticsDto stats = new AccessibilityStatisticsDto();
    
    // 읽기 수준별 통계
    List<Object[]> readingLevelStats = accessibilitySettingsRepository.countByReadingLevel();
    Map<String, Long> readingLevelMap = new HashMap<>();
    for (Object[] row : readingLevelStats) {
      readingLevelMap.put((String) row[0], (Long) row[1]);
    }
    stats.setReadingLevelDistribution(readingLevelMap);
    
    // 색상 스키마별 통계
    List<Object[]> colorSchemeStats = accessibilitySettingsRepository.countByColorScheme();
    Map<String, Long> colorSchemeMap = new HashMap<>();
    for (Object[] row : colorSchemeStats) {
      colorSchemeMap.put((String) row[0], (Long) row[1]);
    }
    stats.setColorSchemeDistribution(colorSchemeMap);
    
    // 기능 사용률
    long totalUsers = accessibilitySettingsRepository.count();
    long voiceGuidanceUsers = accessibilitySettingsRepository.findByVoiceGuidanceEnabledTrue().size();
    long simplifiedUiUsers = accessibilitySettingsRepository.findBySimplifiedUiEnabledTrue().size();
    
    stats.setTotalUsers(totalUsers);
    stats.setVoiceGuidanceUsageRate((double) voiceGuidanceUsers / totalUsers * 100);
    stats.setSimplifiedUiUsageRate((double) simplifiedUiUsers / totalUsers * 100);
    
    return stats;
  }
  
  /**
   * 기본 설정 생성
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AccessibilitySettings createDefaultSettings(Long userId) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    AccessibilitySettings settings = AccessibilitySettings.builder()
      .user(user)
      .build();
    
    // BIF 사용자를 위한 기본 설정
    settings.setSimplifiedUiEnabled(true);
    settings.setSimpleLanguageEnabled(true);
    settings.setLargeTouchTargets(true);
    settings.setVoiceGuidanceEnabled(true);
    
    return accessibilitySettingsRepository.save(settings);
  }
  
  /**
   * DTO 변환
   */
  private AccessibilitySettingsDto toDto(AccessibilitySettings entity) {
    return AccessibilitySettingsDto.builder()
      .settingsId(entity.getSettingsId())
      .userId(entity.getUser().getUserId())
      .highContrastEnabled(entity.getHighContrastEnabled())
      .colorScheme(entity.getColorScheme())
      .fontSize(entity.getFontSize())
      .fontFamily(entity.getFontFamily())
      .voiceGuidanceEnabled(entity.getVoiceGuidanceEnabled())
      .voiceSpeed(entity.getVoiceSpeed())
      .voicePitch(entity.getVoicePitch())
      .voiceLanguage(entity.getVoiceLanguage())
      .simplifiedUiEnabled(entity.getSimplifiedUiEnabled())
      .largeTouchTargets(entity.getLargeTouchTargets())
      .reduceAnimations(entity.getReduceAnimations())
      .showFocusIndicators(entity.getShowFocusIndicators())
      .simpleLanguageEnabled(entity.getSimpleLanguageEnabled())
      .readingLevel(entity.getReadingLevel())
      .showIcons(entity.getShowIcons())
      .useEmojis(entity.getUseEmojis())
      .vibrationEnabled(entity.getVibrationEnabled())
      .visualAlertsEnabled(entity.getVisualAlertsEnabled())
      .audioAlertsEnabled(entity.getAudioAlertsEnabled())
      .stickyKeysEnabled(entity.getStickyKeysEnabled())
      .keyboardShortcutsEnabled(entity.getKeyboardShortcutsEnabled())
      .customSettings(entity.getCustomSettings())
      .profileType(entity.getProfileType())
      .lastSyncedAt(entity.getLastSyncedAt())
      .syncEnabled(entity.getSyncEnabled())
      .createdAt(entity.getCreatedAt())
      .updatedAt(entity.getUpdatedAt())
      .build();
  }
  
  /**
   * DTO에서 엔티티 업데이트
   */
  private void updateFromDto(AccessibilitySettings entity, AccessibilitySettingsDto dto) {
    if (dto.getHighContrastEnabled() != null) entity.setHighContrastEnabled(dto.getHighContrastEnabled());
    if (dto.getColorScheme() != null) entity.setColorScheme(dto.getColorScheme());
    if (dto.getFontSize() != null) entity.setFontSize(dto.getFontSize());
    if (dto.getFontFamily() != null) entity.setFontFamily(dto.getFontFamily());
    if (dto.getVoiceGuidanceEnabled() != null) entity.setVoiceGuidanceEnabled(dto.getVoiceGuidanceEnabled());
    if (dto.getVoiceSpeed() != null) entity.setVoiceSpeed(dto.getVoiceSpeed());
    if (dto.getVoicePitch() != null) entity.setVoicePitch(dto.getVoicePitch());
    if (dto.getVoiceLanguage() != null) entity.setVoiceLanguage(dto.getVoiceLanguage());
    if (dto.getSimplifiedUiEnabled() != null) entity.setSimplifiedUiEnabled(dto.getSimplifiedUiEnabled());
    if (dto.getLargeTouchTargets() != null) entity.setLargeTouchTargets(dto.getLargeTouchTargets());
    if (dto.getReduceAnimations() != null) entity.setReduceAnimations(dto.getReduceAnimations());
    if (dto.getShowFocusIndicators() != null) entity.setShowFocusIndicators(dto.getShowFocusIndicators());
    if (dto.getSimpleLanguageEnabled() != null) entity.setSimpleLanguageEnabled(dto.getSimpleLanguageEnabled());
    if (dto.getReadingLevel() != null) entity.setReadingLevel(dto.getReadingLevel());
    if (dto.getShowIcons() != null) entity.setShowIcons(dto.getShowIcons());
    if (dto.getUseEmojis() != null) entity.setUseEmojis(dto.getUseEmojis());
    if (dto.getVibrationEnabled() != null) entity.setVibrationEnabled(dto.getVibrationEnabled());
    if (dto.getVisualAlertsEnabled() != null) entity.setVisualAlertsEnabled(dto.getVisualAlertsEnabled());
    if (dto.getAudioAlertsEnabled() != null) entity.setAudioAlertsEnabled(dto.getAudioAlertsEnabled());
    if (dto.getStickyKeysEnabled() != null) entity.setStickyKeysEnabled(dto.getStickyKeysEnabled());
    if (dto.getKeyboardShortcutsEnabled() != null) entity.setKeyboardShortcutsEnabled(dto.getKeyboardShortcutsEnabled());
    if (dto.getCustomSettings() != null) entity.setCustomSettings(dto.getCustomSettings());
    if (dto.getSyncEnabled() != null) entity.setSyncEnabled(dto.getSyncEnabled());
  }
  
  /**
   * 텍스트 간소화 (실제 구현 필요)
   */
  private String simplifyTextForLevel(String text, String level) {
    // 임시 구현 - 실제로는 NLP 서비스 사용 필요
    switch (level) {
      case "grade3":
        return text.replaceAll("[.!?]+", ".")
          .replaceAll("\\b\\w{10,}\\b", "긴 단어");
      case "grade5":
        return text.replaceAll("[.!?]+", ".");
      default:
        return text;
    }
  }
  
  /**
   * 메인 메뉴 항목 (간소화 여부에 따라)
   */
  private List<NavigationItemDto> getMainMenuItems(boolean simplified) {
    List<NavigationItemDto> items = new ArrayList<>();
    
    if (simplified) {
      // 간소화된 메뉴 (중요 기능만)
      items.add(new NavigationItemDto("home", "홈", "🏠", 1));
      items.add(new NavigationItemDto("medication", "약", "💊", 2));
      items.add(new NavigationItemDto("emergency", "도움", "🆘", 3));
      items.add(new NavigationItemDto("settings", "설정", "⚙️", 4));
    } else {
      // 전체 메뉴
      items.add(new NavigationItemDto("home", "홈", "🏠", 1));
      items.add(new NavigationItemDto("medication", "복약", "💊", 2));
      items.add(new NavigationItemDto("schedule", "일정", "📅", 3));
      items.add(new NavigationItemDto("location", "위치", "📍", 4));
      items.add(new NavigationItemDto("guardian", "보호자", "👥", 5));
      items.add(new NavigationItemDto("emergency", "긴급", "🆘", 6));
      items.add(new NavigationItemDto("settings", "설정", "⚙️", 7));
    }
    
    return items;
  }
  
  /**
   * 빠른 실행 항목
   */
  private List<QuickActionDto> getQuickActions(Long userId) {
    List<QuickActionDto> actions = new ArrayList<>();
    
    actions.add(new QuickActionDto("call_guardian", "보호자 전화", "📞", "primary"));
    actions.add(new QuickActionDto("take_medication", "약 먹기", "💊", "success"));
    actions.add(new QuickActionDto("emergency", "긴급 도움", "🚨", "danger"));
    
    return actions;
  }
  
  /**
   * 다른 디바이스에 동기화
   */
  private void syncToOtherDevices(Long userId, AccessibilitySettings settings) {
    String destination = "/user/" + userId + "/accessibility/sync";
    messagingTemplate.convertAndSend(destination, toDto(settings));
    log.info("접근성 설정 동기화 알림 전송 - 사용자: {}", userId);
  }
  
  /**
   * 동기화된 디바이스 수 (임시)
   */
  private int getSyncedDeviceCount(Long userId) {
    // 실제로는 디바이스 관리 서비스에서 조회
    return 2;
  }
}