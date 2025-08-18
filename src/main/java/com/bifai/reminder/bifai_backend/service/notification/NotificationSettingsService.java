package com.bifai.reminder.bifai_backend.service.notification;

import com.bifai.reminder.bifai_backend.dto.notification.NotificationSettingsDto;
import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.DeviceRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {
  
  private final UserRepository userRepository;
  private final DeviceRepository deviceRepository;
  private final FcmService fcmService;
  
  /**
   * FCM 토큰 업데이트
   */
  @Transactional
  public void updateFcmToken(Long userId, String deviceId, String fcmToken) {
    // 디바이스 조회 또는 생성
    Device device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
        .orElseGet(() -> {
          Device newDevice = Device.builder()
              .user(userRepository.getReferenceById(userId))
              .deviceId(deviceId)
              .deviceName("Mobile Device")
              .deviceType("MOBILE")
              .isActive(true)
              .build();
          return newDevice;
        });
    
    // FCM 토큰 검증
    if (!fcmService.validateToken(fcmToken)) {
      throw new IllegalArgumentException("유효하지 않은 FCM 토큰입니다");
    }
    
    // 토큰 업데이트
    device.setFcmToken(fcmToken);
    device.setActive(true);
    deviceRepository.save(device);
    
    log.info("FCM 토큰 업데이트 - userId: {}, deviceId: {}", userId, deviceId);
  }
  
  /**
   * FCM 토큰 제거
   */
  @Transactional
  public void removeFcmToken(Long userId, String deviceId) {
    deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
        .ifPresent(device -> {
          device.setFcmToken(null);
          device.setActive(false);
          deviceRepository.save(device);
          log.info("FCM 토큰 제거 - userId: {}, deviceId: {}", userId, deviceId);
        });
  }
  
  /**
   * 알림 설정 조회
   */
  @Transactional(readOnly = true)
  public NotificationSettingsDto getSettings(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // TODO: 실제 사용자 설정 데이터베이스에서 가져오기
    // 현재는 기본값 반환
    return NotificationSettingsDto.getDefault();
  }
  
  /**
   * 알림 설정 업데이트
   */
  @Transactional
  public NotificationSettingsDto updateSettings(Long userId, NotificationSettingsDto settings) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
    
    // TODO: 사용자 설정 데이터베이스에 저장
    // 현재는 받은 설정 그대로 반환
    
    log.info("알림 설정 업데이트 - userId: {}", userId);
    return settings;
  }
  
  /**
   * 모든 알림 비활성화
   */
  @Transactional
  public void disableAllNotifications(Long userId) {
    // 모든 디바이스의 FCM 토큰 비활성화
    deviceRepository.findByUserId(userId).forEach(device -> {
      device.setActive(false);
      deviceRepository.save(device);
    });
    
    log.info("모든 알림 비활성화 - userId: {}", userId);
  }
  
  /**
   * 특정 디바이스 활성화 상태 변경
   */
  @Transactional
  public void setDeviceActive(Long userId, String deviceId, boolean active) {
    deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
        .ifPresent(device -> {
          device.setActive(active);
          deviceRepository.save(device);
          log.info("디바이스 상태 변경 - userId: {}, deviceId: {}, active: {}", 
              userId, deviceId, active);
        });
  }
}