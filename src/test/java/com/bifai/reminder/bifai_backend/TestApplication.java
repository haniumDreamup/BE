package com.bifai.reminder.bifai_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 애플리케이션 클래스
 */
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@Profile("test")
public class TestApplication {
  
  public static void main(String[] args) {
    SpringApplication.run(TestApplication.class, args);
  }
}