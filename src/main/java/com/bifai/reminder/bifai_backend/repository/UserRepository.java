package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User 엔티티를 위한 Repository
 * BIF 사용자 데이터 접근 계층
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자명으로 사용자 찾기 (활성 사용자만)
     * roles를 함께 페치하여 N+1 문제 방지
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.isActive = true")
    Optional<User> findByUsername(@Param("username") String username);

    /**
     * 이메일로 사용자 찾기 (활성 사용자만)
     * roles를 함께 페치하여 N+1 문제 방지
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 사용자명 또는 이메일로 사용자 찾기 (활성 사용자만)
     * roles를 함께 페치하여 N+1 문제 방지
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u WHERE (u.username = :usernameOrEmail OR u.email = :usernameOrEmail) AND u.isActive = true")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);


    /**
     * 캐싱용 활성 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true " +
           "AND u.lastLoginAt > :since " +
           "ORDER BY u.lastLoginAt DESC")
    List<User> findActiveUsersForCaching(@Param("since") LocalDateTime since);
    
    /**
     * 캐싱용 활성 사용자 조회 (매개변수 없는 버전)
     */
    default List<User> findActiveUsersForCaching() {
        return findActiveUsersForCaching(LocalDateTime.now().minusDays(7));
    }
    
    /**
     * 오늘 일정이 있는 사용자 조회
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN Schedule s ON s.user = u " +
           "WHERE u.isActive = true " +
           "AND DATE(s.scheduledTime) = CURRENT_DATE " +
           "ORDER BY u.userId")
    List<User> findUsersWithTodaySchedule();

    /**
     * 사용자명 중복 확인 (활성 사용자만)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.isActive = true")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 이메일 중복 확인 (활성 사용자만)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.isActive = true")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Guardian 관계 기반 조회
     * guardians와 roles를 함께 페치하여 N+1 문제 방지
     */
    @EntityGraph(attributePaths = {"guardians", "roles"})
    @Query("SELECT u FROM User u JOIN u.guardians g WHERE g.id = :guardianId AND u.isActive = true AND g.isActive = true")
    List<User> findByGuardianId(@Param("guardianId") Long guardianId);
    
    @Query("SELECT u FROM User u JOIN u.guardians g WHERE g.primaryPhone = :phoneNumber AND u.isActive = true AND g.isActive = true")
    List<User> findByGuardianPhoneNumber(@Param("phoneNumber") String phoneNumber);
    
    @Query("SELECT u FROM User u JOIN u.guardians g WHERE g.email = :email AND u.isActive = true AND g.isActive = true")
    List<User> findByGuardianEmail(@Param("email") String email);
    
    /**
     * 인지 수준별 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.cognitiveLevel = :cognitiveLevel AND u.isActive = true")
    List<User> findByCognitiveLevel(@Param("cognitiveLevel") User.CognitiveLevel cognitiveLevel);
    
    @Query("SELECT u FROM User u WHERE u.cognitiveLevel IN :cognitiveLevels AND u.isActive = true")
    List<User> findByCognitiveLevelIn(@Param("cognitiveLevels") List<User.CognitiveLevel> cognitiveLevels);
    
    /**
     * 보호자가 없는 사용자 조회 (독립적인 사용자)
     */
    @Query("SELECT u FROM User u WHERE u.guardians IS EMPTY AND u.isActive = true")
    List<User> findUsersWithoutGuardian();
    
    /**
     * 보호자가 있는 사용자 조회
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.guardians g WHERE u.isActive = true AND g.isActive = true")
    List<User> findUsersWithGuardian();
    
    /**
     * 특정 기간 내 로그인한 사용자
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :fromDate AND u.isActive = true")
    List<User> findUsersLoggedInSince(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * 특정 기간 이상 로그인하지 않은 사용자
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :beforeDate AND u.isActive = true")
    List<User> findUsersNotLoggedInSince(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * 최근 활동한 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.lastActivityAt >= :fromDate AND u.isActive = true")
    List<User> findRecentlyActiveUsers(@Param("fromDate") LocalDateTime fromDate);
    
    /**
     * 이메일 인증된 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = true AND u.isActive = true")
    List<User> findVerifiedUsers();
    
    /**
     * 응급 모드가 활성화된 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.emergencyModeEnabled = true AND u.isActive = true")
    List<User> findUsersInEmergencyMode();
    
    /**
     * 사용자 통계 쿼리
     */
    @Query("SELECT u.cognitiveLevel, COUNT(u) FROM User u GROUP BY u.cognitiveLevel")
    List<Object[]> getCognitiveLevelStatistics();
    
    // socialLoginProvider 필드가 User 엔티티에 없으므로 제거
    // List<Object[]> getSocialLoginStatistics();
    
    /**
     * 활성 사용자 조회 (최근 30일 내 로그인)
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :thirtyDaysAgo")
    List<User> findActiveUsers(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    /**
     * 페이징을 위한 활성 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    Page<User> findAllActive(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findAllActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 테스트에서 사용하는 추가 메소드들
    
    /**
     * 활성 사용자만 조회
     */
    List<User> findByIsActiveTrue();
    
    /**
     * 이메일 인증된 사용자 조회
     */
    List<User> findByEmailVerifiedTrue();
    
    /**
     * 전화번호로 존재 여부 확인
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * 이름 또는 이메일로 검색
     */
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchByNameOrEmail(@Param("keyword") String keyword);
    
    /**
     * 활성 사용자 수 카운트
     */
    long countByIsActiveTrue();
    
    /**
     * 이메일 인증된 사용자 수 카운트
     */
    long countByEmailVerifiedTrue();
    
    /**
     * ID로 사용자 조회 (roles 포함)
     * 로그인 및 인증 시 사용
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesById(Long id);
    
    /**
     * ID로 사용자 조회 (guardians 포함)
     * 보호자 정보가 필요한 경우 사용
     */
    @EntityGraph(attributePaths = {"guardians", "guardians.guardianUser"})
    Optional<User> findWithGuardiansById(Long id);
    
    /**
     * ID로 사용자 조회 (모든 기본 연관관계 포함)
     * 사용자 상세 정보 조회 시 사용
     */
    @EntityGraph(attributePaths = {"roles", "guardians", "userPreference"})
    Optional<User> findWithDetailsById(Long id);
    
    /**
     * 모든 활성 사용자 조회 (NotificationScheduler에서 사용)
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();
    
    /**
     * 특정 날짜 이후에 활동한 사용자 페이징 조회 (배치 처리용)
     */
    @Query("SELECT u FROM User u WHERE u.lastActivityAt >= :since AND u.isActive = true")
    Page<User> findActiveUsersAfter(@Param("since") LocalDateTime since, Pageable pageable);
} 