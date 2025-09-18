package com.bifai.reminder.bifai_backend;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA 엔티티 스캔 테스트
 * "Not a managed type" 오류 디버깅용
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class JpaEntityScanTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuardianRepository guardianRepository;

    @Test
    void contextLoads() {
        assertThat(entityManager).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(guardianRepository).isNotNull();
    }

    @Test
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void testUserEntityIsManagedType() {
        // User 엔티티가 JPA에 의해 관리되는지 확인
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("테스트 사용자")
                .passwordHash("hashed")
                .build();

        User savedUser = entityManager.persistAndFlush(user);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUserId()).isNotNull();
    }

    @Test
    @Transactional(isolation = Isolation.SERIALIZABLE)
    void testGuardianEntityIsManagedType() {
        // Guardian 엔티티가 JPA에 의해 관리되는지 확인
        User user = User.builder()
                .username("testuser2")
                .email("test2@example.com")
                .name("테스트 사용자2")
                .passwordHash("hashed")
                .build();
        
        User guardianUser = User.builder()
                .username("guardianuser")
                .email("guardian@example.com")
                .name("보호자")
                .passwordHash("hashed")
                .build();

        entityManager.persistAndFlush(user);
        entityManager.persistAndFlush(guardianUser);

        Guardian guardian = Guardian.builder()
                .user(user)
                .guardianUser(guardianUser)
                .name("보호자 이름")
                .relationship("부모")
                .primaryPhone("010-1234-5678")
                .build();

        Guardian savedGuardian = entityManager.persistAndFlush(guardian);
        assertThat(savedGuardian).isNotNull();
        assertThat(savedGuardian.getId()).isNotNull();
    }

    @Test
    void testCircularReferenceHandling() {
        // 순환 참조가 제대로 처리되는지 확인
        User user = User.builder()
                .username("testuser3")
                .email("test3@example.com")
                .name("테스트 사용자3")
                .passwordHash("hashed")
                .build();

        User savedUser = userRepository.save(user);
        
        // 순환 참조가 있어도 toString()이 정상 작동하는지 확인
        String userString = savedUser.toString();
        assertThat(userString).isNotNull();
        assertThat(userString).contains("username='testuser3'");
    }
}