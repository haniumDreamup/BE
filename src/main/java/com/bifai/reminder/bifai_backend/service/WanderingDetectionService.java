package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.MovementPattern;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.WanderingDetection;
import com.bifai.reminder.bifai_backend.entity.WanderingDetection.RiskLevel;
import com.bifai.reminder.bifai_backend.entity.WanderingDetection.WanderingStatus;
import com.bifai.reminder.bifai_backend.repository.MovementPatternRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.WanderingDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 배회 감지 서비스
 * 사용자의 비정상적인 이동 패턴을 감지하고 대응
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WanderingDetectionService {

  private final WanderingDetectionRepository wanderingDetectionRepository;
  private final MovementPatternRepository movementPatternRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;
  private final NavigationService navigationService;

  private static final double WANDERING_DISTANCE_THRESHOLD = 0.5; // 500m
  private static final int WANDERING_TIME_THRESHOLD = 30; // 30분
  private static final double CIRCULAR_PATTERN_THRESHOLD = 0.8; // 원형 패턴 임계값

  /**
   * 사용자 위치 업데이트 및 배회 감지
   */
  public WanderingDetection checkForWandering(Long userId, Double latitude, Double longitude) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 현재 활성 배회 감지 확인
    WanderingDetection activeDetection = wanderingDetectionRepository
        .findActiveDetectionByUser(user)
        .orElse(null);

    if (activeDetection != null) {
      // 기존 감지 업데이트
      return updateWanderingDetection(activeDetection, latitude, longitude);
    } else {
      // 새로운 배회 감지 필요 여부 확인
      if (isWanderingBehavior(user, latitude, longitude)) {
        return createWanderingDetection(user, latitude, longitude);
      }
    }

    return null;
  }

  /**
   * 배회 행동 판단
   */
  private boolean isWanderingBehavior(User user, Double latitude, Double longitude) {
    // 최근 이동 패턴 확인
    List<MovementPattern> recentPatterns = movementPatternRepository
        .findRecentPatternsByUser(user, LocalDateTime.now().minusHours(1));

    if (recentPatterns.isEmpty()) {
      return false;
    }

    // 패턴 편차 확인
    for (MovementPattern pattern : recentPatterns) {
      double deviation = pattern.calculateDeviation(latitude, longitude);
      if (deviation > WANDERING_DISTANCE_THRESHOLD) {
        log.info("배회 감지: 사용자 {}, 편차 거리 {}km", user.getUserId(), deviation);
        return true;
      }
    }

    // 원형 패턴 확인 (반복적으로 같은 곳을 돌고 있는지)
    if (isCircularPattern(user, latitude, longitude)) {
      log.info("원형 패턴 배회 감지: 사용자 {}", user.getUserId());
      return true;
    }

    return false;
  }

  /**
   * 원형 패턴 확인
   */
  private boolean isCircularPattern(User user, Double currentLat, Double currentLon) {
    // TODO: ML 모델을 통한 원형 패턴 감지 구현
    // 임시로 간단한 로직 사용
    return false;
  }

  /**
   * 배회 감지 생성
   */
  private WanderingDetection createWanderingDetection(User user, Double latitude, Double longitude) {
    WanderingDetection detection = WanderingDetection.builder()
        .user(user)
        .detectedAt(LocalDateTime.now())
        .status(WanderingStatus.DETECTED)
        .riskLevel(RiskLevel.MEDIUM)
        .startLatitude(latitude)
        .startLongitude(longitude)
        .currentLatitude(latitude)
        .currentLongitude(longitude)
        .confidenceScore(0.7f)
        .build();

    detection = wanderingDetectionRepository.save(detection);
    
    // 보호자 알림
    notifyGuardians(user, detection);
    
    return detection;
  }

  /**
   * 배회 감지 업데이트
   */
  private WanderingDetection updateWanderingDetection(
      WanderingDetection detection, 
      Double latitude, 
      Double longitude) {
    
    detection.updateCurrentLocation(latitude, longitude, null);
    
    // 지속 시간 계산
    long durationMinutes = java.time.Duration.between(
        detection.getDetectedAt(), 
        LocalDateTime.now()
    ).toMinutes();
    
    detection.setDurationMinutes((int) durationMinutes);
    
    // 위험 수준 업데이트
    if (durationMinutes > WANDERING_TIME_THRESHOLD) {
      detection.setRiskLevel(RiskLevel.HIGH);
      detection.markInterventionNeeded();
      
      // 내비게이션 제공
      if (!Boolean.TRUE.equals(detection.getNavigationProvided())) {
        provideNavigation(detection);
      }
    }
    
    return wanderingDetectionRepository.save(detection);
  }

  /**
   * 보호자 알림
   */
  private void notifyGuardians(User user, WanderingDetection detection) {
    log.info("보호자 알림: 사용자 {} 배회 감지", user.getUserId());
    detection.notifyGuardian();
    // TODO: 실제 알림 발송 구현
  }

  /**
   * 내비게이션 제공
   */
  private void provideNavigation(WanderingDetection detection) {
    log.info("내비게이션 제공: 감지 ID {}", detection.getId());
    detection.startNavigation();
    
    // 내비게이션 서비스 호출
    navigationService.startHomeNavigation(
        detection.getUser().getUserId(),
        detection.getCurrentLatitude(),
        detection.getCurrentLongitude()
    );
  }

  /**
   * 배회 해결
   */
  public WanderingDetection resolveWandering(Long detectionId, String resolutionMethod) {
    WanderingDetection detection = wanderingDetectionRepository.findById(detectionId)
        .orElseThrow(() -> new IllegalArgumentException("배회 감지를 찾을 수 없습니다."));
    
    detection.resolve(resolutionMethod);
    return wanderingDetectionRepository.save(detection);
  }

  /**
   * 활성 배회 감지 조회
   */
  public List<WanderingDetection> getActiveDetections() {
    return wanderingDetectionRepository.findActiveDetections();
  }

  /**
   * 사용자별 배회 이력 조회
   */
  public List<WanderingDetection> getUserWanderingHistory(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    return wanderingDetectionRepository.findByUserOrderByDetectedAtDesc(user);
  }
}