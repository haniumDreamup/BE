package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.entity.FallEvent;
import com.bifai.reminder.bifai_backend.entity.PoseData;
import com.bifai.reminder.bifai_backend.repository.FallEventRepository;
import com.bifai.reminder.bifai_backend.repository.PoseDataRepository;
import com.bifai.reminder.bifai_backend.service.NotificationService;
import com.bifai.reminder.bifai_backend.service.websocket.WebSocketService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 낙상 감지 서비스
 * MediaPipe Pose 데이터를 분석하여 낙상을 감지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FallDetectionService {
  
  private final PoseDataRepository poseDataRepository;
  private final FallEventRepository fallEventRepository;
  private final NotificationService notificationService;
  private final WebSocketService webSocketService;
  private final ObjectMapper objectMapper;
  
  // 낙상 감지 임계값 (완화된 값)
  private static final float CENTER_Y_THRESHOLD = 0.7f; // 신체 중심 하락 임계값
  private static final float VELOCITY_THRESHOLD = 0.15f; // 속도 임계값 (완화)
  private static final float HORIZONTAL_THRESHOLD = 0.3f; // 수평 판단 임계값
  private static final int NO_MOTION_FRAMES = 30; // 움직임 없음 판단 프레임 수 (1초 @ 30fps)
  private static final float MIN_CONFIDENCE_SCORE = 0.7f; // 최소 신뢰도 (완화)
  
  // 추가 임계값
  private static final float ANGLE_CHANGE_THRESHOLD = 60.0f; // 급격한 각도 변화 (도)
  private static final float POSE_CONFIDENCE_THRESHOLD = 0.5f; // 포즈 감지 최소 신뢰도
  private static final int MIN_STABLE_FRAMES = 10; // 안정 상태 판단 최소 프레임
  
  /**
   * 낙상 감지 실행
   */
  @Transactional
  public Optional<FallEvent> detectFall(PoseData currentFrame) {
    try {
      // 1. 이전 프레임들 조회 (최근 5초)
      List<PoseData> recentFrames = getRecentFrames(
          currentFrame.getUser().getId(), 
          currentFrame.getTimestamp()
      );
      
      if (recentFrames.size() < 30) { // 최소 1초 분량의 데이터 필요
        return Optional.empty();
      }
      
      // 2. 속도 계산
      float velocityY = calculateVelocity(recentFrames, currentFrame);
      currentFrame.setVelocityY(velocityY);
      
      // 3. 자세 분석
      boolean isHorizontal = checkHorizontalPose(currentFrame);
      currentFrame.setIsHorizontal(isHorizontal);
      
      // 4. 움직임 점수 계산
      float motionScore = calculateMotionScore(recentFrames);
      currentFrame.setMotionScore(motionScore);
      
      // 5. 오탐지 필터링
      if (isFalsePositive(currentFrame, recentFrames)) {
        log.debug("오탐지로 판단됨: userId={}, timestamp={}", 
            currentFrame.getUser().getId(), currentFrame.getTimestamp());
        return Optional.empty();
      }
      
      // 6. 낙상 판단
      FallDetectionResult result = analyzeFall(currentFrame, recentFrames, velocityY, isHorizontal, motionScore);
      
      log.debug("낙상 분석 결과 - userId: {}, detected: {}, confidence: {}, velocity: {}, horizontal: {}, motion: {}", 
          currentFrame.getUser().getId(), result.isFallDetected(), result.getConfidence(), 
          velocityY, isHorizontal, motionScore);
      
      if (result.isFallDetected() && result.getConfidence() >= MIN_CONFIDENCE_SCORE) {
        // 7. 중복 감지 체크
        if (isDuplicateFall(currentFrame.getUser().getId())) {
          log.debug("중복 낙상 감지 무시: userId={}", currentFrame.getUser().getId());
          return Optional.empty();
        }
        
        // 8. 낙상 이벤트 생성
        FallEvent fallEvent = createFallEvent(currentFrame, result, recentFrames);
        
        // 9. 알림 전송
        sendFallNotification(fallEvent);
        
        return Optional.of(fallEvent);
      }
      
    } catch (Exception e) {
      log.error("낙상 감지 중 오류", e);
    }
    
    return Optional.empty();
  }
  
  /**
   * 최근 낙상 이벤트 조회
   */
  public List<FallEvent> getRecentFallEvents(Long userId, int hours) {
    LocalDateTime since = LocalDateTime.now().minusHours(hours);
    return fallEventRepository.findByUserIdAndDetectedAtBetween(
        userId, since, LocalDateTime.now()
    );
  }
  
  /**
   * 낙상 이벤트 피드백 업데이트
   */
  @Transactional
  public void updateFallEventFeedback(Long eventId, Boolean isFalsePositive, String userComment) {
    FallEvent event = fallEventRepository.findById(eventId)
        .orElseThrow(() -> new RuntimeException("낙상 이벤트를 찾을 수 없습니다"));
    
    event.setFalsePositive(isFalsePositive);
    event.setUserFeedback(userComment);
    
    if (isFalsePositive) {
      event.setStatus(FallEvent.EventStatus.FALSE_POSITIVE);
    }
    
    fallEventRepository.save(event);
  }
  
  /**
   * 최근 프레임 조회
   */
  private List<PoseData> getRecentFrames(Long userId, LocalDateTime currentTime) {
    LocalDateTime fiveSecondsAgo = currentTime.minusSeconds(5);
    return poseDataRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(
        userId, fiveSecondsAgo, currentTime
    );
  }
  
  /**
   * Y축 속도 계산
   */
  private float calculateVelocity(List<PoseData> recentFrames, PoseData currentFrame) {
    if (recentFrames.isEmpty()) {
      return 0f;
    }
    
    // 가장 최근 프레임과 비교
    PoseData previousFrame = recentFrames.get(0);
    
    // 시간 체크
    long deltaTimeMs = ChronoUnit.MILLIS.between(
        previousFrame.getTimestamp(), 
        currentFrame.getTimestamp()
    );
    
    if (deltaTimeMs <= 0) {
      return 0f;
    }
    
    float deltaY = currentFrame.getCenterY() - previousFrame.getCenterY();
    
    // 속도 = 거리 변화 / 시간 (정규화된 좌표/초)
    // 양수: 하강, 음수: 상승
    return deltaY / (deltaTimeMs / 1000.0f);
  }
  
  /**
   * 수평 자세 확인 (개선)
   */
  private boolean checkHorizontalPose(PoseData poseData) {
    try {
      List<PoseDataDto.LandmarkDto> landmarks = objectMapper.readValue(
          poseData.getLandmarksJson(),
          new TypeReference<List<PoseDataDto.LandmarkDto>>() {}
      );
      
      // 주요 랜드마크 추출
      PoseDataDto.LandmarkDto nose = landmarks.get(0);
      PoseDataDto.LandmarkDto leftShoulder = landmarks.get(11);
      PoseDataDto.LandmarkDto rightShoulder = landmarks.get(12);
      PoseDataDto.LandmarkDto leftHip = landmarks.get(23);
      PoseDataDto.LandmarkDto rightHip = landmarks.get(24);
      PoseDataDto.LandmarkDto leftAnkle = landmarks.get(27);
      PoseDataDto.LandmarkDto rightAnkle = landmarks.get(28);
      
      // 1. 머리와 발의 Y 좌표 차이로 수평 판단
      float avgAnkleY = (leftAnkle.getY() + rightAnkle.getY()) / 2.0f;
      float heightDiff = Math.abs(nose.getY() - avgAnkleY);
      boolean horizontalByHeight = heightDiff < HORIZONTAL_THRESHOLD;
      
      // 2. 어깨와 엉덩이의 Y 좌표 차이로 추가 판단
      float avgShoulderY = (leftShoulder.getY() + rightShoulder.getY()) / 2.0f;
      float avgHipY = (leftHip.getY() + rightHip.getY()) / 2.0f;
      float torsoHeightDiff = Math.abs(avgShoulderY - avgHipY);
      boolean horizontalByTorso = torsoHeightDiff < HORIZONTAL_THRESHOLD * 0.7f;
      
      // 3. 신뢰도가 낮은 랜드마크 체크
      float minVisibility = Math.min(
          Math.min(nose.getVisibility(), leftAnkle.getVisibility()),
          Math.min(rightAnkle.getVisibility(), leftShoulder.getVisibility())
      );
      
      if (minVisibility < POSE_CONFIDENCE_THRESHOLD) {
        return false; // 신뢰도가 낮으면 판단 불가
      }
      
      return horizontalByHeight || horizontalByTorso;
      
    } catch (Exception e) {
      log.error("수평 자세 확인 중 오류", e);
      return false;
    }
  }
  
  /**
   * 움직임 점수 계산
   */
  private float calculateMotionScore(List<PoseData> recentFrames) {
    if (recentFrames.size() < 2) {
      return 1.0f;
    }
    
    float totalMotion = 0f;
    int count = 0;
    
    for (int i = 1; i < Math.min(recentFrames.size(), NO_MOTION_FRAMES); i++) {
      PoseData current = recentFrames.get(i - 1);
      PoseData previous = recentFrames.get(i);
      
      if (current.getCenterY() != null && previous.getCenterY() != null) {
        float motion = Math.abs(current.getCenterY() - previous.getCenterY());
        totalMotion += motion;
        count++;
      }
    }
    
    return count > 0 ? totalMotion / count : 0f;
  }
  
  /**
   * 낙상 분석 (개선된 알고리즘)
   */
  private FallDetectionResult analyzeFall(
      PoseData currentFrame,
      List<PoseData> recentFrames,
      float velocityY,
      boolean isHorizontal,
      float motionScore) {
    
    float confidence = 0f;
    FallEvent.FallSeverity severity = FallEvent.FallSeverity.LOW;
    
    // 포즈 신뢰도 체크
    if (currentFrame.getOverallConfidence() < POSE_CONFIDENCE_THRESHOLD) {
      return new FallDetectionResult(false, 0f, severity);
    }
    
    // 1. 급격한 하강 체크 (가중치 조정)
    boolean rapidDescent = velocityY > VELOCITY_THRESHOLD;
    if (rapidDescent) {
      confidence += 0.3f;
      if (velocityY > VELOCITY_THRESHOLD * 1.5f) {
        confidence += 0.15f; // 매우 빠른 하강
      }
    }
    
    // 2. 낮은 위치 체크
    boolean lowPosition = currentFrame.getCenterY() > CENTER_Y_THRESHOLD;
    if (lowPosition) {
      confidence += 0.25f;
    }
    
    // 3. 수평 자세 체크 (개선)
    if (isHorizontal) {
      confidence += 0.15f;
      if (lowPosition) {
        confidence += 0.15f; // 바닥에 누운 상태
      }
    }
    
    // 4. 움직임 없음 체크 (정교화)
    boolean noMotion = motionScore < 0.01f;
    boolean littleMotion = motionScore < 0.05f;
    
    if (noMotion && lowPosition) {
      confidence += 0.2f;
      severity = FallEvent.FallSeverity.HIGH;
    } else if (littleMotion && lowPosition) {
      confidence += 0.1f;
      severity = FallEvent.FallSeverity.MEDIUM;
    }
    
    // 5. 각도 변화 체크 (추가)
    float angleChange = calculateAngleChange(recentFrames, currentFrame);
    if (angleChange > ANGLE_CHANGE_THRESHOLD) {
      confidence += 0.1f;
    }
    
    // 6. 패턴 기반 분석 (추가)
    if (detectFallPattern(recentFrames, currentFrame)) {
      confidence += 0.1f;
    }
    
    // 7. 심각도 결정 (정교화)
    if (confidence >= 0.85f) {
      severity = FallEvent.FallSeverity.CRITICAL;
    } else if (confidence >= 0.8f && noMotion) {
      severity = FallEvent.FallSeverity.CRITICAL; // 움직임 없으면 위급
    } else if (confidence >= 0.75f) {
      severity = FallEvent.FallSeverity.HIGH;
    } else if (confidence >= 0.7f) {
      severity = FallEvent.FallSeverity.MEDIUM;
    }
    
    // 신뢰도 보정
    confidence = Math.min(confidence, 1.0f);
    
    return new FallDetectionResult(
        confidence >= MIN_CONFIDENCE_SCORE,
        confidence,
        severity
    );
  }
  
  /**
   * 각도 변화 계산
   */
  private float calculateAngleChange(List<PoseData> recentFrames, PoseData currentFrame) {
    if (recentFrames.size() < MIN_STABLE_FRAMES) {
      return 0f;
    }
    
    // 안정 상태의 각도 (1초 전)
    PoseData stableFrame = recentFrames.get(Math.min(30, recentFrames.size() - 1));
    float stableAngle = calculateBodyAngle(stableFrame);
    float currentAngle = calculateBodyAngle(currentFrame);
    
    return Math.abs(currentAngle - stableAngle);
  }
  
  /**
   * 낙상 패턴 감지
   */
  private boolean detectFallPattern(List<PoseData> recentFrames, PoseData currentFrame) {
    if (recentFrames.size() < 15) {
      return false;
    }
    
    // 패턴: 높이 감소 → 급격한 하강 → 정지
    boolean heightDecrease = false;
    boolean rapidDrop = false;
    boolean stopped = false;
    
    // 최근 15프레임 분석 (0.5초)
    for (int i = 0; i < Math.min(15, recentFrames.size() - 1); i++) {
      PoseData prev = recentFrames.get(i + 1);
      PoseData curr = recentFrames.get(i);
      
      if (prev.getCenterY() != null && curr.getCenterY() != null) {
        float deltaY = curr.getCenterY() - prev.getCenterY();
        
        if (deltaY > 0.02f) {
          heightDecrease = true;
        }
        if (deltaY > 0.05f) { // 임계값 완화
          rapidDrop = true;
        }
      }
    }
    
    // 현재 정지 상태 확인 - motionScore가 계산되었는지 확인
    Float motionScore = currentFrame.getMotionScore();
    if (motionScore == null) {
      // motionScore가 없으면 현재와 이전 프레임으로 계산
      if (!recentFrames.isEmpty()) {
        float motion = Math.abs(currentFrame.getCenterY() - recentFrames.get(0).getCenterY());
        stopped = motion < 0.01f;
      }
    } else {
      stopped = motionScore < 0.01f;
    }
    
    return heightDecrease && rapidDrop && stopped;
  }
  
  /**
   * 낙상 이벤트 생성
   */
  private FallEvent createFallEvent(
      PoseData poseData, 
      FallDetectionResult result,
      List<PoseData> recentFrames) {
    
    FallEvent event = FallEvent.builder()
        .user(poseData.getUser())
        .poseSession(poseData.getPoseSession())
        .detectedAt(LocalDateTime.now())
        .severity(result.getSeverity())
        .confidenceScore(result.getConfidence())
        .status(FallEvent.EventStatus.DETECTED)
        .notificationSent(false)
        .falsePositive(false)
        .bodyAngle(calculateBodyAngle(poseData))
        .build();
    
    return fallEventRepository.save(event);
  }
  
  /**
   * 낙상 알림 전송
   */
  private void sendFallNotification(FallEvent fallEvent) {
    try {
      // 1. 기존 알림 서비스로 전송
      notificationService.sendFallAlert(fallEvent);
      
      // 2. WebSocket으로 실시간 알림 전송
      String fallType = determineFallType(fallEvent);
      String severity = translateSeverity(fallEvent.getSeverity());
      webSocketService.broadcastFallAlert(
          fallEvent.getUser().getUserId(),
          fallType,
          severity,
          fallEvent.getConfidenceScore()
      );
      
      fallEvent.setNotificationSent(true);
      fallEvent.setNotificationSentAt(LocalDateTime.now());
      fallEvent.setStatus(FallEvent.EventStatus.NOTIFIED);
      
      fallEventRepository.save(fallEvent);
      
    } catch (Exception e) {
      log.error("낙상 알림 전송 실패", e);
    }
  }
  
  /**
   * 낙상 유형 판단
   */
  private String determineFallType(FallEvent fallEvent) {
    Float angle = fallEvent.getBodyAngle();
    float confidence = fallEvent.getConfidenceScore();
    
    // bodyAngle이 null인 경우 기본값 처리
    if (angle == null) {
      angle = 0.0f;
    }
    
    if (angle > 80.0f && confidence > 0.8f) {
      return "전방 낙상";
    } else if (angle < -80.0f && confidence > 0.8f) {
      return "후방 낙상";
    } else if (Math.abs(angle) < 30.0f) {
      return "측면 낙상";
    } else {
      return "낙상";
    }
  }
  
  /**
   * 심각도 한글 변환
   */
  private String translateSeverity(FallEvent.FallSeverity severity) {
    switch (severity) {
      case CRITICAL: return "위급";
      case HIGH: return "심각";
      case MEDIUM: return "보통";
      case LOW: return "경미";
      default: return "알 수 없음";
    }
  }
  
  /**
   * 신체 각도 계산
   */
  private float calculateBodyAngle(PoseData poseData) {
    try {
      List<PoseDataDto.LandmarkDto> landmarks = objectMapper.readValue(
          poseData.getLandmarksJson(),
          new TypeReference<List<PoseDataDto.LandmarkDto>>() {}
      );
      
      // 어깨와 엉덩이를 연결한 선의 각도 계산
      PoseDataDto.LandmarkDto leftShoulder = landmarks.get(11);
      PoseDataDto.LandmarkDto rightShoulder = landmarks.get(12);
      PoseDataDto.LandmarkDto leftHip = landmarks.get(23);
      PoseDataDto.LandmarkDto rightHip = landmarks.get(24);
      
      float shoulderCenterX = (leftShoulder.getX() + rightShoulder.getX()) / 2.0f;
      float shoulderCenterY = (leftShoulder.getY() + rightShoulder.getY()) / 2.0f;
      float hipCenterX = (leftHip.getX() + rightHip.getX()) / 2.0f;
      float hipCenterY = (leftHip.getY() + rightHip.getY()) / 2.0f;
      
      float deltaX = hipCenterX - shoulderCenterX;
      float deltaY = hipCenterY - shoulderCenterY;
      
      // 라디안을 도로 변환
      return (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
      
    } catch (Exception e) {
      log.error("신체 각도 계산 중 오류", e);
      return 0f;
    }
  }
  
  /**
   * 오탐지 필터링
   */
  private boolean isFalsePositive(PoseData currentFrame, List<PoseData> recentFrames) {
    // 1. 너무 낮은 신뢰도
    Float confidence = currentFrame.getOverallConfidence();
    if (confidence == null || confidence < 0.3f) {
      return true;
    }
    
    // 2. 앉기/눕기 동작 패턴 감지
    if (detectSittingPattern(recentFrames, currentFrame)) {
      return true;
    }
    
    // 3. 운동 동작 패턴 감지
    if (detectExercisePattern(recentFrames)) {
      return true;
    }
    
    // 4. 카메라 각도 급변
    if (detectCameraMovement(recentFrames)) {
      return true;
    }
    
    return false;
  }
  
  /**
   * 앉기 패턴 감지
   */
  private boolean detectSittingPattern(List<PoseData> recentFrames, PoseData currentFrame) {
    if (recentFrames.size() < 60) { // 2초 데이터 필요
      return false;
    }
    
    // 앉기: 천천히 내려가고 일정 높이에서 정지
    int slowDescentCount = 0;
    
    for (int i = 0; i < Math.min(60, recentFrames.size() - 1); i++) {
      PoseData curr = recentFrames.get(i);
      PoseData prev = recentFrames.get(i + 1);
      
      float velocity = (curr.getCenterY() - prev.getCenterY()) / 0.033f; // 30fps 가정
      
      if (velocity > 0 && velocity < 0.1f) { // 천천히 하강
        slowDescentCount++;
      }
    }
    
    // 천천히 내려가고 중간 높이에 있으면 앉기로 판단
    return slowDescentCount > 30 && currentFrame.getCenterY() > 0.4f && currentFrame.getCenterY() < 0.7f;
  }
  
  /**
   * 운동 패턴 감지
   */
  private boolean detectExercisePattern(List<PoseData> recentFrames) {
    if (recentFrames.size() < 90) { // 3초 데이터 필요
      return false;
    }
    
    // 반복적인 상하 움직임 감지
    int upDownCount = 0;
    float prevY = recentFrames.get(0).getCenterY();
    boolean goingUp = false;
    
    for (int i = 1; i < Math.min(90, recentFrames.size()); i++) {
      float currY = recentFrames.get(i).getCenterY();
      
      if (goingUp && currY > prevY + 0.05f) {
        upDownCount++;
        goingUp = false;
      } else if (!goingUp && currY < prevY - 0.05f) {
        upDownCount++;
        goingUp = true;
      }
      
      prevY = currY;
    }
    
    // 3초 동안 3회 이상 반복 움직임이면 운동으로 판단
    return upDownCount >= 6;
  }
  
  /**
   * 카메라 움직임 감지
   */
  private boolean detectCameraMovement(List<PoseData> recentFrames) {
    if (recentFrames.size() < 5) {
      return false;
    }
    
    // 모든 랜드마크가 동시에 같은 방향으로 움직이면 카메라 움직임
    float avgConfidenceDrop = 0f;
    
    for (int i = 0; i < Math.min(5, recentFrames.size() - 1); i++) {
      float currConf = recentFrames.get(i).getOverallConfidence();
      float prevConf = recentFrames.get(i + 1).getOverallConfidence();
      avgConfidenceDrop += (prevConf - currConf);
    }
    
    avgConfidenceDrop /= Math.min(5, recentFrames.size() - 1);
    
    // 급격한 신뢰도 하락은 카메라 움직임
    return avgConfidenceDrop > 0.3f;
  }
  
  /**
   * 중복 낙상 감지 체크
   */
  private boolean isDuplicateFall(Long userId) {
    // 최근 30초 내 낙상 이벤트 확인
    LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
    List<FallEvent> recentFalls = fallEventRepository.findByUserIdAndDetectedAtAfter(
        userId, thirtySecondsAgo
    );
    
    return !recentFalls.isEmpty();
  }
  
  /**
   * 낙상 감지 결과
   */
  private static class FallDetectionResult {
    private final boolean fallDetected;
    private final float confidence;
    private final FallEvent.FallSeverity severity;
    
    public FallDetectionResult(boolean fallDetected, float confidence, FallEvent.FallSeverity severity) {
      this.fallDetected = fallDetected;
      this.confidence = confidence;
      this.severity = severity;
    }
    
    public boolean isFallDetected() {
      return fallDetected;
    }
    
    public float getConfidence() {
      return confidence;
    }
    
    public FallEvent.FallSeverity getSeverity() {
      return severity;
    }
  }
}