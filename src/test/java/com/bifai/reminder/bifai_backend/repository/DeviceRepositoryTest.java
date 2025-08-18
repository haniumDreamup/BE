package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * DeviceRepository 테스트
 * BIF 사용자의 디바이스 관리 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DeviceRepository 테스트")
class DeviceRepositoryTest {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private User testUser;
    private Device testDevice;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        TestDataFactory.resetCounter();
        
        // Role 생성 및 영속화
        Role userRole = TestDataFactory.createRole();
        entityManager.persistAndFlush(userRole);
        
        // User 생성 및 영속화
        testUser = TestDataFactory.createUserWithRole(userRole);
        testUser = entityManager.persistAndFlush(testUser);
        
        // Device 생성 (아직 영속화 안함)
        testDevice = TestDataFactory.createDevice(testUser);
        
        // 영속성 컨텍스트 초기화
        entityManager.clear();
    }
    
    @Test
    @DisplayName("디바이스 저장 - 성공")
    void saveDevice_Success() {
        // when
        Device savedDevice = deviceRepository.save(testDevice);
        entityManager.flush();
        
        // then
        assertThat(savedDevice.getId()).isNotNull();
        assertThat(savedDevice.getDeviceIdentifier()).isEqualTo(testDevice.getDeviceIdentifier());
        assertThat(savedDevice.getDeviceName()).startsWith("테스트기기");
        assertThat(savedDevice.getDeviceType()).isEqualTo("WEARABLE");
        assertThat(savedDevice.getUser().getUserId()).isEqualTo(testUser.getUserId());
    }
    
    @Test
    @DisplayName("디바이스 조회 - ID로 조회")
    void findById_Success() {
        // given
        Device savedDevice = entityManager.persistAndFlush(testDevice);
        entityManager.clear();
        
        // when
        Optional<Device> foundDevice = deviceRepository.findById(savedDevice.getId());
        
        // then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getDeviceSerialNumber()).startsWith("SN-");
        assertThat(foundDevice.get().getBatteryLevel()).isEqualTo(85);
    }
    
    @Test
    @DisplayName("사용자별 디바이스 조회")
    void findByUser_Success() {
        // given
        Device device1 = entityManager.persistAndFlush(testDevice);
        
        Device device2 = TestDataFactory.createDevice(testUser);
        device2.setDeviceType("TABLET");
        device2 = entityManager.persistAndFlush(device2);
        
        entityManager.clear();
        
        // when
        List<Device> devices = deviceRepository.findByUser(testUser);
        
        // then
        assertThat(devices).hasSize(2);
        assertThat(devices).extracting("deviceType")
                .containsExactlyInAnyOrder("WEARABLE", "TABLET");
    }
    
    @Test
    @DisplayName("디바이스 식별자로 조회")
    void findByDeviceIdentifier_Success() {
        // given
        Device savedDevice = entityManager.persistAndFlush(testDevice);
        entityManager.clear();
        
        // when
        Optional<Device> foundDevice = deviceRepository.findByDeviceIdentifier(savedDevice.getDeviceIdentifier());
        
        // then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getId()).isEqualTo(savedDevice.getId());
    }
    
    @Test
    @DisplayName("푸시 토큰으로 조회")
    void findByPushToken_Success() {
        // given
        testDevice.setPushToken("test-push-token-123");
        Device savedDevice = entityManager.persistAndFlush(testDevice);
        entityManager.clear();
        
        // when
        Optional<Device> foundDevice = deviceRepository.findByPushToken("test-push-token-123");
        
        // then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getId()).isEqualTo(savedDevice.getId());
    }
    
    @Test
    @DisplayName("활성 디바이스 조회")
    void findActiveDevices_Success() {
        // given
        Device activeDevice = entityManager.persistAndFlush(testDevice);
        
        Device inactiveDevice = TestDataFactory.createDevice(testUser);
        inactiveDevice.setIsActive(false);
        entityManager.persistAndFlush(inactiveDevice);
        
        entityManager.clear();
        
        // when
        List<Device> activeDevices = deviceRepository.findByIsActiveTrue();
        
        // then
        assertThat(activeDevices).hasSize(1);
        assertThat(activeDevices.get(0).getId()).isEqualTo(activeDevice.getId());
    }
    
    @Test
    @DisplayName("배터리 부족 디바이스 조회")
    void findLowBatteryDevices_Success() {
        // given
        testDevice.setBatteryLevel(15);
        Device lowBatteryDevice = entityManager.persistAndFlush(testDevice);
        
        Device normalDevice = TestDataFactory.createDevice(testUser);
        normalDevice.setBatteryLevel(90);
        entityManager.persistAndFlush(normalDevice);
        
        entityManager.clear();
        
        // when
        List<Device> lowBatteryDevices = deviceRepository.findByBatteryLevelLessThanAndIsActiveTrue(20);
        
        // then
        assertThat(lowBatteryDevices).hasSize(1);
        assertThat(lowBatteryDevices.get(0).getBatteryLevel()).isEqualTo(15);
    }
    
    @Test
    @DisplayName("오래된 동기화 디바이스 조회")
    void findDevicesNotSyncedRecently_Success() {
        // given
        testDevice.setLastSyncAt(LocalDateTime.now().minusDays(3));
        Device oldSyncDevice = entityManager.persistAndFlush(testDevice);
        
        Device recentDevice = TestDataFactory.createDevice(testUser);
        recentDevice.setLastSyncAt(LocalDateTime.now().minusHours(1));
        entityManager.persistAndFlush(recentDevice);
        
        entityManager.clear();
        
        // when
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(1);
        List<Device> oldDevices = deviceRepository.findByLastSyncAtBeforeAndIsActiveTrue(cutoffTime);
        
        // then
        assertThat(oldDevices).hasSize(1);
        assertThat(oldDevices.get(0).getId()).isEqualTo(oldSyncDevice.getId());
    }
    
    @Test
    @DisplayName("중복 디바이스 식별자 - 실패")
    void saveDuplicateDeviceIdentifier_Failure() {
        // given
        entityManager.persistAndFlush(testDevice);
        
        Device duplicateDevice = TestDataFactory.createDevice(testUser);
        duplicateDevice.setDeviceIdentifier(testDevice.getDeviceIdentifier());
        
        // when & then
        assertThatThrownBy(() -> {
            deviceRepository.save(duplicateDevice);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("디바이스 타입별 조회")
    void findByDeviceType_Success() {
        // given
        Device wearableDevice = entityManager.persistAndFlush(testDevice);
        
        Device tabletDevice = TestDataFactory.createDevice(testUser);
        tabletDevice.setDeviceType("TABLET");
        entityManager.persistAndFlush(tabletDevice);
        
        entityManager.clear();
        
        // when
        List<Device> wearables = deviceRepository.findByDeviceType("WEARABLE");
        
        // then
        assertThat(wearables).hasSize(1);
        assertThat(wearables.get(0).getId()).isEqualTo(wearableDevice.getId());
    }
    
    @Test
    @DisplayName("디바이스 정보 업데이트")
    void updateDevice_Success() {
        // given
        Device savedDevice = entityManager.persistAndFlush(testDevice);
        entityManager.clear();
        
        // when
        Device deviceToUpdate = deviceRepository.findById(savedDevice.getId()).orElseThrow();
        deviceToUpdate.setBatteryLevel(50);
        deviceToUpdate.setLastSyncAt(LocalDateTime.now());
        Device updatedDevice = deviceRepository.save(deviceToUpdate);
        entityManager.flush();
        entityManager.clear();
        
        // then
        Device foundDevice = deviceRepository.findById(updatedDevice.getId()).orElseThrow();
        assertThat(foundDevice.getBatteryLevel()).isEqualTo(50);
    }
    
    @Test
    @DisplayName("디바이스 삭제")
    void deleteDevice_Success() {
        // given
        Device savedDevice = entityManager.persistAndFlush(testDevice);
        entityManager.clear();
        
        // when
        deviceRepository.deleteById(savedDevice.getId());
        entityManager.flush();
        
        // then
        Optional<Device> deletedDevice = deviceRepository.findById(savedDevice.getId());
        assertThat(deletedDevice).isEmpty();
    }
    
    @Test
    @DisplayName("사용자별 디바이스 수 조회")
    void countByUser_Success() {
        // given
        entityManager.persistAndFlush(testDevice);
        
        Device device2 = TestDataFactory.createDevice(testUser);
        entityManager.persistAndFlush(device2);
        
        Device device3 = TestDataFactory.createDevice(testUser);
        entityManager.persistAndFlush(device3);
        
        entityManager.clear();
        
        // when
        long count = deviceRepository.countByUser(testUser);
        
        // then
        assertThat(count).isEqualTo(3);
    }
    
    @Test
    @DisplayName("페이징 조회")
    void findAllWithPaging_Success() {
        // given
        for (int i = 0; i < 5; i++) {
            Device device = TestDataFactory.createDevice(testUser);
            entityManager.persistAndFlush(device);
        }
        entityManager.clear();
        
        // when
        Page<Device> firstPage = deviceRepository.findAll(PageRequest.of(0, 2));
        Page<Device> secondPage = deviceRepository.findAll(PageRequest.of(1, 2));
        
        // then
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(secondPage.getContent()).hasSize(2);
    }
}