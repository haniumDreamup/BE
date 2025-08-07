package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 연결 로그 엔티티 - 디바이스 연결 상태 변화 추적
 * BIF 사용자의 디바이스 온라인/오프라인 상태 모니터링
 */
@Entity
@Table(name = "connectivity_logs",
       indexes = {
           @Index(name = "idx_connectivity_device_time", columnList = "device_device_id, timestamp"),
           @Index(name = "idx_connectivity_status", columnList = "connectionStatus"),
           @Index(name = "idx_connectivity_duration", columnList = "durationSeconds")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConnectivityLog extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long connectivityLogId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_device_id", nullable = false)
    private Device device;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus connectionStatus;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * 연결 지속 시간 (초 단위)
     */
    private Long durationSeconds;
    
    /**
     * 신호 강도 (0-100)
     */
    private Integer signalStrength;
    
    /**
     * 네트워크 타입
     */
    @Enumerated(EnumType.STRING)
    private NetworkType networkType;
    
    /**
     * 연결 품질 점수 (1-10)
     */
    private Integer qualityScore;
    
    /**
     * 연결 해제 이유
     */
    @Enumerated(EnumType.STRING)
    private DisconnectionReason disconnectionReason;
    
    @Column(length = 1000)
    private String errorMessage; // 연결 오류 시 메시지
    
    @Column(length = 500)
    private String notes; // 추가 메모
    
    /**
     * IP 주소 (보안상 해시화 저장 고려)
     */
    @Column(length = 45)
    private String ipAddress;
    
    /**
     * 연결 시점의 위치 위도
     */
    @Column(name = "connectivity_latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * 연결 시점의 위치 경도
     */
    @Column(name = "connectivity_longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    /**
     * 연결 상태 업데이트
     */
    public void updateConnectionStatus(ConnectionStatus status, LocalDateTime timestamp) {
        this.connectionStatus = status;
        this.timestamp = timestamp;
    }
    
    /**
     * 연결 지속 시간 계산 및 설정
     */
    public void calculateAndSetDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            this.durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
    
    /**
     * 연결 품질 업데이트
     */
    public void updateQuality(Integer signalStrength, Integer qualityScore) {
        this.signalStrength = signalStrength;
        this.qualityScore = qualityScore;
    }
    
    /**
     * 연결 해제 정보 설정
     */
    public void setDisconnectionInfo(DisconnectionReason reason, String errorMessage) {
        this.disconnectionReason = reason;
        this.errorMessage = errorMessage;
    }
    
    /**
     * 노트 업데이트
     */
    public void updateNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectivityLog)) return false;
        ConnectivityLog that = (ConnectivityLog) o;
        return connectivityLogId != null && connectivityLogId.equals(that.connectivityLogId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "ConnectivityLog{" +
                "connectivityLogId=" + connectivityLogId +
                ", connectionStatus=" + connectionStatus +
                ", timestamp=" + timestamp +
                ", durationSeconds=" + durationSeconds +
                ", qualityScore=" + qualityScore +
                '}';
    }
    
    /**
     * 연결 상태 (Device 엔티티와 동일하게 확장)
     */
    public enum ConnectionStatus {
        ONLINE("온라인", "정상 연결"),
        OFFLINE("오프라인", "연결 끊김"),
        SLEEP("절전모드", "저전력 모드"),
        CONNECTING("연결중", "연결 시도 중"),
        DISCONNECTING("연결해제중", "연결 해제 중"),
        ERROR("오류", "연결 오류 발생");
        
        private final String displayName;
        private final String description;
        
        ConnectionStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 네트워크 타입
     */
    public enum NetworkType {
        WIFI("WiFi", "무선 네트워크"),
        CELLULAR_4G("4G", "4G 모바일 네트워크"),
        CELLULAR_5G("5G", "5G 모바일 네트워크"),
        BLUETOOTH("Bluetooth", "블루투스 연결"),
        ETHERNET("Ethernet", "유선 네트워크"),
        UNKNOWN("알 수 없음", "알 수 없는 네트워크");
        
        private final String displayName;
        private final String description;
        
        NetworkType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 연결 해제 이유
     */
    public enum DisconnectionReason {
        USER_REQUEST("사용자 요청", "사용자가 직접 연결 해제"),
        NETWORK_ERROR("네트워크 오류", "네트워크 문제로 인한 연결 해제"),
        TIMEOUT("시간 초과", "응답 시간 초과"),
        LOW_BATTERY("배터리 부족", "배터리 부족으로 인한 절전 모드"),
        SIGNAL_WEAK("신호 약함", "신호 강도 부족"),
        DEVICE_ERROR("디바이스 오류", "디바이스 하드웨어 문제"),
        SERVER_MAINTENANCE("서버 점검", "서버 점검으로 인한 연결 해제"),
        UNKNOWN("알 수 없음", "알 수 없는 이유");
        
        private final String displayName;
        private final String description;
        
        DisconnectionReason(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 