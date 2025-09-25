package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.bifai.reminder.bifai_backend.dto.notification.NotificationSettingsDto;
import com.bifai.reminder.bifai_backend.dto.notification.TestNotificationRequest;
import com.bifai.reminder.bifai_backend.dto.notification.UpdateFcmTokenRequest;
import com.bifai.reminder.bifai_backend.service.notification.FcmService;
import com.bifai.reminder.bifai_backend.service.notification.NotificationScheduler;
import com.bifai.reminder.bifai_backend.service.notification.NotificationSettingsService;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관리 API")
public class NotificationController {
  
  private final FcmService fcmService;
  private final NotificationScheduler notificationScheduler;
  private final NotificationSettingsService settingsService;
  
  @PostMapping("/fcm-token")
  @Operation(summary = "FCM 토큰 업데이트", 
      description = "디바이스의 FCM 토큰을 업데이트합니다")
  public ResponseEntity<ApiResponse<Void>> updateFcmToken(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody UpdateFcmTokenRequest request) {
    
    log.info("FCM 토큰 업데이트 - user: {}, deviceId: {}", 
        userDetails.getUserId(), request.getDeviceId());
    
    settingsService.updateFcmToken(
        userDetails.getUserId(), 
        request.getDeviceId(), 
        request.getFcmToken()
    );
    
    return ResponseEntity.ok(ApiResponse.success(null, "FCM 토큰이 업데이트되었습니다"));
  }
  
  @DeleteMapping("/fcm-token/{deviceId}")
  @Operation(summary = "FCM 토큰 삭제", 
      description = "디바이스의 FCM 토큰을 삭제합니다")
  public ResponseEntity<ApiResponse<Void>> removeFcmToken(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @PathVariable String deviceId) {
    
    log.info("FCM 토큰 삭제 - user: {}, deviceId: {}", 
        userDetails.getUserId(), deviceId);
    
    settingsService.removeFcmToken(userDetails.getUserId(), deviceId);
    
    return ResponseEntity.ok(ApiResponse.success(null, "FCM 토큰이 삭제되었습니다"));
  }
  
  @GetMapping("/settings")
  @Operation(summary = "알림 설정 조회", 
      description = "사용자의 알림 설정을 조회합니다")
  public ResponseEntity<ApiResponse<NotificationSettingsDto>> getSettings(
      @AuthenticationPrincipal BifUserDetails userDetails) {
    
    NotificationSettingsDto settings = settingsService.getSettings(userDetails.getUserId());
    return ResponseEntity.ok(ApiResponse.success(settings));
  }
  
  @PutMapping("/settings")
  @Operation(summary = "알림 설정 업데이트", 
      description = "사용자의 알림 설정을 업데이트합니다")
  public ResponseEntity<ApiResponse<NotificationSettingsDto>> updateSettings(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @Valid @RequestBody NotificationSettingsDto request) {
    
    log.info("알림 설정 업데이트 - user: {}", userDetails.getUserId());
    
    NotificationSettingsDto updated = settingsService.updateSettings(
        userDetails.getUserId(), request);
    
    return ResponseEntity.ok(ApiResponse.success(updated, "알림 설정이 업데이트되었습니다"));
  }
  
  
  @PostMapping("/emergency")
  @Operation(summary = "긴급 알림 전송", 
      description = "보호자들에게 긴급 알림을 전송합니다")
  public ResponseEntity<ApiResponse<Void>> sendEmergencyAlert(
      @AuthenticationPrincipal BifUserDetails userDetails,
      @RequestParam String message,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {
    
    log.warn("긴급 알림 요청 - user: {}, message: {}", 
        userDetails.getUserId(), message);
    
    notificationScheduler.sendEmergencyToGuardians(
        userDetails.getUserId(),
        message,
        latitude,
        longitude
    );
    
    return ResponseEntity.ok(ApiResponse.success(null, "긴급 알림이 전송되었습니다"));
  }
  
  @PostMapping("/validate-token")
  @Operation(summary = "FCM 토큰 검증", 
      description = "FCM 토큰의 유효성을 검증합니다")
  public ResponseEntity<ApiResponse<Boolean>> validateFcmToken(
      @RequestParam String token) {
    
    boolean isValid = fcmService.validateToken(token);
    
    return ResponseEntity.ok(ApiResponse.success(
        isValid, 
        isValid ? "유효한 토큰입니다" : "유효하지 않은 토큰입니다"
    ));
  }
}