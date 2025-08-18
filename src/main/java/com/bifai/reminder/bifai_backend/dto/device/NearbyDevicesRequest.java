package com.bifai.reminder.bifai_backend.dto.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDevicesRequest {
  
  private Double radius; // 검색 반경 (미터)
  private String deviceType; // 디바이스 타입 필터
  private Double latitude; // 검색 중심 위도
  private Double longitude; // 검색 중심 경도
  private Boolean includeInactive; // 비활성 디바이스 포함 여부
  private String wifiBssid; // 특정 WiFi 네트워크 필터
}