package com.bifai.reminder.bifai_backend.batch;

import com.bifai.reminder.bifai_backend.entity.InteractionPattern;
import com.bifai.reminder.bifai_backend.repository.InteractionPatternRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 패턴 분석 결과 ItemWriter
 * InteractionPattern을 데이터베이스에 저장
 */
@Slf4j
public class PatternAnalysisItemWriter implements ItemWriter<InteractionPattern> {
  
  @Autowired
  private InteractionPatternRepository patternRepository;
  
  @Override
  public void write(Chunk<? extends InteractionPattern> patterns) throws Exception {
    // null이 아닌 패턴만 필터링
    List<InteractionPattern> validPatterns = patterns.getItems().stream()
      .filter(pattern -> pattern != null)
      .collect(Collectors.toList());
    
    if (validPatterns.isEmpty()) {
      log.debug("No valid patterns to write");
      return;
    }
    
    log.info("Writing {} interaction patterns to database", validPatterns.size());
    
    // 배치 저장
    List<InteractionPattern> saved = patternRepository.saveAll(validPatterns);
    
    // 이상 패턴 로깅
    long anomalyCount = saved.stream()
      .filter(p -> Boolean.TRUE.equals(p.getIsAnomaly()))
      .count();
    
    if (anomalyCount > 0) {
      log.warn("Detected {} anomalous patterns in this batch", anomalyCount);
      
      // 높은 이상 점수를 가진 패턴 상세 로깅
      saved.stream()
        .filter(p -> Boolean.TRUE.equals(p.getIsAnomaly()) && p.getAnomalyScore() > 80)
        .forEach(p -> log.warn("High anomaly detected - User: {}, Score: {}, Type: {}", 
                              p.getUser().getUserId(), 
                              p.getAnomalyScore(), 
                              p.getPatternType()));
    }
    
    log.info("Successfully wrote {} patterns, {} anomalies detected", 
            saved.size(), anomalyCount);
  }
}