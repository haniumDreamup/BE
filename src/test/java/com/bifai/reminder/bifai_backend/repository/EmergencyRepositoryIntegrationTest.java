package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Emergency;
import com.bifai.reminder.bifai_backend.entity.Emergency.*;
import com.bifai.reminder.bifai_backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * EmergencyRepository 통합 테스트
 * 실제 H2 데이터베이스와 함께 동작하는 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("EmergencyRepository 통합 테스트")
class EmergencyRepositoryIntegrationTest {

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;
    private Emergency activeEmergency;
    private Emergency resolvedEmergency;

    @BeforeEach
    void setUp() {
        // 테스트 사용자들 생성
        testUser1 = User.builder()
                .username("testuser1")
                .email("test1@example.com")
                .name("테스트 사용자1")
                .cognitiveLevel(User.CognitiveLevel.MILD)
                .isActive(true)
                .build();
        testUser1 = userRepository.save(testUser1);

        testUser2 = User.builder()
                .username("testuser2")
                .email("test2@example.com")
                .name("테스트 사용자2")
                .cognitiveLevel(User.CognitiveLevel.MODERATE)
                .isActive(true)
                .build();
        testUser2 = userRepository.save(testUser2);

        // 활성 긴급 상황 생성
        activeEmergency = Emergency.builder()
                .user(testUser1)
                .type(EmergencyType.FALL_DETECTION)
                .status(EmergencyStatus.ACTIVE)
                .latitude(37.5665)
                .longitude(126.9780)
                .address("서울시 중구")
                .description("낙상 감지됨")
                .severity(EmergencySeverity.HIGH)
                .triggeredBy(TriggerSource.AI_DETECTION)
                .fallConfidence(85.5)
                .imageUrl("/images/fall_detected.jpg")
                .build();
        activeEmergency = emergencyRepository.save(activeEmergency);

        // 해결된 긴급 상황 생성
        resolvedEmergency = Emergency.builder()
                .user(testUser2)
                .type(EmergencyType.MANUAL_ALERT)
                .status(EmergencyStatus.RESOLVED)
                .latitude(37.5000)
                .longitude(127.0000)
                .address("서울시 강남구")
                .description("수동 호출")
                .severity(EmergencySeverity.MEDIUM)
                .triggeredBy(TriggerSource.USER)
                .resolvedAt(LocalDateTime.now().minusHours(1))
                .resolvedBy("보호자1")
                .resolutionNotes("문제 해결됨")
                .responseTimeSeconds(1800) // 30분
                .build();
        resolvedEmergency = emergencyRepository.save(resolvedEmergency);
    }

    @Test
    @DisplayName("사용자 ID로 긴급상황 조회 - 생성시간 역순")
    void findByUserIdOrderByCreatedAtDesc_Success() {
        // Given - 같은 사용자의 추가 긴급상황 생성
        Emergency additionalEmergency = Emergency.builder()
                .user(testUser1)
                .type(EmergencyType.GEOFENCE_EXIT)
                .status(EmergencyStatus.NOTIFIED)
                .severity(EmergencySeverity.LOW)
                .triggeredBy(TriggerSource.SYSTEM)
                .build();
        emergencyRepository.save(additionalEmergency);

        // When
        Page<Emergency> result = emergencyRepository.findByUserIdOrderByCreatedAtDesc(
                testUser1.getUserId(), PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(result.getContent().get(1).getCreatedAt());
        assertThat(result.getContent()).extracting("type")
                .containsExactly(EmergencyType.GEOFENCE_EXIT, EmergencyType.FALL_DETECTION);
    }

    @Test
    @DisplayName("활성 긴급상황 조회")
    void findActiveEmergencies_Success() {
        // Given
        List<EmergencyStatus> activeStatuses = Arrays.asList(
                EmergencyStatus.ACTIVE, 
                EmergencyStatus.NOTIFIED
        );

        // When
        List<Emergency> activeEmergencies = emergencyRepository.findActiveEmergencies(activeStatuses);

        // Then
        assertThat(activeEmergencies).hasSize(1);
        assertThat(activeEmergencies.get(0).getStatus()).isEqualTo(EmergencyStatus.ACTIVE);
        assertThat(activeEmergencies.get(0).getUser().getUserId()).isEqualTo(testUser1.getUserId());
    }

    @Test
    @DisplayName("사용자별 특정 상태의 긴급상황 조회")
    void findByUserIdAndStatus_Success() {
        // When
        List<Emergency> resolvedEmergencies = emergencyRepository.findByUserIdAndStatus(
                testUser2.getUserId(), EmergencyStatus.RESOLVED);

        // Then
        assertThat(resolvedEmergencies).hasSize(1);
        assertThat(resolvedEmergencies.get(0).getStatus()).isEqualTo(EmergencyStatus.RESOLVED);
        assertThat(resolvedEmergencies.get(0).getResolvedBy()).isEqualTo("보호자1");
    }

    @Test
    @DisplayName("사용자의 최근 긴급상황 조회")
    void findFirstByUserIdOrderByCreatedAtDesc_Success() {
        // Given - 더 최근 긴급상황 추가
        Emergency recentEmergency = Emergency.builder()
                .user(testUser1)
                .type(EmergencyType.PANIC_BUTTON)
                .status(EmergencyStatus.ACTIVE)
                .severity(EmergencySeverity.CRITICAL)
                .triggeredBy(TriggerSource.USER)
                .build();
        emergencyRepository.save(recentEmergency);

        // When
        Optional<Emergency> result = emergencyRepository.findFirstByUserIdOrderByCreatedAtDesc(testUser1.getUserId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(EmergencyType.PANIC_BUTTON);
        assertThat(result.get().getSeverity()).isEqualTo(EmergencySeverity.CRITICAL);
    }

    @Test
    @DisplayName("기간별 긴급상황 조회")
    void findByUserIdAndCreatedAtBetween_Success() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        // When
        List<Emergency> emergencies = emergencyRepository.findByUserIdAndCreatedAtBetween(
                testUser1.getUserId(), startTime, endTime);

        // Then
        assertThat(emergencies).hasSize(1);
        assertThat(emergencies.get(0).getType()).isEqualTo(EmergencyType.FALL_DETECTION);
    }

    @Test
    @DisplayName("긴급상황 통계 조회")
    void getEmergencyStatsByUser_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);

