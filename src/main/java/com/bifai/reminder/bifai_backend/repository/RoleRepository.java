package com.bifai.reminder.bifai_backend.repository;

import com.bifai.reminder.bifai_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Role 엔티티를 위한 Repository
 * BIF 시스템 권한 관리를 위한 데이터 접근 계층
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 권한명으로 조회
     */
    Optional<Role> findByName(String name);
    
    /**
     * 권한명 리스트로 조회
     */
    @Query("SELECT r FROM Role r WHERE r.name IN :names AND r.isActive = true")
    List<Role> findByNameIn(@Param("names") List<String> names);
    
    /**
     * 활성화된 모든 권한 조회
     */
    @Query("SELECT r FROM Role r WHERE r.isActive = true")
    List<Role> findAllActive();
    
    /**
     * 사용자별 권한 조회 - User 엔티티에서 직접 조회하도록 변경
     */
    @Query("SELECT u.roles FROM User u WHERE u.userId = :userId AND u.isActive = true")
    Set<Role> findByUserId(@Param("userId") Long userId);
    
    /**
     * 권한명 존재 여부 확인
     */
    boolean existsByName(String name);
    
    /**
     * 특정 권한을 가진 사용자 수 조회
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND r.isActive = true AND u.isActive = true")
    long countUsersWithRole(@Param("roleName") String roleName);
    
    /**
     * 한국어 이름으로 권한 조회
     */
    @Query("SELECT r FROM Role r WHERE r.koreanName = :koreanName AND r.isActive = true")
    Optional<Role> findByKoreanName(@Param("koreanName") String koreanName);
}