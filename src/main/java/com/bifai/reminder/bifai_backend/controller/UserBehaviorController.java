package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog;
import com.bifai.reminder.bifai_backend.entity.UserBehaviorLog.ActionType;
import com.bifai.reminder.bifai_backend.event.UserBehaviorEvent;
import com.bifai.reminder.bifai_backend.service.UserBehaviorLoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 사용자 행동 로깅 컨트롤러
 * 클라이언트에서 발생한 이벤트를 수집
 */
@RestController
@RequestMapping("/api/v1/behavior")
@RequiredArgsConstructor
@Tag(name = "User Behavior", description = "사용자 행동 로깅 API")
@Slf4j
public class UserBehaviorController {
  
  private final UserBehaviorLoggingService loggingService;
  private final ApplicationEventPublisher eventPublisher;
  
  /**
   * 사용자 행동 로그 전송
   */
  @PostMapping("/log")
  @Operation(summary = "행동 로그 전송", description = "사용자의 인터랙션을 서버로 전송합니다")
  public ResponseEntity<ApiResponse<String>> logBehavior(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody BehaviorLogRequest request,
      HttpServletRequest httpRequest) {
    
    try {
      // 사용자 ID 추출 (실제 구현에서는 UserDetails에서 추출)
      Long userId = getUserIdFromUserDetails(userDetails);
      
      // 비동기 로깅
      CompletableFuture<UserBehaviorLog> future = loggingService.logUserBehaviorAsync(
          userId,
          request.getSessionId(),
          request.getActionType(),
          request.getActionDetail(),
          httpRequest
      );
      
      // 이벤트 발행
      eventPublisher.publishEvent(
          UserBehaviorEvent.withDetail(
              this,
              userId,
              request.getSessionId(),
              request.getActionType(),
              request.getActionDetail()
          )
      );
      
      return ResponseEntity.ok(ApiResponse.success("로그가 성공적으로 전송되었습니다"));
      
    } catch (Exception e) {
      log.error("행동 로그 전송 실패", e);
      return ResponseEntity.ok(ApiResponse.error("로그 전송에 실패했습니다"));
    }
  }
  
  /**
   * 배치 로그 전송
   */
  @PostMapping("/batch")
  @Operation(summary = "배치 로그 전송", description = "여러 개의 로그를 한 번에 전송합니다")
  public ResponseEntity<ApiResponse<BatchLogResponse>> logBehaviorBatch(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody BatchLogRequest request,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromUserDetails(userDetails);
    int successCount = 0;
    int failCount = 0;
    
    for (BehaviorLogRequest logRequest : request.getLogs()) {
      try {
        loggingService.logUserBehaviorAsync(
            userId,
            logRequest.getSessionId(),
            logRequest.getActionType(),
            logRequest.getActionDetail(),
            httpRequest
        );
        successCount++;
      } catch (Exception e) {
        log.error("배치 로그 처리 실패", e);
        failCount++;
      }
    }
    
    BatchLogResponse response = new BatchLogResponse(successCount, failCount);
    return ResponseEntity.ok(ApiResponse.success(response));
  }
  
  /**
   * 페이지 뷰 로그
   */
  @PostMapping("/pageview")
  @Operation(summary = "페이지 뷰 로그", description = "페이지 조회 이벤트를 기록합니다")
  public ResponseEntity<ApiResponse<String>> logPageView(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody PageViewRequest request,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromUserDetails(userDetails);
    
    Map<String, Object> detail = new HashMap<>();
    detail.put("pageTitle", request.getPageTitle());
    detail.put("duration", request.getDuration());
    detail.put("scrollDepth", request.getScrollDepth());
    
    loggingService.logUserBehaviorAsync(
        userId,
        request.getSessionId(),
        ActionType.PAGE_VIEW,
        detail,
        httpRequest
    );
    
    return ResponseEntity.ok(ApiResponse.success("페이지 뷰가 기록되었습니다"));
  }
  