        // When
        List<Object[]> stats = emergencyRepository.getEmergencyStatsByUser(
                testUser1.getUserId(), startDate);

        // Then
        assertThat(stats).hasSize(1);
        assertThat(stats.get(0)[0]).isEqualTo(EmergencyType.FALL_DETECTION);
        assertThat(stats.get(0)[1]).isEqualTo(1L);
    }

    @Test
    @DisplayName("위치 기반 긴급상황 조회")
    void findNearbyEmergencies_Success() {
        // Given - 활성 상태 긴급상황으로 변경
        activeEmergency.setStatus(EmergencyStatus.ACTIVE);
        emergencyRepository.save(activeEmergency);

        // When - 1km 반경 내 활성 긴급상황 조회
        List<Emergency> nearbyEmergencies = emergencyRepository.findNearbyEmergencies(
                37.5665, 126.9780, 1.0, EmergencyStatus.ACTIVE);

        // Then
        assertThat(nearbyEmergencies).hasSize(1);
        assertThat(nearbyEmergencies.get(0).getAddress()).isEqualTo("서울시 중구");
    }

    @Test
    @DisplayName("긴급상황 ID로 단일 조회")
    void findById_Success() {
        // When
        Optional<Emergency> result = emergencyRepository.findById(activeEmergency.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getLatitude()).isEqualTo(37.5665);
        assertThat(result.get().getLongitude()).isEqualTo(126.9780);
        assertThat(result.get().getAddress()).isEqualTo("서울시 중구");
    }

    @Test
    @DisplayName("긴급상황 수정")
    void updateEmergency_Success() {
        // Given
        Emergency emergency = emergencyRepository.findById(activeEmergency.getId()).orElseThrow();

        // When
        emergency.setStatus(EmergencyStatus.RESOLVED);
        emergency.setResolvedAt(LocalDateTime.now());
        emergency.setResolvedBy("시스템");
        emergency.setResolutionNotes("자동 해결됨");
        emergency.setResponseTimeSeconds(600);
        Emergency updated = emergencyRepository.save(emergency);

        // Then
        assertThat(updated.getStatus()).isEqualTo(EmergencyStatus.RESOLVED);
        assertThat(updated.getResolvedBy()).isEqualTo("시스템");
        assertThat(updated.getResolutionNotes()).isEqualTo("자동 해결됨");
        assertThat(updated.getResponseTimeSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("긴급상황 삭제")
    void deleteEmergency_Success() {
        // Given
        Long emergencyId = activeEmergency.getId();

        // When
        emergencyRepository.deleteById(emergencyId);

        // Then
        Optional<Emergency> result = emergencyRepository.findById(emergencyId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자의 긴급상황 개수 조회")
    void countByUser_Success() {
        // When
        List<Emergency> emergencies = emergencyRepository.findByUserIdAndStatus(
                testUser1.getUserId(), EmergencyStatus.ACTIVE);

        // Then
        assertThat(emergencies).hasSize(1);
        assertThat(emergencies.get(0).getUser().getUserId()).isEqualTo(testUser1.getUserId());
    }
}