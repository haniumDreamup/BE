package com.bifai.reminder.bifai_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 컨트롤러
 */
@RestController
public class HealthController {
  
  @GetMapping("/api/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Application is running");
    return ResponseEntity.ok(response);
  }
  
  
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> basicHealth() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Application is running");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api/v1/health")
  public ResponseEntity<Map<String, String>> healthV1() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Application is running");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api/health/liveness")
  public ResponseEntity<Map<String, String>> liveness() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Application is alive");
    return ResponseEntity.ok(response);
  }

  @GetMapping("/api/health/readiness")
  public ResponseEntity<Map<String, String>> readiness() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    response.put("message", "Application is ready");
    return ResponseEntity.ok(response);
  }

  /**
   * 테스트용 Health 체크 엔드포인트 (기존 TestController에서 이동)
   */
  @GetMapping("/api/test/health")
  public String testHealthCheck() {
    return "OK";
  }
}