  /**
   * 클릭 이벤트 로그
   */
  @PostMapping("/click")
  @Operation(summary = "클릭 이벤트 로그", description = "버튼 또는 링크 클릭을 기록합니다")
  public ResponseEntity<ApiResponse<String>> logClick(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody ClickEventRequest request,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromUserDetails(userDetails);
    
    Map<String, Object> detail = new HashMap<>();
    detail.put("elementId", request.getElementId());
    detail.put("elementText", request.getElementText());
    detail.put("elementType", request.getElementType());
    detail.put("position", request.getPosition());
    
    ActionType actionType = determineClickActionType(request.getElementType());
    
    loggingService.logUserBehaviorAsync(
        userId,
        request.getSessionId(),
        actionType,
        detail,
        httpRequest
    );
    
    return ResponseEntity.ok(ApiResponse.success("클릭 이벤트가 기록되었습니다"));
  }
  
  /**
   * 오류 로그
   */
  @PostMapping("/error")
  @Operation(summary = "오류 로그", description = "클라이언트에서 발생한 오류를 기록합니다")
  public ResponseEntity<ApiResponse<String>> logError(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody ErrorLogRequest request,
      HttpServletRequest httpRequest) {
    
    Long userId = getUserIdFromUserDetails(userDetails);
    
    Map<String, Object> detail = new HashMap<>();
    detail.put("errorMessage", request.getErrorMessage());
    detail.put("errorCode", request.getErrorCode());
    detail.put("stackTrace", request.getStackTrace());
    detail.put("userAction", request.getUserAction());
    
    loggingService.logUserBehaviorAsync(
        userId,
        request.getSessionId(),
        ActionType.ERROR,
        detail,
        httpRequest
    );
    
    return ResponseEntity.ok(ApiResponse.success("오류가 기록되었습니다"));
  }
  
  private Long getUserIdFromUserDetails(UserDetails userDetails) {
    // 실제 구현에서는 UserDetails에서 userId를 추출
    // 여기서는 임시로 1L 반환
    return 1L;
  }
  
  private ActionType determineClickActionType(String elementType) {
    if ("button".equalsIgnoreCase(elementType)) {
      return ActionType.BUTTON_CLICK;
    } else if ("link".equalsIgnoreCase(elementType) || "a".equalsIgnoreCase(elementType)) {
      return ActionType.LINK_CLICK;
    } else if ("image".equalsIgnoreCase(elementType) || "img".equalsIgnoreCase(elementType)) {
      return ActionType.IMAGE_CLICK;
    }
    return ActionType.BUTTON_CLICK;
  }
  
  /**
   * 행동 로그 요청 DTO
   */
  @lombok.Data
  public static class BehaviorLogRequest {
    private String sessionId;
    private ActionType actionType;
    private Map<String, Object> actionDetail;
  }
  
  /**
   * 배치 로그 요청 DTO
   */
  @lombok.Data
  public static class BatchLogRequest {
    private java.util.List<BehaviorLogRequest> logs;
  }
  
  /**
   * 배치 로그 응답 DTO
   */
  @lombok.Data
  @lombok.AllArgsConstructor
  public static class BatchLogResponse {
    private int successCount;
    private int failCount;
  }
  
  /**
   * 페이지 뷰 요청 DTO
   */
  @lombok.Data
  public static class PageViewRequest {
    private String sessionId;
    private String pageTitle;
    private Long duration;  // 페이지 체류 시간 (ms)
    private Integer scrollDepth;  // 스크롤 깊이 (%)
  }
  
  /**
   * 클릭 이벤트 요청 DTO
   */
  @lombok.Data
  public static class ClickEventRequest {
    private String sessionId;
    private String elementId;
    private String elementText;
    private String elementType;  // button, link, image
    private Map<String, Integer> position;  // x, y 좌표
  }
  
  /**
   * 오류 로그 요청 DTO
   */
  @lombok.Data
  public static class ErrorLogRequest {
    private String sessionId;
    private String errorMessage;
    private String errorCode;
    private String stackTrace;
    private String userAction;  // 오류 발생 시 사용자가 하던 작업
  }
}