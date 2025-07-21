package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/v1/health")
public class HealthController {
    
    @Value("${app.name:BIF-AI Reminder Backend}")
    private String appName;
    
    @Value("${app.version:0.0.1}")
    private String appVersion;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("Health check requested");
        
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("application", appName);
        healthData.put("version", appVersion);
        healthData.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(healthData)
                .message("시스템이 정상적으로 작동하고 있습니다")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }
    
    @GetMapping("/simple")
    public ResponseEntity<String> simpleHealth() {
        return ResponseEntity.ok("OK");
    }
}