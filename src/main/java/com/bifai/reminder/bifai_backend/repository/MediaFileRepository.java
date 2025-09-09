package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.MediaFile;
import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadType;
import com.bifai.reminder.bifai_backend.entity.MediaFile.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 미디어 파일 레포지토리
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
  
  /**
   * 미디어 ID로 조회
   */
  Optional<MediaFile> findByMediaId(String mediaId);
  
  /**
   * 미디어 ID와 사용자로 조회
   */
  Optional<MediaFile> findByMediaIdAndUser_UserId(String mediaId, Long userId);
  
  /**
   * 업로드 ID로 조회
   */
  Optional<MediaFile> findByUploadId(String uploadId);
  
  /**
   * 사용자의 미디어 파일 목록 조회
   */
  Page<MediaFile> findByUser_UserIdAndIsDeletedFalse(Long userId, Pageable pageable);
  
  /**
   * 사용자의 특정 타입 미디어 파일 목록 조회
   */
  Page<MediaFile> findByUser_UserIdAndUploadTypeAndIsDeletedFalse(
      Long userId, UploadType uploadType, Pageable pageable);
  
  /**
   * 기간별 미디어 파일 조회
   */
  @Query("SELECT m FROM MediaFile m WHERE m.user.userId = :userId " +
         "AND m.isDeleted = false " +
         "AND m.createdAt BETWEEN :startDate AND :endDate " +
         "ORDER BY m.createdAt DESC")
  Page<MediaFile> findByUserAndDateRange(
      @Param("userId") Long userId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate,
      Pageable pageable);
  
  /**
   * 처리 대기중인 미디어 파일 조회
   */
  List<MediaFile> findByUploadStatusInAndCreatedAtBefore(
      List<UploadStatus> statuses, LocalDateTime threshold);
  
  /**
   * 사용자의 최근 업로드 파일 조회
   */
  List<MediaFile> findTop10ByUser_UserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
  
  /**
   * 관련 엔티티로 미디어 파일 조회
   */
  List<MediaFile> findByRelatedEntityIdAndRelatedEntityTypeAndIsDeletedFalse(
      Long entityId, String entityType);
  
  /**
   * 사용자의 전체 파일 크기 합계
   */
  @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM MediaFile m " +
         "WHERE m.user.userId = :userId AND m.isDeleted = false")
  Long getTotalFileSizeByUser(@Param("userId") Long userId);
  
  /**
   * 특정 기간 내 업로드 수 조회
   */
  @Query("SELECT COUNT(m) FROM MediaFile m " +
         "WHERE m.user.userId = :userId " +
         "AND m.createdAt >= :since " +
         "AND m.isDeleted = false")
  Long countUploadsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
  
  /**
   * 삭제할 파일 조회 (30일 이상 삭제 표시된 파일)
   */
  @Query("SELECT m FROM MediaFile m " +
         "WHERE m.isDeleted = true " +
         "AND m.deletedAt < :threshold")
  List<MediaFile> findFilesToPermanentlyDelete(@Param("threshold") LocalDateTime threshold);
  
  /**
   * 사용자와 업로드 타입으로 미디어 파일 조회 (최신순)
   */
  List<MediaFile> findByUserUserIdAndUploadTypeOrderByCreatedAtDesc(Long userId, UploadType uploadType);
  
  /**
   * 사용자의 모든 미디어 파일 조회 (최신순)
   */
  List<MediaFile> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}