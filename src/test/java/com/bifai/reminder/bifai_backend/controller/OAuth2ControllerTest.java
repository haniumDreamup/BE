package com.bifai.reminder.bifai_backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OAuth2ControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("OAuth2 로그인 URL 조회 성공")
  void testGetOAuth2LoginUrls() throws Exception {
    mockMvc.perform(get("/api/v1/auth/oauth2/login-urls"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.data.kakao").value("/oauth2/authorization/kakao"))
      .andExpect(jsonPath("$.data.naver").value("/oauth2/authorization/naver"))
      .andExpect(jsonPath("$.data.google").value("/oauth2/authorization/google"))
      .andExpect(jsonPath("$.message").value("소셜 로그인 주소를 가져왔습니다."));
  }
}