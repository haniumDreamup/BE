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
  
  @GetMapping("/api/v1/health")
  public ResponseEntity<Map<String, String>> healthV1() {
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
}