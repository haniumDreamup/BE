package com.bifai.reminder.bifai_backend.service.mobile;

import com.bifai.reminder.bifai_backend.dto.mobile.MobileLoginRequest;
import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.DeviceRepository;
import com.bifai.reminder.bifai_backend.service.cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 모바일 디바이스 관리 서비스
 * 
 * 사용자의 모바일 디바이스 정보와 푸시 토큰을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MobileDeviceService {
  
  private final DeviceRepository deviceRepository;
  private final RedisCacheService cacheService;
  
  private static final String DEVICE_USER_KEY_PREFIX = "device:user:";
  private static final String DEVICE_PUSH_KEY_PREFIX = "device:push:";
  
  /**
   * 디바이스 등록 또는 업데이트
   * 
   * @param user 사용자
   * @param request 로그인 요청 (디바이스 정보 포함)
   */
  public void registerDevice(User user, MobileLoginRequest request) {
    log.info("디바이스 등록: userId={}, deviceId={}", user.getUserId(), request.getDeviceId());
    
    // 기존 디바이스 조회 또는 새로 생성
    Device device = deviceRepository.findByDeviceIdAndUser(request.getDeviceId(), user)
        .orElseGet(() -> Device.builder()
            .user(user)
            .deviceId(request.getDeviceId())
            .deviceType(request.getDeviceType())
            .isActive(true)
            .build());
    
    // 디바이스 정보 업데이트
    device.setDeviceName(request.getDeviceModel());
    device.setDeviceModel(request.getDeviceModel());
    device.setOsVersion(request.getOsVersion());
    device.setAppVersion(request.getAppVersion());
    device.setLastActiveAt(LocalDateTime.now());
    
    // FCM 토큰이 있으면 업데이트
    if (request.getPushToken() != null) {
      device.setFcmToken(request.getPushToken());
      device.setNotificationsEnabled(true);
    }
    
    deviceRepository.save(device);
    
    // Redis에 디바이스-사용자 매핑 저장
    cacheService.set(
        DEVICE_USER_KEY_PREFIX + request.getDeviceId(),
        user.getUserId().toString(),
        7L,
        TimeUnit.DAYS
    );
    
    log.info("디바이스 등록 완료: deviceId={}", request.getDeviceId());
  }
  
  /**
   * 푸시 토큰 업데이트
   * 
   * @param deviceId 디바이스 ID
   * @param pushToken FCM 토큰
   */
  public void updatePushToken(String deviceId, String pushToken) {
    log.info("푸시 토큰 업데이트: deviceId={}", deviceId);
    
    deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
      device.setFcmToken(pushToken);
      device.setNotificationsEnabled(true);
      deviceRepository.save(device);
      
      // Redis에도 캐시
      cacheService.set(
          DEVICE_PUSH_KEY_PREFIX + deviceId,
          pushToken,
          7L,
          TimeUnit.DAYS
      );
    });
  }
  
  /**
   * 푸시 토큰 제거
   * 
   * @param deviceId 디바이스 ID
   */
  public void removePushToken(String deviceId) {
    log.info("푸시 토큰 제거: deviceId={}", deviceId);
    
    deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
      device.setFcmToken(null);
      device.setNotificationsEnabled(false);
      deviceRepository.save(device);
      
      // Redis에서도 제거
      cacheService.delete(DEVICE_PUSH_KEY_PREFIX + deviceId);
    });
  }
  
  /**
   * 디바이스 ID로 사용자 ID 조회
   * 
   * @param deviceId 디바이스 ID
   * @return 사용자 ID
   */
  public Long getUserIdByDeviceId(String deviceId) {
    // 먼저 Redis에서 확인
    String cachedUserId = (String) cacheService.get(DEVICE_USER_KEY_PREFIX + deviceId);
    if (cachedUserId != null) {
      return Long.parseLong(cachedUserId);
    }
    
    // DB에서 조회
    Optional<Device> device = deviceRepository.findByDeviceId(deviceId);
    if (device.isPresent()) {
      Long userId = device.get().getUser().getUserId();
      
      // Redis에 캐시
      cacheService.set(
          DEVICE_USER_KEY_PREFIX + deviceId,
          userId.toString(),
          7L,
          TimeUnit.DAYS
      );
      
      return userId;
    }
    
    return null;
  }
  
  /**
   * 사용자의 활성 디바이스 목록 조회
   * 
   * @param userId 사용자 ID
   * @return 활성 디바이스 목록
   */
  public java.util.List<Device> getActiveDevices(Long userId) {
    return deviceRepository.findByUser_UserIdAndIsActiveTrue(userId);
  }
  
  /**
   * 디바이스 비활성화
   * 
   * @param deviceId 디바이스 ID
   */
  public void deactivateDevice(String deviceId) {
    log.info("디바이스 비활성화: deviceId={}", deviceId);
    
    deviceRepository.findByDeviceId(deviceId).ifPresent(device -> {
      device.setIsActive(false);
      device.setFcmToken(null);
      device.setNotificationsEnabled(false);
      deviceRepository.save(device);
      
      // Redis에서 제거
      cacheService.delete(DEVICE_USER_KEY_PREFIX + deviceId);
      cacheService.delete(DEVICE_PUSH_KEY_PREFIX + deviceId);
    });
  }
  
  /**
   * 오래된 비활성 디바이스 정리
   * 
   * @param daysInactive 비활성 일수
   */
  @Transactional
  public void cleanupInactiveDevices(int daysInactive) {
    LocalDateTime threshold = LocalDateTime.now().minusDays(daysInactive);
    
    deviceRepository.findByLastActiveAtBeforeAndIsActiveTrue(threshold)
        .forEach(device -> {
          log.info("비활성 디바이스 정리: deviceId={}", device.getDeviceId());
          device.setIsActive(false);
          device.setFcmToken(null);
          deviceRepository.save(device);
        });
  }
}