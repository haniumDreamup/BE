package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.InteractionPattern;
import com.bifai.reminder.bifai_backend.entity.InteractionPattern.PatternType;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import com.bifai.reminder.bifai_backend.repository.InteractionPatternRepository;
import com.bifai.reminder.bifai_backend.repository.UserBehaviorLogRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 인터랙션 패턴 분석 서비스
 * 사용자 행동 로그를 분석하여 패턴을 추출하고 이상 징후를 감지
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InteractionPatternAnalysisService {
  
  private final InteractionPatternRepository patternRepository;
  private final UserBehaviorLogRepository behaviorLogRepository;
  private final UserRepository userRepository;
  
  /**
   * 사용자의 일일 패턴 분석
   */
  @Transactional
  public InteractionPattern analyzeDailyPattern(Long userId, LocalDateTime date) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
    LocalDateTime endOfDay = startOfDay.plusDays(1);
    
    // 해당 일자의 로그 조회
    List<UserBehaviorLog> logs = behaviorLogRepository.findByUserAndTimestampBetween(
      user, startOfDay, endOfDay);
    
    if (logs.isEmpty()) {
      log.info("분석할 로그가 없습니다. userId: {}, date: {}", userId, date);
      return null;
    }
    
    // 패턴 분석
    InteractionPattern pattern = InteractionPattern.builder()
      .user(user)
      .patternType(PatternType.DAILY)
      .analysisDate(LocalDateTime.now())
      .timeWindowStart(startOfDay)
      .timeWindowEnd(endOfDay)
      .sampleSize(logs.size())
      .build();
    
    // 메트릭 계산
    calculateMetrics(pattern, logs);
    
    // 네비게이션 경로 분석
    analyzeNavigationPaths(pattern, logs);
    
    // 시간대별 활동 분석
    analyzeHourlyActivity(pattern, logs);
    
    // 이상 패턴 감지
    detectAnomalies(pattern, user, PatternType.DAILY);
    
    return patternRepository.save(pattern);
  }
  
  /**
   * 실시간 패턴 분석 (최근 1시간)
   */
  @Async("taskExecutor")
  @Transactional
  public CompletableFuture<InteractionPattern> analyzeRealtimePattern(Long userId) {
    try {
      User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
      
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime oneHourAgo = now.minusHours(1);
      
      List<UserBehaviorLog> logs = behaviorLogRepository.findByUserAndTimestampBetween(
        user, oneHourAgo, now);
      
      if (logs.size() < 5) { // 최소 5개 이상의 로그가 있어야 분석
        return CompletableFuture.completedFuture(null);
      }
      
      InteractionPattern pattern = InteractionPattern.builder()
        .user(user)
        .patternType(PatternType.REALTIME)
        .analysisDate(now)
        .timeWindowStart(oneHourAgo)
        .timeWindowEnd(now)
        .sampleSize(logs.size())
        .build();
      
      calculateMetrics(pattern, logs);
      detectAnomalies(pattern, user, PatternType.REALTIME);
      
      InteractionPattern saved = patternRepository.save(pattern);
      
      // 이상 패턴 감지 시 알림
      if (Boolean.TRUE.equals(saved.getIsAnomaly()) && saved.getAnomalyScore() > 80) {
        log.warn("높은 이상 패턴 감지! userId: {}, score: {}", userId, saved.getAnomalyScore());
        // TODO: 알림 서비스 호출
      }
      
      return CompletableFuture.completedFuture(saved);
      
    } catch (Exception e) {
      log.error("실시간 패턴 분석 실패 - userId: {}", userId, e);
      return CompletableFuture.failedFuture(e);
    }
  }
  
  /**
   * 세션별 패턴 분석
   */
  @Transactional
  public InteractionPattern analyzeSessionPattern(Long userId, String sessionId) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    List<UserBehaviorLog> logs = behaviorLogRepository.findByUserAndSessionId(user, sessionId);
    
    if (logs.isEmpty()) {
      return null;
    }
    
    // 세션 시작/종료 시간 계산
    LocalDateTime sessionStart = logs.stream()
      .map(UserBehaviorLog::getTimestamp)
      .min(LocalDateTime::compareTo)
      .orElse(LocalDateTime.now());
    
    LocalDateTime sessionEnd = logs.stream()
      .map(UserBehaviorLog::getTimestamp)
      .max(LocalDateTime::compareTo)
      .orElse(LocalDateTime.now());
    
    InteractionPattern pattern = InteractionPattern.builder()
      .user(user)
      .patternType(PatternType.SESSION)
      .analysisDate(LocalDateTime.now())
      .timeWindowStart(sessionStart)
      .timeWindowEnd(sessionEnd)
      .sampleSize(logs.size())
      .build();
    
    calculateMetrics(pattern, logs);
    analyzeNavigationPaths(pattern, logs);
    
    // 세션 길이 계산
    Duration sessionDuration = Duration.between(sessionStart, sessionEnd);
    pattern.setAvgSessionDuration((double) sessionDuration.getSeconds());
    
    return patternRepository.save(pattern);
  }
  
  /**
   * 메트릭 계산
   */
  private void calculateMetrics(InteractionPattern pattern, List<UserBehaviorLog> logs) {
    if (logs.isEmpty()) return;
    
    // 클릭 빈도 계산 (분당)
    long clicks = logs.stream()
      .filter(log -> log.getActionType() == ActionType.BUTTON_CLICK)
      .count();
    Duration duration = Duration.between(pattern.getTimeWindowStart(), pattern.getTimeWindowEnd());
    double minutes = Math.max(duration.toMinutes(), 1);
    pattern.setClickFrequency(clicks / minutes);
    
    // 페이지 뷰 수
    long pageViews = logs.stream()
      .filter(log -> log.getActionType() == ActionType.PAGE_VIEW)
      .count();
    pattern.setPageViewCount((int) pageViews);
    
    // 고유 페이지 수
    long uniquePages = logs.stream()
      .filter(log -> log.getActionType() == ActionType.PAGE_VIEW)
      .map(UserBehaviorLog::getPageUrl)
      .distinct()
      .count();
    pattern.setUniquePagesVisited((int) uniquePages);
    
    // 에러율 계산
    long errors = logs.stream()
      .filter(log -> log.getActionType() == ActionType.ERROR || 
                     log.getActionType() == ActionType.FORM_ERROR)
      .count();
    pattern.setErrorRate(logs.isEmpty() ? 0.0 : (double) errors / logs.size() * 100);
    
    // 기능 사용 횟수
    long featureUse = logs.stream()
      .filter(log -> log.getActionType() == ActionType.FEATURE_USE)
      .count();
    pattern.setFeatureUsageCount((int) featureUse);
    
    // 평균 응답 시간
    double avgResponseTime = logs.stream()
      .filter(log -> log.getResponseTimeMs() != null)
      .mapToInt(UserBehaviorLog::getResponseTimeMs)
      .average()
      .orElse(0.0);
    pattern.setAvgResponseTime(avgResponseTime);
    
    // Top 기능 분석
    Map<String, Long> featureCounts = logs.stream()
      .filter(log -> log.getActionType() == ActionType.FEATURE_USE)
      .map(log -> log.getActionDetail().getOrDefault("feature", "unknown").toString())
      .collect(Collectors.groupingBy(feature -> feature, Collectors.counting()));
    
    pattern.setTopFeatures(new HashMap<>(featureCounts));
  }
  
  /**
   * 네비게이션 경로 분석
   */
  private void analyzeNavigationPaths(InteractionPattern pattern, List<UserBehaviorLog> logs) {
    List<String> navigationSequence = logs.stream()
      .filter(log -> log.getActionType() == ActionType.PAGE_VIEW)
      .sorted(Comparator.comparing(UserBehaviorLog::getTimestamp))
      .map(UserBehaviorLog::getPageUrl)
      .collect(Collectors.toList());
    
    if (navigationSequence.size() < 2) return;
    
    // 페이지 전환 패턴 분석
    Map<String, Integer> transitions = new HashMap<>();
    for (int i = 0; i < navigationSequence.size() - 1; i++) {
      String from = navigationSequence.get(i);
      String to = navigationSequence.get(i + 1);
      String transition = from + " -> " + to;
      transitions.merge(transition, 1, Integer::sum);
    }
    
    // 가장 빈번한 경로 저장
    Map<String, Object> paths = new HashMap<>();
    paths.put("sequence", navigationSequence);
    paths.put("transitions", transitions);
    paths.put("totalPages", navigationSequence.size());
    
    pattern.setNavigationPaths(paths);
  }
  
  /**
   * 시간대별 활동 분석
   */
  private void analyzeHourlyActivity(InteractionPattern pattern, List<UserBehaviorLog> logs) {
    Map<Integer, Long> hourlyCount = logs.stream()
      .collect(Collectors.groupingBy(
        log -> log.getTimestamp().getHour(),
        Collectors.counting()
      ));
    
    Map<String, Object> hourlyActivity = new HashMap<>();
    hourlyCount.forEach((hour, count) -> 
      hourlyActivity.put(String.format("%02d:00", hour), count));
    
    pattern.setHourlyActivity(hourlyActivity);
  }
  
  /**
   * 이상 패턴 감지 (3σ 규칙)
   */
  private void detectAnomalies(InteractionPattern pattern, User user, PatternType type) {
    // 최근 30일간의 베이스라인 메트릭 계산
    LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
    Object[] baselineData = patternRepository.calculateBaselineMetrics(
      user, type, thirtyDaysAgo, LocalDateTime.now());
    
    if (baselineData == null || baselineData[0] == null) {
      pattern.setIsAnomaly(false);
      pattern.setConfidenceLevel(0.5); // 데이터 부족
      return;
    }
    
    // 평균과 표준편차 추출
    Double avgClickFreq = (Double) baselineData[0];
    Double avgSessionDur = (Double) baselineData[1];
    Double avgErrorRate = (Double) baselineData[2];
    Double stdClickFreq = (Double) baselineData[3];
    Double stdSessionDur = (Double) baselineData[4];
    Double stdErrorRate = (Double) baselineData[5];
    
    // 베이스라인 저장
    Map<String, Object> baseline = new HashMap<>();
    baseline.put("avgClickFreq", avgClickFreq);
    baseline.put("stdClickFreq", stdClickFreq);
    baseline.put("avgErrorRate", avgErrorRate);
    baseline.put("stdErrorRate", stdErrorRate);
    pattern.setBaselineMetrics(baseline);
    
    // 각 메트릭에 대해 이상 감지
    boolean isAnomaly = false;
    double maxAnomalyScore = 0;
    
    // 클릭 빈도 이상 감지
    if (pattern.getClickFrequency() != null && stdClickFreq != null && stdClickFreq > 0) {
      double zScore = Math.abs((pattern.getClickFrequency() - avgClickFreq) / stdClickFreq);
      if (zScore > 3) {
        isAnomaly = true;
        maxAnomalyScore = Math.max(maxAnomalyScore, zScore * 20);
      }
    }
    
    // 에러율 이상 감지
    if (pattern.getErrorRate() != null && stdErrorRate != null && stdErrorRate > 0) {
      double zScore = Math.abs((pattern.getErrorRate() - avgErrorRate) / stdErrorRate);
      if (zScore > 3) {
        isAnomaly = true;
        maxAnomalyScore = Math.max(maxAnomalyScore, zScore * 25); // 에러는 가중치 높게
      }
    }
    
    pattern.setIsAnomaly(isAnomaly);
    pattern.setAnomalyScore(Math.min(maxAnomalyScore, 100));
    pattern.setConfidenceLevel(calculateConfidence(pattern.getSampleSize()));
  }
  
  /**
   * 신뢰도 계산 (샘플 크기 기반)
   */
  private double calculateConfidence(Integer sampleSize) {
    if (sampleSize == null || sampleSize < 10) return 0.3;
    if (sampleSize < 50) return 0.5;
    if (sampleSize < 100) return 0.7;
    if (sampleSize < 500) return 0.85;
    return 0.95;
  }
  
  /**
   * 사용자의 최근 패턴 조회
   */
  @Cacheable(value = "recentPatterns", key = "#userId")
  public List<InteractionPattern> getRecentPatterns(Long userId, int days) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    LocalDateTime since = LocalDateTime.now().minusDays(days);
    return patternRepository.findByUserAndAnalysisDateAfterOrderByAnalysisDateDesc(user, since);
  }
  
  /**
   * 이상 패턴 조회
   */
  public List<InteractionPattern> getAnomalousPatterns(Long userId, int days) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    
    LocalDateTime endDate = LocalDateTime.now();
    LocalDateTime startDate = endDate.minusDays(days);
    
    return patternRepository.findAnomalousPatterns(user, startDate, endDate);
  }
}