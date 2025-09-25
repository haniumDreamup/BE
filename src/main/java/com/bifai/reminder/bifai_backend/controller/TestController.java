package com.bifai.reminder.bifai_backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/**
 * 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
  

  /**
   * 날짜 파라미터 테스트용 엔드포인트 (400 에러 테스트용)
   */
  @GetMapping("/date")
  public String testDateParameter(
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return "Date received: " + date;
  }

  /**
   * 에코 테스트 엔드포인트
   */
  @PostMapping("/echo")
  public ResponseEntity<String> echo(@RequestBody(required = false) String message) {
    String response = "Echo: " + (message != null ? message : "empty");
    return ResponseEntity.ok(response);
  }
}