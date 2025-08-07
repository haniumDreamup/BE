package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.EmergencyContact;
import com.bifai.reminder.bifai_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 긴급 연락처 레포지토리
 * 보호자 및 의료진 연락처 데이터 접근
 */
@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

  /**
   * 사용자별 연락처 우선순위 순으로 조회
   */
  List<EmergencyContact> findByUserOrderByPriorityAsc(User user);

  /**
   * 사용자별 활성화된 연락처 조회
   */
  List<EmergencyContact> findByUserAndIsActiveTrueOrderByPriorityAsc(User user);

  /**
   * 사용자별 연락처 페이징 조회
   */
  Page<EmergencyContact> findByUser(User user, Pageable pageable);

  /**
   * 사용자와 ID로 연락처 조회
   */
  Optional<EmergencyContact> findByIdAndUser(Long id, User user);

  /**
   * 주요 연락처 조회
   */
  Optional<EmergencyContact> findByUserAndIsPrimaryTrue(User user);

  /**
   * 연락처 타입별 조회
   */
  List<EmergencyContact> findByUserAndContactType(User user, EmergencyContact.ContactType contactType);

  /**
   * 의료진 연락처 조회
   */
  @Query("SELECT ec FROM EmergencyContact ec WHERE ec.user = :user " +
         "AND (ec.contactType IN ('DOCTOR', 'NURSE') OR ec.isMedicalProfessional = true) " +
         "AND ec.isActive = true ORDER BY ec.priority ASC")
  List<EmergencyContact> findMedicalContacts(@Param("user") User user);

  /**
   * 현재 연락 가능한 연락처 조회
   */
  @Query("SELECT ec FROM EmergencyContact ec WHERE ec.user = :user " +
         "AND ec.isActive = true AND ec.canReceiveAlerts = true " +
         "AND (ec.availableStartTime IS NULL OR " +
         "(FUNCTION('TIME', CURRENT_TIMESTAMP) >= ec.availableStartTime " +
         "AND FUNCTION('TIME', CURRENT_TIMESTAMP) <= ec.availableEndTime)) " +
         "ORDER BY ec.priority ASC")
  List<EmergencyContact> findAvailableContacts(@Param("user") User user);

  /**
   * 전화번호로 연락처 존재 여부 확인
   */
  boolean existsByUserAndPhoneNumber(User user, String phoneNumber);

  /**
   * 이메일로 연락처 존재 여부 확인
   */
  boolean existsByUserAndEmail(User user, String email);

  /**
   * 사용자별 연락처 개수
   */
  long countByUser(User user);

  /**
   * 사용자별 활성화된 연락처 개수
   */
  long countByUserAndIsActiveTrue(User user);

  /**
   * 검증된 연락처만 조회
   */
  List<EmergencyContact> findByUserAndVerifiedTrueOrderByPriorityAsc(User user);

  /**
   * 특정 권한을 가진 연락처 조회
   */
  @Query("SELECT ec FROM EmergencyContact ec WHERE ec.user = :user " +
         "AND ec.isActive = true AND ec.canMakeDecisions = true " +
         "ORDER BY ec.priority ASC")
  List<EmergencyContact> findDecisionMakers(@Param("user") User user);

  /**
   * 위치 접근 권한이 있는 연락처 조회
   */
  List<EmergencyContact> findByUserAndCanAccessLocationTrueAndIsActiveTrue(User user);

  /**
   * 건강 데이터 접근 권한이 있는 연락처 조회
   */
  List<EmergencyContact> findByUserAndCanAccessHealthDataTrueAndIsActiveTrue(User user);

  /**
   * 최근 연락한 연락처 조회
   */
  List<EmergencyContact> findByUserAndLastContactedAtAfterOrderByLastContactedAtDesc(
      User user, LocalDateTime after);

  /**
   * 응답률이 높은 연락처 순으로 조회
   */
  @Query("SELECT ec FROM EmergencyContact ec WHERE ec.user = :user " +
         "AND ec.isActive = true AND ec.responseRate >= :minResponseRate " +
         "ORDER BY ec.responseRate DESC, ec.priority ASC")
  List<EmergencyContact> findHighResponseContacts(
      @Param("user") User user,
      @Param("minResponseRate") Double minResponseRate);

  /**
   * 연락처 우선순위 일괄 업데이트
   */
  @Modifying
  @Query("UPDATE EmergencyContact ec SET ec.priority = ec.priority + 1 " +
         "WHERE ec.user = :user AND ec.priority >= :fromPriority")
  void incrementPriorities(@Param("user") User user, @Param("fromPriority") Integer fromPriority);

  /**
   * 검증 코드로 연락처 찾기
   */
  Optional<EmergencyContact> findByUserAndVerificationCode(User user, String verificationCode);

  /**
   * 특정 요일에 연락 가능한 연락처
   */
  @Query("SELECT ec FROM EmergencyContact ec WHERE ec.user = :user " +
         "AND ec.isActive = true AND ec.availableDays LIKE %:dayOfWeek% " +
         "ORDER BY ec.priority ASC")
  List<EmergencyContact> findAvailableOnDay(
      @Param("user") User user,
      @Param("dayOfWeek") String dayOfWeek);

  /**
   * 평균 응답 시간이 빠른 연락처 조회
   */
  @Query("SELECT ec FROM EmergencyContact ec WHERE ec.user = :user " +
         "AND ec.isActive = true AND ec.averageResponseTimeMinutes IS NOT NULL " +
         "AND ec.averageResponseTimeMinutes <= :maxMinutes " +
         "ORDER BY ec.averageResponseTimeMinutes ASC")
  List<EmergencyContact> findFastResponseContacts(
      @Param("user") User user,
      @Param("maxMinutes") Integer maxMinutes);

  /**
   * 관계별 연락처 조회
   */
  List<EmergencyContact> findByUserAndRelationshipOrderByPriorityAsc(User user, String relationship);

  /**
   * 언어 선호도별 연락처 조회
   */
  List<EmergencyContact> findByUserAndLanguagePreferenceOrderByPriorityAsc(
      User user, String languagePreference);
}