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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public User getCurrentUser() {
        BifUserDetails userDetails = (BifUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        
        return userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
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
            currentUser.setPhoneVerified(false); // 전화번호 변경 시 재인증 필요
        }
        
        if (request.getDateOfBirth() != null) {
            currentUser.setDateOfBirth(request.getDateOfBirth());
        }
        
        if (request.getGender() != null) {
            currentUser.setGender(request.getGender());
        }
        
        if (request.getAddress() != null) {
            currentUser.setAddress(request.getAddress());
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
        return userRepository.findByEmail(email);
    }
    
    /**
     * 사용자 ID로 조회
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
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
        User user = getUserById(userId);
        
        // 새로운 역할 세트 생성
        Set<Role> newRoles = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("역할을 찾을 수 없습니다: " + roleId));
            newRoles.add(role);
        }
        
        // 최소 하나의 역할은 필수
        if (newRoles.isEmpty()) {
            throw new IllegalArgumentException("최소 하나 이상의 역할이 필요합니다");
        }
        
        user.setRoles(newRoles);
        return userRepository.save(user);
    }
}