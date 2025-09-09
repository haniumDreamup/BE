package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceRequest;
import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceResponse;
import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GeofenceRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * GeofenceService 통합 테스트
 * 실제 Repository와 함께 동작하는 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(GeofenceService.class)
@DisplayName("GeofenceService 통합 테스트")
class GeofenceServiceIntegrationTest {

    @Autowired
    private GeofenceService geofenceService;

    @Autowired
    private GeofenceRepository geofenceRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private GeofenceRequest testGeofenceRequest;

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

        // 테스트 지오펜스 요청 데이터 생성
        testGeofenceRequest = GeofenceRequest.builder()
                .name("테스트 안전 구역")
                .description("테스트용 안전 구역입니다")
                .centerLatitude(37.5665)
                .centerLongitude(126.9780)
                .radiusMeters(500)
                .address("서울시 중구 세종대로")
                .type(Geofence.GeofenceType.HOME)
                .isActive(true)
                .alertOnEntry(false)
                .alertOnExit(true)
                .priority(1)
                .build();
    }

    @Test
    @DisplayName("지오펜스 생성 - 성공")
    void createGeofence_Success() {
        // When
        GeofenceResponse response = geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("테스트 안전 구역");
        assertThat(response.getDescription()).isEqualTo("테스트용 안전 구역입니다");
        assertThat(response.getCenterLatitude()).isEqualTo(37.5665);
        assertThat(response.getCenterLongitude()).isEqualTo(126.9780);
        assertThat(response.getRadiusMeters()).isEqualTo(500);
        assertThat(response.getType()).isEqualTo(Geofence.GeofenceType.HOME);
        assertThat(response.getIsActive()).isTrue();

        // 데이터베이스에서 실제 저장 확인
        Geofence savedGeofence = geofenceRepository.findById(response.getId()).orElse(null);
        assertThat(savedGeofence).isNotNull();
        assertThat(savedGeofence.getName()).isEqualTo("테스트 안전 구역");
    }

    @Test
    @DisplayName("지오펜스 조회 - 성공")
    void getGeofence_Success() {
        // Given - 지오펜스 먼저 생성
        GeofenceResponse created = geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);

        // When
        GeofenceResponse response = geofenceService.getGeofence(testUser.getUserId(), created.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(created.getId());
        assertThat(response.getName()).isEqualTo("테스트 안전 구역");
        assertThat(response.getType()).isEqualTo(Geofence.GeofenceType.HOME);
    }

    @Test
    @DisplayName("사용자의 모든 지오펜스 조회")
    void getUserGeofences_Success() {
        // Given - 여러 지오펜스 생성
        geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);
        
        GeofenceRequest request2 = GeofenceRequest.builder()
                .name("직장 안전 구역")
                .description("직장 구역")
                .centerLatitude(37.5000)
                .centerLongitude(127.0000)
                .radiusMeters(300)
                .address("서울시 강남구")
                .type(Geofence.GeofenceType.WORK)
                .isActive(true)
                .priority(2)
                .build();
        geofenceService.createGeofence(testUser.getUserId(), request2);

        // When
        List<GeofenceResponse> geofences = geofenceService.getUserGeofences(testUser.getUserId());

        // Then
        assertThat(geofences).hasSize(2);
        assertThat(geofences).extracting("name")
            .containsExactlyInAnyOrder("테스트 안전 구역", "직장 안전 구역");
    }

    @Test
    @DisplayName("활성화된 지오펜스만 조회")
    void getActiveGeofences_Success() {
        // Given - 활성/비활성 지오펜스 생성
        geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);
        
        GeofenceRequest inactiveRequest = GeofenceRequest.builder()
                .name("비활성 구역")
                .description("비활성 구역")
                .centerLatitude(37.4000)
                .centerLongitude(127.1000)
                .radiusMeters(200)
                .type(Geofence.GeofenceType.CUSTOM)
                .isActive(false) // 비활성
                .priority(1)
                .build();
        geofenceService.createGeofence(testUser.getUserId(), inactiveRequest);

        // When
        List<GeofenceResponse> activeGeofences = geofenceService.getActiveGeofences(testUser.getUserId());

        // Then
        assertThat(activeGeofences).hasSize(1);
        assertThat(activeGeofences.get(0).getName()).isEqualTo("테스트 안전 구역");
        assertThat(activeGeofences.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("지오펜스 수정 - 성공")
    void updateGeofence_Success() {
        // Given - 지오펜스 생성
        GeofenceResponse created = geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);

        // When - 수정 요청
        GeofenceRequest updateRequest = GeofenceRequest.builder()
                .name("수정된 안전 구역")
                .description("수정된 설명")
                .centerLatitude(37.5700)
                .centerLongitude(126.9800)
                .radiusMeters(600)
                .address("수정된 주소")
                .type(Geofence.GeofenceType.HOSPITAL)
                .isActive(false)
                .alertOnEntry(true)
                .alertOnExit(false)
                .priority(3)
                .build();

        GeofenceResponse updated = geofenceService.updateGeofence(
            testUser.getUserId(), created.getId(), updateRequest);

        // Then
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("수정된 안전 구역");
        assertThat(updated.getDescription()).isEqualTo("수정된 설명");
        assertThat(updated.getRadiusMeters()).isEqualTo(600);
        assertThat(updated.getType()).isEqualTo(Geofence.GeofenceType.HOSPITAL);
        assertThat(updated.getIsActive()).isFalse();

        // 데이터베이스에서 실제 수정 확인
        Geofence savedGeofence = geofenceRepository.findById(created.getId()).orElse(null);
        assertThat(savedGeofence).isNotNull();
        assertThat(savedGeofence.getName()).isEqualTo("수정된 안전 구역");
    }

    @Test
    @DisplayName("지오펜스 삭제 - 성공")
    void deleteGeofence_Success() {
        // Given - 지오펜스 생성
        GeofenceResponse created = geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);

        // When
        geofenceService.deleteGeofence(testUser.getUserId(), created.getId());

        // Then - 데이터베이스에서 삭제 확인
        boolean exists = geofenceRepository.existsById(created.getId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("지오펜스 활성화/비활성화 토글")
    void toggleGeofenceActive_Success() {
        // Given - 활성 지오펜스 생성
        GeofenceResponse created = geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest);
        assertThat(created.getIsActive()).isTrue();

        // When - 비활성화로 토글
        GeofenceResponse toggled = geofenceService.toggleGeofenceActive(testUser.getUserId(), created.getId());

        // Then
        assertThat(toggled.getIsActive()).isFalse();

        // When - 다시 활성화로 토글
        GeofenceResponse toggledAgain = geofenceService.toggleGeofenceActive(testUser.getUserId(), created.getId());

        // Then
        assertThat(toggledAgain.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("타입별 지오펜스 조회")
    void getGeofencesByType_Success() {
        // Given - 다양한 타입의 지오펜스 생성
        geofenceService.createGeofence(testUser.getUserId(), testGeofenceRequest); // HOME

        GeofenceRequest workRequest = GeofenceRequest.builder()
                .name("직장")
                .centerLatitude(37.5000)
                .centerLongitude(127.0000)
                .radiusMeters(300)
                .type(Geofence.GeofenceType.WORK)
                .build();
        geofenceService.createGeofence(testUser.getUserId(), workRequest);

        // When
        List<GeofenceResponse> homeGeofences = geofenceService.getGeofencesByType(
            testUser.getUserId(), Geofence.GeofenceType.HOME);
        List<GeofenceResponse> workGeofences = geofenceService.getGeofencesByType(
            testUser.getUserId(), Geofence.GeofenceType.WORK);

        // Then
        assertThat(homeGeofences).hasSize(1);
        assertThat(homeGeofences.get(0).getType()).isEqualTo(Geofence.GeofenceType.HOME);
        
        assertThat(workGeofences).hasSize(1);
        assertThat(workGeofences.get(0).getType()).isEqualTo(Geofence.GeofenceType.WORK);
    }

    @Test
    @DisplayName("지오펜스 페이징 조회")
    void getUserGeofencesPaged_Success() {
        // Given - 여러 지오펜스 생성
        for (int i = 1; i <= 5; i++) {
            GeofenceRequest request = GeofenceRequest.builder()
                    .name("구역 " + i)
                    .centerLatitude(37.500 + i)
                    .centerLongitude(127.000 + i)
                    .radiusMeters(100 * i)
                    .type(Geofence.GeofenceType.CUSTOM)
                    .build();
            geofenceService.createGeofence(testUser.getUserId(), request);
        }

        // When
        Page<GeofenceResponse> page = geofenceService.getUserGeofencesPaged(
            testUser.getUserId(), PageRequest.of(0, 3));

        // Then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasNext()).isTrue();
    }
}