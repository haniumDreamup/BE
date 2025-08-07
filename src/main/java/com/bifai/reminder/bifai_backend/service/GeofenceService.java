package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceRequest;
import com.bifai.reminder.bifai_backend.dto.geofence.GeofenceResponse;
import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.GeofenceRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 지오펜스 관리 서비스
 * 안전 구역 CRUD 및 관리 기능
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GeofenceService {

  private final GeofenceRepository geofenceRepository;
  private final UserRepository userRepository;
  
  private static final int MAX_GEOFENCES_PER_USER = 10;
  
  /**
   * 지오펜스 생성
   */
  public GeofenceResponse createGeofence(Long userId, GeofenceRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    // 최대 개수 체크
    long currentCount = geofenceRepository.countByUser(user);
    if (currentCount >= MAX_GEOFENCES_PER_USER) {
      throw new IllegalStateException(
          String.format("안전 구역은 최대 %d개까지만 만들 수 있어요.", MAX_GEOFENCES_PER_USER)
      );
    }
    
    // 중복 이름 체크
    if (geofenceRepository.existsByUserAndName(user, request.getName())) {
      throw new IllegalArgumentException("같은 이름의 안전 구역이 이미 있어요.");
    }
    
    Geofence geofence = Geofence.builder()
        .user(user)
        .name(request.getName())
        .description(request.getDescription())
        .centerLatitude(request.getCenterLatitude())
        .centerLongitude(request.getCenterLongitude())
        .radiusMeters(request.getRadiusMeters())
        .address(request.getAddress())
        .type(request.getType() != null ? request.getType() : Geofence.GeofenceType.CUSTOM)
        .isActive(request.getIsActive() != null ? request.getIsActive() : true)
        .alertOnEntry(request.getAlertOnEntry() != null ? request.getAlertOnEntry() : false)
        .alertOnExit(request.getAlertOnExit() != null ? request.getAlertOnExit() : true)
        .startTime(request.getStartTime() != null ? LocalTime.parse(request.getStartTime()) : null)
        .endTime(request.getEndTime() != null ? LocalTime.parse(request.getEndTime()) : null)
        .activeDays(request.getActiveDays())
        .priority(request.getPriority() != null ? request.getPriority() : 1)
        .createdBy(user)
        .build();
    
    geofence = geofenceRepository.save(geofence);
    
    log.info("Geofence created: {} for user {}", geofence.getName(), userId);
    
    return convertToResponse(geofence);
  }
  
  /**
   * 지오펜스 수정
   */
  public GeofenceResponse updateGeofence(Long userId, Long geofenceId, GeofenceRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    Geofence geofence = geofenceRepository.findByIdAndUser(geofenceId, user)
        .orElseThrow(() -> new ResourceNotFoundException("안전 구역을 찾을 수 없습니다."));
    
    // 이름 변경 시 중복 체크
    if (!geofence.getName().equals(request.getName()) &&
        geofenceRepository.existsByUserAndName(user, request.getName())) {
      throw new IllegalArgumentException("같은 이름의 안전 구역이 이미 있어요.");
    }
    
    // 업데이트
    geofence.setName(request.getName());
    geofence.setDescription(request.getDescription());
    geofence.setCenterLatitude(request.getCenterLatitude());
    geofence.setCenterLongitude(request.getCenterLongitude());
    geofence.setRadiusMeters(request.getRadiusMeters());
    geofence.setAddress(request.getAddress());
    
    if (request.getType() != null) {
      geofence.setType(request.getType());
    }
    if (request.getIsActive() != null) {
      geofence.setIsActive(request.getIsActive());
    }
    if (request.getAlertOnEntry() != null) {
      geofence.setAlertOnEntry(request.getAlertOnEntry());
    }
    if (request.getAlertOnExit() != null) {
      geofence.setAlertOnExit(request.getAlertOnExit());
    }
    if (request.getStartTime() != null) {
      geofence.setStartTime(LocalTime.parse(request.getStartTime()));
    }
    if (request.getEndTime() != null) {
      geofence.setEndTime(LocalTime.parse(request.getEndTime()));
    }
    if (request.getActiveDays() != null) {
      geofence.setActiveDays(request.getActiveDays());
    }
    if (request.getPriority() != null) {
      geofence.setPriority(request.getPriority());
    }
    
    geofence = geofenceRepository.save(geofence);
    
    log.info("Geofence updated: {} for user {}", geofence.getName(), userId);
    
    return convertToResponse(geofence);
  }
  
  /**
   * 지오펜스 삭제
   */
  public void deleteGeofence(Long userId, Long geofenceId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    Geofence geofence = geofenceRepository.findByIdAndUser(geofenceId, user)
        .orElseThrow(() -> new ResourceNotFoundException("안전 구역을 찾을 수 없습니다."));
    
    geofenceRepository.delete(geofence);
    
    log.info("Geofence deleted: {} for user {}", geofence.getName(), userId);
  }
  
  /**
   * 지오펜스 단일 조회
   */
  @Transactional(readOnly = true)
  public GeofenceResponse getGeofence(Long userId, Long geofenceId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    Geofence geofence = geofenceRepository.findByIdAndUser(geofenceId, user)
        .orElseThrow(() -> new ResourceNotFoundException("안전 구역을 찾을 수 없습니다."));
    
    return convertToResponse(geofence);
  }
  
  /**
   * 사용자의 모든 지오펜스 조회
   */
  @Transactional(readOnly = true)
  public List<GeofenceResponse> getUserGeofences(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<Geofence> geofences = geofenceRepository
        .findByUserOrderByPriorityDescCreatedAtDesc(user);
    
    return geofences.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }
  
  /**
   * 사용자의 지오펜스 페이징 조회
   */
  @Transactional(readOnly = true)
  public Page<GeofenceResponse> getUserGeofencesPaged(Long userId, Pageable pageable) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    Page<Geofence> geofences = geofenceRepository.findByUser(user, pageable);
    
    return geofences.map(this::convertToResponse);
  }
  
  /**
   * 활성화된 지오펜스만 조회
   */
  @Transactional(readOnly = true)
  public List<GeofenceResponse> getActiveGeofences(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<Geofence> activeGeofences = geofenceRepository.findByUserAndIsActiveTrue(user);
    
    return activeGeofences.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }
  
  /**
   * 타입별 지오펜스 조회
   */
  @Transactional(readOnly = true)
  public List<GeofenceResponse> getGeofencesByType(Long userId, Geofence.GeofenceType type) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    List<Geofence> geofences = geofenceRepository.findByUserAndType(user, type);
    
    return geofences.stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }
  
  /**
   * 지오펜스 활성화/비활성화 토글
   */
  public GeofenceResponse toggleGeofenceActive(Long userId, Long geofenceId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    Geofence geofence = geofenceRepository.findByIdAndUser(geofenceId, user)
        .orElseThrow(() -> new ResourceNotFoundException("안전 구역을 찾을 수 없습니다."));
    
    geofence.setIsActive(!geofence.getIsActive());
    geofence = geofenceRepository.save(geofence);
    
    log.info("Geofence {} toggled to {}", geofence.getName(), 
            geofence.getIsActive() ? "active" : "inactive");
    
    return convertToResponse(geofence);
  }
  
  /**
   * 지오펜스 우선순위 변경
   */
  public void updateGeofencePriorities(Long userId, List<Long> geofenceIds) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    
    int priority = geofenceIds.size();
    for (Long geofenceId : geofenceIds) {
      Geofence geofence = geofenceRepository.findByIdAndUser(geofenceId, user)
          .orElseThrow(() -> new ResourceNotFoundException("안전 구역을 찾을 수 없습니다: " + geofenceId));
      
      geofence.setPriority(priority--);
      geofenceRepository.save(geofence);
    }
    
    log.info("Updated priorities for {} geofences for user {}", geofenceIds.size(), userId);
  }
  
  /**
   * Entity를 Response DTO로 변환
   */
  private GeofenceResponse convertToResponse(Geofence geofence) {
    return GeofenceResponse.builder()
        .id(geofence.getId())
        .name(geofence.getName())
        .description(geofence.getDescription())
        .centerLatitude(geofence.getCenterLatitude())
        .centerLongitude(geofence.getCenterLongitude())
        .radiusMeters(geofence.getRadiusMeters())
        .address(geofence.getAddress())
        .type(geofence.getType())
        .isActive(geofence.getIsActive())
        .alertOnEntry(geofence.getAlertOnEntry())
        .alertOnExit(geofence.getAlertOnExit())
        .startTime(geofence.getStartTime() != null ? geofence.getStartTime().toString() : null)
        .endTime(geofence.getEndTime() != null ? geofence.getEndTime().toString() : null)
        .activeDays(geofence.getActiveDays())
        .priority(geofence.getPriority())
        .createdAt(geofence.getCreatedAt())
        .updatedAt(geofence.getUpdatedAt())
        .build();
  }
}