package com.bifai.reminder.bifai_backend.event;

import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 사용자 행동 이벤트
 * 사용자 인터랙션이 발생할 때 발행되는 이벤트
 */
@Getter
public class UserBehaviorEvent extends ApplicationEvent {
  
  private final Long userId;
  private final String sessionId;
  private final ActionType actionType;
  private final Map<String, Object> actionDetail;
  private final String pageUrl;
  private final LocalDateTime eventTimestamp;
  
  public UserBehaviorEvent(Object source, Long userId, String sessionId, 
                          ActionType actionType, Map<String, Object> actionDetail,
                          String pageUrl) {
    super(source);
    this.userId = userId;
    this.sessionId = sessionId;
    this.actionType = actionType;
    this.actionDetail = actionDetail;
    this.pageUrl = pageUrl;
    this.eventTimestamp = LocalDateTime.now();
  }
  
  /**
   * 간단한 이벤트 생성 헬퍼
   */
  public static UserBehaviorEvent of(Object source, Long userId, String sessionId, 
                                     ActionType actionType) {
    return new UserBehaviorEvent(source, userId, sessionId, actionType, null, null);
  }
  
  /**
   * 상세 이벤트 생성 헬퍼
   */
  public static UserBehaviorEvent withDetail(Object source, Long userId, String sessionId,
                                             ActionType actionType, Map<String, Object> detail) {
    return new UserBehaviorEvent(source, userId, sessionId, actionType, detail, null);
  }
}