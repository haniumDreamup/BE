package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 설정 Repository
 * BIF 사용자의 개인화된 설정 관리를 위한 데이터 접근 계층
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    /**
     * 사용자 ID로 설정 조회
     */
    Optional<UserPreference> findByUser_UserId(Long userId);
    
    /**
     * 알림 설정이 활성화된 사용자 조회
     */
    @Query("SELECT up FROM UserPreference up WHERE up.notificationEnabled = true")
    List<UserPreference> findUsersWithNotificationsEnabled();
    
    /**
     * 위치 추적이 활성화된 사용자 조회
     */
    // Location tracking 필드가 엔티티에 없으므로 제거
    // List<UserPreference> findUsersWithLocationTrackingEnabled();
    
    /**
     * 특정 언어 설정 사용자 조회
     */
    @Query("SELECT up FROM UserPreference up WHERE up.languageCode = :language")
    List<UserPreference> findByNotificationLanguage(@Param("language") String language);
    
    /**
     * 음성 안내가 활성화된 사용자 조회
     */
    @Query("SELECT up FROM UserPreference up WHERE up.voiceGuidanceEnabled = true")
    List<UserPreference> findUsersWithVoiceGuidanceEnabled();
    
    /**
     * 간소화 모드가 활성화된 사용자 조회
     */
    @Query("SELECT up FROM UserPreference up WHERE up.uiComplexityLevel = 'SIMPLE'")
    List<UserPreference> findUsersInSimplifiedMode();
    
    /**
     * 긴급 연락처 알림이 활성화된 사용자 조회
     */
    @Query("SELECT up FROM UserPreference up WHERE up.emergencyAutoCall = true")
    List<UserPreference> findUsersWithEmergencyAlertsEnabled();
    
    /**
     * 글꼴 크기별 사용자 통계
     */
    @Query("SELECT up.textSize, COUNT(up) FROM UserPreference up " +
           "GROUP BY up.textSize")
    List<Object[]> countByFontSize();
    
    /**
     * 테마별 사용자 통계
     */
    @Query("SELECT up.themePreference, COUNT(up) FROM UserPreference up " +
           "GROUP BY up.themePreference")
    List<Object[]> countByTheme();
    
    /**
     * 사용자 설정 존재 여부 확인
     */
    boolean existsByUser_UserId(Long userId);
}