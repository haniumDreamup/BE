package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.config.TestRedisConfiguration;
import com.bifai.reminder.bifai_backend.config.TestVisionConfiguration;
import com.bifai.reminder.bifai_backend.security.jwt.JwtAuthenticationFilter;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Controller 테스트 기본 설정
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestRedisConfiguration.class, TestVisionConfiguration.class})
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.ai.openai.api.OpenAiApiAutoConfiguration",
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.speech.api-key=test-key",
    "app.jwt.secret=testSecretKeyForJWTTokenGenerationAndValidation1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "app.jwt.access-token-expiration-ms=900000",
    "app.jwt.refresh-token-expiration-ms=604800000",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.data.redis.timeout=2000",
    "security.enabled=false"
})
public abstract class BaseControllerTest {

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  @MockitoBean
  protected JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  protected JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean
  protected BifUserDetailsService userDetailsService;
  
  @MockitoBean
  protected com.bifai.reminder.bifai_backend.repository.UserRepository userRepository;
}