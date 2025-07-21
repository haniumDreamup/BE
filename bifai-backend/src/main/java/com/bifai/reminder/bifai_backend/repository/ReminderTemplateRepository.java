package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.ReminderTemplate;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * BIF 사용자의 알림 템플릿 데이터 접근을 위한 Repository
 * 템플릿 성과 분석, 사용 통계, 개인화 기능 포함
 */
@Repository
public interface ReminderTemplateRepository extends JpaRepository<ReminderTemplate, Long> {

    /**
     * 사용자의 활성화된 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.isActive = true ORDER BY rt.usageCount DESC")
    List<ReminderTemplate> findActiveTemplatesByUser(@Param("user") User user);

    /**
     * 시스템 제공 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isSystemTemplate = true AND rt.isActive = true ORDER BY rt.usageCount DESC")
    List<ReminderTemplate> findActiveSystemTemplates();

    /**
     * 특정 타입의 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.templateType = :templateType AND rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) ORDER BY rt.usageCount DESC")
    List<ReminderTemplate> findTemplatesByType(
            @Param("user") User user,
            @Param("templateType") ReminderTemplate.TemplateType templateType);

    /**
     * 높은 성과의 템플릿 조회 (만족도 4점 이상 또는 성공률 80% 이상)
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) AND (rt.satisfactionScore >= 4.0 OR rt.successRate >= 80.0) ORDER BY rt.satisfactionScore DESC, rt.successRate DESC")
    List<ReminderTemplate> findHighPerformingTemplates(@Param("user") User user);

    /**
     * 인기 있는 템플릿 조회 (사용 횟수 10회 이상)
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) AND rt.usageCount >= 10 ORDER BY rt.usageCount DESC")
    List<ReminderTemplate> findPopularTemplates(@Param("user") User user);

    /**
     * 특정 복잡도 수준의 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.complexityLevel = :complexityLevel AND rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) ORDER BY rt.usageCount DESC")
    List<ReminderTemplate> findTemplatesByComplexityLevel(
            @Param("user") User user,
            @Param("complexityLevel") ReminderTemplate.ComplexityLevel complexityLevel);

    /**
     * 특정 감정적 톤의 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.emotionalTone = :emotionalTone AND rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) ORDER BY rt.usageCount DESC")
    List<ReminderTemplate> findTemplatesByEmotionalTone(
            @Param("user") User user,
            @Param("emotionalTone") ReminderTemplate.EmotionalTone emotionalTone);

    /**
     * 사용자별 템플릿 사용 통계
     */
    @Query("SELECT rt.templateType, SUM(rt.usageCount), AVG(rt.satisfactionScore), AVG(rt.successRate) FROM ReminderTemplate rt WHERE rt.user = :user GROUP BY rt.templateType")
    List<Object[]> getTemplateUsageStatsByUser(@Param("user") User user);

    /**
     * 가장 성공적인 템플릿 조회 (성공률 기준)
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) AND rt.successRate IS NOT NULL ORDER BY rt.successRate DESC")
    List<ReminderTemplate> findMostSuccessfulTemplates(@Param("user") User user, Pageable pageable);

    /**
     * 가장 만족도가 높은 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isActive = true AND (rt.user = :user OR rt.isSystemTemplate = true) AND rt.satisfactionScore IS NOT NULL ORDER BY rt.satisfactionScore DESC")
    List<ReminderTemplate> findMostSatisfyingTemplates(@Param("user") User user, Pageable pageable);

    /**
     * 사용자가 만든 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.isSystemTemplate = false ORDER BY rt.createdAt DESC")
    Page<ReminderTemplate> findUserCreatedTemplates(@Param("user") User user, Pageable pageable);

    /**
     * 성과가 낮은 템플릿 조회 (개선이 필요한)
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.isActive = true AND rt.usageCount > 0 AND (rt.satisfactionScore < 3.0 OR rt.successRate < 50.0) ORDER BY rt.satisfactionScore ASC, rt.successRate ASC")
    List<ReminderTemplate> findUnderperformingTemplates(@Param("user") User user);

    /**
     * 사용되지 않은 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.usageCount = 0 ORDER BY rt.createdAt ASC")
    List<ReminderTemplate> findUnusedTemplates(@Param("user") User user);

    /**
     * 템플릿 이름으로 검색
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.isActive = true AND LOWER(rt.templateName) LIKE LOWER(CONCAT('%', :templateName, '%'))")
    List<ReminderTemplate> findTemplatesByNameContaining(@Param("user") User user, @Param("templateName") String templateName);

    /**
     * 특정 우선순위 설정을 가진 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.defaultPriority = :priority AND rt.isActive = true")
    List<ReminderTemplate> findTemplatesByDefaultPriority(@Param("user") User user, @Param("priority") Integer priority);

    /**
     * 보호자 알림이 설정된 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.notifyGuardian = true AND rt.isActive = true")
    List<ReminderTemplate> findTemplatesWithGuardianNotification(@Param("user") User user);

    /**
     * 음성 안내가 활성화된 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.voiceEnabled = true AND rt.isActive = true")
    List<ReminderTemplate> findVoiceEnabledTemplates(@Param("user") User user);

    /**
     * 템플릿 사용 횟수 증가
     */
    @Modifying
    @Query("UPDATE ReminderTemplate rt SET rt.usageCount = rt.usageCount + 1 WHERE rt.id = :templateId")
    void incrementUsageCount(@Param("templateId") Long templateId);

