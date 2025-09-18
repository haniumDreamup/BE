package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.Guardian.ApprovalStatus;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * GuardianRepository 테스트
 * BIF 사용자의 보호자 관계 관리 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("GuardianRepository 테스트")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GuardianRepositoryTest {
    
    @Autowired
    private GuardianRepository guardianRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User bifUser;
    private User guardianUser;
    private Guardian testGuardian;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        guardianRepository.deleteAll();
        userRepository.deleteAll();
        
        // BIF 사용자
        bifUser = userRepository.save(TestDataBuilder.createUser());
        
        // 보호자 사용자
        guardianUser = TestDataBuilder.createUserWithEmail("guardian@example.com");
        guardianUser.setName("김보호");
        guardianUser.setUsername("guardian");
        guardianUser.setPhoneNumber("010-9999-8888");
        guardianUser = userRepository.save(guardianUser);
        
        testGuardian = TestDataBuilder.createGuardian(bifUser, guardianUser);
    }
    
    @Test
    @DisplayName("보호자 관계 저장 - 성공")
    void saveGuardian_Success() {
        // when
        Guardian savedGuardian = guardianRepository.save(testGuardian);
        
        // then
        assertThat(savedGuardian.getId()).isNotNull();
        assertThat(savedGuardian.getUser().getUserId()).isEqualTo(bifUser.getUserId());
        assertThat(savedGuardian.getGuardianUser().getUserId()).isEqualTo(guardianUser.getUserId());
        assertThat(savedGuardian.getRelationship()).isEqualTo("부모");
        assertThat(savedGuardian.getIsPrimary()).isTrue();
    }
    
    @Test
    @DisplayName("보호자 조회 - ID로 조회")
    void findById_Success() {
        // given
        Guardian savedGuardian = guardianRepository.save(testGuardian);
        
        // when
        Optional<Guardian> foundGuardian = guardianRepository.findById(savedGuardian.getId());
        
        // then
        assertThat(foundGuardian).isPresent();
        assertThat(foundGuardian.get().getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(foundGuardian.get().getCanViewLocation()).isTrue();
    }
    
    @Test
    @DisplayName("사용자별 보호자 목록 조회")
    void findByUser_Success() {
        // given
        guardianRepository.save(testGuardian);
        
        // 두 번째 보호자 추가
        User secondGuardian = TestDataBuilder.createUserWithEmail("guardian2@example.com");
        secondGuardian.setUsername("guardian2");
        secondGuardian.setPhoneNumber("010-7777-6666");
        secondGuardian = userRepository.save(secondGuardian);
        
        Guardian secondRelation = TestDataBuilder.createGuardian(bifUser, secondGuardian);
        secondRelation.setRelationship("형제");
        secondRelation.setIsPrimary(false);
        guardianRepository.save(secondRelation);
        
        // when
        List<Guardian> guardians = guardianRepository.findByUser(bifUser);
        
        // then
        assertThat(guardians).hasSize(2);
        assertThat(guardians).extracting("relationship")
            .containsExactlyInAnyOrder("부모", "형제");
    }
    
    @Test
    @DisplayName("보호자 사용자별 피보호자 조회")
    void findByGuardianUser_Success() {
        // given
        guardianRepository.save(testGuardian);
        
        // 같은 보호자가 다른 사용자도 보호
        User anotherBifUser = TestDataBuilder.createUserWithEmail("another@example.com");
        anotherBifUser.setUsername("anotheruser");
        anotherBifUser.setPhoneNumber("010-5555-4444");
        anotherBifUser = userRepository.save(anotherBifUser);
        
        Guardian anotherRelation = TestDataBuilder.createGuardian(anotherBifUser, guardianUser);
        guardianRepository.save(anotherRelation);
        
        // when
        List<Guardian> guardianRelations = guardianRepository.findByGuardianUser(guardianUser);
        
        // then
        assertThat(guardianRelations).hasSize(2);
    }
    
    @Test
    @DisplayName("승인된 보호자 관계 조회")
    void findByUserAndApprovalStatus_Success() {
        // given
        guardianRepository.save(testGuardian);
        
        // 대기 중인 보호자 추가
        User pendingGuardian = TestDataBuilder.createUserWithEmail("pending@example.com");
        pendingGuardian.setUsername("pending");
        pendingGuardian.setPhoneNumber("010-3333-2222");
        pendingGuardian = userRepository.save(pendingGuardian);
        
        Guardian pendingRelation = TestDataBuilder.createGuardian(bifUser, pendingGuardian);
        pendingRelation.setApprovalStatus(ApprovalStatus.PENDING);
        pendingRelation.setApprovedAt(null);
        guardianRepository.save(pendingRelation);
        
        // when
        List<Guardian> approvedGuardians = guardianRepository.findByUserAndApprovalStatus(bifUser, ApprovalStatus.APPROVED);
        List<Guardian> pendingGuardians = guardianRepository.findByUserAndApprovalStatus(bifUser, ApprovalStatus.PENDING);
        
        // then
        assertThat(approvedGuardians).hasSize(1);
        assertThat(pendingGuardians).hasSize(1);
    }
    
    @Test
    @DisplayName("주 보호자 조회")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void findByUserAndIsPrimaryTrue_Success() {
        // given
        guardianRepository.save(testGuardian);
        
        // 일반 보호자 추가
        User normalGuardian = TestDataBuilder.createUserWithEmail("normal@example.com");
        normalGuardian.setUsername("normal");
        normalGuardian.setPhoneNumber("010-1111-0000");
        normalGuardian = userRepository.save(normalGuardian);
        
        Guardian normalRelation = TestDataBuilder.createGuardian(bifUser, normalGuardian);
        normalRelation.setIsPrimary(false);
        guardianRepository.save(normalRelation);
        
        // when
        Optional<Guardian> primaryGuardian = guardianRepository.findByUserAndIsPrimaryTrue(bifUser);
        
        // then
        assertThat(primaryGuardian).isPresent();
        assertThat(primaryGuardian.get().getGuardianUser().getEmail()).isEqualTo("guardian@example.com");
    }
    
    @Test
    @DisplayName("알림 수신 가능한 보호자 조회")
    void findByUserAndCanReceiveAlertsTrue_Success() {
        // given
        guardianRepository.save(testGuardian);
        
        // 알림 수신 불가 보호자 추가
        User noAlertGuardian = TestDataBuilder.createUserWithEmail("noalert@example.com");
        noAlertGuardian.setUsername("noalert");
        noAlertGuardian.setPhoneNumber("010-0000-9999");
        noAlertGuardian = userRepository.save(noAlertGuardian);
        
        Guardian noAlertRelation = TestDataBuilder.createGuardian(bifUser, noAlertGuardian);
        noAlertRelation.setCanReceiveAlerts(false);
        guardianRepository.save(noAlertRelation);
        
        // when
        List<Guardian> alertGuardians = guardianRepository.findByUserAndCanReceiveAlertsTrue(bifUser);
        
        // then
        assertThat(alertGuardians).hasSize(1);
        assertThat(alertGuardians.get(0).getCanReceiveAlerts()).isTrue();
    }
    
    @Test
    @DisplayName("중복 보호자 관계 - 실패")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void saveDuplicateRelation_Fail() {
        // given
        guardianRepository.save(testGuardian);
        guardianRepository.flush(); // 첫 번째 저장을 강제 커밋

        // 같은 사용자-보호자 쌍으로 다시 생성
        Guardian duplicateRelation = TestDataBuilder.createGuardian(bifUser, guardianUser);

        // when & then
        assertThatThrownBy(() -> {
            guardianRepository.save(duplicateRelation);
            guardianRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("긴급 연락 우선순위별 조회")
    void findByEmergencyContactPriority_Success() {
        // given
        guardianRepository.save(testGuardian); // priority = 1
        
        // 우선순위 2 보호자
        User secondPriorityGuardian = TestDataBuilder.createUserWithEmail("second@example.com");
        secondPriorityGuardian.setUsername("second");
        secondPriorityGuardian.setPhoneNumber("010-2222-1111");
        secondPriorityGuardian = userRepository.save(secondPriorityGuardian);
        
        Guardian secondPriority = TestDataBuilder.createGuardian(bifUser, secondPriorityGuardian);
        secondPriority.setEmergencyPriority(2);
        secondPriority.setIsPrimary(false);
        guardianRepository.save(secondPriority);
        
        // when
        List<Guardian> orderedGuardians = guardianRepository.findByUserOrderByEmergencyPriority(bifUser);
        
        // then
        assertThat(orderedGuardians).hasSize(2);
        assertThat(orderedGuardians.get(0).getEmergencyPriority()).isEqualTo(1);
        assertThat(orderedGuardians.get(1).getEmergencyPriority()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("권한별 보호자 조회")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void findByPermissions_Success() {
        // given
        guardianRepository.save(testGuardian); // 모든 권한 있음
        
        // 위치 조회만 가능한 보호자
        User limitedGuardian = TestDataBuilder.createUserWithEmail("limited@example.com");
        limitedGuardian.setUsername("limited");
        limitedGuardian.setPhoneNumber("010-8888-7777");
        limitedGuardian = userRepository.save(limitedGuardian);
        
        Guardian limitedRelation = TestDataBuilder.createGuardian(bifUser, limitedGuardian);
        limitedRelation.setCanModifySettings(false);
        limitedRelation.setCanReceiveAlerts(false);
        guardianRepository.save(limitedRelation);
        
        // when
        List<Guardian> canModifySettings = guardianRepository.findByUserAndCanModifySettingsTrue(bifUser);
        List<Guardian> canViewLocation = guardianRepository.findByUserAndCanViewLocationTrue(bifUser);
        
        // then
        assertThat(canModifySettings).hasSize(1);
        assertThat(canViewLocation).hasSize(2); // 둘 다 위치는 볼 수 있음
    }
    
    @Test
    @DisplayName("보호자 관계 업데이트")
    void updateGuardian_Success() {
        // given
        Guardian savedGuardian = guardianRepository.save(testGuardian);
        
        // when
        savedGuardian.setRelationship("친척");
        savedGuardian.setCanModifySettings(false);
        savedGuardian.setEmergencyPriority(2);
        Guardian updatedGuardian = guardianRepository.save(savedGuardian);
        
        // then
        assertThat(updatedGuardian.getRelationship()).isEqualTo("친척");
        assertThat(updatedGuardian.getCanModifySettings()).isFalse();
        assertThat(updatedGuardian.getEmergencyPriority()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("보호자 관계 삭제")
    void deleteGuardian_Success() {
        // given
        Guardian savedGuardian = guardianRepository.save(testGuardian);
        Long guardianId = savedGuardian.getId();
        
        // when
        guardianRepository.deleteById(guardianId);
        
        // then
        assertThat(guardianRepository.findById(guardianId)).isEmpty();
    }
    
    @Test
    @DisplayName("비활성 보호자 관계 조회")
    void findByIsActive_Success() {
        // given
        guardianRepository.save(testGuardian); // isActive = true
        
        // 비활성 보호자
        User inactiveGuardian = TestDataBuilder.createUserWithEmail("inactive@example.com");
        inactiveGuardian.setUsername("inactive");
        inactiveGuardian.setPhoneNumber("010-6666-5555");
        inactiveGuardian = userRepository.save(inactiveGuardian);
        
        Guardian inactiveRelation = TestDataBuilder.createGuardian(bifUser, inactiveGuardian);
        inactiveRelation.setIsActive(false);
        guardianRepository.save(inactiveRelation);
        
        // when
        List<Guardian> activeGuardians = guardianRepository.findByUserAndIsActiveTrue(bifUser);
        
        // then
        assertThat(activeGuardians).hasSize(1);
        assertThat(activeGuardians.get(0).getIsActive()).isTrue();
    }
    
    @Test
    @DisplayName("페이징 조회")
    void findAllWithPaging_Success() {
        // given
        guardianRepository.save(testGuardian);
        
        // 추가 보호자 생성
        for (int i = 0; i < 4; i++) {
            User guardian = TestDataBuilder.createUserWithEmail("guardian" + i + "@example.com");
            guardian.setUsername("guardian" + i);
            guardian.setPhoneNumber("010-000" + i + "-000" + i);
            guardian = userRepository.save(guardian);
            
            Guardian relation = TestDataBuilder.createGuardian(bifUser, guardian);
            relation.setIsPrimary(false);
            guardianRepository.save(relation);
        }
        
        // when
        Page<Guardian> firstPage = guardianRepository.findAll(PageRequest.of(0, 3));
        
        // then
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
    
    @Test
    @DisplayName("필수 필드 누락 - 실패")
    void saveWithoutRequiredFields_Fail() {
        // given
        Guardian invalidGuardian = Guardian.builder()
                .user(bifUser)
                // guardianUser 누락
                .relationship("부모")
                .build();
        
        // when & then
        assertThatThrownBy(() -> {
            guardianRepository.save(invalidGuardian);
            guardianRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}