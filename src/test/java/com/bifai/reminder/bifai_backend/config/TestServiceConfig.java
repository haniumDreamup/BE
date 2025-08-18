package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseResponseDto;
import com.bifai.reminder.bifai_backend.service.pose.PoseDataService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 테스트용 Service Mock 설정
 */
@TestConfiguration
@Profile("test")
public class TestServiceConfig {

  @Bean
  @Primary
  public PoseDataService mockPoseDataService() {
    PoseDataService mockService = mock(PoseDataService.class);
    
    // processPoseData Mock
    PoseResponseDto mockResponse = PoseResponseDto.builder()
        .sessionId("test-session-123")
        .frameCount(1)
        .fallDetected(false)
        .confidenceScore(95.0f)
        .severity("NONE")
        .message("포즈 데이터가 정상적으로 처리되었습니다")
        .build();
    when(mockService.processPoseData(any(PoseDataDto.class))).thenReturn(mockResponse);
    
    // FallStatus Mock (메서드가 있는지 확인 필요)
    FallStatusDto mockFallStatus = FallStatusDto.builder()
        .userId(1L)
        .lastChecked(LocalDateTime.now())
        .recentFallEvents(new ArrayList<>())
        .isMonitoring(true)
        .sessionActive(true)
        .currentSessionId("test-session-123")
        .build();
    // checkFallStatus 메서드가 실제로 존재하는지 확인 필요
    
    return mockService;
  }
}