package com.bifai.reminder.bifai_backend.security.userdetails;

import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * BIF 사용자를 위한 UserDetailsService 구현체
 * Spring Security 인증을 위한 사용자 정보 로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BifUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("사용자 로드 시도: {}", usernameOrEmail);

        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다: {}", usernameOrEmail);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + usernameOrEmail);
                });

        log.debug("사용자 로드 성공: userId={}, username={}", user.getUserId(), user.getUsername());
        return new BifUserDetails(user);
    }

    /**
     * 사용자 ID로 UserDetails 로드
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("사용자 ID로 로드 시도: {}", userId);

        User user = userRepository.findById(userId)
                .filter(u -> u.getIsActive()) // 활성 사용자 확인
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다: userId={}", userId);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
                });

        log.debug("사용자 ID로 로드 성공: userId={}, username={}", user.getUserId(), user.getUsername());
        return new BifUserDetails(user);
    }
} 