package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.SafeRoute;
import com.bifai.reminder.bifai_backend.entity.SafeRoute.DifficultyLevel;
import com.bifai.reminder.bifai_backend.entity.SafeRoute.RouteType;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.SafeRouteRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 내비게이션 서비스
 * 안전한 경로 안내 및 음성 내비게이션 제공
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NavigationService {

  private final SafeRouteRepository safeRouteRepository;
  private final UserRepository userRepository;
  private final VoiceGuidanceService voiceGuidanceService;

  /**
   * 집으로 가는 내비게이션 시작
   */
  public SafeRoute startHomeNavigation(Long userId, Double currentLat, Double currentLon) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 집 경로 찾기
    SafeRoute homeRoute = safeRouteRepository
        .findByUserAndRouteTypeAndIsActiveTrue(user, RouteType.HOME)
        .stream()
        .filter(SafeRoute::isUsable)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("집 경로가 설정되지 않았습니다."));

    // 경로 사용 기록
    homeRoute.recordUsage();
    safeRouteRepository.save(homeRoute);

    // 음성 안내 시작
    startVoiceGuidance(user, homeRoute, currentLat, currentLon);

    log.info("집 내비게이션 시작: 사용자 {}, 경로 {}", userId, homeRoute.getRouteName());
    return homeRoute;
  }

  /**
   * 특정 목적지로의 내비게이션 시작
   */
  public SafeRoute startNavigation(
      Long userId, 
      Double currentLat, 
      Double currentLon,
      Double destinationLat,
      Double destinationLon,
      String destinationName) {
    
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    // 목적지와 일치하는 기존 경로 찾기
    SafeRoute existingRoute = findMatchingRoute(user, destinationLat, destinationLon);
    
    if (existingRoute != null) {
      existingRoute.recordUsage();
      safeRouteRepository.save(existingRoute);
      startVoiceGuidance(user, existingRoute, currentLat, currentLon);
      return existingRoute;
    }

    // 새 경로 생성
    SafeRoute newRoute = createSafeRoute(
        user, 
        currentLat, 
        currentLon, 
        destinationLat, 
        destinationLon, 
        destinationName
    );
    
    startVoiceGuidance(user, newRoute, currentLat, currentLon);
    return newRoute;
  }

  /**
   * 안전한 경로 생성
   */
  private SafeRoute createSafeRoute(
      User user,
      Double startLat,
      Double startLon,
      Double endLat,
      Double endLon,
      String destinationName) {
    
    SafeRoute route = SafeRoute.builder()
        .user(user)
        .routeName(destinationName + " 가는 길")
        .routeType(RouteType.CUSTOM)
        .startLatitude(startLat)
        .startLongitude(startLon)
        .endLatitude(endLat)
        .endLongitude(endLon)
        .endAddress(destinationName)
        .difficultyLevel(DifficultyLevel.MODERATE)
        .isActive(true)
        .build();

    // 거리 계산
    double distance = calculateDistance(startLat, startLon, endLat, endLon);
    route.setTotalDistance((float) distance);
    route.setEstimatedTime((int) (distance * 15)); // 평균 보행 속도 4km/h 기준

    // 안전도 평가
    route.calculateSafetyScore();

    return safeRouteRepository.save(route);
  }

  /**
   * 음성 안내 시작
   */
  private void startVoiceGuidance(User user, SafeRoute route, Double currentLat, Double currentLon) {
    String guidance = generateSimpleGuidance(route, currentLat, currentLon);
    voiceGuidanceService.speak(guidance, user.getPreferredLanguage());
  }

  /**
   * 간단한 길 안내 생성
   */
  private String generateSimpleGuidance(SafeRoute route, Double currentLat, Double currentLon) {
    StringBuilder guidance = new StringBuilder();
    
    // 목적지 안내
    guidance.append(route.getEndAddress()).append("로 가는 길을 안내합니다. ");
    
    // 거리와 예상 시간
    if (route.getTotalDistance() != null) {
      guidance.append("총 거리는 ").append(String.format("%.1f", route.getTotalDistance()))
              .append("킬로미터이고, ");
    }
    if (route.getEstimatedTime() != null) {
      guidance.append("예상 시간은 약 ").append(route.getEstimatedTime())
              .append("분입니다. ");
    }
    
    // 주요 랜드마크 안내
    if (route.getStartLandmark() != null) {
      guidance.append(route.getStartLandmark()).append("에서 출발하세요. ");
    }
    
    // 안전 주의사항
    guidance.append("횡단보도를 건널 때는 꼭 신호를 확인하세요.");
    
    return guidance.toString();
  }

  /**
   * 매칭되는 경로 찾기
   */
  private SafeRoute findMatchingRoute(User user, Double destLat, Double destLon) {
    List<SafeRoute> userRoutes = safeRouteRepository.findByUserAndIsActiveTrue(user);
    
    final double TOLERANCE = 0.001; // 약 100m 허용 오차
    
    return userRoutes.stream()
        .filter(route -> 
            Math.abs(route.getEndLatitude() - destLat) < TOLERANCE &&
            Math.abs(route.getEndLongitude() - destLon) < TOLERANCE
        )
        .findFirst()
        .orElse(null);
  }

  /**
   * 거리 계산 (Haversine formula)
   */
  private double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
    final double R = 6371; // 지구 반경 (km)
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /**
   * 사용자의 모든 안전 경로 조회
   */
  public List<SafeRoute> getUserSafeRoutes(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    return safeRouteRepository.findByUserAndIsActiveTrue(user);
  }

  /**
   * 주 경로 설정
   */
  public SafeRoute setPrimaryRoute(Long routeId) {
    SafeRoute route = safeRouteRepository.findById(routeId)
        .orElseThrow(() -> new IllegalArgumentException("경로를 찾을 수 없습니다."));
    
    // 기존 주 경로 해제
    safeRouteRepository.findByUserAndIsPrimaryTrue(route.getUser())
        .forEach(r -> {
          r.setIsPrimary(false);
          safeRouteRepository.save(r);
        });
    
    // 새 주 경로 설정
    route.setAsPrimary();
    return safeRouteRepository.save(route);
  }

  /**
   * 야간 안전 경로 조회
   */
  public List<SafeRoute> getNightSafeRoutes(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    
    return safeRouteRepository.findByUserAndIsActiveTrue(user)
        .stream()
        .filter(SafeRoute::isSafeAtNight)
        .collect(Collectors.toList());
  }
}