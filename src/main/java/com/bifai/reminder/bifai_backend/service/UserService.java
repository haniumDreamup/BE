package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.dto.user.UserUpdateRequest;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.exception.ResourceNotFoundException;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 사용자 관리 서비스
 * BIF 사용자 정보 조회 및 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in security context");
            throw new SecurityException("인증 정보가 없습니다");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            log.error("Authentication principal is null");
            throw new SecurityException("인증 정보가 올바르지 않습니다");
        }

        if (!(principal instanceof BifUserDetails)) {
            log.error("Expected BifUserDetails but got: {}", principal.getClass().getSimpleName());
            throw new SecurityException("올바르지 않은 인증 타입입니다");
        }

        BifUserDetails userDetails = (BifUserDetails) principal;
        Long userId = userDetails.getUserId();

        if (userId == null) {
            log.error("User ID is null in BifUserDetails");
            throw new SecurityException("사용자 ID를 찾을 수 없습니다");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("사용자를 찾을 수 없습니다");
                });

        // Initialize lazy collections to avoid LazyInitializationException
        try {
            if (user.getGuardians() != null) {
                user.getGuardians().size(); // Force initialization
            }
            if (user.getGuardianFor() != null) {
                user.getGuardianFor().size(); // Force initialization
            }
            if (user.getRoles() != null) {
                user.getRoles().size(); // Force initialization
            }
            if (user.getDevices() != null) {
                user.getDevices().size(); // Force initialization
            }
            if (user.getSchedules() != null) {
                user.getSchedules().size(); // Force initialization
            }
            if (user.getNotifications() != null) {
                user.getNotifications().size(); // Force initialization
            }
        } catch (Exception e) {
            log.debug("Some lazy collections could not be initialized, which is normal: {}", e.getMessage());
        }

        return user;
    }

    /**
     * 현재 사용자 정보 수정
     */
    @Transactional
    public User updateCurrentUser(UserUpdateRequest request) {
        User currentUser = getCurrentUser();
        
        // 수정 가능한 필드들만 업데이트
        if (request.getName() != null) {
            currentUser.setName(request.getName());
            currentUser.setFullName(request.getName());
        }
        
        if (request.getNickname() != null) {
            currentUser.setNickname(request.getNickname());
        }
        
        if (request.getPhoneNumber() != null) {
            currentUser.setPhoneNumber(request.getPhoneNumber());
        }
        
        if (request.getGender() != null) {
            currentUser.setGender(request.getGender());
        }
        
        
        if (request.getEmergencyContactName() != null) {
            currentUser.setEmergencyContactName(request.getEmergencyContactName());
        }
        
        if (request.getEmergencyContactPhone() != null) {
            currentUser.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }
        
        if (request.getLanguagePreference() != null) {
            currentUser.setLanguagePreference(request.getLanguagePreference());
        }
        
        if (request.getProfileImageUrl() != null) {
            currentUser.setProfileImageUrl(request.getProfileImageUrl());
        }
        
        return userRepository.save(currentUser);
    }

    /**
     * 이메일로 사용자 찾기
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("Email is null or empty when finding user");
            return Optional.empty();
        }
        
        return userRepository.findByEmail(email.trim().toLowerCase());
    }
    
    /**
     * 사용자 ID로 조회
     */
    public User getUserById(Long userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("사용자를 찾을 수 없습니다");
                });
    }

    /**
     * 전체 사용자 목록 조회
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 사용자 비활성화
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("사용자 비활성화: userId={}", userId);
    }

    /**
     * 사용자 활성화
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("사용자 활성화: userId={}", userId);
    }

    /**
     * 사용자 역할 수정
     */
    @Transactional
    public User updateUserRoles(Long userId, Set<Long> roleIds) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(roleIds, "Role IDs cannot be null");
        
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("최소 하나 이상의 역할이 필요합니다");
        }
        
        User user = getUserById(userId);
        
        // 새로운 역할 세트 생성
        Set<Role> newRoles = new HashSet<>();
        for (Long roleId : roleIds) {
            if (roleId == null) {
                log.warn("Skipping null role ID for user: {}", userId);
                continue;
            }
            
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> {
                        log.error("Role not found with ID: {} for user: {}", roleId, userId);
                        return new ResourceNotFoundException("역할을 찾을 수 없습니다: " + roleId);
                    });
            newRoles.add(role);
        }
        
        // 유효한 역할이 없는 경우 확인
        if (newRoles.isEmpty()) {
            throw new IllegalArgumentException("유효한 역할이 없습니다");
        }
        
        user.setRoles(newRoles);
        return userRepository.save(user);
    }
}