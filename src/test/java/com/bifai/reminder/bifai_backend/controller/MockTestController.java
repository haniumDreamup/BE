package com.bifai.reminder.bifai_backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 보안 테스트를 위한 Mock 컨트롤러
 */
@RestController
@RequestMapping("/api")
@Profile("test")
public class MockTestController {
  
  @GetMapping("/test/health")
  public ResponseEntity<Map<String, String>> testHealth() {
    Map<String, String> response = new HashMap<>();
    response.put("status", "UP");
    return ResponseEntity.ok(response);
  }
  
  @GetMapping("/users")
  public ResponseEntity<Map<String, String>> getUsers(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String id) {
    Map<String, String> response = new HashMap<>();
    response.put("status", "OK");
    return ResponseEntity.ok(response);
  }
  
  @PostMapping("/users")
  public ResponseEntity<Map<String, String>> createUser(@RequestBody Map<String, Object> user) {
    Map<String, String> response = new HashMap<>();
    response.put("status", "CREATED");
    return ResponseEntity.ok(response);
  }
  
  @PostMapping("/comments")
  public ResponseEntity<Map<String, String>> createComment(
      @RequestBody(required = false) Map<String, Object> comment,
      @RequestParam(required = false) String text) {
    Map<String, String> response = new HashMap<>();
    response.put("status", "CREATED");
    return ResponseEntity.ok(response);
  }
  
  @GetMapping("/files/**")
  public ResponseEntity<Map<String, String>> getFile(
      @RequestParam(required = false) String path) {
    Map<String, String> response = new HashMap<>();
    response.put("status", "OK");
    return ResponseEntity.ok(response);
  }
}