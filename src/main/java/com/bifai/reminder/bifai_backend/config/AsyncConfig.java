package com.bifai.reminder.bifai_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 * 로깅 및 이벤트 처리를 위한 스레드 풀 구성
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
  
  @Bean(name = "taskExecutor")
  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    
    // 스레드 풀 설정
    executor.setCorePoolSize(4);  // 기본 스레드 수
    executor.setMaxPoolSize(10);  // 최대 스레드 수
    executor.setQueueCapacity(100);  // 큐 용량
    executor.setThreadNamePrefix("BIF-Async-");
    executor.setKeepAliveSeconds(60);
    
    // 거부 정책: 호출자 스레드에서 실행
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    
    // 종료 시 태스크 완료 대기
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    
    executor.initialize();
    
    log.info("비동기 태스크 실행자 설정 완료 - 코어: {}, 최대: {}, 큐: {}", 
             executor.getCorePoolSize(), 
             executor.getMaxPoolSize(), 
             executor.getQueueCapacity());
    
    return executor;
  }
  
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new CustomAsyncExceptionHandler();
  }
  
  /**
   * 비동기 예외 처리기
   */
  public static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    
    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
      log.error("비동기 메서드 실행 중 예외 발생 - 메서드: {}, 파라미터: {}", 
                method.getName(), params, throwable);
      
      // 여기에 추가적인 에러 처리 로직 구현 가능
      // 예: 알림 전송, 에러 로그 DB 저장 등
    }
  }
}