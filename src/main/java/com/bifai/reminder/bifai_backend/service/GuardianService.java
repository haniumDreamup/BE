package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.guardian.GuardianRequest;
import com.bifai.reminder.bifai_backend.dto.guardian.GuardianPermissionRequest;
import com.bifai.reminder.bifai_backend.entity.Guardian;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.GuardianRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 보호자 관리 서비스
 * BIF 사용자와 보호자 간의 관계 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * 현재 사용자의 보호자 목록 조회
     *
     * <p>Best Practice: @Transactional(readOnly = true)를 메서드에 명시하여
     * 트랜잭션 범위 내에서 Guardian의 Lazy 필드 접근 보장</p>
     */
    @Transactional(readOnly = true)
    public List<Guardian> getMyGuardians() {
        User currentUser = getCurrentUser();
        // 활성 상태(isActive=true)인 Guardian만 조회 (PENDING 상태 포함, 삭제된 보호자는 제외)
        return guardianRepository.findByUserAndIsActiveTrue(currentUser);
    }

    /**
     * 현재 보호자가 보호 중인 사용자 목록 조회
     *
     * <p>Best Practice: @Transactional(readOnly = true)를 메서드에 명시하여
     * 트랜잭션 범위 내에서 Guardian의 Lazy 필드 접근 보장</p>
     */
    @Transactional(readOnly = true)
    public List<Guardian> getProtectedUsers() {
        User currentGuardian = getCurrentUser();
        return guardianRepository.findByGuardianUserAndIsActiveTrue(currentGuardian);
    }

    /**
     * 보호자 등록 요청
     */
    @Transactional
    public Guardian requestGuardian(GuardianRequest request) {
        User currentUser = getCurrentUser();
        
        // 요청받은 이메일로 보호자 사용자 조회
        User guardianUser = userRepository.findByEmail(request.getGuardianEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다"));
        
        // 이미 등록된 보호자인지 확인
        if (guardianRepository.existsByUserAndGuardianUser(currentUser, guardianUser)) {
            throw new IllegalArgumentException("이미 등록된 보호자입니다");
        }
        
        // 자기 자신을 보호자로 등록할 수 없음
        if (currentUser.getUserId().equals(guardianUser.getUserId())) {
            throw new IllegalArgumentException("자기 자신을 보호자로 등록할 수 없습니다");
        }
        
        // Guardian 엔티티 생성
        Guardian guardian = Guardian.builder()
                .user(currentUser)
                .guardianUser(guardianUser)
                .name(request.getGuardianName())
                .relationship(request.getRelationship())
                .primaryPhone(request.getPrimaryPhone())
                .secondaryPhone(request.getSecondaryPhone())
                .email(guardianUser.getEmail())
                .canViewLocation(request.getCanViewLocation())
                .canModifySettings(request.getCanModifySettings())
                .canReceiveAlerts(request.getCanReceiveAlerts())
                .isPrimary(false)
                .approvalStatus(Guardian.ApprovalStatus.PENDING)
                .isActive(true)
                .build();
        
        // 보호자에게 ROLE_GUARDIAN 역할 부여
        assignGuardianRole(guardianUser);
        
        Guardian savedGuardian = guardianRepository.save(guardian);
        
        // TODO: 보호자에게 승인 요청 알림 발송
        log.info("보호자 등록 요청: userId={}, guardianId={}", currentUser.getUserId(), guardianUser.getUserId());
        
        return savedGuardian;
    }

    /**
     * 보호자 요청 승인
     */
    @Transactional
    public Guardian approveGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("보호자 관계를 찾을 수 없습니다"));
        
        if (guardian.getApprovalStatus() != Guardian.ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 요청입니다");
        }
        
        guardian.setApprovalStatus(Guardian.ApprovalStatus.APPROVED);
        guardian.setApprovedAt(LocalDateTime.now());
        
        return guardianRepository.save(guardian);
    }

    /**
     * 보호자 요청 거절
     */
    @Transactional
    public void rejectGuardian(Long guardianId, String reason) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("보호자 관계를 찾을 수 없습니다"));
        
        if (guardian.getApprovalStatus() != Guardian.ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 요청입니다");
        }
        
        guardian.setApprovalStatus(Guardian.ApprovalStatus.REJECTED);
        guardian.setRejectionReason(reason);
        guardian.setIsActive(false);
        
        guardianRepository.save(guardian);
    }

    /**
     * 보호자 권한 수정
     */
    @Transactional
    public Guardian updatePermissions(Long guardianId, GuardianPermissionRequest request) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("보호자 관계를 찾을 수 없습니다"));
        
        guardian.setCanViewLocation(request.getCanViewLocation());
        guardian.setCanModifySettings(request.getCanModifySettings());
        guardian.setCanReceiveAlerts(request.getCanReceiveAlerts());
        
        if (request.getEmergencyPriority() != null) {
            guardian.setEmergencyPriority(request.getEmergencyPriority());
        }
        
        return guardianRepository.save(guardian);
    }

    /**
     * 보호자 삭제
     */
    @Transactional
    public void removeGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("보호자 관계를 찾을 수 없습니다"));
        
        guardian.setIsActive(false);
        guardian.setTerminatedAt(LocalDateTime.now());
        
        guardianRepository.save(guardian);
    }

    /**
     * 보호 관계 해제 (보호자 측에서)
     */
    @Transactional
    public void removeRelationship(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new ResourceNotFoundException("보호자 관계를 찾을 수 없습니다"));
        
        guardian.setIsActive(false);
        guardian.setTerminatedAt(LocalDateTime.now());
        guardian.setTerminatedBy("GUARDIAN");
        
        guardianRepository.save(guardian);
    }

    /**
     * 특정 사용자의 보호자인지 확인 (Method Security용)
     *
     * <p>Best Practice: 조회 메서드에 @Transactional(readOnly = true) 추가</p>
     */
    @Transactional(readOnly = true)
    public boolean isGuardianOf(Long userId) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        return guardianRepository.existsByUserAndGuardianUserAndIsActiveTrueAndApprovalStatus(
                targetUser, currentUser, Guardian.ApprovalStatus.APPROVED);
    }

    /**
     * 해당 보호자 관계를 승인할 수 있는지 확인
     *
     * <p>Best Practice: Guardian의 user, guardianUser에 접근하므로 트랜잭션 필요</p>
     */
    @Transactional(readOnly = true)
    public boolean canApproveGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElse(null);

        if (guardian == null) return false;

        User currentUser = getCurrentUser();
        // guardianUser는 LAZY이므로 트랜잭션 필요
        return guardian.getGuardianUser().getUserId().equals(currentUser.getUserId())
                && guardian.getApprovalStatus() == Guardian.ApprovalStatus.PENDING;
    }

    /**
     * 해당 보호자 관계를 거절할 수 있는지 확인
     *
     * <p>Best Practice: Guardian의 Lazy 필드 접근을 위해 트랜잭션 필요</p>
     */
    @Transactional(readOnly = true)
    public boolean canRejectGuardian(Long guardianId) {
        return canApproveGuardian(guardianId);
    }

    /**
     * 나의 보호자인지 확인
     *
     * <p>Best Practice: Guardian의 user에 접근하므로 트랜잭션 필요</p>
     */
    @Transactional(readOnly = true)
    public boolean isMyGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElse(null);

        if (guardian == null) return false;

        User currentUser = getCurrentUser();
        // user는 LAZY이므로 트랜잭션 필요
        return guardian.getUser().getUserId().equals(currentUser.getUserId());
    }

    /**
     * 보호 관계를 해제할 수 있는지 확인
     *
     * <p>Best Practice: Guardian의 guardianUser에 접근하므로 트랜잭션 필요</p>
     */
    @Transactional(readOnly = true)
    public boolean canRemoveRelationship(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElse(null);

        if (guardian == null) return false;

        User currentUser = getCurrentUser();
        // guardianUser는 LAZY이므로 트랜잭션 필요
        return guardian.getGuardianUser().getUserId().equals(currentUser.getUserId())
                && guardian.getIsActive();
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    private User getCurrentUser() {
        BifUserDetails userDetails = (BifUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        return userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 사용자에게 보호자 역할 부여
     */
    private void assignGuardianRole(User user) {
        Role guardianRole = roleRepository.findByName("ROLE_GUARDIAN")
                .orElseThrow(() -> new ResourceNotFoundException("보호자 역할을 찾을 수 없습니다"));
        
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(guardianRole);
        user.setRoles(roles);
        
        userRepository.save(user);
    }
}