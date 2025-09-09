package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserService 통합 테스트
 * 실제 Repository와 함께 동작하는 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(UserService.class)
@DisplayName("UserService 통합 테스트")
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자들 생성
        testUser1 = User.builder()
                .username("testuser1")
                .email("test1@example.com")
                .name("테스트 사용자1")
                .cognitiveLevel(User.CognitiveLevel.MILD)
                .isActive(true)
                .build();
        testUser1 = userRepository.save(testUser1);

        testUser2 = User.builder()
                .username("testuser2")
                .email("test2@example.com")
                .name("테스트 사용자2")
                .cognitiveLevel(User.CognitiveLevel.MODERATE)
                .isActive(false) // 비활성 사용자
                .build();
        testUser2 = userRepository.save(testUser2);
    }

    @Test
    @DisplayName("사용자 ID로 조회 - 성공")
    void getUserById_Success() {
        // When & Then - 실제 서비스 메소드 호출
        assertThatNoException().isThrownBy(() -> {
            User result = userService.getUserById(testUser1.getUserId());
            assertThat(result.getUsername()).isEqualTo("testuser1");
            assertThat(result.getEmail()).isEqualTo("test1@example.com");
        });
    }

    @Test
    @DisplayName("사용자 ID로 조회 - 존재하지 않는 사용자")
    void getUserById_NotFound() {
        // When & Then - 예외 발생 확인
        assertThatThrownBy(() -> {
            userService.getUserById(999L);
        }).isInstanceOf(Exception.class); // ResourceNotFoundException or similar
    }

    @Test
    @DisplayName("이메일로 조회 - 성공")
    void findByEmail_Success() {
        // When
        Optional<User> result = userService.findByEmail("test1@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(testUser1.getUserId());
    }

    @Test
    @DisplayName("전체 사용자 페이지 조회")
    void getAllUsers() {
        // When
        Page<User> result = userService.getAllUsers(PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(2); // testUser1, testUser2
        assertThat(result.getContent()).extracting("username")
            .containsExactlyInAnyOrder("testuser1", "testuser2");
    }

    @Test
    @DisplayName("사용자 활성화")
    void activateUser() {
        // Given - testUser2는 비활성 상태
        assertThat(testUser2.isActive()).isFalse();

        // When & Then - 실제 서비스 호출
        assertThatNoException().isThrownBy(() -> {
            userService.activateUser(testUser2.getUserId());
        });

        // 데이터베이스에서 실제 변경사항 확인
        User updatedUser = userRepository.findById(testUser2.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.isActive()).isTrue();
    }

    @Test
    @DisplayName("사용자 비활성화")
    void deactivateUser() {
        // Given - testUser1는 활성 상태
        assertThat(testUser1.isActive()).isTrue();

        // When & Then - 실제 서비스 호출
        assertThatNoException().isThrownBy(() -> {
            userService.deactivateUser(testUser1.getUserId());
        });

        // 데이터베이스에서 실제 변경사항 확인
        User updatedUser = userRepository.findById(testUser1.getUserId()).orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.isActive()).isFalse();
    }
}