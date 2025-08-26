package com.bifai.reminder.bifai_backend.security;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * Security 테스트용 Mock Controller
 * SecurityConfigTest에서 사용하는 엔드포인트 제공
 */
@RestController
@TestComponent
@Profile("test")
@RequestMapping("/api")
public class TestSecurityController {
  
  @GetMapping("/health")
  public String health() {
    return "OK";
  }
  
  @GetMapping("/users")
  public String getUsers(@RequestParam(required = false) String name,
                         @RequestParam(required = false) String id) {
    return "users";
  }
  
  @PostMapping("/users")
  public String createUser(@RequestBody(required = false) String body) {
    return "created";
  }
  
  @PostMapping("/comments")
  public String createComment(@RequestBody(required = false) String body,
                              @RequestParam(required = false) String text) {
    return "comment";
  }
  
  @GetMapping("/files/{type}")
  public String getFile(@PathVariable String type,
                        @RequestParam(required = false) String path) {
    return "file";
  }
}