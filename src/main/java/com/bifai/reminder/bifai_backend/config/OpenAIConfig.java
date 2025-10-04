package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAI ChatGPT 설정
 * Spring AI를 사용한 ChatClient 빈 생성
 *
 * Note: Spring AI Auto-configuration이 OpenAiChatModel을 자동으로 생성합니다.
 * application.yml의 spring.ai.openai 설정을 사용합니다.
 */
@Slf4j
@Configuration
@Profile("!test")
public class OpenAIConfig {

  @Value("${spring.ai.openai.api-key:}")
  private String apiKey;

  /**
   * ChatClient 빈 생성
   *
   * OpenAiChatModel은 Spring AI Auto-configuration에서 자동 생성
   * OpenAiChatModel이 있을 때만 ChatClient를 생성
   */
  @Bean
  @ConditionalOnBean(OpenAiChatModel.class)
  public ChatClient chatClient(@Autowired OpenAiChatModel chatModel) {
    if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("sk-dummy")) {
      log.warn("OpenAI API 키가 유효하지 않습니다");
      return null;
    }

    log.info("OpenAI ChatClient 초기화 완료");
    return ChatClient.builder(chatModel).build();
  }
}
