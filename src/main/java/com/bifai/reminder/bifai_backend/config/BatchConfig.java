package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.batch.PatternAnalysisItemProcessor;
import com.bifai.reminder.bifai_backend.batch.PatternAnalysisItemReader;
import com.bifai.reminder.bifai_backend.batch.PatternAnalysisItemWriter;
import com.bifai.reminder.bifai_backend.entity.InteractionPattern;
import com.bifai.reminder.bifai_backend.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch 설정
 * 패턴 분석 배치 작업 구성
 */
@Configuration
@ConditionalOnProperty(name = "spring.batch.job.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {
  
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  
  /**
   * 패턴 분석 Job
   */
  @Bean
  public Job patternAnalysisJob(Step dailyPatternAnalysisStep,
                                Step weeklyPatternAnalysisStep,
                                Step anomalyDetectionStep) {
    return new JobBuilder("patternAnalysisJob", jobRepository)
      .incrementer(new RunIdIncrementer())
      .start(dailyPatternAnalysisStep)
      .next(weeklyPatternAnalysisStep)
      .next(anomalyDetectionStep)
      .build();
  }
  
  /**
   * 일일 패턴 분석 Step
   */
  @Bean
  public Step dailyPatternAnalysisStep(ItemReader<User> userReader,
                                       ItemProcessor<User, InteractionPattern> dailyPatternProcessor,
                                       ItemWriter<InteractionPattern> patternWriter) {
    return new StepBuilder("dailyPatternAnalysisStep", jobRepository)
      .<User, InteractionPattern>chunk(10, transactionManager)
      .reader(userReader)
      .processor(dailyPatternProcessor)
      .writer(patternWriter)
      .taskExecutor(batchTaskExecutor())
      .throttleLimit(4)
      .build();
  }
  
  /**
   * 주간 패턴 분석 Step
   */
  @Bean
  public Step weeklyPatternAnalysisStep(ItemReader<User> userReader,
                                        ItemProcessor<User, InteractionPattern> weeklyPatternProcessor,
                                        ItemWriter<InteractionPattern> patternWriter) {
    return new StepBuilder("weeklyPatternAnalysisStep", jobRepository)
      .<User, InteractionPattern>chunk(5, transactionManager)
      .reader(userReader)
      .processor(weeklyPatternProcessor)
      .writer(patternWriter)
      .build();
  }
  
  /**
   * 이상 패턴 감지 Step
   */
  @Bean
  public Step anomalyDetectionStep(ItemReader<InteractionPattern> patternReader,
                                   ItemProcessor<InteractionPattern, InteractionPattern> anomalyProcessor,
                                   ItemWriter<InteractionPattern> anomalyWriter) {
    return new StepBuilder("anomalyDetectionStep", jobRepository)
      .<InteractionPattern, InteractionPattern>chunk(20, transactionManager)
      .reader(patternReader)
      .processor(anomalyProcessor)
      .writer(anomalyWriter)
      .build();
  }
  
  /**
   * 배치용 TaskExecutor
   */
  @Bean
  public TaskExecutor batchTaskExecutor() {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-");
    executor.setConcurrencyLimit(4);
    return executor;
  }
  
  /**
   * 사용자 Reader
   */
  @Bean
  @StepScope
  public PatternAnalysisItemReader userReader(@Value("#{jobParameters['date']}") String date) {
    PatternAnalysisItemReader reader = new PatternAnalysisItemReader();
    reader.setAnalysisDate(date);
    return reader;
  }
  
  /**
   * 일일 패턴 Processor
   */
  @Bean
  @StepScope
  public PatternAnalysisItemProcessor dailyPatternProcessor(
    @Value("#{jobParameters['date']}") String date) {
    PatternAnalysisItemProcessor processor = new PatternAnalysisItemProcessor();
    processor.setAnalysisDate(date);
    processor.setPatternType("DAILY");
    return processor;
  }
  
  /**
   * 주간 패턴 Processor
   */
  @Bean
  @StepScope
  public PatternAnalysisItemProcessor weeklyPatternProcessor(
    @Value("#{jobParameters['date']}") String date) {
    PatternAnalysisItemProcessor processor = new PatternAnalysisItemProcessor();
    processor.setAnalysisDate(date);
    processor.setPatternType("WEEKLY");
    return processor;
  }
  
  /**
   * 패턴 Writer
   */
  @Bean
  public PatternAnalysisItemWriter patternWriter() {
    return new PatternAnalysisItemWriter();
  }
}