package com.bifai.reminder.bifai_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 디바이스 엔티티 - 사용자가 사용하는 디바이스 정보 관리
 */
@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Device extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @Column(name = "device_id", nullable = false, unique = true, length = 255)
    private String deviceId; // 모바일 디바이스 고유 ID
    
    @Column(name = "device_identifier", unique = true, length = 255)
    private String deviceIdentifier; // MAC 주소 등 고유 식별자
    
    @Column(name = "device_serial_number", length = 100)
    private String deviceSerialNumber;
    
    @Column(name = "device_model", length = 100)
    private String deviceModel;
    
    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;
    
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;
    
    @Column(length = 100)
    private String manufacturer;
    
    @Column(length = 100)
    private String model;
    
    @Column(name = "os_type", length = 50)
    private String osType;
    
    @Column(name = "os_version", length = 50)
    private String osVersion;
    
    @Column(name = "app_version", length = 50)
    private String appVersion;
    
    @Column(name = "firmware_version", length = 50)
    private String firmwareVersion;
    
    @Column(name = "push_token", length = 500)
    private String pushToken;
    
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;
    
    @Column(name = "notifications_enabled")
    @Builder.Default
    private Boolean notificationsEnabled = true;
    
    @Column(name = "battery_level")
    private Integer batteryLevel;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @Column(name = "pairing_code", length = 20)
    private String pairingCode;
    
    @Column(name = "paired_at")
    private LocalDateTime pairedAt;
    
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
    
    // 네트워크 정보
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "wifi_ssid", length = 100)
    private String wifiSsid;
    
    @Column(name = "wifi_bssid", length = 50)
    private String wifiBssid;
    
    // 위치 정보
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    // 추가 정보
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    // 관계 매핑
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BatteryHistory> batteryHistories;
    
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConnectivityLog> connectivityLogs;
    
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LocationHistory> locationHistories;
    
    // CapturedImage는 User와 직접 연결되어 있으므로 Device와의 관계 제거
    
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activityLogs;
    
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HealthMetric> healthMetrics;
    
    /**
     * 배터리 레벨 업데이트
     */
    public void updateBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
    
    /**
     * 마지막 동기화 시간 업데이트
     */
    public void updateLastSync() {
        this.lastSyncAt = LocalDateTime.now();
    }
    
    /**
     * 푸시 토큰 업데이트
     */
    public void updatePushToken(String pushToken) {
        this.pushToken = pushToken;
    }
    
    /**
     * 페어링 완료 처리
     */
    public void completePairing() {
        this.pairedAt = LocalDateTime.now();
        this.pairingCode = null; // 페어링 코드 초기화
        this.isActive = true;
    }
    
    /**
     * 디바이스 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * 활성화 상태 설정
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * 활성화 상태 조회
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
    
    
    /**
     * 베스트 프랙티스: ID 기반 equals 구현
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;
        Device other = (Device) o;
        
        // id가 null인 경우 객체 참조로만 비교
        if (this.id == null || other.id == null) {
            return false;
        }
        
        return this.id.equals(other.id);
    }
    
    /**
     * 베스트 프랙티스: 안정적인 hashCode 구현
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
    
    /**
     * 베스트 프랙티스: 간결한 toString (연관관계 제외)
     */
    @Override
    public String toString() {
        return String.format("Device{id=%d, identifier='%s', name='%s', type='%s'}", 
                           id, deviceIdentifier, deviceName, deviceType);
    }
    
} 