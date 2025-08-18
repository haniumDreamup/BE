package com.bifai.reminder.bifai_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 위치 기록 엔티티 - BIF 사용자의 위치 이력 관리
 * 위치 추적 및 패턴 분석을 위한 독립적인 위치 데이터 저장
 */
@Entity
@Table(name = "location_history",
       indexes = {
           @Index(name = "idx_location_history_user_time", columnList = "user_id, captured_at DESC"),
           @Index(name = "idx_location_history_device", columnList = "device_id"),
           @Index(name = "idx_location_history_type", columnList = "location_type"),
           @Index(name = "idx_location_history_coordinates", columnList = "latitude, longitude")
       })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LocationHistory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;
    
    @NotNull
    @Column(precision = 10, nullable = false)
    private BigDecimal latitude;
    
    @NotNull
    @Column(precision = 11, nullable = false)
    private BigDecimal longitude;
    
    @Column(precision = 8)
    private BigDecimal accuracy; // 미터 단위
    
    @Column(precision = 8)
    private BigDecimal altitude; // 미터 단위
    
    @Column(precision = 6)
    private BigDecimal speed; // m/s
    
    @Column(precision = 5)
    private BigDecimal heading; // 도(degree)
    
    @Column(name = "location_type", length = 30)
    @Enumerated(EnumType.STRING)
    private LocationType locationType;
    
    @Column(length = 500)
    private String address;
    
    @NotNull
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;
    
    /**
     * 위치 정보 업데이트
     */
    public void updateLocation(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.capturedAt = LocalDateTime.now();
    }
    
    /**
     * 주소 정보 업데이트
     */
    public void updateAddress(String address, LocationType locationType) {
        this.address = address;
        this.locationType = locationType;
    }
    
    /**
     * 이동 정보 업데이트
     */
    public void updateMovementInfo(BigDecimal speed, BigDecimal heading, BigDecimal altitude) {
        this.speed = speed;
        this.heading = heading;
        this.altitude = altitude;
    }
    
    /**
     * 위치 간 거리 계산 (Haversine formula)
     * @param otherLocation 다른 위치
     * @return 거리 (미터)
     */
    public double calculateDistance(LocationHistory otherLocation) {
        if (otherLocation == null) return 0;
        
        double earthRadius = 6371000; // 미터
        double lat1Rad = Math.toRadians(this.latitude.doubleValue());
        double lat2Rad = Math.toRadians(otherLocation.latitude.doubleValue());
        double deltaLat = Math.toRadians(otherLocation.latitude.doubleValue() - this.latitude.doubleValue());
        double deltaLon = Math.toRadians(otherLocation.longitude.doubleValue() - this.longitude.doubleValue());
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
    
    /**
     * 위치 정보를 문자열로 반환
     */
    public String toLocationString() {
        return String.format("%.6f,%.6f", latitude.doubleValue(), longitude.doubleValue());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationHistory)) return false;
        LocationHistory other = (LocationHistory) o;
        return id != null && id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("Locations{id=%d, lat=%s, lng=%s, type=%s, capturedAt=%s}",
                id, latitude, longitude, locationType, capturedAt);
    }
    
    /**
     * 위치 유형
     */
    public enum LocationType {
        HOME("집"),
        WORK("직장"),
        SCHOOL("학교"),
        HOSPITAL("병원"),
        PHARMACY("약국"),
        RESTAURANT("식당"),
        SHOP("상점"),
        PARK("공원"),
        TRANSIT("이동중"),
        OUTDOOR("야외"),
        INDOOR("실내"),
        UNKNOWN("알수없음");
        
        private final String displayName;
        
        LocationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}