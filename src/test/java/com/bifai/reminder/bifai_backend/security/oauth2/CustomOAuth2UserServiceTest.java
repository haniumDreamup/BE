package com.bifai.reminder.bifai_backend.security.oauth2;

import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:h2:mem:testdb",
  "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CustomOAuth2UserServiceTest {

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private RoleRepository roleRepository;

  @Autowired
  private CustomOAuth2UserService customOAuth2UserService;

  private Role userRole;

  @BeforeEach
  void setUp() {
    userRole = Role.builder()
      .id(1L)
      .name("USER")
      .build();
  }

  @Test
  @DisplayName("새로운 카카오 사용자 OAuth2 로그인 성공")
  void testLoadUser_NewKakaoUser_Success() {
    // Given
    Map<String, Object> attributes = createKakaoAttributes();
    OAuth2UserRequest userRequest = createOAuth2UserRequest("kakao", attributes);
    
    when(userRepository.findByEmail("test@kakao.com")).thenReturn(Optional.empty());
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setUserId(1L);
      return user;
    });

    // When
    OAuth2User result = customOAuth2UserService.loadUser(userRequest);

    // Then
    assertThat(result).isNotNull();
    assertThat((Long) result.getAttribute("id")).isEqualTo(12345L);
    assertThat(result.getName()).isEqualTo("1");
    
    verify(userRepository).save(argThat(user -> 
      user.getEmail().equals("test@kakao.com") &&
      user.getName().equals("테스트유저") &&
      user.getProvider().equals("kakao") &&
      user.getProviderId().equals("12345")
    ));
  }

  @Test
  @DisplayName("기존 구글 사용자 OAuth2 로그인 성공")
  void testLoadUser_ExistingGoogleUser_Success() {
    // Given
    Map<String, Object> attributes = createGoogleAttributes();
    OAuth2UserRequest userRequest = createOAuth2UserRequest("google", attributes);
    
    User existingUser = User.builder()
      .userId(1L)
      .email("test@gmail.com")
      .name("Old Name")
      .provider("google")
      .providerId("google123")
      .isActive(true)
      .roles(Set.of(userRole))
      .build();
    
    when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingUser));
    when(userRepository.save(any(User.class))).thenReturn(existingUser);

    // When
    OAuth2User result = customOAuth2UserService.loadUser(userRequest);

    // Then
    assertThat(result).isNotNull();
    assertThat((String) result.getAttribute("sub")).isEqualTo("google123");
    
    verify(userRepository).save(argThat(user -> 
      user.getName().equals("Test User") &&
      user.getProfileImageUrl().equals("https://example.com/photo.jpg")
    ));
  }

  @Test
  @DisplayName("네이버 사용자 OAuth2 로그인 성공")
  void testLoadUser_NaverUser_Success() {
    // Given
    Map<String, Object> attributes = createNaverAttributes();
    OAuth2UserRequest userRequest = createOAuth2UserRequest("naver", attributes);
    
    when(userRepository.findByEmail("test@naver.com")).thenReturn(Optional.empty());
    when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setUserId(1L);
      return user;
    });

    // When
    OAuth2User result = customOAuth2UserService.loadUser(userRequest);

    // Then
    assertThat(result).isNotNull();
    Map<String, Object> response = (Map<String, Object>) result.getAttribute("response");
    assertThat(response.get("id")).isEqualTo("naver123");
    
    verify(userRepository).save(argThat(user -> 
      user.getEmail().equals("test@naver.com") &&
      user.getName().equals("네이버유저") &&
      user.getProvider().equals("naver") &&
      user.getProviderId().equals("naver123")
    ));
  }

  private OAuth2UserRequest createOAuth2UserRequest(String registrationId, Map<String, Object> attributes) {
    ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
      .clientId("test-client-id")
      .clientSecret("test-client-secret")
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .redirectUri("http://localhost:8080/login/oauth2/code/" + registrationId)
      .authorizationUri("https://provider.com/oauth/authorize")
      .tokenUri("https://provider.com/oauth/token")
      .userInfoUri("https://provider.com/userinfo")
      .userNameAttributeName("id")
      .clientName("Test Provider")
      .build();

    OAuth2AccessToken accessToken = new OAuth2AccessToken(
      OAuth2AccessToken.TokenType.BEARER,
      "test-token",
      Instant.now(),
      Instant.now().plusSeconds(3600)
    );

    OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, accessToken);
    
    // Mock the super.loadUser() call
    OAuth2User oAuth2User = new DefaultOAuth2User(
      Collections.emptyList(),
      attributes,
      registrationId.equals("naver") ? "response" : (registrationId.equals("google") ? "sub" : "id")
    );
    doReturn(oAuth2User).when(customOAuth2UserService).loadUser(any());
    
    return request;
  }

  private Map<String, Object> createKakaoAttributes() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("id", 12345L);
    
    Map<String, Object> properties = new HashMap<>();
    properties.put("nickname", "테스트유저");
    properties.put("profile_image", "https://kakao.com/profile.jpg");
    attributes.put("properties", properties);
    
    Map<String, Object> kakaoAccount = new HashMap<>();
    kakaoAccount.put("email", "test@kakao.com");
    attributes.put("kakao_account", kakaoAccount);
    
    return attributes;
  }

  private Map<String, Object> createGoogleAttributes() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("sub", "google123");
    attributes.put("name", "Test User");
    attributes.put("email", "test@gmail.com");
    attributes.put("picture", "https://example.com/photo.jpg");
    return attributes;
  }

  private Map<String, Object> createNaverAttributes() {
    Map<String, Object> response = new HashMap<>();
    response.put("id", "naver123");
    response.put("name", "네이버유저");
    response.put("email", "test@naver.com");
    response.put("profile_image", "https://naver.com/profile.jpg");
    
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("response", response);
    return attributes;
  }
}