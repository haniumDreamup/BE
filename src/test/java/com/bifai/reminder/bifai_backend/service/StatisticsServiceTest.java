package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.statistics.DailyActivityStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.GeofenceStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.SafetyStatsDto;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private GeofenceRepository geofenceRepository;
    
    @Mock
    private LocationHistoryRepository locationHistoryRepository;
    
    @Mock
    private EmergencyRepository emergencyRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private User testUser;
    private Geofence testGeofence;
    private LocationHistory testLocationHistory;
    private Emergency testEmergency;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .userId(1L)
            .email("test@example.com")
            .fullName("테스트 사용자")
            .build();

        testGeofence = new Geofence();
        testGeofence.setId(1L);
        testGeofence.setName("테스트 지오펜스");
        testGeofence.setCenterLatitude(37.5665);
        testGeofence.setCenterLongitude(126.9780);
        testGeofence.setRadiusMeters(100);
        testGeofence.setUser(testUser);

        testLocationHistory = LocationHistory.builder()
            .id(1L)
            .user(testUser)
            .latitude(BigDecimal.valueOf(37.5665))
            .longitude(BigDecimal.valueOf(126.9780))
            .address("서울특별시 중구 명동")
            .capturedAt(LocalDateTime.now())
            .build();

        testEmergency = Emergency.builder()
            .id(1L)
            .user(testUser)
            .type(Emergency.EmergencyType.PANIC_BUTTON)
            .severity(Emergency.EmergencySeverity.HIGH)
            .status(Emergency.EmergencyStatus.RESOLVED)
            .createdAt(LocalDateTime.now())
            .description("테스트 응급상황")
            .build();
    }

    @Test
    @DisplayName("지오펜스 통계 조회 - 정상 케이스")
    void getGeofenceStatistics_Success() {
        // Given
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(geofenceRepository.findByUserUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        GeofenceStatsDto result = statisticsService.getGeofenceStatistics(userId, startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId).isEqualTo(userId);
        assertThat(result.startDate).isEqualTo(startDate);
        assertThat(result.endDate).isEqualTo(endDate);
        assertThat(result.totalGeofences).isEqualTo(1);
        assertThat(result.topGeofences).hasSize(1);
        assertThat(result.dailyActivity).isNotEmpty();
    }

    @Test
    @DisplayName("지오펜스 통계 조회 - 기본값 설정")
    void getGeofenceStatistics_WithDefaultDates() {
        // Given
        Long userId = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(geofenceRepository.findByUserUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        GeofenceStatsDto result = statisticsService.getGeofenceStatistics(userId, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.startDate).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(result.endDate).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("지오펜스 통계 조회 - 사용자 없음")
    void getGeofenceStatistics_UserNotFound() {
        // Given
        Long userId = 999L;
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            statisticsService.getGeofenceStatistics(userId, startDate, endDate))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("일별 활동 통계 조회 - 여러 날짜")
    void getDailyActivityStatistics_MultipleDay_Success() {
        // Given
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now();
        
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));

        // When
        List<DailyActivityStatsDto> result = statisticsService.getDailyActivityStatistics(userId, startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 3일간
        assertThat(result.get(0).userId).isEqualTo(userId);
        assertThat(result.get(0).hourlyBreakdown).hasSize(24);
    }

    @Test
    @DisplayName("일별 활동 통계 조회 - 기본값 설정")
    void getDailyActivityStatistics_WithDefaultDates() {
        // Given
        Long userId = 1L;
        
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));

        // When
        List<DailyActivityStatsDto> result = statisticsService.getDailyActivityStatistics(userId, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(8); // 기본 7일 + 1
    }

    @Test
    @DisplayName("단일 날짜 일일 활동 통계")
    void getDailyActivityStatistics_SingleDay_Success() {
        // Given
        Long userId = 1L;
        LocalDate date = LocalDate.now();
        
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));

        // When
        DailyActivityStatsDto result = statisticsService.getDailyActivityStatistics(userId, date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId).isEqualTo(userId);
        assertThat(result.date).isEqualTo(date);
    }

    @Test
    @DisplayName("단일 날짜 일일 활동 통계 - 데이터 없음")
    void getDailyActivityStatistics_SingleDay_NoData() {
        // Given
        Long userId = 1L;
        LocalDate date = LocalDate.now().minusDays(100);
        
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        DailyActivityStatsDto result = statisticsService.getDailyActivityStatistics(userId, date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalLocations).isZero();
        assertThat(result.totalDistanceKm).isZero();
    }

    @Test
    @DisplayName("안전도 통계 조회 - 정상 케이스")
    void getSafetyStatistics_Success() {
        // Given
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        when(geofenceRepository.findByUserUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testEmergency));

        // When
        SafetyStatsDto result = statisticsService.getSafetyStatistics(userId, startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId).isEqualTo(userId);
        assertThat(result.startDate).isEqualTo(startDate);
        assertThat(result.endDate).isEqualTo(endDate);
        assertThat(result.totalSosAlerts).isEqualTo(1);
        assertThat(result.resolvedSosAlerts).isEqualTo(1);
        assertThat(result.safetyScore).isGreaterThanOrEqualTo(0);
        assertThat(result.dailyStats).isNotEmpty();
        assertThat(result.recentIncidents).hasSize(1);
    }

    @Test
    @DisplayName("안전도 통계 조회 - 기본값 설정")
    void getSafetyStatistics_WithDefaultDates() {
        // Given
        Long userId = 1L;
        
        when(geofenceRepository.findByUserUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        SafetyStatsDto result = statisticsService.getSafetyStatistics(userId, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.startDate).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(result.endDate).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("안전도 통계 - 다양한 응급상황 타입")
    void getSafetyStatistics_WithDifferentEmergencyTypes() {
        // Given
        Long userId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        
        Emergency geofenceEmergency = Emergency.builder()
            .id(2L)
            .user(testUser)
            .type(Emergency.EmergencyType.GEOFENCE_EXIT)
            .severity(Emergency.EmergencySeverity.MEDIUM)
            .status(Emergency.EmergencyStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
        
        when(geofenceRepository.findByUserUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testLocationHistory));
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(testEmergency, geofenceEmergency));

        // When
        SafetyStatsDto result = statisticsService.getSafetyStatistics(userId, startDate, endDate);

        // Then
        assertThat(result.totalSosAlerts).isEqualTo(2);
        assertThat(result.resolvedSosAlerts).isEqualTo(1);
        assertThat(result.pendingSosAlerts).isEqualTo(1);
        assertThat(result.geofenceViolations).isEqualTo(1);
    }

    @Test
    @DisplayName("지오펜스 내부 위치 테스트")
    void testLocationInsideGeofence() {
        // Given - 지오펜스 중심 좌표와 동일한 위치
        LocationHistory centerLocation = LocationHistory.builder()
            .id(2L)
            .user(testUser)
            .latitude(BigDecimal.valueOf(37.5665))
            .longitude(BigDecimal.valueOf(126.9780))
            .capturedAt(LocalDateTime.now())
            .build();
        
        when(geofenceRepository.findByUserUserIdAndIsActive(1L, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(centerLocation));
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        SafetyStatsDto result = statisticsService.getSafetyStatistics(1L, 
            LocalDate.now().minusDays(1), LocalDate.now());

        // Then - 중심점에 있으므로 안전점수가 높아야 함
        assertThat(result.safetyScore).isEqualTo(100);
    }

    @Test
    @DisplayName("빈 위치 데이터로 안전점수 계산")
    void testSafetyScoreWithEmptyLocations() {
        // Given
        Long userId = 1L;
        
        when(geofenceRepository.findByUserUserIdAndIsActive(userId, true))
            .thenReturn(Arrays.asList(testGeofence));
        when(locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());
        when(emergencyRepository.findByUserIdAndCreatedAtBetween(
            eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        SafetyStatsDto result = statisticsService.getSafetyStatistics(userId, 
            LocalDate.now().minusDays(1), LocalDate.now());

        // Then - 위치 데이터가 없으면 안전점수 100
        assertThat(result.safetyScore).isEqualTo(100);
    }

    @Test
    @DisplayName("내부 클래스 - GeofenceStats Builder 테스트")
    void testGeofenceStatsBuilder() {
        // When
        StatisticsService.GeofenceStats stats = StatisticsService.GeofenceStats.builder()
            .geofenceId(1L)
            .geofenceName("테스트")
            .entriesCount(5)
            .exitsCount(3)
            .averageStayMinutes(120L)
            .lastVisit(LocalDateTime.now())
            .build();

        // Then
        assertThat(stats.getGeofenceId()).isEqualTo(1L);
        assertThat(stats.getGeofenceName()).isEqualTo("테스트");
        assertThat(stats.getEntriesCount()).isEqualTo(5);
        assertThat(stats.getExitsCount()).isEqualTo(3);
        assertThat(stats.getAverageStayMinutes()).isEqualTo(120L);
        assertThat(stats.getLastVisit()).isNotNull();
    }

    @Test
    @DisplayName("내부 클래스 - DailyStats Builder 테스트")
    void testDailyStatsBuilder() {
        // When
        StatisticsService.DailyStats stats = StatisticsService.DailyStats.builder()
            .date(LocalDate.now())
            .locationCount(10)
            .uniqueGeofences(3)
            .totalDistanceKm(5.5)
            .activeHours(8)
            .build();

        // Then
        assertThat(stats.getDate()).isEqualTo(LocalDate.now());
        assertThat(stats.getLocationCount()).isEqualTo(10);
        assertThat(stats.getUniqueGeofences()).isEqualTo(3);
        assertThat(stats.getTotalDistanceKm()).isEqualTo(5.5);
        assertThat(stats.getActiveHours()).isEqualTo(8);
    }

    @Test
    @DisplayName("내부 클래스 - GeofenceStatistics getters/setters 테스트")
    void testGeofenceStatisticsGettersSetters() {
        // Given
        StatisticsService.GeofenceStatistics stats = new StatisticsService.GeofenceStatistics();
        
        // When
        stats.setPeriodDays(30);
        stats.setStartDate(LocalDate.now().minusDays(30));
        stats.setEndDate(LocalDate.now());
        stats.setTotalLocations(100);
        stats.setTotalEntries(20);
        stats.setTotalExits(18);
        stats.setMostVisitedGeofence("홈");
        stats.setAverageStayMinutes(120);

        // Then
        assertThat(stats.getPeriodDays()).isEqualTo(30);
        assertThat(stats.getStartDate()).isEqualTo(LocalDate.now().minusDays(30));
        assertThat(stats.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(stats.getTotalLocations()).isEqualTo(100);
        assertThat(stats.getTotalEntries()).isEqualTo(20);
        assertThat(stats.getTotalExits()).isEqualTo(18);
        assertThat(stats.getMostVisitedGeofence()).isEqualTo("홈");
        assertThat(stats.getAverageStayMinutes()).isEqualTo(120);
    }

    @Test
    @DisplayName("내부 클래스 - DailyActivityStatistics getters/setters 테스트")
    void testDailyActivityStatisticsGettersSetters() {
        // Given
        StatisticsService.DailyActivityStatistics stats = new StatisticsService.DailyActivityStatistics();
        
        // When
        stats.setPeriodDays(7);
        stats.setAverageDailyLocations(15.5);

        // Then
        assertThat(stats.getPeriodDays()).isEqualTo(7);
        assertThat(stats.getAverageDailyLocations()).isEqualTo(15.5);
    }

    @Test
    @DisplayName("내부 클래스 - SafetyStatistics getters/setters 테스트")
    void testSafetyStatisticsGettersSetters() {
        // Given
        StatisticsService.SafetyStatistics stats = new StatisticsService.SafetyStatistics();
        
        // When
        stats.setPeriodDays(30);
        stats.setTimeInSafeZonesMinutes(1200);
        stats.setTimeOutsideSafeZonesMinutes(300);
        stats.setSafetyPercentage(80.0);
        stats.setEmergencyAlertsCount(2);

        // Then
        assertThat(stats.getPeriodDays()).isEqualTo(30);
        assertThat(stats.getTimeInSafeZonesMinutes()).isEqualTo(1200);
        assertThat(stats.getTimeOutsideSafeZonesMinutes()).isEqualTo(300);
        assertThat(stats.getSafetyPercentage()).isEqualTo(80.0);
        assertThat(stats.getEmergencyAlertsCount()).isEqualTo(2);
    }
}