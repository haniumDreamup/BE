package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.User.CognitiveLevel;
import com.bifai.reminder.bifai_backend.exception.DuplicateResourceException;
import com.bifai.reminder.bifai_backend.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserRepository 테스트
 * BIF 사용자 관련 데이터 접근 계층 테스트
 */
@DisplayName("UserRepository 테스트")
class UserRepositoryTest extends BaseRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        userRepository.deleteAll();
        testUser = TestDataBuilder.createUser();
    }
    
    @Test
    @DisplayName("사용자 저장 - 성공")
    void saveUser_Success() {
        // when
        User savedUser = userRepository.save(testUser);
        
        // then
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(savedUser.getName()).isEqualTo(testUser.getName());
        assertThat(savedUser.getCognitiveLevel()).isEqualTo(CognitiveLevel.MODERATE);
    }
    
    @Test
    @DisplayName("사용자 조회 - ID로 조회")
    void findById_Success() {
        // given
        User savedUser = userRepository.save(testUser);
        
        // when
        Optional<User> foundUser = userRepository.findById(savedUser.getUserId());
        
        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }
    
    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 ID")
    void findById_NotFound() {
        // when
        Optional<User> foundUser = userRepository.findById(999L);
        
        // then
        assertThat(foundUser).isEmpty();
    }
    
    @Test
    @DisplayName("사용자 조회 - 이메일로 조회")
    void findByEmail_Success() {
        // given
        userRepository.save(testUser);
        
        // when
        Optional<User> foundUser = userRepository.findByEmail(testUser.getEmail());
        
        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getName()).isEqualTo(testUser.getName());
    }
    
    @Test
    @DisplayName("사용자 조회 - username으로 조회")
    void findByUsername_Success() {
        // given
        userRepository.save(testUser);
        
        // when
        Optional<User> foundUser = userRepository.findByUsername(testUser.getUsername());
        
        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }
    
    @Test
    @DisplayName("중복 이메일 저장 - 실패")
    void saveDuplicateEmail_Fail() {
        // given
        userRepository.save(testUser);
        User duplicateUser = TestDataBuilder.createUserWithEmail(testUser.getEmail());
        duplicateUser.setUsername("different");
        
        // when & then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            userRepository.flush(); // 즉시 DB에 반영
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("중복 username 저장 - 실패")
    void saveDuplicateUsername_Fail() {
        // given
        userRepository.save(testUser);
        User duplicateUser = TestDataBuilder.createUserWithEmail("different@example.com");
        duplicateUser.setUsername(testUser.getUsername());
        
        // when & then
        assertThatThrownBy(() -> {
            userRepository.save(duplicateUser);
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
    
    @Test
    @DisplayName("활성 사용자 조회")
    void findByIsActiveTrue_Success() {
        // given
        User activeUser = userRepository.save(testUser);
        
        User inactiveUser = TestDataBuilder.createUserWithEmail("inactive@example.com");
        inactiveUser.setIsActive(false);
        userRepository.save(inactiveUser);
        
        // when
        List<User> activeUsers = userRepository.findByIsActiveTrue();
        
        // then
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getUserId()).isEqualTo(activeUser.getUserId());
    }
    
    @Test
    @DisplayName("인지 수준별 사용자 조회")
    void findByCognitiveLevel_Success() {
        // given
        userRepository.save(testUser); // MODERATE
        
        User mildUser = TestDataBuilder.createUserWithEmail("mild@example.com");
        mildUser.setCognitiveLevel(CognitiveLevel.MILD);
        userRepository.save(mildUser);
        
        // when
        List<User> moderateUsers = userRepository.findByCognitiveLevel(CognitiveLevel.MODERATE);
        List<User> mildUsers = userRepository.findByCognitiveLevel(CognitiveLevel.MILD);
        
        // then
        assertThat(moderateUsers).hasSize(1);
        assertThat(mildUsers).hasSize(1);
    }
    
    @Test
    @DisplayName("이메일 인증된 사용자 조회")
    void findByEmailVerifiedTrue_Success() {
        // given
        userRepository.save(testUser); // emailVerified = true
        
        User unverifiedUser = TestDataBuilder.createUserWithEmail("unverified@example.com");
        unverifiedUser.setEmailVerified(false);
        userRepository.save(unverifiedUser);
        
        // when
        List<User> verifiedUsers = userRepository.findByEmailVerifiedTrue();
        
        // then
        assertThat(verifiedUsers).hasSize(1);
        assertThat(verifiedUsers.get(0).getEmail()).isEqualTo(testUser.getEmail());
    }
    
    @Test
    @DisplayName("마지막 로그인 시간 기준 조회")
    void findInactiveUsers_Success() {
        // given
        // 최근 로그인한 사용자
        testUser.updateLastLogin();
        userRepository.save(testUser);
        
        // 로그인하지 않은 사용자 (lastLoginAt이 null)
        User inactiveUser = TestDataBuilder.createUserWithEmail("inactive@example.com");
        userRepository.save(inactiveUser);
        
        // when
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<User> inactiveUsers = userRepository.findUsersNotLoggedInSince(threshold);
        
        // then
        // null인 lastLoginAt은 조회되지 않으므로 결과가 0이어야 함
        assertThat(inactiveUsers).hasSize(0);
        
        // 최근 로그인한 사용자 조회
        List<User> activeUsers = userRepository.findUsersLoggedInSince(threshold);
        assertThat(activeUsers).hasSize(1);
        assertThat(activeUsers.get(0).getEmail()).isEqualTo(testUser.getEmail());
    }
    
    @Test
    @DisplayName("전화번호로 존재 여부 확인")
    void existsByPhoneNumber_Success() {
        // given
        userRepository.save(testUser);
        
        // when
        boolean exists = userRepository.existsByPhoneNumber(testUser.getPhoneNumber());
        boolean notExists = userRepository.existsByPhoneNumber("010-0000-0000");
        
        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    @DisplayName("이메일로 존재 여부 확인")
    void existsByEmail_Success() {
        // given
        userRepository.save(testUser);
        
        // when
        boolean exists = userRepository.existsByEmail(testUser.getEmail());
        boolean notExists = userRepository.existsByEmail("notexist@example.com");
        
        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    @DisplayName("사용자 수정")
    void updateUser_Success() {
        // given
        User savedUser = userRepository.save(testUser);
        
        // when
        savedUser.updateCognitiveLevel(CognitiveLevel.MILD);
        savedUser.setEmergencyMode(true);
        User updatedUser = userRepository.save(savedUser);
        
        // then
        assertThat(updatedUser.getCognitiveLevel()).isEqualTo(CognitiveLevel.MILD);
        assertThat(updatedUser.getEmergencyModeEnabled()).isTrue();
    }
    
    @Test
    @DisplayName("사용자 삭제")
    void deleteUser_Success() {
        // given
        User savedUser = userRepository.save(testUser);
        Long userId = savedUser.getUserId();
        
        // when
        userRepository.deleteById(userId);
        
        // then
        assertThat(userRepository.findById(userId)).isEmpty();
    }
    
    @Test
    @DisplayName("페이징 조회")
    void findAllWithPaging_Success() {
        // given
        for (int i = 0; i < 5; i++) {
            User user = TestDataBuilder.createUserWithEmail("user" + i + "@example.com");
            userRepository.save(user);
        }
        
        // when
        Page<User> firstPage = userRepository.findAll(PageRequest.of(0, 3));
        Page<User> secondPage = userRepository.findAll(PageRequest.of(1, 3));
        
        // then
        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent()).hasSize(2);
    }
    
    @Test
    @DisplayName("검색 기능 - 이름 또는 이메일")
    void searchByNameOrEmail_Success() {
        // given
        userRepository.save(testUser);
        
        User anotherUser = TestDataBuilder.createUserWithEmail("another@example.com");
        anotherUser.setName("이영희");
        userRepository.save(anotherUser);
        
        // when
        List<User> foundByName = userRepository.searchByNameOrEmail("김철수");
        List<User> foundByEmail = userRepository.searchByNameOrEmail("another");
        List<User> foundByPartial = userRepository.searchByNameOrEmail("영희");
        
        // then
        assertThat(foundByName).hasSize(1);
        assertThat(foundByEmail).hasSize(1);
        assertThat(foundByPartial).hasSize(1);
    }
    
    @Test
    @DisplayName("필수 필드 누락 - 실패")
    void saveWithoutRequiredFields_Fail() {
        // given
        User invalidUser = User.builder()
                .username("test")
                // email 누락
                .build();
        
        // when & then
        assertThatThrownBy(() -> {
            userRepository.save(invalidUser);
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}