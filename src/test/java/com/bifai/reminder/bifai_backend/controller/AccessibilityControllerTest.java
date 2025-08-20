package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.accessibility.*;
import java.util.ArrayList;
import com.bifai.reminder.bifai_backend.service.AccessibilityService;
import com.bifai.reminder.bifai_backend.service.VoiceGuidanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AccessibilityController 테스트
 * 100% 커버리지 목표
 */
@WebMvcTest(AccessibilityController.class)
@WithMockUser(username = "testuser")
class AccessibilityControllerTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @org.springframework.boot.test.mock.mockito.MockBean
  private AccessibilityService accessibilityService;
  
  @org.springframework.boot.test.mock.mockito.MockBean
  private VoiceGuidanceService voiceGuidanceService;
  
  private AccessibilitySettingsDto testSettings;
  
  @BeforeEach
  void setUp() {
    testSettings = AccessibilitySettingsDto.builder()
      .userId(1L)
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
  @DisplayName("GET /api/accessibility/settings/{userId} - 설정 조회 성공")
  void getSettings_Success() throws Exception {
    // Given
    when(accessibilityService.getSettings(1L)).thenReturn(testSettings);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/settings/1"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.userId").value(1))
      .andExpect(jsonPath("$.data.voiceGuidanceEnabled").value(true))
      .andExpect(jsonPath("$.data.simplifiedUiEnabled").value(true))
      .andExpect(jsonPath("$.data.readingLevel").value("grade5"));
    
    verify(accessibilityService).getSettings(1L);
  }
  
  @Test
  @DisplayName("PUT /api/accessibility/settings/{userId} - 설정 업데이트 성공")
  void updateSettings_Success() throws Exception {
    // Given
    AccessibilitySettingsDto updateDto = AccessibilitySettingsDto.builder()
      .highContrastEnabled(true)
      .fontSize("large")
      .voiceSpeed(1.5f)
      .build();
    
    when(accessibilityService.updateSettings(eq(1L), org.mockito.ArgumentMatchers.any(AccessibilitySettingsDto.class)))
      .thenReturn(testSettings);
    
    // When & Then
    mockMvc.perform(put("/api/accessibility/settings/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateDto)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data").exists())
      .andExpect(jsonPath("$.message").value("설정이 업데이트되었습니다"));
    
    verify(accessibilityService).updateSettings(eq(1L), org.mockito.ArgumentMatchers.any(AccessibilitySettingsDto.class));
  }
  
  @Test
  @DisplayName("POST /api/accessibility/settings/{userId}/profile - 프로파일 적용 성공")
  void applyProfile_Success() throws Exception {
    // Given
    Map<String, String> request = new HashMap<>();
    request.put("profileType", "visual-impaired");
    
    when(accessibilityService.applyProfile(1L, "visual-impaired"))
      .thenReturn(testSettings);
    
    // When & Then
    mockMvc.perform(post("/api/accessibility/settings/1/profile")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data").exists())
      .andExpect(jsonPath("$.message").value("프로파일이 적용되었습니다"));
    
    verify(accessibilityService).applyProfile(1L, "visual-impaired");
  }
  
  @Test
  @DisplayName("GET /api/accessibility/voice-guidance - 음성 안내 생성 성공")
  void generateVoiceGuidance_Success() throws Exception {
    // Given
    String expectedGuidance = "확인 버튼. 두 번 탭하세요";
    when(voiceGuidanceService.generateVoiceGuidance(eq(1L), eq("button_click"), anyMap()))
      .thenReturn(expectedGuidance);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/voice-guidance")
        .param("userId", "1")
        .param("context", "button_click")
        .param("buttonName", "확인"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.text").value(expectedGuidance))
      .andExpect(jsonPath("$.data.context").value("button_click"));
    
    verify(voiceGuidanceService).generateVoiceGuidance(eq(1L), eq("button_click"), anyMap());
  }
  
  @Test
  @DisplayName("GET /api/accessibility/aria-label - ARIA 라벨 생성 성공")
  void generateAriaLabel_Success() throws Exception {
    // Given
    String expectedLabel = "저장 버튼, 비활성화됨";
    when(voiceGuidanceService.generateAriaLabel(eq("button"), eq("저장"), anyMap()))
      .thenReturn(expectedLabel);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/aria-label")
        .param("elementType", "button")
        .param("label", "저장")
        .param("disabled", "true"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.label").value(expectedLabel))
      .andExpect(jsonPath("$.data.elementType").value("button"));
    
    verify(voiceGuidanceService).generateAriaLabel(eq("button"), eq("저장"), anyMap());
  }
  
  @Test
  @DisplayName("GET /api/accessibility/screen-reader-hint - 스크린리더 힌트 생성 성공")
  void generateScreenReaderHint_Success() throws Exception {
    // Given
    String expectedHint = "설정 메뉴를 선택하려면 두 번 탭하세요";
    when(voiceGuidanceService.generateScreenReaderHint("tap", "설정 메뉴"))
      .thenReturn(expectedHint);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/screen-reader-hint")
        .param("action", "tap")
        .param("target", "설정 메뉴"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.hint").value(expectedHint))
      .andExpect(jsonPath("$.data.action").value("tap"));
    
    verify(voiceGuidanceService).generateScreenReaderHint("tap", "설정 메뉴");
  }
  
  @Test
  @DisplayName("GET /api/accessibility/color-schemes - 색상 스키마 목록 조회")
  void getColorSchemes_Success() throws Exception {
    // Given
    List<ColorSchemeDto> schemes = Arrays.asList(
      ColorSchemeDto.builder()
        .id("default")
        .name("기본")
        .description("기본 색상 테마")
        .backgroundColor("#FFFFFF")
        .textColor("#000000")
        .primaryColor("#007BFF")
        .build(),
      ColorSchemeDto.builder()
        .id("dark")
        .name("다크 모드")
        .description("어두운 배경")
        .backgroundColor("#000000")
        .textColor("#FFFFFF")
        .primaryColor("#0056B3")
        .build()
    );
    
    when(accessibilityService.getAvailableColorSchemes()).thenReturn(schemes);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/color-schemes"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(2)))
      .andExpect(jsonPath("$.data[0].id").value("default"))
      .andExpect(jsonPath("$.data[1].id").value("dark"));
    
    verify(accessibilityService).getAvailableColorSchemes();
  }
  
  @Test
  @DisplayName("GET /api/accessibility/color-scheme/{userId} - 현재 색상 스키마 조회")
  void getCurrentColorScheme_Success() throws Exception {
    // Given
    ColorSchemeDto scheme = ColorSchemeDto.builder()
      .id("dark")
      .name("다크 모드")
      .description("어두운 배경")
      .backgroundColor("#000000")
      .textColor("#FFFFFF")
      .primaryColor("#0056B3")
      .build();
    
    when(accessibilityService.getCurrentColorScheme(1L)).thenReturn(scheme);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/color-scheme/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.id").value("dark"))
      .andExpect(jsonPath("$.data.name").value("다크 모드"));
    
    verify(accessibilityService).getCurrentColorScheme(1L);
  }
  
  @Test
  @DisplayName("GET /api/accessibility/navigation/{userId} - 간소화된 네비게이션 조회")
  void getSimplifiedNavigation_Success() throws Exception {
    // Given
    SimplifiedNavigationDto navigation = SimplifiedNavigationDto.builder()
      .simplified(true)
      .maxDepth(2)
      .mainMenuItems(new ArrayList<>())
      .breadcrumbsEnabled(false)
      .build();
    
    when(accessibilityService.getSimplifiedNavigation(1L)).thenReturn(navigation);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/navigation/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.simplified").value(true))
      .andExpect(jsonPath("$.data.maxDepth").value(2))
      .andExpect(jsonPath("$.data.mainMenuItems", org.hamcrest.Matchers.hasSize(0)));
    
    verify(accessibilityService).getSimplifiedNavigation(1L);
  }
  
  @Test
  @DisplayName("GET /api/accessibility/touch-targets/{userId} - 터치 타겟 정보 조회")
  void getTouchTargets_Success() throws Exception {
    // Given
    TouchTargetDto touchInfo = TouchTargetDto.builder()
      .minSize(48)
      .recommendedSize(56)
      .spacing(12)
      .deviceType("tablet")
      .wcagCompliant(true)
      .build();
    
    when(accessibilityService.getTouchTargetInfo(1L, "tablet")).thenReturn(touchInfo);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/touch-targets/1")
        .param("deviceType", "tablet"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.minSize").value(48))
      .andExpect(jsonPath("$.data.recommendedSize").value(56))
      .andExpect(jsonPath("$.data.wcagCompliant").value(true));
    
    verify(accessibilityService).getTouchTargetInfo(1L, "tablet");
  }
  
  @Test
  @DisplayName("POST /api/accessibility/simplify-text - 텍스트 간소화 성공")
  void simplifyText_Success() throws Exception {
    // Given
    Map<String, String> request = new HashMap<>();
    request.put("text", "복잡한 문장입니다.");
    request.put("targetLevel", "grade3");
    
    SimplifiedTextResponse response = SimplifiedTextResponse.builder()
      .originalText("복잡한 문장입니다.")
      .simplifiedText("쉬운 문장입니다.")
      .readingLevel("grade3")
      .wordCount(3)
      .build();
    
    when(accessibilityService.simplifyText(eq(1L), eq("복잡한 문장입니다."), eq("grade3")))
      .thenReturn(response);
    
    // When & Then
    mockMvc.perform(post("/api/accessibility/simplify-text")
        .with(csrf())
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.originalText").value("복잡한 문장입니다."))
      .andExpect(jsonPath("$.data.simplifiedText").value("쉬운 문장입니다."))
      .andExpect(jsonPath("$.data.readingLevel").value("grade3"));
    
    verify(accessibilityService).simplifyText(1L, "복잡한 문장입니다.", "grade3");
  }
  
  @Test
  @DisplayName("POST /api/accessibility/settings/{userId}/sync - 설정 동기화 성공")
  void syncSettings_Success() throws Exception {
    // Given
    SyncStatusDto syncStatus = SyncStatusDto.builder()
      .userId(1L)
      .syncedAt(LocalDateTime.now())
      .success(true)
      .syncedDevices(2)
      .message("동기화 완료")
      .build();
    
    when(accessibilityService.syncSettings(1L)).thenReturn(syncStatus);
    
    // When & Then
    mockMvc.perform(post("/api/accessibility/settings/1/sync")
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.userId").value(1))
      .andExpect(jsonPath("$.data.success").value(true))
      .andExpect(jsonPath("$.data.syncedDevices").value(2))
      .andExpect(jsonPath("$.message").value("설정이 동기화되었습니다"));
    
    verify(accessibilityService).syncSettings(1L);
  }
  
  @Test
  @DisplayName("GET /api/accessibility/statistics - 통계 조회 성공")
  void getStatistics_Success() throws Exception {
    // Given
    AccessibilityStatisticsDto stats = new AccessibilityStatisticsDto();
    stats.setTotalUsers(100L);
    
    Map<String, Long> readingLevelDist = new HashMap<>();
    readingLevelDist.put("grade3", 30L);
    readingLevelDist.put("grade5", 50L);
    readingLevelDist.put("grade7", 20L);
    stats.setReadingLevelDistribution(readingLevelDist);
    
    Map<String, Long> colorSchemeDist = new HashMap<>();
    colorSchemeDist.put("default", 60L);
    colorSchemeDist.put("dark", 40L);
    stats.setColorSchemeDistribution(colorSchemeDist);
    
    stats.setVoiceGuidanceUsageRate(75.0);
    stats.setSimplifiedUiUsageRate(80.0);
    stats.setHighContrastUsageRate(25.0);
    
    when(accessibilityService.getStatistics()).thenReturn(stats);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/statistics"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.totalUsers").value(100))
      .andExpect(jsonPath("$.data.readingLevelDistribution.grade3").value(30))
      .andExpect(jsonPath("$.data.readingLevelDistribution.grade5").value(50))
      .andExpect(jsonPath("$.data.voiceGuidanceUsageRate").value(75.0));
    
    verify(accessibilityService).getStatistics();
  }
  
  @Test
  @DisplayName("GET /api/accessibility/settings/{userId} - 설정 조회 실패")
  void getSettings_NotFound() throws Exception {
    // Given
    when(accessibilityService.getSettings(999L))
      .thenThrow(new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/settings/999"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.error.message").value("사용자를 찾을 수 없습니다"));
    
    verify(accessibilityService).getSettings(999L);
  }
  
  @Test
  @DisplayName("PUT /api/accessibility/settings/{userId} - 설정 업데이트 실패")
  void updateSettings_BadRequest() throws Exception {
    // Given
    AccessibilitySettingsDto invalidDto = AccessibilitySettingsDto.builder()
      .voiceSpeed(3.0f) // 범위 초과
      .build();
    
    when(accessibilityService.updateSettings(eq(1L), org.mockito.ArgumentMatchers.any(AccessibilitySettingsDto.class)))
      .thenThrow(new IllegalArgumentException("음성 속도는 0.5와 2.0 사이여야 합니다"));
    
    // When & Then
    mockMvc.perform(put("/api/accessibility/settings/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(invalidDto)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.error.message").value("음성 속도는 0.5와 2.0 사이여야 합니다"));
  }
  
  @Test
  @DisplayName("POST /api/accessibility/settings/{userId}/sync - 동기화 실패")
  void syncSettings_Failed() throws Exception {
    // Given
    when(accessibilityService.syncSettings(1L))
      .thenThrow(new RuntimeException("네트워크 오류"));
    
    // When & Then
    mockMvc.perform(post("/api/accessibility/settings/1/sync")
        .with(csrf()))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.error.message").value("동기화 중 오류가 발생했습니다"));
  }
  
  @Test
  @DisplayName("GET /api/accessibility/voice-guidance - 파라미터 없이 호출")
  void generateVoiceGuidance_NoParams() throws Exception {
    // Given
    when(voiceGuidanceService.generateVoiceGuidance(eq(1L), eq("default"), anyMap()))
      .thenReturn("기본 안내");
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/voice-guidance")
        .param("userId", "1")
        .param("context", "default"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.text").value("기본 안내"));
  }
  
  @Test
  @DisplayName("GET /api/accessibility/touch-targets/{userId} - 디바이스 타입 없이 조회")
  void getTouchTargets_NoDeviceType() throws Exception {
    // Given
    TouchTargetDto touchInfo = TouchTargetDto.builder()
      .minSize(44)
      .recommendedSize(48)
      .spacing(8)
      .deviceType("mobile")
      .wcagCompliant(true)
      .build();
    
    when(accessibilityService.getTouchTargetInfo(1L, null)).thenReturn(touchInfo);
    
    // When & Then
    mockMvc.perform(get("/api/accessibility/touch-targets/1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.deviceType").value("mobile"));
    
    verify(accessibilityService).getTouchTargetInfo(1L, null);
  }
  
  @Test
  @DisplayName("POST /api/accessibility/simplify-text - targetLevel 없이 호출")
  void simplifyText_NoTargetLevel() throws Exception {
    // Given
    Map<String, String> request = new HashMap<>();
    request.put("text", "복잡한 문장입니다.");
    
    SimplifiedTextResponse response = SimplifiedTextResponse.builder()
      .originalText("복잡한 문장입니다.")
      .simplifiedText("쉬운 문장입니다.")
      .readingLevel("grade5")
      .wordCount(3)
      .build();
    
    when(accessibilityService.simplifyText(eq(1L), eq("복잡한 문장입니다."), isNull()))
      .thenReturn(response);
    
    // When & Then
    mockMvc.perform(post("/api/accessibility/simplify-text")
        .with(csrf())
        .param("userId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.readingLevel").value("grade5"));
    
    verify(accessibilityService).simplifyText(1L, "복잡한 문장입니다.", null);
  }
  
  @Test
  @DisplayName("POST /api/accessibility/settings/{userId}/profile - 잘못된 프로파일 타입")
  void applyProfile_InvalidType() throws Exception {
    // Given
    Map<String, String> request = new HashMap<>();
    request.put("profileType", "invalid");
    
    when(accessibilityService.applyProfile(1L, "invalid"))
      .thenThrow(new IllegalArgumentException("잘못된 프로파일 타입입니다"));
    
    // When & Then
    mockMvc.perform(post("/api/accessibility/settings/1/profile")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.error.message").value("잘못된 프로파일 타입입니다"));
  }
}