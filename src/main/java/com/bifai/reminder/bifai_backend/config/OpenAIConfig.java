package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

  /**
   * ChatClient 빈 생성
   *
   * OpenAiChatModel이 존재하고, API 키가 설정된 경우에만 생성
   */
  @Bean
  @ConditionalOnBean(OpenAiChatModel.class)
  @ConditionalOnProperty(name = "spring.ai.openai.api-key")
  public ChatClient chatClient(OpenAiChatModel chatModel) {
    log.info("OpenAI ChatClient 초기화 완료");
    return ChatClient.builder(chatModel).build();
  }
}
