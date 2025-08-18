package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 단순 UserRepository 테스트
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "fcm.enabled=false",
    "logging.level.root=WARN"
})
class SimpleUserRepositoryTest {
  
  @Autowired
  private UserRepository userRepository;
  
  @Test
  void saveAndFindUser() {
    // Given
    User user = User.builder()
        .username("testuser")
        .email("test@example.com")
        .name("테스트 사용자")
        .phoneNumber("010-1234-5678")
        .cognitiveLevel(User.CognitiveLevel.MODERATE)
        .languagePreference("ko")
        .isActive(true)
        .build();
    
    // When
    User savedUser = userRepository.save(user);
    
    // Then
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getUsername()).isEqualTo("testuser");
    assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
  }
}