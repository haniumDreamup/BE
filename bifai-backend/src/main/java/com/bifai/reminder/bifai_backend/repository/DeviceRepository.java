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
}