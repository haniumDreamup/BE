package com.bifai.reminder.bifai_backend.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLContext;

/**
 * HTTP Client 테스트 설정
 * Spring Boot 3.5에서 필요한 Apache HttpClient 5 설정
 */
@TestConfiguration
@Profile("test")
public class TestHttpClientConfig {
  
  @Bean
  @Primary
  public RestTemplate restTemplate() {
    // SSL Context 생성
    SSLContext sslContext = SSLContexts.createSystemDefault();
    
    // Connection Manager 생성
    PoolingHttpClientConnectionManager connectionManager = 
      PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(SSLConnectionSocketFactory.getSystemSocketFactory())
        .build();
    
    // HttpClient 생성
    CloseableHttpClient httpClient = HttpClients.custom()
      .setConnectionManager(connectionManager)
      .build();
    
    // HttpComponentsClientHttpRequestFactory 생성
    HttpComponentsClientHttpRequestFactory factory = 
      new HttpComponentsClientHttpRequestFactory(httpClient);
    factory.setConnectTimeout(5000);
    factory.setConnectionRequestTimeout(5000);
    
    return new RestTemplate(factory);
  }
  
  @Bean
  @Primary
  public WebClient webClient() {
    return WebClient.builder()
      .baseUrl("http://localhost:8080")
      .build();
  }
}