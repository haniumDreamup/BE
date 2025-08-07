package com.bifai.reminder.bifai_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OAuth2Controller 단독 테스트
 * MockMvc를 standalone 모드로 설정하여 의존성 문제 해결
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Controller 단독 테스트")
class OAuth2ControllerStandaloneTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();
  private OAuth2Controller oAuth2Controller;

  @BeforeEach
  void setUp() {
    // OAuth2Controller는 서비스 의존성이 없으므로 직접 생성
    oAuth2Controller = new OAuth2Controller();
    
    // MockMvc를 standalone 모드로 설정
    mockMvc = MockMvcBuilders.standaloneSetup(oAuth2Controller)
        .build();
  }

  @Test
  @DisplayName("OAuth2 로그인 URL 조회 성공")
  void testGetOAuth2LoginUrls() throws Exception {
    // when & then
    mockMvc.perform(get("/api/v1/auth/oauth2/login-urls"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.kakao").value("/oauth2/authorization/kakao"))
      .andExpect(jsonPath("$.data.naver").value("/oauth2/authorization/naver"))
      .andExpect(jsonPath("$.data.google").value("/oauth2/authorization/google"))
      .andExpect(jsonPath("$.message").value("소셜 로그인 주소를 가져왔습니다."));
  }
}