    /**
     * 템플릿 만족도 점수 업데이트
     */
    @Modifying
    @Query("UPDATE ReminderTemplate rt SET rt.satisfactionScore = :satisfactionScore WHERE rt.id = :templateId")
    void updateSatisfactionScore(@Param("templateId") Long templateId, @Param("satisfactionScore") BigDecimal satisfactionScore);

    /**
     * 템플릿 성공률 업데이트
     */
    @Modifying
    @Query("UPDATE ReminderTemplate rt SET rt.successRate = :successRate WHERE rt.id = :templateId")
    void updateSuccessRate(@Param("templateId") Long templateId, @Param("successRate") BigDecimal successRate);

    /**
     * 사용자별 평균 만족도 조회
     */
    @Query("SELECT AVG(rt.satisfactionScore) FROM ReminderTemplate rt WHERE rt.user = :user AND rt.satisfactionScore IS NOT NULL")
    Optional<BigDecimal> getAverageSatisfactionByUser(@Param("user") User user);

    /**
     * 사용자별 평균 성공률 조회
     */
    @Query("SELECT AVG(rt.successRate) FROM ReminderTemplate rt WHERE rt.user = :user AND rt.successRate IS NOT NULL")
    Optional<BigDecimal> getAverageSuccessRateByUser(@Param("user") User user);

    /**
     * 템플릿 타입별 평균 성과 조회
     */
    @Query("SELECT rt.templateType, AVG(rt.satisfactionScore), AVG(rt.successRate), SUM(rt.usageCount) FROM ReminderTemplate rt WHERE rt.user = :user GROUP BY rt.templateType")
    List<Object[]> getPerformanceByTemplateType(@Param("user") User user);

    /**
     * 복잡도별 템플릿 성과 조회
     */
    @Query("SELECT rt.complexityLevel, AVG(rt.satisfactionScore), AVG(rt.successRate), COUNT(rt) FROM ReminderTemplate rt WHERE rt.user = :user GROUP BY rt.complexityLevel")
    List<Object[]> getPerformanceByComplexityLevel(@Param("user") User user);

    /**
     * 감정적 톤별 템플릿 성과 조회
     */
    @Query("SELECT rt.emotionalTone, AVG(rt.satisfactionScore), AVG(rt.successRate), COUNT(rt) FROM ReminderTemplate rt WHERE rt.user = :user GROUP BY rt.emotionalTone")
    List<Object[]> getPerformanceByEmotionalTone(@Param("user") User user);

    /**
     * 전체 시스템 템플릿 성과 순위
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isSystemTemplate = true AND rt.isActive = true ORDER BY (COALESCE(rt.satisfactionScore, 0) * 0.4 + COALESCE(rt.successRate, 0) * 0.4 + (rt.usageCount * 0.2)) DESC")
    List<ReminderTemplate> findTopPerformingSystemTemplates(Pageable pageable);

    /**
     * 사용자의 최근 사용 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.usageCount > 0 ORDER BY rt.updatedAt DESC")
    List<ReminderTemplate> findRecentlyUsedTemplatesByUser(@Param("user") User user, Pageable pageable);

    /**
     * 특정 시각적 표시를 가진 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.visualIndicator = :indicator AND rt.isActive = true")
    List<ReminderTemplate> findTemplatesByVisualIndicator(@Param("user") User user, @Param("indicator") String indicator);

    /**
     * 비활성화된 템플릿 조회
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.user = :user AND rt.isActive = false ORDER BY rt.updatedAt DESC")
    List<ReminderTemplate> findInactiveTemplatesByUser(@Param("user") User user);

    /**
     * 복제 가능한 시스템 템플릿 조회 (사용자가 아직 복제하지 않은)
     */
    @Query("SELECT rt FROM ReminderTemplate rt WHERE rt.isSystemTemplate = true AND rt.isActive = true AND rt.templateType NOT IN (SELECT urt.templateType FROM ReminderTemplate urt WHERE urt.user = :user)")
    List<ReminderTemplate> findAvailableSystemTemplatesForUser(@Param("user") User user);
} 