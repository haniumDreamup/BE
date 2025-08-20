package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 접근성 설정 Repository
 */
@Repository
public interface AccessibilitySettingsRepository extends JpaRepository<AccessibilitySettings, Long> {
  
  /**
   * 사용자 ID로 접근성 설정 조회
   */
  @Query("SELECT a FROM AccessibilitySettings a WHERE a.user.userId = :userId")
  Optional<AccessibilitySettings> findByUserId(@Param("userId") Long userId);
  
  /**
   * 사용자로 접근성 설정 조회
   */
  Optional<AccessibilitySettings> findByUser(User user);
  
  /**
   * 프로파일 타입별 설정 조회
   */
  List<AccessibilitySettings> findByProfileType(String profileType);
  
  /**
   * 음성 안내가 활성화된 사용자 조회
   */
  List<AccessibilitySettings> findByVoiceGuidanceEnabledTrue();
  
  /**
   * 간소화 UI가 활성화된 사용자 조회
   */
  List<AccessibilitySettings> findBySimplifiedUiEnabledTrue();
  
  /**
   * 동기화가 필요한 설정 조회
   */
  @Query("SELECT a FROM AccessibilitySettings a WHERE a.syncEnabled = true " +
         "AND (a.lastSyncedAt IS NULL OR a.lastSyncedAt < :threshold)")
  List<AccessibilitySettings> findSettingsNeedingSync(@Param("threshold") LocalDateTime threshold);
  
  /**
   * 특정 언어 사용자 조회
   */
  List<AccessibilitySettings> findByVoiceLanguage(String voiceLanguage);
  
  /**
   * 읽기 수준별 사용자 수 카운트
   */
  @Query("SELECT a.readingLevel, COUNT(a) FROM AccessibilitySettings a " +
         "GROUP BY a.readingLevel")
  List<Object[]> countByReadingLevel();
  
  /**
   * 색상 스키마별 사용자 수 카운트
   */
  @Query("SELECT a.colorScheme, COUNT(a) FROM AccessibilitySettings a " +
         "GROUP BY a.colorScheme")
  List<Object[]> countByColorScheme();
}