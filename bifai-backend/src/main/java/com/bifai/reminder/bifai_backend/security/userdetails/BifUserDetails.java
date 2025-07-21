package com.bifai.reminder.bifai_backend.security.userdetails;

import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * BIF 사용자를 위한 UserDetails 구현체
 * Spring Security 인증에 필요한 사용자 정보 제공
 */
@RequiredArgsConstructor
public class BifUserDetails implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 사용자의 권한을 GrantedAuthority로 변환
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            return user.getRoles().stream()
                    .filter(Role::getIsActive)
                    .map(role -> new SimpleGrantedAuthority(role.getName()))
                    .collect(Collectors.toList());
        }
        
        // 기본적으로 USER 권한 부여
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 로직 필요 시 구현
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠금 로직 필요 시 구현
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호 만료 로직 필요 시 구현
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 비활성 사용자는 비활성화
        return user.getIsActive();
    }

    /**
     * 원본 User 엔티티 반환
     */
    public User getUser() {
        return user;
    }

    /**
     * 사용자 ID 반환
     */
    public Long getUserId() {
        return user.getUserId();
    }

    /**
     * 이메일 반환
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * 전체 이름 반환
     */
    public String getFullName() {
        return user.getFullName();
    }
} 