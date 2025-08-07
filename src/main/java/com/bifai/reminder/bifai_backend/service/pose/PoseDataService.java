package com.bifai.reminder.bifai_backend.service.pose;

import com.bifai.reminder.bifai_backend.dto.pose.FallStatusDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseDataDto;
import com.bifai.reminder.bifai_backend.dto.pose.PoseResponseDto;
import com.bifai.reminder.bifai_backend.entity.*;
import com.bifai.reminder.bifai_backend.repository.PoseDataRepository;
import com.bifai.reminder.bifai_backend.repository.PoseSessionRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Pose 데이터 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoseDataService {
  
  private final PoseDataRepository poseDataRepository;
  private final PoseSessionRepository poseSessionRepository;
  private final UserRepository userRepository;
  private final FallDetectionService fallDetectionService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  
  private static final String POSE_BUFFER_KEY = "pose:buffer:";
  private static final int BUFFER_SIZE = 150; // 5초 @ 30fps
  private static final long BUFFER_TTL = 300; // 5분
  
  /**
   * 단일 Pose 데이터 처리
   */
  @Transactional
  public PoseResponseDto processPoseData(PoseDataDto poseDataDto) {
    try {
      // 1. 세션 확인 또는 생성
      PoseSession session = getOrCreateSession(poseDataDto);
      
      // 2. PoseData 엔티티 생성 및 저장
      PoseData poseData = convertAndSavePoseData(poseDataDto, session);
      
      // 3. Redis 버퍼에 추가 (실시간 처리용)
      addToBuffer(poseDataDto);
      
      // 4. 낙상 감지 실행
      Optional<FallEvent> fallEvent = fallDetectionService.detectFall(poseData);
      
      // 5. 결과 구성
      PoseResponseDto.PoseResponseDtoBuilder responseBuilder = PoseResponseDto.builder()
          .sessionId(session.getSessionId())
          .frameCount(session.getTotalFrames())
          .fallDetected(fallEvent.isPresent())
          .message("포즈 데이터가 성공적으로 처리되었습니다");
      
      if (fallEvent.isPresent()) {
        FallEvent event = fallEvent.get();
        responseBuilder
            .fallEventId(event.getId())
            .confidenceScore(event.getConfidenceScore())
            .severity(event.getSeverity().toString())
            .message("낙상이 감지되었습니다! 보호자에게 알림을 전송했습니다.");
      }
      
      return responseBuilder.build();
      
    } catch (Exception e) {
      log.error("Pose 데이터 처리 중 오류", e);
      throw new RuntimeException("Pose 데이터 처리 실패", e);
    }
  }
  
  /**
   * 일괄 Pose 데이터 처리
   */
  @Transactional
  public List<PoseResponseDto> processPoseDataBatch(List<PoseDataDto> poseDataList) {
    if (poseDataList.isEmpty()) {
      return new ArrayList<>();
    }
    
    List<PoseResponseDto> responses = new ArrayList<>();
    
    try {
      // 1. 세션 확인
      PoseSession session = getOrCreateSession(poseDataList.get(0));
      
      // 2. 각 프레임 처리
      for (int i = 0; i < poseDataList.size(); i++) {
        PoseDataDto dto = poseDataList.get(i);
        
        // PoseData 엔티티 생성 및 저장
        PoseData poseData = convertAndSavePoseData(dto, session);
        
        // 마지막 프레임에서만 낙상 감지
        Optional<FallEvent> fallEvent = Optional.empty();
        if (i == poseDataList.size() - 1) {
          fallEvent = fallDetectionService.detectFall(poseData);
        }
        
        // 응답 생성
        PoseResponseDto.PoseResponseDtoBuilder responseBuilder = PoseResponseDto.builder()
            .sessionId(session.getSessionId())
            .frameCount(session.getTotalFrames() + i + 1)
            .fallDetected(fallEvent.isPresent())
            .message("프레임 처리 완료");
        
        if (fallEvent.isPresent()) {
          FallEvent event = fallEvent.get();
          responseBuilder
              .fallEventId(event.getId())
              .confidenceScore(event.getConfidenceScore())
              .severity(event.getSeverity().toString())
              .message("낙상이 감지되었습니다!");
        }
        
        responses.add(responseBuilder.build());
      }
      
      // 세션 프레임 수 업데이트
      session.setTotalFrames(session.getTotalFrames() + poseDataList.size());
      poseSessionRepository.save(session);
      
    } catch (Exception e) {
      log.error("Pose 데이터 일괄 처리 중 오류", e);
      throw new RuntimeException("Pose 데이터 일괄 처리 실패", e);
    }
    
    return responses;
  }
  
  /**
   * 낙상 감지 상태 조회
   */
  public Map<String, Object> getFallDetectionStatus(Long userId) {
    Map<String, Object> status = new HashMap<>();
    
    try {
      // Redis에서 실시간 상태 확인
      String bufferKey = POSE_BUFFER_KEY + userId;
      List<Object> recentData = redisTemplate.opsForList().range(bufferKey, -10, -1);
      
      status.put("userId", userId);
      status.put("monitoring", recentData != null && !recentData.isEmpty());
      status.put("lastUpdateTime", recentData != null && !recentData.isEmpty() ? 
          LocalDateTime.now() : null);
      
      // 최근 낙상 이벤트 조회
      List<FallEvent> recentFalls = fallDetectionService.getRecentFallEvents(userId, 24);
      status.put("recentFalls", recentFalls.stream()
          .map(this::mapFallEventToDto)
          .collect(Collectors.toList()));
      
    } catch (Exception e) {
      log.error("낙상 상태 조회 중 오류", e);
      throw new RuntimeException("낙상 상태 조회 실패", e);
    }
    
    return status;
  }
  
  /**
   * 낙상 감지 피드백 처리
   */
  @Transactional
  public void processFeedback(Map<String, Object> feedback) {
    Long eventId = Long.valueOf(feedback.get("eventId").toString());
    Boolean isFalsePositive = (Boolean) feedback.get("falsePositive");
    String userComment = (String) feedback.get("comment");
    
    fallDetectionService.updateFallEventFeedback(eventId, isFalsePositive, userComment);
  }
  
  /**
   * 세션 확인 또는 생성
   */
  private PoseSession getOrCreateSession(PoseDataDto poseDataDto) {
    String sessionId = poseDataDto.getSessionId();
    
    if (sessionId == null || sessionId.isEmpty()) {
      sessionId = UUID.randomUUID().toString();
    }
    
    final String finalSessionId = sessionId; // Make it effectively final
    
    return poseSessionRepository.findBySessionId(finalSessionId)
        .orElseGet(() -> {
          User user = userRepository.findById(poseDataDto.getUserId())
              .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
          
          PoseSession newSession = PoseSession.builder()
              .sessionId(finalSessionId)
              .user(user)
              .startTime(LocalDateTime.now())
              .status(PoseSession.SessionStatus.ACTIVE)
              .totalFrames(0)
              .build();
          
          return poseSessionRepository.save(newSession);
        });
  }
  
  /**
   * PoseData 변환 및 저장
   */
  private PoseData convertAndSavePoseData(PoseDataDto dto, PoseSession session) {
    try {
      // 랜드마크 데이터를 JSON으로 변환
      String landmarksJson = objectMapper.writeValueAsString(dto.getLandmarks());
      
      // 신체 중심점 계산
      float centerY = calculateCenterY(dto.getLandmarks());
      
      PoseData poseData = PoseData.builder()
          .user(session.getUser())
          .poseSession(session)
          .timestamp(dto.getTimestamp())
          .frameNumber(dto.getFrameNumber())
          .landmarksJson(landmarksJson)
          .overallConfidence(dto.getOverallConfidence())
          .centerY(centerY)
          .build();
      
      return poseDataRepository.save(poseData);
      
    } catch (Exception e) {
      log.error("PoseData 변환 중 오류", e);
      throw new RuntimeException("PoseData 변환 실패", e);
    }
  }
  
  /**
   * Redis 버퍼에 데이터 추가
   */
  private void addToBuffer(PoseDataDto poseDataDto) {
    String bufferKey = POSE_BUFFER_KEY + poseDataDto.getUserId();
    
    try {
      redisTemplate.opsForList().rightPush(bufferKey, poseDataDto);
      
      // 버퍼 크기 제한
      Long size = redisTemplate.opsForList().size(bufferKey);
      if (size != null && size > BUFFER_SIZE) {
        redisTemplate.opsForList().leftPop(bufferKey);
      }
      
      // TTL 설정
      redisTemplate.expire(bufferKey, BUFFER_TTL, TimeUnit.SECONDS);
      
    } catch (Exception e) {
      log.warn("Redis 버퍼 업데이트 실패", e);
    }
  }
  
  /**
   * 신체 중심점 Y 좌표 계산
   */
  private float calculateCenterY(List<PoseDataDto.LandmarkDto> landmarks) {
    // 엉덩이 중심점 계산 (LEFT_HIP + RIGHT_HIP) / 2
    PoseDataDto.LandmarkDto leftHip = landmarks.get(23);
    PoseDataDto.LandmarkDto rightHip = landmarks.get(24);
    
    return (leftHip.getY() + rightHip.getY()) / 2.0f;
  }
  
  /**
   * FallEvent를 DTO로 변환
   */
  private Map<String, Object> mapFallEventToDto(FallEvent event) {
    Map<String, Object> dto = new HashMap<>();
    dto.put("id", event.getId());
    dto.put("detectedAt", event.getDetectedAt());
    dto.put("severity", event.getSeverity());
    dto.put("status", event.getStatus());
    dto.put("confidence", event.getConfidenceScore());
    return dto;
  }
  
  /**
   * 낙상 피드백 제출
   */
  public void submitFallFeedback(Long eventId, Boolean isFalsePositive, String userComment) {
    fallDetectionService.updateFallEventFeedback(eventId, isFalsePositive, userComment);
  }
  
  /**
   * 사용자의 낙상 상태 조회
   */
  public FallStatusDto getFallStatus(Long userId) {
    // 현재 활성 세션 확인
    Optional<PoseSession> activeSession = poseSessionRepository.findByUserIdAndStatus(
        userId, PoseSession.SessionStatus.ACTIVE
    );
    
    // 최근 24시간 낙상 이벤트
    List<FallEvent> recentEvents = fallDetectionService.getRecentFallEvents(userId, 24);
    
    // FallEventDto로 변환
    List<FallStatusDto.FallEventDto> fallEventDtos = recentEvents.stream()
        .map(event -> FallStatusDto.FallEventDto.builder()
            .eventId(event.getId())
            .detectedAt(event.getDetectedAt())
            .severity(event.getSeverity())
            .confidenceScore(event.getConfidenceScore())
            .status(event.getStatus())
            .falsePositive(event.getFalsePositive())
            .build())
        .collect(Collectors.toList());
    
    return FallStatusDto.builder()
        .userId(userId)
        .lastChecked(LocalDateTime.now())
        .recentFallEvents(fallEventDtos)
        .isMonitoring(activeSession.isPresent())
        .sessionActive(activeSession.isPresent())
        .currentSessionId(activeSession.map(PoseSession::getSessionId).orElse(null))
        .build();
  }
}