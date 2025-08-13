package com.bifai.reminder.bifai_backend.event;

import com.bifai.reminder.bifai_backend.service.UserBehaviorLoggingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 사용자 행동 이벤트 리스너
 * 발행된 이벤트를 받아 비동기로 로깅 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorEventListener {
  
  private final UserBehaviorLoggingService loggingService;
  
  /**
   * 사용자 행동 이벤트 처리
   */
  @EventListener
  @Async("taskExecutor")
  public void handleUserBehaviorEvent(UserBehaviorEvent event) {
    try {
      log.debug("사용자 행동 이벤트 수신 - userId: {}, actionType: {}", 
               event.getUserId(), event.getActionType());
      
      // 현재 요청 컨텍스트 가져오기
      ServletRequestAttributes attributes = 
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        
        // 비동기 로깅
        loggingService.logUserBehaviorAsync(
            event.getUserId(),
            event.getSessionId(),
            event.getActionType(),
            event.getActionDetail(),
            request
        );
      } else {
        // 요청 컨텍스트가 없는 경우 간단한 로깅
        loggingService.logSimpleEvent(
            event.getUserId(),
            event.getSessionId(),
            event.getActionType(),
            "이벤트 처리 - 요청 컨텍스트 없음"
        );
      }
      
    } catch (Exception e) {
      log.error("사용자 행동 이벤트 처리 실패 - userId: {}, actionType: {}", 
               event.getUserId(), event.getActionType(), e);
    }
  }
}