package com.bifai.reminder.bifai_backend.batch;

import com.bifai.reminder.bifai_backend.entity.InteractionPattern;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.service.InteractionPatternAnalysisService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 패턴 분석 ItemProcessor
 * 사용자별로 패턴을 분석하여 InteractionPattern 생성
 */
@Slf4j
public class PatternAnalysisItemProcessor implements ItemProcessor<User, InteractionPattern> {
  
  @Autowired
  private InteractionPatternAnalysisService analysisService;
  
  @Setter
  private String analysisDate;
  
  @Setter
  private String patternType;
  
  @Override
  public InteractionPattern process(User user) throws Exception {
    try {
      log.debug("Processing pattern analysis for user: {}, type: {}", 
               user.getUserId(), patternType);
      
      LocalDateTime date = analysisDate != null 
        ? LocalDate.parse(analysisDate, DateTimeFormatter.ISO_DATE).atStartOfDay()
        : LocalDateTime.now();
      
      InteractionPattern pattern = null;
      
      switch (patternType) {
        case "DAILY":
          pattern = analysisService.analyzeDailyPattern(user.getUserId(), date);
          break;
          
        case "WEEKLY":
          // 주간 패턴 분석 (구현 필요)
          pattern = analyzeWeeklyPattern(user, date);
          break;
          
        case "REALTIME":
          pattern = analysisService.analyzeRealtimePattern(user.getUserId())
            .get(); // 동기 처리를 위해 get() 호출
          break;
          
        default:
          log.warn("Unknown pattern type: {}", patternType);
      }
      
      if (pattern != null) {
        log.info("Pattern analysis completed for user: {}, anomaly: {}", 
                user.getUserId(), pattern.getIsAnomaly());
      }
      
      return pattern;
      
    } catch (Exception e) {
      log.error("Error processing pattern for user: {}", user.getUserId(), e);
      // null 반환하여 이 아이템 스킵
      return null;
    }
  }
  
  private InteractionPattern analyzeWeeklyPattern(User user, LocalDateTime date) {
    // TODO: 주간 패턴 분석 로직 구현
    log.info("Weekly pattern analysis for user: {} (placeholder)", user.getUserId());
    return null;
  }
}