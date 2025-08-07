package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.GeofenceEvent;
import com.bifai.reminder.bifai_backend.entity.LocationHistory;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GeofenceEventRepository;
import com.bifai.reminder.bifai_backend.repository.GeofenceRepository;
import com.bifai.reminder.bifai_backend.repository.LocationHistoryRepository;
import com.bifai.reminder.bifai_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 지오펜스 모니터링 서비스
 * 실시간 위치 추적 및 안전 구역 이탈 감지
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GeofenceMonitoringService {

  private final GeofenceRepository geofenceRepository;
  private final GeofenceEventRepository geofenceEventRepository;
  private final LocationHistoryRepository locationHistoryRepository;
  private final NotificationService notificationService;
  
  private static final int DWELL_TIME_THRESHOLD_SECONDS = 300; // 5분
  private static final int WARNING_DISTANCE_METERS = 50; // 경계 근처 50m
  
  /**
   * 위치 업데이트 처리 및 지오펜스 이벤트 감지
   */
  public void processLocationUpdate(User user, double latitude, double longitude, Double accuracy) {
    log.debug("Processing location update for user {}: {}, {}", user.getId(), latitude, longitude);
    
    // 현재 위치 저장
    LocationHistory locationHistory = saveLocationHistory(user, latitude, longitude, accuracy);
    
    // 활성화된 지오펜스 조회
    List<Geofence> activeGeofences = geofenceRepository.findByUserAndIsActiveTrue(user);
    
    // 각 지오펜스에 대해 진입/이탈 체크
    for (Geofence geofence : activeGeofences) {
      checkGeofenceStatus(user, geofence, latitude, longitude, accuracy);
    }
    
    // 위험 구역 체크
    checkDangerZones(user, latitude, longitude);
    
    // 장시간 체류 감지
    checkDwellTime(user, latitude, longitude);
  }
  
  /**
   * 지오펜스 진입/이탈 상태 체크
   */
  private void checkGeofenceStatus(User user, Geofence geofence, double latitude, double longitude, Double accuracy) {
    boolean isCurrentlyInside = geofence.containsLocation(latitude, longitude);
    
    // 마지막 이벤트 조회
    Optional<GeofenceEvent> lastEvent = geofenceEventRepository
        .findTopByUserAndGeofenceOrderByCreatedAtDesc(user, geofence);
    
    GeofenceEvent.EventType lastEventType = lastEvent
        .map(GeofenceEvent::getEventType)
        .orElse(GeofenceEvent.EventType.EXIT);
    
    // 상태 변경 감지
    if (isCurrentlyInside && lastEventType == GeofenceEvent.EventType.EXIT) {
      // 진입 이벤트
      createGeofenceEvent(user, geofence, GeofenceEvent.EventType.ENTRY, 
                         latitude, longitude, accuracy);
      
    } else if (!isCurrentlyInside && lastEventType == GeofenceEvent.EventType.ENTRY) {
      // 이탈 이벤트
      GeofenceEvent exitEvent = createGeofenceEvent(user, geofence, 
                                                    GeofenceEvent.EventType.EXIT,
                                                    latitude, longitude, accuracy);
      
      // 체류 시간 계산
      if (lastEvent.isPresent()) {
        long durationSeconds = Duration.between(
            lastEvent.get().getCreatedAt(),
            LocalDateTime.now()
        ).getSeconds();
        exitEvent.setDurationSeconds(durationSeconds);
        geofenceEventRepository.save(exitEvent);
      }
      
      // 안전 구역 이탈 시 알림
      if (geofence.getAlertOnExit() && geofence.getType() != Geofence.GeofenceType.DANGER_ZONE) {
        sendGeofenceAlert(user, geofence, "안전 구역을 벗어났어요", exitEvent);
      }
    }
    
    // 경계 근처 경고
    double distance = calculateDistance(geofence.getCenterLatitude(), 
                                       geofence.getCenterLongitude(),
                                       latitude, longitude);
    
    if (Math.abs(distance - geofence.getRadiusMeters()) <= WARNING_DISTANCE_METERS) {
      checkBoundaryWarning(user, geofence, latitude, longitude, accuracy);
    }
  }
  
  /**
   * 지오펜스 이벤트 생성
   */
  private GeofenceEvent createGeofenceEvent(User user, Geofence geofence,
                                           GeofenceEvent.EventType eventType,
                                           double latitude, double longitude,
                                           Double accuracy) {
    
    GeofenceEvent event = GeofenceEvent.builder()
        .user(user)
        .geofence(geofence)
        .eventType(eventType)
        .latitude(latitude)
        .longitude(longitude)
        .accuracy(accuracy)
        .riskLevel(determineRiskLevel(geofence, eventType))
        .build();
    
    event = geofenceEventRepository.save(event);
    
    // 고위험 이벤트 즉시 알림
    if (event.isHighRisk() && event.requiresNotification()) {
      sendGeofenceAlert(user, geofence, "긴급 상황 감지", event);
    }
    
    log.info("Geofence event created: {} for user {} at geofence {}", 
             eventType, user.getId(), geofence.getName());
    
    return event;
  }
  
  /**
   * 위험도 수준 결정
   */
  private GeofenceEvent.RiskLevel determineRiskLevel(Geofence geofence, 
                                                     GeofenceEvent.EventType eventType) {
    // 위험 구역 진입
    if (geofence.getType() == Geofence.GeofenceType.DANGER_ZONE && 
        eventType == GeofenceEvent.EventType.ENTRY) {
      return GeofenceEvent.RiskLevel.HIGH;
    }
    
    // 안전 구역 이탈
    if ((geofence.getType() == Geofence.GeofenceType.HOME || 
         geofence.getType() == Geofence.GeofenceType.SAFE_ZONE) &&
        eventType == GeofenceEvent.EventType.EXIT) {
      return GeofenceEvent.RiskLevel.MEDIUM;
    }
    
    // 경고 이벤트
    if (eventType == GeofenceEvent.EventType.WARNING) {
      return GeofenceEvent.RiskLevel.HIGH;
    }
    
    return GeofenceEvent.RiskLevel.LOW;
  }
  
  /**
   * 위험 구역 체크
   */
  private void checkDangerZones(User user, double latitude, double longitude) {
    List<Geofence> dangerZones = geofenceRepository.findActiveDangerZones(user);
    
    for (Geofence dangerZone : dangerZones) {
      if (dangerZone.containsLocation(latitude, longitude)) {
        // 위험 구역 내 위치 확인
        GeofenceEvent warningEvent = createGeofenceEvent(
            user, dangerZone,
            GeofenceEvent.EventType.WARNING,
            latitude, longitude, null
        );
        
        warningEvent.setRiskLevel(GeofenceEvent.RiskLevel.CRITICAL);
        warningEvent.setNotes("위험 구역에 진입했습니다. 즉시 벗어나세요.");
        geofenceEventRepository.save(warningEvent);
        
        // 긴급 알림
        sendUrgentAlert(user, dangerZone, warningEvent);
      }
    }
  }
  
  /**
   * 장시간 체류 감지
   */
  private void checkDwellTime(User user, double latitude, double longitude) {
    // 최근 위치 이력 조회
    List<LocationHistory> recentLocations = locationHistoryRepository
        .findByUserAndCapturedAtAfterOrderByCapturedAtDesc(
            user, 
            LocalDateTime.now().minusMinutes(30)
        );
    
    if (recentLocations.size() < 5) {
      return;
    }
    
    // 같은 위치에 머물러 있는지 확인
    boolean isStationary = recentLocations.stream()
        .allMatch(loc -> {
          double distance = calculateDistance(
              loc.getLatitude().doubleValue(), 
              loc.getLongitude().doubleValue(),
              latitude, longitude
          );
          return distance < 50; // 50m 이내
        });
    
    if (isStationary) {
      // 체류 이벤트 생성
      List<Geofence> nearbyGeofences = findNearbyGeofences(user, latitude, longitude, 100);
      
      if (!nearbyGeofences.isEmpty()) {
        Geofence nearestGeofence = nearbyGeofences.get(0);
        
        GeofenceEvent dwellEvent = createGeofenceEvent(
            user, nearestGeofence,
            GeofenceEvent.EventType.DWELL,
            latitude, longitude, null
        );
        
        dwellEvent.setDurationSeconds((long) DWELL_TIME_THRESHOLD_SECONDS);
        dwellEvent.setNotes("같은 위치에 30분 이상 머물러 있습니다.");
        geofenceEventRepository.save(dwellEvent);
        
        log.info("Long dwell time detected for user {} at location {}, {}", 
                user.getId(), latitude, longitude);
      }
    }
  }
  
  /**
   * 경계 근처 경고 체크
   */
  private void checkBoundaryWarning(User user, Geofence geofence, 
                                   double latitude, double longitude, Double accuracy) {
    // 최근 경고 이벤트 확인
    LocalDateTime recentTime = LocalDateTime.now().minusMinutes(5);
    List<GeofenceEvent> recentWarnings = geofenceEventRepository
        .findByUserAndEventTypeAndCreatedAtAfter(
            user, GeofenceEvent.EventType.WARNING, recentTime
        );
    
    // 최근에 경고를 보냈으면 스킵
    if (!recentWarnings.isEmpty()) {
      return;
    }
    
    // 경고 이벤트 생성
    GeofenceEvent warningEvent = createGeofenceEvent(
        user, geofence,
        GeofenceEvent.EventType.WARNING,
        latitude, longitude, accuracy
    );
    
    warningEvent.setNotes("안전 구역 경계 근처에 있습니다.");
    geofenceEventRepository.save(warningEvent);
  }
  
  /**
   * 위치 이력 저장
   */
  private LocationHistory saveLocationHistory(User user, double latitude, 
                                             double longitude, Double accuracy) {
    LocationHistory history = LocationHistory.builder()
        .user(user)
        .latitude(new java.math.BigDecimal(latitude))
        .longitude(new java.math.BigDecimal(longitude))
        .accuracy(accuracy != null ? new java.math.BigDecimal(accuracy) : null)
        .capturedAt(LocalDateTime.now())
        .build();
    
    return locationHistoryRepository.save(history);
  }
  
  /**
   * 지오펜스 알림 전송
   */
  private void sendGeofenceAlert(User user, Geofence geofence, String message, 
                                GeofenceEvent event) {
    try {
      String fullMessage = String.format("%s: %s", geofence.getName(), message);
      
      notificationService.createNotification(
          user.getId(),
          "위치 알림",
          fullMessage
      );
      
      event.setNotificationSent(true);
      event.setNotificationSentAt(LocalDateTime.now());
      geofenceEventRepository.save(event);
      
      log.info("Geofence alert sent to user {}: {}", user.getId(), fullMessage);
      
    } catch (Exception e) {
      log.error("Failed to send geofence alert to user {}: {}", user.getId(), e.getMessage());
    }
  }
  
  /**
   * 긴급 알림 전송
   */
  private void sendUrgentAlert(User user, Geofence geofence, GeofenceEvent event) {
    String urgentMessage = String.format(
        "⚠️ 긴급: %s에 진입했습니다. 안전한 곳으로 이동하세요!",
        geofence.getName()
    );
    
    sendGeofenceAlert(user, geofence, urgentMessage, event);
    
    // 보호자에게도 알림
    // TODO: Guardian notification implementation
  }
  
  /**
   * 근처 지오펜스 찾기
   */
  private List<Geofence> findNearbyGeofences(User user, double latitude, 
                                            double longitude, int radiusMeters) {
    List<Geofence> allGeofences = geofenceRepository.findByUserAndIsActiveTrue(user);
    
    return allGeofences.stream()
        .filter(g -> {
          double distance = calculateDistance(
              g.getCenterLatitude(), g.getCenterLongitude(),
              latitude, longitude
          );
          return distance <= radiusMeters + g.getRadiusMeters();
        })
        .sorted((g1, g2) -> {
          double d1 = calculateDistance(g1.getCenterLatitude(), g1.getCenterLongitude(),
                                       latitude, longitude);
          double d2 = calculateDistance(g2.getCenterLatitude(), g2.getCenterLongitude(),
                                       latitude, longitude);
          return Double.compare(d1, d2);
        })
        .collect(Collectors.toList());
  }
  
  /**
   * 두 좌표 간 거리 계산 (미터 단위)
   */
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371000; // 지구 반지름 (미터)
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
  
  /**
   * 사용자의 현재 지오펜스 상태 조회
   */
  public Map<String, Object> getCurrentGeofenceStatus(User user) {
    Map<String, Object> status = new HashMap<>();
    
    // 마지막 위치 조회
    Optional<LocationHistory> lastLocation = locationHistoryRepository
        .findTopByUserOrderByCapturedAtDesc(user);
    
    if (lastLocation.isEmpty()) {
      status.put("hasLocation", false);
      return status;
    }
    
    LocationHistory location = lastLocation.get();
    status.put("hasLocation", true);
    status.put("lastUpdate", location.getCapturedAt());
    status.put("latitude", location.getLatitude().doubleValue());
    status.put("longitude", location.getLongitude().doubleValue());
    
    // 현재 위치한 지오펜스들
    List<Geofence> currentGeofences = findNearbyGeofences(
        user, location.getLatitude().doubleValue(), 
        location.getLongitude().doubleValue(), 0
    );
    
    status.put("currentGeofences", currentGeofences.stream()
        .map(g -> Map.of(
            "id", g.getId(),
            "name", g.getName(),
            "type", g.getType().name()
        ))
        .collect(Collectors.toList())
    );
    
    // 최근 이벤트
    List<GeofenceEvent> recentEvents = geofenceEventRepository
        .findByUserAndCreatedAtBetween(
            user,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now()
        );
    
    status.put("recentEventCount", recentEvents.size());
    status.put("hasWarnings", recentEvents.stream()
        .anyMatch(e -> e.getEventType() == GeofenceEvent.EventType.WARNING));
    
    return status;
  }
}