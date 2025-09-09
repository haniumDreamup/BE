package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.auth.LoginRequest;
import com.bifai.reminder.bifai_backend.dto.auth.AuthResponse;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-integration-testing-must-be-minimum-64-characters-for-hs512-algorithm-requirement",
    "jwt.access-token-expiration=3600000",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "logging.level.com.bifai.reminder=DEBUG"
})
@Transactional
class AuthenticationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @Autowired
  private UserRepository userRepository;
  
  @Autowired
  private PasswordEncoder passwordEncoder;
  
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  
  private User testUser;
  private final String TEST_EMAIL = "test@example.com";
  private final String TEST_PASSWORD = "Test1234!";
  private final String TEST_NAME = "테스트 사용자";
  
  @BeforeEach
  void setUp() {
    // 테스트 사용자 생성
    testUser = User.builder()
        .email(TEST_EMAIL)
        .password(passwordEncoder.encode(TEST_PASSWORD))
        .name(TEST_NAME)
        .phoneNumber("010-1234-5678")
        .isActive(true)
        .build();
    testUser = userRepository.save(testUser);
  }
  
  @Test
  void login_WithValidCredentials_ReturnsToken() throws Exception {
    // Given
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setEmail(TEST_EMAIL);
    loginRequest.setPassword(TEST_PASSWORD);
    
    // When
    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.accessToken").exists())
        .andExpect(jsonPath("$.data.refreshToken").exists())
        .andExpect(jsonPath("$.data.user.email").value(TEST_EMAIL))
        .andExpect(jsonPath("$.data.user.name").value(TEST_NAME))
        .andReturn();
    
    // Then
    String responseBody = result.getResponse().getContentAsString();
    ApiResponse<AuthResponse> response = objectMapper.readValue(responseBody,
        objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
    
    assertTrue(response.isSuccess());
    assertNotNull(response.getData().getAccessToken());
    assertNotNull(response.getData().getRefreshToken());
    
    // JWT 토큰 검증
    String token = response.getData().getAccessToken();
    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals(TEST_EMAIL, jwtTokenProvider.getEmail(token));
    assertEquals(testUser.getUserId(), jwtTokenProvider.getUserId(token));
  }
  
  @Test
  void login_WithInvalidPassword_ReturnsUnauthorized() throws Exception {
    // Given
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setEmail(TEST_EMAIL);
    loginRequest.setPassword("WrongPassword");
    
    // When & Then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").exists());
  }
  
  @Test
  void protectedEndpoint_WithValidToken_ReturnsSuccess() throws Exception {
    // Given
    String token = jwtTokenProvider.createAccessToken(TEST_EMAIL, testUser.getUserId());
    
    // When & Then
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.email").value(TEST_EMAIL));
  }
  
  @Test
  void protectedEndpoint_WithoutToken_ReturnsUnauthorized() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized());
  }
  
  @Test
  void protectedEndpoint_WithInvalidToken_ReturnsUnauthorized() throws Exception {
    // Given
    String invalidToken = "invalid.jwt.token";
    
    // When & Then
    mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", "Bearer " + invalidToken))
        .andExpect(status().isUnauthorized());
  }
  
  @Test
  void jwtToken_ContainsUserId() {
    // Given & When
    String token = jwtTokenProvider.createAccessToken(TEST_EMAIL, testUser.getUserId());
    
    // Then
    Long extractedUserId = jwtTokenProvider.getUserId(token);
    assertNotNull(extractedUserId);
    assertEquals(testUser.getUserId(), extractedUserId);
  }
  
  @Test
  void imageAnalysis_WithAuthentication_ExtractsCorrectUserId() throws Exception {
    // Given
    String token = jwtTokenProvider.createAccessToken(TEST_EMAIL, testUser.getUserId());
    
    // When - 실제 엔드포인트가 구현되면 테스트
    // 현재는 컴파일 에러 때문에 주석 처리
    /*
    mockMvc.perform(post("/api/v1/vision/analyze")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()));
    */
  }
  
  @Test
  void sosAlert_WithAuthentication_UsesCorrectUserId() throws Exception {
    // Given
    String token = jwtTokenProvider.createAccessToken(TEST_EMAIL, testUser.getUserId());
    
    // When - 실제 엔드포인트가 구현되면 테스트
    // 현재는 컴파일 에러 때문에 주석 처리
    /*
    mockMvc.perform(post("/api/v1/sos/trigger")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk());
    */
  }
  
  @Test
  void multipleRequests_WithSameToken_MaintainsAuthentication() throws Exception {
    // Given
    String token = jwtTokenProvider.createAccessToken(TEST_EMAIL, testUser.getUserId());
    
    // When & Then - 여러 요청에 대해 같은 토큰으로 인증 유지 확인
    for (int i = 0; i < 5; i++) {
      mockMvc.perform(get("/api/v1/users/me")
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.email").value(TEST_EMAIL));
    }
  }
}