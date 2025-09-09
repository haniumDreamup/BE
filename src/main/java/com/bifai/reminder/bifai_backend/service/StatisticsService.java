package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.statistics.DailyActivityStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.GeofenceStatsDto;
import com.bifai.reminder.bifai_backend.dto.statistics.SafetyStatsDto;
import com.bifai.reminder.bifai_backend.entity.Geofence;
import com.bifai.reminder.bifai_backend.entity.LocationHistory;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.GeofenceRepository;
import com.bifai.reminder.bifai_backend.repository.LocationHistoryRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.EmergencyRepository;
import com.bifai.reminder.bifai_backend.entity.Emergency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 통계 서비스
 * 사용자 활동, 위치, 안전 관련 통계 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
  
  private final UserRepository userRepository;
  private final GeofenceRepository geofenceRepository;
  private final LocationHistoryRepository locationHistoryRepository;
  private final EmergencyRepository emergencyRepository;
  
  /**
   * 지오펜스 출입 통계 조회
   * @param userId 사용자 ID
   * @param startDate 조회 시작 날짜
   * @param endDate 조회 종료 날짜
   * @return 지오펜스별 출입 통계
   */
  public GeofenceStatsDto getGeofenceStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
    // 기본값 설정
    if (startDate == null) {
      startDate = LocalDate.now().minusDays(30);
    }
    if (endDate == null) {
      endDate = LocalDate.now();
    }
    
    int days = (int) startDate.until(endDate).getDays() + 1;
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    
    // 사용자의 모든 지오펜스 조회
    List<Geofence> userGeofences = geofenceRepository.findByUserUserIdAndIsActive(userId, true);
    
    // 기간 내 위치 이력 조회
    List<LocationHistory> locationHistories = 
        locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            userId, startDateTime, endDateTime);
    
    // 지오펜스별 출입 통계 계산
    List<GeofenceStatsDto.GeofenceEntry> geofenceEntries = new ArrayList<>();
    List<GeofenceStatsDto.DailyGeofenceActivity> dailyActivity = new ArrayList<>();
    
    int totalEntries = 0;
    int totalExits = 0;
    int totalViolations = 0;
    
    for (Geofence geofence : userGeofences) {
      GeofenceStats stats = calculateGeofenceStats(geofence, locationHistories);
      totalEntries += stats.getEntriesCount();
      totalExits += stats.getExitsCount();
      
      int violations = calculateGeofenceViolations(geofence.getId(), userId, startDateTime, endDateTime);
      
      geofenceEntries.add(GeofenceStatsDto.GeofenceEntry.builder()
          .geofenceId(geofence.getId())
          .geofenceName(geofence.getName())
          .entryCount(stats.getEntriesCount())
          .exitCount(stats.getExitsCount())
          .violations(violations)
          .build());
      
      totalViolations += violations;
    }
    
    // 일일 활동 통계 생성 (실제 데이터 기반)
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.atTime(23, 59, 59);
      
      List<LocationHistory> dayLocations = locationHistories.stream()
          .filter(loc -> !loc.getCapturedAt().isBefore(dayStart) && !loc.getCapturedAt().isAfter(dayEnd))
          .collect(Collectors.toList());
      
      int dailyEntries = 0;
      int dailyExits = 0;
      int dailyViolations = 0;
      
      for (Geofence geofence : userGeofences) {
        GeofenceStats dayStats = calculateGeofenceStats(geofence, dayLocations);
        dailyEntries += dayStats.getEntriesCount();
        dailyExits += dayStats.getExitsCount();
        dailyViolations += calculateGeofenceViolations(geofence.getId(), userId, dayStart, dayEnd);
      }
      
      dailyActivity.add(GeofenceStatsDto.DailyGeofenceActivity.builder()
          .date(date)
          .entries(dailyEntries)
          .exits(dailyExits)
          .violations(dailyViolations)
          .build());
    }
    
    double avgDailyEntries = days > 0 ? (double) totalEntries / days : 0;
    
    log.info("지오펜스 통계 조회 완료: 사용자 {}, 기간 {}일, 총 {}개 지오펜스", 
        userId, days, userGeofences.size());
    
    return GeofenceStatsDto.builder()
        .userId(userId)
        .startDate(startDate)
        .endDate(endDate)
        .totalGeofences(userGeofences.size())
        .totalEntries(totalEntries)
        .totalExits(totalExits)
        .totalViolations(totalViolations)
        .avgDailyEntries(avgDailyEntries)
        .topGeofences(geofenceEntries)
        .dailyActivity(dailyActivity)
        .build();
  }
  
  /**
   * 일별 활동 통계 (여러 날짜)
   */
  public List<DailyActivityStatsDto> getDailyActivityStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
    // 기본값 설정
    if (startDate == null) {
      startDate = LocalDate.now().minusDays(7);
    }
    if (endDate == null) {
      endDate = LocalDate.now();
    }
    
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    
    List<LocationHistory> locationHistories = 
        locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            userId, startDateTime, endDateTime);
    
    // 일별 그룹화
    Map<LocalDate, List<LocationHistory>> dailyGroups = locationHistories.stream()
        .collect(Collectors.groupingBy(loc -> loc.getCapturedAt().toLocalDate()));
    
    List<DailyActivityStatsDto> dailyStats = new ArrayList<>();
    
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      List<LocationHistory> dayLocations = dailyGroups.getOrDefault(date, Collections.emptyList());
      
      LocalDateTime firstActivity = null;
      LocalDateTime lastActivity = null;
      if (!dayLocations.isEmpty()) {
        firstActivity = dayLocations.stream()
            .map(LocationHistory::getCapturedAt)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        lastActivity = dayLocations.stream()
            .map(LocationHistory::getCapturedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
      }
      
      DailyActivityStatsDto stats = DailyActivityStatsDto.builder()
          .userId(userId)
          .date(date)
          .totalLocations(dayLocations.size())
          .totalDistanceKm(calculateTotalDistance(dayLocations))
          .movementCount(dayLocations.size() > 1 ? dayLocations.size() - 1 : 0)
          .firstActivity(firstActivity)
          .lastActivity(lastActivity)
          .hourlyBreakdown(calculateHourlyBreakdown(dayLocations))
          .topLocations(calculateTopLocations(dayLocations))
          .build();
      
      dailyStats.add(stats);
    }
    
    return dailyStats;
  }
  
  /**
   * 단일 날짜 일일 활동 통계
   */
  public DailyActivityStatsDto getDailyActivityStatistics(Long userId, LocalDate date) {
    List<DailyActivityStatsDto> stats = getDailyActivityStatistics(userId, date, date);
    return stats.isEmpty() ? null : stats.get(0);
  }
  
  /**
   * 안전도 통계
   */
  public SafetyStatsDto getSafetyStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
    // 기본값 설정
    if (startDate == null) {
      startDate = LocalDate.now().minusDays(30);
    }
    if (endDate == null) {
      endDate = LocalDate.now();
    }
    
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
    
    // 안전 지역(지오펜스) 내 시간 vs 외부 시간 계산
    List<Geofence> safeZones = geofenceRepository.findByUserUserIdAndIsActive(userId, true);
    List<LocationHistory> locations = 
        locationHistoryRepository.findByUser_UserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            userId, startDateTime, endDateTime);
    
    // 실제 Emergency 데이터로 안전 통계 계산
    List<Emergency> emergencies = emergencyRepository.findByUserIdAndCreatedAtBetween(
        userId, startDateTime, endDateTime);
    
    int totalSosAlerts = emergencies.size();
    int resolvedSosAlerts = (int) emergencies.stream()
        .filter(e -> e.getStatus() == Emergency.EmergencyStatus.RESOLVED)
        .count();
    int pendingSosAlerts = (int) emergencies.stream()
        .filter(e -> e.getStatus() == Emergency.EmergencyStatus.ACTIVE || 
                     e.getStatus() == Emergency.EmergencyStatus.TRIGGERED ||
                     e.getStatus() == Emergency.EmergencyStatus.NOTIFIED)
        .count();
    int geofenceViolations = (int) emergencies.stream()
        .filter(e -> e.getType() == Emergency.EmergencyType.GEOFENCE_EXIT)
        .count();
    int safetyScore = calculateSafetyScore(locations, safeZones);
    
    // 실제 데이터 기반 일일 안전 통계 생성
    List<SafetyStatsDto.DailySafetyStats> dailyStats = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      LocalDateTime dayStart = date.atStartOfDay();
      LocalDateTime dayEnd = date.atTime(23, 59, 59);
      
      List<Emergency> dayEmergencies = emergencies.stream()
          .filter(e -> !e.getCreatedAt().isBefore(dayStart) && !e.getCreatedAt().isAfter(dayEnd))
          .collect(Collectors.toList());
      
      int dailySosAlerts = dayEmergencies.size();
      int dailyGeofenceViolations = (int) dayEmergencies.stream()
          .filter(e -> e.getType() == Emergency.EmergencyType.GEOFENCE_EXIT)
          .count();
      
      List<LocationHistory> dayLocations = locations.stream()
          .filter(loc -> !loc.getCapturedAt().isBefore(dayStart) && !loc.getCapturedAt().isAfter(dayEnd))
          .collect(Collectors.toList());
      
      int dailySafetyScore = calculateSafetyScore(dayLocations, safeZones);
      
      dailyStats.add(SafetyStatsDto.DailySafetyStats.builder()
          .date(date)
          .sosAlerts(dailySosAlerts)
          .geofenceViolations(dailyGeofenceViolations)
          .safetyScore(dailySafetyScore)
          .hasIncidents(dailySosAlerts > 0 || dailyGeofenceViolations > 0)
          .build());
    }
    
    // 실제 최근 안전 인시던트
    List<SafetyStatsDto.SafetyIncident> recentIncidents = emergencies.stream()
        .sorted((e1, e2) -> e2.getCreatedAt().compareTo(e1.getCreatedAt()))
        .limit(10)
        .map(emergency -> SafetyStatsDto.SafetyIncident.builder()
            .incidentId(emergency.getId())
            .incidentType(emergency.getType().getDescription())
            .severity(emergency.getSeverity().getDescription())
            .occurredAt(emergency.getCreatedAt())
            .address(emergency.getAddress())
            .status(emergency.getStatus().getDescription())
            .description(emergency.getDescription())
            .latitude(emergency.getLatitude() != null ? emergency.getLatitude() : 0.0)
            .longitude(emergency.getLongitude() != null ? emergency.getLongitude() : 0.0)
            .build())
        .collect(Collectors.toList());
        
    
    return SafetyStatsDto.builder()
        .userId(userId)
        .startDate(startDate)
        .endDate(endDate)
        .totalSosAlerts(totalSosAlerts)
        .resolvedSosAlerts(resolvedSosAlerts)
        .pendingSosAlerts(pendingSosAlerts)
        .geofenceViolations(geofenceViolations)
        .safetyScore(safetyScore)
        .recentIncidents(recentIncidents)
        .dailyStats(dailyStats)
        .build();
  }
  
  /**
   * 안전 점수 계산
   */
  private int calculateSafetyScore(List<LocationHistory> locations, List<Geofence> safeZones) {
    if (locations.isEmpty()) return 100;
    
    long safeLocationCount = locations.stream()
        .mapToLong(location -> isInAnySafeZone(location, safeZones) ? 1 : 0)
        .sum();
    
    return (int) ((double) safeLocationCount / locations.size() * 100);
  }
  
  /**
   * 지오펜스 개별 통계 계산
   */
  private GeofenceStats calculateGeofenceStats(Geofence geofence, List<LocationHistory> locations) {
    int entriesCount = 0;
    int exitsCount = 0;
    long totalStayTime = 0;
    
    // 간단한 출입 감지 로직 (실제로는 더 복잡한 알고리즘 필요)
    Boolean wasInside = null;
    LocalDateTime entryTime = null;
    
    for (LocationHistory location : locations) {
      boolean isInside = isInsideGeofence(location, geofence);
      
      if (wasInside != null) {
        if (!wasInside && isInside) {
          // 진입
          entriesCount++;
          entryTime = location.getCapturedAt();
        } else if (wasInside && !isInside) {
          // 퇴장
          exitsCount++;
          if (entryTime != null) {
            totalStayTime += ChronoUnit.MINUTES.between(entryTime, location.getCapturedAt());
          }
        }
      }
      
      wasInside = isInside;
    }
    
    return GeofenceStats.builder()
        .geofenceId(geofence.getId())
        .geofenceName(geofence.getName())
        .entriesCount(entriesCount)
        .exitsCount(exitsCount)
        .averageStayMinutes(entriesCount > 0 ? totalStayTime / entriesCount : 0)
        .lastVisit(findLastVisit(geofence, locations))
        .build();
  }
  
  /**
   * 위치가 지오펜스 내부에 있는지 확인
   */
  private boolean isInsideGeofence(LocationHistory location, Geofence geofence) {
    // 간단한 원형 지오펜스 계산 (실제로는 더 정확한 계산 필요)
    double distance = calculateDistance(
        location.getLatitude().doubleValue(), location.getLongitude().doubleValue(),
        geofence.getCenterLatitude(), geofence.getCenterLongitude()
    );
    
    return distance <= geofence.getRadiusMeters();
  }
  
  /**
   * 두 지점 간 거리 계산 (하버사인 공식)
   */
  private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371; // 지구 반지름 (km)
    
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    
    return R * c * 1000; // 미터로 변환
  }
  
  /**
   * 어떤 안전 지역에라도 속하는지 확인
   */
  private boolean isInAnySafeZone(LocationHistory location, List<Geofence> safeZones) {
    return safeZones.stream()
        .anyMatch(geofence -> isInsideGeofence(location, geofence));
  }
  
  /**
   * 지오펜스 위반 계산
   */
  private int calculateGeofenceViolations(Long geofenceId, Long userId, LocalDateTime start, LocalDateTime end) {
    List<Emergency> violations = emergencyRepository.findByUserIdAndCreatedAtBetween(userId, start, end)
        .stream()
        .filter(e -> e.getType() == Emergency.EmergencyType.GEOFENCE_EXIT)
        .collect(Collectors.toList());
    return violations.size();
  }
  
  /**
   * 시간별 활동 분석
   */
  private List<DailyActivityStatsDto.HourlyActivity> calculateHourlyBreakdown(List<LocationHistory> locations) {
    Map<Integer, List<LocationHistory>> hourlyGroups = locations.stream()
        .collect(Collectors.groupingBy(loc -> loc.getCapturedAt().getHour()));
    
    List<DailyActivityStatsDto.HourlyActivity> hourlyBreakdown = new ArrayList<>();
    for (int hour = 0; hour < 24; hour++) {
      List<LocationHistory> hourLocations = hourlyGroups.getOrDefault(hour, Collections.emptyList());
      double hourDistance = calculateTotalDistance(hourLocations);
      int movements = hourLocations.size() > 1 ? hourLocations.size() - 1 : 0;
      
      hourlyBreakdown.add(DailyActivityStatsDto.HourlyActivity.builder()
          .hour(hour)
          .locationCount(hourLocations.size())
          .distanceKm(hourDistance)
          .movements(movements)
          .build());
    }
    return hourlyBreakdown;
  }
  
  /**
   * 상위 방문 위치 분석
   */
  private List<DailyActivityStatsDto.LocationActivity> calculateTopLocations(List<LocationHistory> locations) {
    if (locations.isEmpty()) return Collections.emptyList();
    
    Map<String, List<LocationHistory>> locationGroups = locations.stream()
        .filter(loc -> loc.getAddress() != null && !loc.getAddress().trim().isEmpty())
        .collect(Collectors.groupingBy(
            loc -> loc.getAddress().length() > 50 ? 
                   loc.getAddress().substring(0, 50) + "..." : 
                   loc.getAddress()
        ));
    
    return locationGroups.entrySet().stream()
        .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
        .limit(5)
        .map(entry -> {
          List<LocationHistory> groupLocations = entry.getValue();
          LocationHistory firstLocation = groupLocations.get(0);
          return DailyActivityStatsDto.LocationActivity.builder()
              .latitude(firstLocation.getLatitude().doubleValue())
              .longitude(firstLocation.getLongitude().doubleValue())
              .address(entry.getKey())
              .visitCount(groupLocations.size())
              .totalTimeMinutes(0L) // TODO: 실제 체류 시간 계산
              .build();
        })
        .collect(Collectors.toList());
  }
  
  /**
   * 고유한 지오펜스 개수 계산
   */
  private int countUniqueGeofences(List<LocationHistory> locations) {
    // TODO: 실제 지오펜스 매칭 로직 구현
    return (int) (locations.size() / 10.0); // 임시 계산
  }
  
  /**
   * 총 이동 거리 계산
   */
  private double calculateTotalDistance(List<LocationHistory> locations) {
    if (locations.size() < 2) return 0.0;
    
    double totalDistance = 0.0;
    for (int i = 1; i < locations.size(); i++) {
      LocationHistory prev = locations.get(i - 1);
      LocationHistory curr = locations.get(i);
      
      totalDistance += calculateDistance(
          prev.getLatitude().doubleValue(), prev.getLongitude().doubleValue(),
          curr.getLatitude().doubleValue(), curr.getLongitude().doubleValue()
      );
    }
    
    return totalDistance / 1000.0; // km로 변환
  }
  
  /**
   * 활동 시간 계산
   */
  private int calculateActiveHours(List<LocationHistory> locations) {
    if (locations.isEmpty()) return 0;
    
    LocalDateTime earliest = locations.stream()
        .map(LocationHistory::getCapturedAt)
        .min(LocalDateTime::compareTo)
        .orElse(LocalDateTime.now());
    
    LocalDateTime latest = locations.stream()
        .map(LocationHistory::getCapturedAt)
        .max(LocalDateTime::compareTo)
        .orElse(LocalDateTime.now());
    
    return (int) ChronoUnit.HOURS.between(earliest, latest);
  }
  
  /**
   * 마지막 방문 시간 찾기
   */
  private LocalDateTime findLastVisit(Geofence geofence, List<LocationHistory> locations) {
    return locations.stream()
        .filter(loc -> isInsideGeofence(loc, geofence))
        .map(LocationHistory::getCapturedAt)
        .max(LocalDateTime::compareTo)
        .orElse(null);
  }
  
  // DTO 클래스들
  public static class GeofenceStatistics {
    private int periodDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalLocations;
    private int totalEntries;
    private int totalExits;
    private String mostVisitedGeofence;
    private long averageStayMinutes;
    private List<GeofenceStats> geofenceStats;
    
    // getters and setters
    public int getPeriodDays() { return periodDays; }
    public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public int getTotalLocations() { return totalLocations; }
    public void setTotalLocations(int totalLocations) { this.totalLocations = totalLocations; }
    
    public int getTotalEntries() { return totalEntries; }
    public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
    
    public int getTotalExits() { return totalExits; }
    public void setTotalExits(int totalExits) { this.totalExits = totalExits; }
    
    public String getMostVisitedGeofence() { return mostVisitedGeofence; }
    public void setMostVisitedGeofence(String mostVisitedGeofence) { this.mostVisitedGeofence = mostVisitedGeofence; }
    
    public long getAverageStayMinutes() { return averageStayMinutes; }
    public void setAverageStayMinutes(long averageStayMinutes) { this.averageStayMinutes = averageStayMinutes; }
    
    public List<GeofenceStats> getGeofenceStats() { return geofenceStats; }
    public void setGeofenceStats(List<GeofenceStats> geofenceStats) { this.geofenceStats = geofenceStats; }
  }
  
  public static class GeofenceStats {
    private Long geofenceId;
    private String geofenceName;
    private int entriesCount;
    private int exitsCount;
    private long averageStayMinutes;
    private LocalDateTime lastVisit;
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
      private GeofenceStats stats = new GeofenceStats();
      
      public Builder geofenceId(Long geofenceId) { stats.geofenceId = geofenceId; return this; }
      public Builder geofenceName(String name) { stats.geofenceName = name; return this; }
      public Builder entriesCount(int count) { stats.entriesCount = count; return this; }
      public Builder exitsCount(int count) { stats.exitsCount = count; return this; }
      public Builder averageStayMinutes(long minutes) { stats.averageStayMinutes = minutes; return this; }
      public Builder lastVisit(LocalDateTime time) { stats.lastVisit = time; return this; }
      
      public GeofenceStats build() { return stats; }
    }
    
    // getters
    public Long getGeofenceId() { return geofenceId; }
    public String getGeofenceName() { return geofenceName; }
    public int getEntriesCount() { return entriesCount; }
    public int getExitsCount() { return exitsCount; }
    public long getAverageStayMinutes() { return averageStayMinutes; }
    public LocalDateTime getLastVisit() { return lastVisit; }
  }
  
  public static class DailyActivityStatistics {
    private int periodDays;
    private List<DailyStats> dailyStats;
    private double averageDailyLocations;
    
    // getters and setters
    public int getPeriodDays() { return periodDays; }
    public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }
    
    public List<DailyStats> getDailyStats() { return dailyStats; }
    public void setDailyStats(List<DailyStats> dailyStats) { this.dailyStats = dailyStats; }
    
    public double getAverageDailyLocations() { return averageDailyLocations; }
    public void setAverageDailyLocations(double averageDailyLocations) { this.averageDailyLocations = averageDailyLocations; }
  }
  
  public static class DailyStats {
    private LocalDate date;
    private int locationCount;
    private int uniqueGeofences;
    private double totalDistanceKm;
    private int activeHours;
    
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
      private DailyStats stats = new DailyStats();
      
      public Builder date(LocalDate date) { stats.date = date; return this; }
      public Builder locationCount(int count) { stats.locationCount = count; return this; }
      public Builder uniqueGeofences(int count) { stats.uniqueGeofences = count; return this; }
      public Builder totalDistanceKm(double distance) { stats.totalDistanceKm = distance; return this; }
      public Builder activeHours(int hours) { stats.activeHours = hours; return this; }
      
      public DailyStats build() { return stats; }
    }
    
    // getters
    public LocalDate getDate() { return date; }
    public int getLocationCount() { return locationCount; }
    public int getUniqueGeofences() { return uniqueGeofences; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public int getActiveHours() { return activeHours; }
  }
  
  public static class SafetyStatistics {
    private int periodDays;
    private long timeInSafeZonesMinutes;
    private long timeOutsideSafeZonesMinutes;
    private double safetyPercentage;
    private int emergencyAlertsCount;
    
    // getters and setters
    public int getPeriodDays() { return periodDays; }
    public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }
    
    public long getTimeInSafeZonesMinutes() { return timeInSafeZonesMinutes; }
    public void setTimeInSafeZonesMinutes(long timeInSafeZonesMinutes) { this.timeInSafeZonesMinutes = timeInSafeZonesMinutes; }
    
    public long getTimeOutsideSafeZonesMinutes() { return timeOutsideSafeZonesMinutes; }
    public void setTimeOutsideSafeZonesMinutes(long timeOutsideSafeZonesMinutes) { this.timeOutsideSafeZonesMinutes = timeOutsideSafeZonesMinutes; }
    
    public double getSafetyPercentage() { return safetyPercentage; }
    public void setSafetyPercentage(double safetyPercentage) { this.safetyPercentage = safetyPercentage; }
    
    public int getEmergencyAlertsCount() { return emergencyAlertsCount; }
    public void setEmergencyAlertsCount(int emergencyAlertsCount) { this.emergencyAlertsCount = emergencyAlertsCount; }
  }
}