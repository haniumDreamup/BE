package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 디바이스 Repository
 * BIF 사용자의 디바이스 정보 관리를 위한 데이터 접근 계층
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    
    /**
     * 사용자의 모든 디바이스 조회
     */
    List<Device> findByUser_UserId(Long userId);
    
    /**
     * 사용자의 활성 디바이스 조회
     */
    List<Device> findByUser_UserIdAndIsActiveTrue(Long userId);
    
    /**
     * 디바이스 식별자로 조회
     */
    Optional<Device> findByDeviceIdentifier(String deviceIdentifier);
    
    /**
     * 푸시 토큰으로 디바이스 조회
     */
    Optional<Device> findByPushToken(String pushToken);
    
    /**
     * 특정 시간 이후 동기화되지 않은 디바이스 조회
     */
    @Query("SELECT d FROM Device d WHERE d.lastSyncAt < :threshold AND d.isActive = true")
    List<Device> findInactiveDevices(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 사용자의 주 디바이스 조회 (가장 최근 동기화된 디바이스)
     */
    @Query("SELECT d FROM Device d WHERE d.user.userId = :userId AND d.isActive = true " +
           "ORDER BY d.lastSyncAt DESC")
    Optional<Device> findPrimaryDeviceByUserId(@Param("userId") Long userId);
    
    /**
     * 배터리 부족 디바이스 조회
     */
    @Query("SELECT d FROM Device d WHERE d.batteryLevel < :threshold AND d.isActive = true")
    List<Device> findLowBatteryDevices(@Param("threshold") Integer threshold);
    
    /**
     * 디바이스 존재 여부 확인
     */
    boolean existsByDeviceIdentifier(String deviceIdentifier);
    
    /**
     * 사용자의 디바이스 개수 조회
     */
    long countByUser_UserIdAndIsActiveTrue(Long userId);
    
    // 테스트에서 사용하는 추가 메소드들
    
    /**
     * 사용자의 모든 디바이스 조회
     */
    List<Device> findByUser(User user);
    
    /**
     * 사용자의 활성 디바이스 조회
     */
    List<Device> findByUserAndIsActiveTrue(User user);
    
    /**
     * 오래된 동기화 디바이스 조회
     */
    @Query("SELECT d FROM Device d WHERE d.lastSyncAt < :threshold")
    List<Device> findDevicesNotSyncedSince(@Param("threshold") LocalDateTime threshold);
    
    /**
     * 디바이스 타입별 조회
     */
    List<Device> findByDeviceType(String deviceType);
    
    /**
     * 사용자별 디바이스 수 조회
     */
    long countByUser(User user);
    
    /**
     * 디바이스 ID로 조회
     */
    Optional<Device> findByDeviceId(String deviceId);
    
    /**
     * 디바이스 ID와 사용자로 조회
     */
    Optional<Device> findByDeviceIdAndUser(String deviceId, User user);
    
    /**
     * 마지막 활동 시간 이전 활성 디바이스 조회
     */
    List<Device> findByLastActiveAtBeforeAndIsActiveTrue(LocalDateTime threshold);
    
    /**
     * 활성 디바이스 조회
     */
    List<Device> findByIsActiveTrue();
    
    /**
     * 배터리 레벨이 특정 값 이하이고 활성인 디바이스 조회
     */
    List<Device> findByBatteryLevelLessThanAndIsActiveTrue(Integer batteryLevel);
    
    /**
     * 마지막 동기화 시간이 특정 시간 이전이고 활성인 디바이스 조회
     */
    List<Device> findByLastSyncAtBeforeAndIsActiveTrue(LocalDateTime threshold);
    
    /**
     * 사용자 ID와 디바이스 ID로 조회
     */
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.deviceId = :deviceId")
    Optional<Device> findByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
    
    /**
     * 사용자 ID로 모든 디바이스 조회
     */
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId")
    List<Device> findByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 활성 디바이스 조회 (FCM 토큰 있는 것만)
     */
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.isActive = true")
    List<Device> findActiveDevicesByUserId(@Param("userId") Long userId);
    
    /**
     * 사용자의 가장 최근 활성 디바이스 조회
     */
    @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.isActive = true ORDER BY d.lastSeen DESC")
    Optional<Device> findActiveDeviceByUserId(@Param("userId") Long userId);
    
    /**
     * 같은 WiFi 네트워크의 디바이스 조회
     */
    @Query("SELECT d FROM Device d WHERE d.wifiBssid = :bssid AND d.isActive = true")
    List<Device> findByWifiBssid(@Param("bssid") String bssid);
    
    /**
     * 위치 기반 근처 디바이스 조회 (Haversine 공식 사용)
     */
    @Query("SELECT d FROM Device d WHERE d.isActive = true AND " +
           "(6371000 * acos(cos(radians(:lat)) * cos(radians(d.latitude)) * " +
           "cos(radians(d.longitude) - radians(:lng)) + " +
           "sin(radians(:lat)) * sin(radians(d.latitude)))) <= :radius")
    List<Device> findNearbyDevices(@Param("lat") Double latitude, 
                                   @Param("lng") Double longitude, 
                                   @Param("radius") Double radius);
    
    
    /**
     * 보호자-피보호자 관계 디바이스 조회
     */
    @Query("SELECT DISTINCT d FROM Device d " +
           "JOIN Guardian g ON (g.guardian.id = :userId OR g.ward.id = :userId) " +
           "WHERE (d.user.id = g.guardian.id OR d.user.id = g.ward.id) " +
           "AND d.user.id != :userId " +
           "AND d.isActive = true")
    List<Device> findGuardianRelatedDevices(@Param("userId") Long userId);
}