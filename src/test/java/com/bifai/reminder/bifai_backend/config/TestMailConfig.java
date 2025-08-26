package com.bifai.reminder.bifai_backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 메일 관련 테스트 설정
 */
@TestConfiguration
@Profile("test")
public class TestMailConfig {
  
  @MockBean
  private JavaMailSender javaMailSender;
}