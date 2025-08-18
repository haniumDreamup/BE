package com.bifai.reminder.bifai_backend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {
  
  @NotBlank(message = "디바이스 ID는 필수입니다")
  private String deviceId;
  
  @NotBlank(message = "디바이스 이름은 필수입니다")
  private String deviceName;
  
  @NotBlank(message = "디바이스 타입은 필수입니다")
  private String deviceType; // MOBILE, TABLET, WATCH, HUB
  
  private String osVersion;
  private String appVersion;
  
  private NetworkInfo networkInfo;
  private LocationInfo location;
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NetworkInfo {
    private String ipAddress;
    private String wifiSsid;
    private String wifiBssid;
    private String subnet;
    private String gateway;
  }
  
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LocationInfo {
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    private Double accuracy;
    private String address;
  }
}