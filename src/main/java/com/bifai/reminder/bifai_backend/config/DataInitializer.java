package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.entity.Role;
import com.bifai.reminder.bifai_backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 필수 데이터 초기화
 * 기본 역할(Role) 데이터를 생성합니다.
 */
@Slf4j
//@Component  // 일시적으로 비활성화하여 애플리케이션 시작 테스트
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("데이터 초기화 시작");
        initializeRoles();
        log.info("데이터 초기화 완료");
    }

    /**
     * 시스템 기본 역할 초기화
     */
    private void initializeRoles() {
        for (Role.SystemRole systemRole : Role.SystemRole.values()) {
            try {
                // 이미 존재하는 역할인지 확인
                if (!roleRepository.existsByName(systemRole.getRoleName())) {
                    Role role = Role.builder()
                            .name(systemRole.getRoleName())
                            .koreanName(systemRole.getKoreanName())
                            .description(systemRole.getDescription())
                            .isActive(true)
                            .build();
                    
                    roleRepository.save(role);
                    log.info("역할 생성 완료: {} ({})", role.getName(), role.getKoreanName());
                } else {
                    log.debug("역할이 이미 존재함: {}", systemRole.getRoleName());
                }
            } catch (Exception e) {
                log.error("역할 생성 실패: {} - {}", systemRole.getRoleName(), e.getMessage());
            }
        }
    }
}