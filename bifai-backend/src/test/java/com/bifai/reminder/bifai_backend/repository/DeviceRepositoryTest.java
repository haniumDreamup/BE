package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Device;
import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    private User testUser;
    private Device testDevice;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        deviceRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        
        // 기본 Role 생성
        Role userRole = Role.builder()
                .name("ROLE_USER")
                .koreanName("사용자")
                .description("BIF 일반 사용자")
                .isActive(true)
                .build();
        roleRepository.save(userRole);
        
        // User 생성 및 Role 설정
        testUser = TestDataBuilder.createUser();
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);
        
        testDevice = TestDataBuilder.createDevice(testUser);
    }
    
    @Test
    @DisplayName("디바이스 저장 - 성공")
    void saveDevice_Success() {
        // when
        Device savedDevice = deviceRepository.save(testDevice);
        
        // then
        assertThat(savedDevice.getId()).isNotNull();
        assertThat(savedDevice.getDeviceIdentifier()).isEqualTo(testDevice.getDeviceIdentifier());
        assertThat(savedDevice.getDeviceName()).isEqualTo("철수의 스마트워치");
        assertThat(savedDevice.getDeviceType()).isEqualTo("WEARABLE");
        assertThat(savedDevice.getUser().getUserId()).isEqualTo(testUser.getUserId());
    }
    
    @Test
    @DisplayName("디바이스 조회 - ID로 조회")
    void findById_Success() {
        // given
        Device savedDevice = deviceRepository.save(testDevice);
        
        // when
        Optional<Device> foundDevice = deviceRepository.findById(savedDevice.getId());
        
        // then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getDeviceSerialNumber()).isEqualTo("SN-12345");
        assertThat(foundDevice.get().getBatteryLevel()).isEqualTo(85);
    }
    
    @Test
    @DisplayName("사용자별 디바이스 조회")
    void findByUser_Success() {
        // given
        Device device1 = deviceRepository.save(testDevice);
        
        Device device2 = TestDataBuilder.createDevice(testUser);
        device2.setDeviceName("철수의 스마트폰");
        device2.setDeviceType("SMARTPHONE");
        deviceRepository.save(device2);
        
        // when
        List<Device> userDevices = deviceRepository.findByUser(testUser);
        
        // then
        assertThat(userDevices).hasSize(2);
        assertThat(userDevices).extracting("deviceType")
            .containsExactlyInAnyOrder("WEARABLE", "SMARTPHONE");
    }
    
    @Test
    @DisplayName("디바이스 식별자로 조회")
    void findByDeviceIdentifier_Success() {
        // given
        deviceRepository.save(testDevice);
        
        // when
        Optional<Device> foundDevice = deviceRepository.findByDeviceIdentifier(testDevice.getDeviceIdentifier());
        
        // then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getDeviceName()).isEqualTo("철수의 스마트워치");
    }
    
    @Test
    @DisplayName("푸시 토큰으로 조회")
    void findByPushToken_Success() {
        // given
        deviceRepository.save(testDevice);
        
        // when
        Optional<Device> foundDevice = deviceRepository.findByPushToken(testDevice.getPushToken());
        
        // then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getDeviceIdentifier()).isEqualTo(testDevice.getDeviceIdentifier());
    }
    
    @Test
    @DisplayName("활성 디바이스 조회")
    void findByUserAndIsActiveTrue_Success() {
        // given
        deviceRepository.save(testDevice);
        
        Device inactiveDevice = TestDataBuilder.createDevice(testUser);
        inactiveDevice.setIsActive(false);
        inactiveDevice.setDeviceIdentifier("MAC-INACTIVE");
        deviceRepository.save(inactiveDevice);
        
        // when
        List<Device> activeDevices = deviceRepository.findByUserAndIsActiveTrue(testUser);
        
        // then
        assertThat(activeDevices).hasSize(1);
        assertThat(activeDevices.get(0).getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("배터리 부족 디바이스 조회")
    void findLowBatteryDevices_Success() {
        // given
        testDevice.setBatteryLevel(15); // 배터리 부족
        deviceRepository.save(testDevice);
        
        Device normalDevice = TestDataBuilder.createDevice(testUser);
        normalDevice.setDeviceIdentifier("MAC-NORMAL");
        normalDevice.setBatteryLevel(80);
        deviceRepository.save(normalDevice);
        
        // when
        List<Device> lowBatteryDevices = deviceRepository.findLowBatteryDevices(20);
        
        // then
        assertThat(lowBatteryDevices).hasSize(1);
        assertThat(lowBatteryDevices.get(0).getBatteryLevel()).isLessThanOrEqualTo(20);
    }
    
    @Test
    @DisplayName("오래된 동기화 디바이스 조회")
    void findDevicesNotSyncedSince_Success() {
        // given
        // 마지막 동기화를 2일 전으로 설정
        testDevice.setLastSyncAt(LocalDateTime.now().minusDays(2));
        deviceRepository.save(testDevice);
        
        Device recentDevice = TestDataBuilder.createDevice(testUser);
        recentDevice.setDeviceIdentifier("MAC-RECENT");
        recentDevice.setLastSyncAt(LocalDateTime.now().minusHours(1));
        deviceRepository.save(recentDevice);
        
        // when
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        List<Device> outdatedDevices = deviceRepository.findDevicesNotSyncedSince(threshold);
        
        // then
        assertThat(outdatedDevices).hasSize(1);
        assertThat(outdatedDevices.get(0).getDeviceIdentifier()).contains("MAC-");
    }
    
    @Test
    @DisplayName("중복 디바이스 식별자 - 실패")
    void saveDuplicateIdentifier_Fail() {
        // given
        deviceRepository.save(testDevice);
        
        Device duplicateDevice = TestDataBuilder.createDevice(testUser);
        duplicateDevice.setDeviceIdentifier(testDevice.getDeviceIdentifier());
        
        // when & then
        assertThatThrownBy(() -> {
            deviceRepository.save(duplicateDevice);
            deviceRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("디바이스 타입별 조회")
    void findByDeviceType_Success() {
        // given
        deviceRepository.save(testDevice);
        
        Device smartphone = TestDataBuilder.createDevice(testUser);
        smartphone.setDeviceIdentifier("MAC-PHONE");
        smartphone.setDeviceType("SMARTPHONE");
        deviceRepository.save(smartphone);
        
        // when
        List<Device> wearables = deviceRepository.findByDeviceType("WEARABLE");
        List<Device> smartphones = deviceRepository.findByDeviceType("SMARTPHONE");
        
        // then
        assertThat(wearables).hasSize(1);
        assertThat(smartphones).hasSize(1);
        assertThat(wearables.get(0).getDeviceName()).contains("스마트워치");
    }
    
    @Test
    @DisplayName("디바이스 정보 업데이트")
    void updateDevice_Success() {
        // given
        Device savedDevice = deviceRepository.save(testDevice);
        
        // when
        savedDevice.setBatteryLevel(50);
        savedDevice.setLastSyncAt(LocalDateTime.now());
        savedDevice.setAppVersion("1.1.0");
        Device updatedDevice = deviceRepository.save(savedDevice);
        
        // then
        assertThat(updatedDevice.getBatteryLevel()).isEqualTo(50);
        assertThat(updatedDevice.getAppVersion()).isEqualTo("1.1.0");
        assertThat(updatedDevice.getLastSyncAt()).isAfter(LocalDateTime.now().minusMinutes(1));
    }
    
    @Test
    @DisplayName("디바이스 삭제")
    void deleteDevice_Success() {
        // given
        Device savedDevice = deviceRepository.save(testDevice);
        Long deviceId = savedDevice.getId();
        
        // when
        deviceRepository.deleteById(deviceId);
        
        // then
        assertThat(deviceRepository.findById(deviceId)).isEmpty();
    }
    
    @Test
    @DisplayName("사용자별 디바이스 수 조회")
    void countByUser_Success() {
        // given
        deviceRepository.save(testDevice);
        
        Device anotherDevice = TestDataBuilder.createDevice(testUser);
        anotherDevice.setDeviceIdentifier("MAC-ANOTHER");
        deviceRepository.save(anotherDevice);
        
        // when
        long deviceCount = deviceRepository.countByUser(testUser);
        
        // then
        assertThat(deviceCount).isEqualTo(2);
    }
    
    @Test
    @DisplayName("필수 필드 누락 - 실패")
    void saveWithoutRequiredFields_Fail() {
        // given
        Device invalidDevice = Device.builder()
                .user(testUser)
                // deviceIdentifier 누락
                .deviceName("테스트 기기")
                .build();
        
        // when & then
        assertThatThrownBy(() -> {
            deviceRepository.save(invalidDevice);
            deviceRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("페이징 조회")
    void findAllWithPaging_Success() {
        // given
        for (int i = 0; i < 5; i++) {
            Device device = TestDataBuilder.createDevice(testUser);
            device.setDeviceIdentifier("MAC-" + i);
            device.setDeviceName("기기 " + i);
            deviceRepository.save(device);
        }
        
        // when
        Page<Device> firstPage = deviceRepository.findAll(PageRequest.of(0, 3));
        
        // then
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}