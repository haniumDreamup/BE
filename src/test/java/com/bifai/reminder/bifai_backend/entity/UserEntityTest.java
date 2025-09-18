package com.bifai.reminder.bifai_backend.entity;

import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User 엔티티 테스트
 * JPA 엔티티가 제대로 인식되는지 확인
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.jwt.secret=testSecretKeyForJWTTokenGenerationAndValidation1234567890",
    "app.jwt.access-token-expiration-ms=900000",
    "app.jwt.refresh-token-expiration-ms=604800000"
})
class UserEntityTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void contextLoads() {
        assertThat(userRepository).isNotNull();
    }
}