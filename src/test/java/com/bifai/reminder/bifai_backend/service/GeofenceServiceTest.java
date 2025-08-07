package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceRequest;
import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceResponse;
import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.GeofenceRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 지오펜스 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeofenceService 테스트")
class GeofenceServiceTest {

  @InjectMocks
  private GeofenceService geofenceService;

  @Mock
  private GeofenceRepository geofenceRepository;

  @Mock
  private UserRepository userRepository;

  private User testUser;
  private Geofence testGeofence;
  private GeofenceRequest testRequest;

  @BeforeEach
  void setUp() {
    testUser = User.builder()
        .userId(1L)
        .username("testuser")
        .email("test@example.com")
        .build();

    testGeofence = Geofence.builder()
        .id(1L)
        .user(testUser)
        .name("우리집")
        .description("안전한 집")
        .centerLatitude(37.5665)
        .centerLongitude(126.9780)
        .radiusMeters(100)
        .type(Geofence.GeofenceType.HOME)
        .isActive(true)
        .alertOnExit(true)
        .build();

    testRequest = GeofenceRequest.builder()
        .name("우리집")
        .description("안전한 집")
        .centerLatitude(37.5665)
        .centerLongitude(126.9780)
        .radiusMeters(100)
        .type(Geofence.GeofenceType.HOME)
        .isActive(true)
        .alertOnExit(true)
        .build();
  }

  @Test
  @DisplayName("지오펜스 생성 성공")
  void createGeofence_Success() {
    // Given
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.countByUser(testUser)).willReturn(0L);
    given(geofenceRepository.existsByUserAndName(testUser, "우리집")).willReturn(false);
    given(geofenceRepository.save(any(Geofence.class))).willReturn(testGeofence);

    // When
    GeofenceResponse response = geofenceService.createGeofence(1L, testRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getName()).isEqualTo("우리집");
    assertThat(response.getCenterLatitude()).isEqualTo(37.5665);
    assertThat(response.getCenterLongitude()).isEqualTo(126.9780);
    assertThat(response.getRadiusMeters()).isEqualTo(100);
    
    verify(geofenceRepository).save(any(Geofence.class));
  }

  @Test
  @DisplayName("지오펜스 생성 실패 - 최대 개수 초과")
  void createGeofence_Failure_MaxCount() {
    // Given
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.countByUser(testUser)).willReturn(10L);

    // When & Then
    assertThatThrownBy(() -> geofenceService.createGeofence(1L, testRequest))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("안전 구역은 최대 10개까지만");
  }

  @Test
  @DisplayName("지오펜스 생성 실패 - 중복 이름")
  void createGeofence_Failure_DuplicateName() {
    // Given
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.countByUser(testUser)).willReturn(0L);
    given(geofenceRepository.existsByUserAndName(testUser, "우리집")).willReturn(true);

    // When & Then
    assertThatThrownBy(() -> geofenceService.createGeofence(1L, testRequest))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("같은 이름의 안전 구역이 이미 있어요");
  }

  @Test
  @DisplayName("지오펜스 수정 성공")
  void updateGeofence_Success() {
    // Given
    GeofenceRequest updateRequest = GeofenceRequest.builder()
        .name("회사")
        .description("직장")
        .centerLatitude(37.5172)
        .centerLongitude(127.0473)
        .radiusMeters(200)
        .type(Geofence.GeofenceType.WORK)
        .build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(testGeofence));
    given(geofenceRepository.existsByUserAndName(testUser, "회사")).willReturn(false);
    given(geofenceRepository.save(any(Geofence.class))).willReturn(testGeofence);

    // When
    GeofenceResponse response = geofenceService.updateGeofence(1L, 1L, updateRequest);

    // Then
    assertThat(response).isNotNull();
    verify(geofenceRepository).save(testGeofence);
  }

  @Test
  @DisplayName("지오펜스 삭제 성공")
  void deleteGeofence_Success() {
    // Given
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(testGeofence));

    // When
    geofenceService.deleteGeofence(1L, 1L);

    // Then
    verify(geofenceRepository).delete(testGeofence);
  }

  @Test
  @DisplayName("사용자 지오펜스 목록 조회")
  void getUserGeofences_Success() {
    // Given
    List<Geofence> geofences = Arrays.asList(testGeofence);
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.findByUserOrderByPriorityDescCreatedAtDesc(testUser))
        .willReturn(geofences);

    // When
    List<GeofenceResponse> responses = geofenceService.getUserGeofences(1L);

    // Then
    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).getName()).isEqualTo("우리집");
  }

  @Test
  @DisplayName("활성화된 지오펜스만 조회")
  void getActiveGeofences_Success() {
    // Given
    List<Geofence> activeGeofences = Arrays.asList(testGeofence);
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.findByUserAndIsActiveTrue(testUser))
        .willReturn(activeGeofences);

    // When
    List<GeofenceResponse> responses = geofenceService.getActiveGeofences(1L);

    // Then
    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).getIsActive()).isTrue();
  }

  @Test
  @DisplayName("지오펜스 활성화/비활성화 토글")
  void toggleGeofenceActive_Success() {
    // Given
    testGeofence.setIsActive(true);
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(testGeofence));
    given(geofenceRepository.save(testGeofence)).willReturn(testGeofence);

    // When
    GeofenceResponse response = geofenceService.toggleGeofenceActive(1L, 1L);

    // Then
    assertThat(testGeofence.getIsActive()).isFalse();
    verify(geofenceRepository).save(testGeofence);
  }

  @Test
  @DisplayName("지오펜스 조회 실패 - 존재하지 않는 지오펜스")
  void getGeofence_NotFound() {
    // Given
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(geofenceRepository.findByIdAndUser(999L, testUser)).willReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> geofenceService.getGeofence(1L, 999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("안전 구역을 찾을 수 없습니다");
  }
}