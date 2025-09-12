package com.bifai.reminder.bifai_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
  
  @GetMapping("/health")
  public String healthCheck() {
    return "OK";
  }
}