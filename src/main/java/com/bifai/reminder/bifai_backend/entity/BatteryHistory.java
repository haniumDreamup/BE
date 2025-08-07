package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 배터리 히스토리 엔티티 - 디바이스 배터리 상태 변화 추적
 * BIF 사용자의 디바이스 배터리 관리를 위한 히스토리 로깅
 */
@Entity
@Table(name = "battery_history",
       indexes = {
           @Index(name = "idx_battery_device_time", columnList = "device_device_id, recordedAt"),
           @Index(name = "idx_battery_level", columnList = "batteryLevel"),
           @Index(name = "idx_battery_charging", columnList = "isCharging")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BatteryHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batteryHistoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_device_id", nullable = false)
    private Device device;
    
    @Min(value = 0, message = "배터리 레벨은 0 이상이어야 합니다")
    @Max(value = 100, message = "배터리 레벨은 100 이하여야 합니다")
    @Column(nullable = false)
    private Integer batteryLevel;
    
    @Builder.Default
    private Boolean isCharging = false;
    
    @Builder.Default
    private Boolean isLowBattery = false; // 20% 이하 시 true
    
    @Builder.Default
    private Boolean isCriticalBattery = false; // 5% 이하 시 true
    
    @Column(nullable = false)
    private LocalDateTime recordedAt;
    
    /**
     * 배터리 위험 수준 평가
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BatteryRiskLevel riskLevel = BatteryRiskLevel.NORMAL;
    
    @Column(length = 500)
    private String notes; // 특별한 상황에 대한 메모
    
    /**
     * 배터리 상태 업데이트
     */
    public void updateBatteryStatus(Integer batteryLevel, Boolean isCharging) {
        this.batteryLevel = batteryLevel;
        this.isCharging = isCharging;
        this.recordedAt = LocalDateTime.now();
        
        // 배터리 상태에 따른 플래그 설정
        this.isLowBattery = batteryLevel <= 20;
        this.isCriticalBattery = batteryLevel <= 5;
        
        // 위험 수준 계산
        this.riskLevel = calculateRiskLevel(batteryLevel, isCharging);
    }
    
    /**
     * 배터리 위험 수준 계산
     */
    private BatteryRiskLevel calculateRiskLevel(Integer batteryLevel, Boolean isCharging) {
        if (batteryLevel <= 5) {
            return BatteryRiskLevel.CRITICAL;
        } else if (batteryLevel <= 10) {
            return BatteryRiskLevel.HIGH;
        } else if (batteryLevel <= 20) {
            return BatteryRiskLevel.MEDIUM;
        } else if (batteryLevel <= 30 && !isCharging) {
            return BatteryRiskLevel.LOW;
        }
        return BatteryRiskLevel.NORMAL;
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
        if (!(o instanceof BatteryHistory)) return false;
        BatteryHistory that = (BatteryHistory) o;
        return batteryHistoryId != null && batteryHistoryId.equals(that.batteryHistoryId);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "BatteryHistory{" +
                "batteryHistoryId=" + batteryHistoryId +
                ", batteryLevel=" + batteryLevel +
                ", isCharging=" + isCharging +
                ", riskLevel=" + riskLevel +
                ", recordedAt=" + recordedAt +
                '}';
    }
    
    /**
     * 배터리 위험 수준
     */
    public enum BatteryRiskLevel {
        NORMAL("정상", "문제없음"),
        LOW("낮음", "주의 필요"),
        MEDIUM("보통", "충전 권장"),
        HIGH("높음", "충전 필요"),
        CRITICAL("심각", "즉시 충전 필요");
        
        private final String displayName;
        private final String description;
        
        BatteryRiskLevel(String displayName, String description) {
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