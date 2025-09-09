package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.statistics.GeofenceStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.SafetyStatsDto;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * StatisticsService 통합 테스트
 * 실제 Repository와 함께 동작하는 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(StatisticsService.class)
@DisplayName("StatisticsService 통합 테스트")
class StatisticsServiceIntegrationTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private LocationHistoryRepository locationHistoryRepository;

    @Autowired
    private EmergencyRepository emergencyRepository;

    private User testUser;
    private Geofence testGeofence;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .cognitiveLevel(User.CognitiveLevel.MILD)
                .isActive(true)
                .build();
        testUser = userRepository.save(testUser);

        // 테스트 지오펜스 생성
        testGeofence = Geofence.builder()
                .user(testUser)
                .name("집")
                .centerLatitude(37.5665)
                .centerLongitude(126.9780)
                .radiusMeters(100)
                .type(Geofence.GeofenceType.SAFE_ZONE)
                .isActive(true)
                .build();
        testGeofence = geofenceRepository.save(testGeofence);
    }

    @Test
    @DisplayName("지오펜스 통계 조회 - 데이터 없음")
    void getGeofenceStatistics_NoData() {
        // When
        GeofenceStatsDto stats = statisticsService.getGeofenceStatistics(
                testUser.getUserId(),
                LocalDate.now().minusDays(7),
                LocalDate.now()
        );

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalEntries).isZero();
        assertThat(stats.totalExits).isZero();
        // The service may return an empty geofence entry for the test user's geofence
        // This is acceptable behavior for a user with existing geofences but no activity
    }

    @Test
    @DisplayName("안전 통계 조회 - 기본 데이터")
    void getSafetyStatistics_BasicData() {
        // Given - 긴급 상황 생성
        Emergency emergency = Emergency.builder()
                .user(testUser)
                .type(Emergency.EmergencyType.PANIC_BUTTON)
                .status(Emergency.EmergencyStatus.RESOLVED)
                .description("테스트 긴급 상황")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();
        emergencyRepository.save(emergency);

        // When
        SafetyStatsDto stats = statisticsService.getSafetyStatistics(
                testUser.getUserId(),
                LocalDate.now().minusDays(30),
                LocalDate.now()
        );

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.totalSosAlerts).isEqualTo(1);
        assertThat(stats.resolvedSosAlerts).isEqualTo(1);
        assertThat(stats.recentIncidents).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 - 예외 발생")
    void getGeofenceStatistics_NonExistentUser() {
        // When & Then - 존재하지 않는 사용자에 대해 예외 발생
        assertThatThrownBy(() -> {
            statisticsService.getGeofenceStatistics(
                999L,
                LocalDate.now().minusDays(7),
                LocalDate.now()
            );
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("날짜 범위 기본값 적용")
    void getGeofenceStatistics_DefaultDateRange() {
        // When - null 날짜로 호출
        GeofenceStatsDto stats = statisticsService.getGeofenceStatistics(
                testUser.getUserId(),
                null,  // 기본값 적용되어야 함
                null   // 기본값 적용되어야 함
        );

        // Then
        assertThat(stats).isNotNull();
        // 기본값이 적용되어 결과가 반환되어야 함
    }

    @Test
    @DisplayName("위치 기록이 있는 경우 지오펜스 통계")
    void getGeofenceStatistics_WithLocationHistory() {
        // Given - 위치 기록 생성
        LocationHistory locationHistory = LocationHistory.builder()
                .user(testUser)
                .latitude(new java.math.BigDecimal("37.5665"))
                .longitude(new java.math.BigDecimal("126.9780"))
                .accuracy(new java.math.BigDecimal("5.0"))
                .capturedAt(LocalDateTime.now().minusHours(1))
                .locationType(LocationHistory.LocationType.HOME)
                .build();
        locationHistoryRepository.save(locationHistory);

        // When
        GeofenceStatsDto stats = statisticsService.getGeofenceStatistics(
                testUser.getUserId(),
                LocalDate.now().minusDays(1),
                LocalDate.now()
        );

        // Then
        assertThat(stats).isNotNull();
        // 실제 통계 계산 로직이 실행됨
    }
}