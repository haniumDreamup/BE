package com.bifai.reminder.bifai_backend.service;

import com.bifai.reminder.bifai_backend.entity.AccessibilitySettings;
import com.bifai.reminder.bifai_backend.entity.User;
import com.bifai.reminder.bifai_backend.repository.AccessibilitySettingsRepository;
import com.bifai.reminder.bifai_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AccessibilitySettings 초기화 전용 서비스
 * Self-invocation 문제 해결을 위해 별도 Bean으로 분리
 *
 * Spring AOP Proxy는 같은 클래스 내부의 메서드 호출(this.method())에 대해
 * 트랜잭션을 적용하지 않음. 따라서 쓰기 트랜잭션이 필요한 초기화 로직을
 * 별도 Bean으로 분리하여 프록시를 통한 호출 보장.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccessibilitySettingsInitializer {

  private final AccessibilitySettingsRepository accessibilitySettingsRepository;
  private final UserRepository userRepository;

  /**
   * 기본 접근성 설정 생성
   * REQUIRES_NEW: 부모 read-only 트랜잭션과 독립적인 새 쓰기 트랜잭션 생성
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
  public AccessibilitySettings createDefaultSettings(Long userId) {
    log.info("🔧 createDefaultSettings 시작 - userId: {}, Transaction active: {}, Read-only: {}",
             userId,
             org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),
             org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly());

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

    AccessibilitySettings settings = AccessibilitySettings.builder()
      .user(user)
      .build();

    // BIF 사용자를 위한 기본 설정
    settings.setSimplifiedUiEnabled(true);
    settings.setSimpleLanguageEnabled(true);
    settings.setLargeTouchTargets(true);
    settings.setVoiceGuidanceEnabled(true);

    log.info("💾 Attempting to save AccessibilitySettings...");
    AccessibilitySettings saved = accessibilitySettingsRepository.save(settings);
    log.info("✅ AccessibilitySettings saved - settingsId: {}", saved.getSettingsId());

    return saved;
  }
}
