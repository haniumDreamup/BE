package com.bifai.reminder.bifai_backend.dto.device;

import com.bifai.reminder.bifai_backend.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDiscoveryResponse {
  
  private String deviceId;
  private String deviceName;
  private String deviceType;
  private String ipAddress;
  private String wifiSsid;
  private Double latitude;
  private Double longitude;
  private Boolean isActive;
  private LocalDateTime lastSeen;
  private String userName;
  private Long userId;
  
  // 추가 정보
  private String osVersion;
  private String appVersion;
  private Double distanceMeters; // 현재 위치로부터의 거리
  
  public static DeviceDiscoveryResponse from(Device device) {
    return DeviceDiscoveryResponse.builder()
        .deviceId(device.getDeviceId())
        .deviceName(device.getDeviceName())
        .deviceType(device.getDeviceType())
        .ipAddress(device.getIpAddress())
        .wifiSsid(device.getWifiSsid())
        .latitude(device.getLatitude())
        .longitude(device.getLongitude())
        .isActive(device.getIsActive())
        .lastSeen(device.getLastSeen())
        .userName(device.getUser() != null ? device.getUser().getName() : null)
        .userId(device.getUser() != null ? device.getUser().getId() : null)
        .osVersion(device.getOsVersion())
        .appVersion(device.getAppVersion())
        .build();
  }
}