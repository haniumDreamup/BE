package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 단순 UserRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=true",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.data.jpa.repositories.enabled=true",
    "spring.jpa.defer-datasource-initialization=false",
    "spring.flyway.enabled=false",
    "fcm.enabled=false",
    "logging.level.root=WARN"
})
@EnableJpaAuditing
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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