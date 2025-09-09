package com.bifai.reminder.bifai_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Simple HTTP client configuration for basic functionality
 */
@Configuration
public class SimpleRestTemplateConfig {
  
  /**
   * Basic RestTemplate bean with default configuration
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
  
  /**
   * Basic WebClient bean with default configuration
   */
  @Bean
  public WebClient webClient() {
    return WebClient.builder().build();
  }
}