package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.NotificationTemplate;
import com.bifai.reminder.bifai_backend.entity.NotificationTemplate.EventType;
import com.bifai.reminder.bifai_backend.entity.NotificationTemplate.SeverityLevel;
import com.bifai.reminder.bifai_backend.entity.NotificationTemplate.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 템플릿 레포지토리
 * 템플릿 조회, 관리 및 사용 통계 관리
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

  /**
   * 템플릿 코드로 조회
   */
  Optional<NotificationTemplate> findByTemplateCode(String templateCode);

  /**
   * 템플릿 코드와 언어로 조회
   */
  Optional<NotificationTemplate> findByTemplateCodeAndLanguageCode(String templateCode, String languageCode);

  /**
   * 이벤트 타입으로 활성 템플릿 조회
   */
  List<NotificationTemplate> findByEventTypeAndIsActiveTrue(EventType eventType);

  /**
   * 이벤트 타입과 언어로 활성 템플릿 조회
   */
  Optional<NotificationTemplate> findByEventTypeAndLanguageCodeAndIsActiveTrue(
      EventType eventType, 
      String languageCode
  );

  /**
   * 심각도 레벨별 템플릿 조회
   */
  List<NotificationTemplate> findBySeverityLevelAndIsActiveTrue(SeverityLevel severityLevel);

  /**
   * 템플릿 타입별 조회
   */
  List<NotificationTemplate> findByTemplateTypeAndIsActiveTrue(TemplateType templateType);

  /**
   * 우선순위 순으로 정렬된 활성 템플릿
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
         "ORDER BY t.priority DESC, t.createdAt DESC")
  List<NotificationTemplate> findActiveTemplatesOrderByPriority();

  /**
   * 즉시 처리가 필요한 템플릿
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
         "AND t.requiresImmediateAction = true " +
         "ORDER BY t.priority DESC")
  List<NotificationTemplate> findUrgentTemplates();

  /**
   * 에스컬레이션이 필요한 템플릿
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
         "AND t.escalationMinutes IS NOT NULL AND t.escalationMinutes > 0")
  List<NotificationTemplate> findTemplatesRequiringEscalation();

  /**
   * 태그로 템플릿 검색
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
         "AND t.tags LIKE %:tag%")
  List<NotificationTemplate> findByTag(@Param("tag") String tag);

  /**
   * 이벤트 타입과 심각도로 조회
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.eventType = :eventType " +
         "AND t.severityLevel = :severityLevel AND t.isActive = true " +
         "ORDER BY t.priority DESC")
  List<NotificationTemplate> findByEventTypeAndSeverity(
      @Param("eventType") EventType eventType,
      @Param("severityLevel") SeverityLevel severityLevel
  );

  /**
   * 가장 많이 사용된 템플릿 조회
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
         "ORDER BY t.usageCount DESC")
  List<NotificationTemplate> findMostUsedTemplates();

  /**
   * 최근 사용된 템플릿 조회
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.lastUsedAt IS NOT NULL " +
         "ORDER BY t.lastUsedAt DESC")
  List<NotificationTemplate> findRecentlyUsedTemplates();

  /**
   * 사용 횟수 증가
   */
  @Modifying
  @Query("UPDATE NotificationTemplate t SET t.usageCount = t.usageCount + 1, " +
         "t.lastUsedAt = :now WHERE t.id = :templateId")
  void incrementUsageCount(
      @Param("templateId") Long templateId, 
      @Param("now") LocalDateTime now
  );

  /**
   * 템플릿 활성화/비활성화
   */
  @Modifying
  @Query("UPDATE NotificationTemplate t SET t.isActive = :active WHERE t.id = :templateId")
  void updateActiveStatus(@Param("templateId") Long templateId, @Param("active") Boolean active);

  /**
   * 언어별 템플릿 존재 확인
   */
  boolean existsByTemplateCodeAndLanguageCode(String templateCode, String languageCode);

  /**
   * 활성 템플릿 수 조회
   */
  @Query("SELECT COUNT(t) FROM NotificationTemplate t WHERE t.isActive = true")
  long countActiveTemplates();

  /**
   * 이벤트 타입별 템플릿 수 조회
   */
  @Query("SELECT t.eventType, COUNT(t) FROM NotificationTemplate t " +
         "WHERE t.isActive = true GROUP BY t.eventType")
  List<Object[]> countTemplatesByEventType();

  /**
   * 다중 채널 템플릿 조회
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.templateType = 'MULTI_CHANNEL' " +
         "AND t.isActive = true ORDER BY t.priority DESC")
  List<NotificationTemplate> findMultiChannelTemplates();

  /**
   * 특정 기간 동안 생성된 템플릿
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.createdAt BETWEEN :startDate AND :endDate")
  List<NotificationTemplate> findTemplatesCreatedBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate
  );

  /**
   * 사용되지 않은 템플릿 조회
   */
  @Query("SELECT t FROM NotificationTemplate t WHERE t.usageCount = 0 " +
         "AND t.createdAt < :beforeDate")
  List<NotificationTemplate> findUnusedTemplates(@Param("beforeDate") LocalDateTime beforeDate);
}