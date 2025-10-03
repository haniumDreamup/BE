package com.bifai.reminder.bifai_backend.integration;

import com.bifai.reminder.bifai_backend.service.mobile.FcmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Firebase Cloud Messaging 통합 테스트
 *
 * 실행 조건:
 * - FCM_ENABLED=true
 * - firebase-service-account.json 파일 존재
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "FCM_ENABLED", matches = "true")
class FcmServiceIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(FcmServiceIntegrationTest.class);

  @Autowired(required = false)
  private FcmService fcmService;

  @Test
  void testFcmService_빈이_생성됨() {
    // Given & When
    log.info("FCM Service 빈 확인");

    // Then
    if (System.getenv("FCM_ENABLED") != null &&
        System.getenv("FCM_ENABLED").equals("true")) {
      assertThat(fcmService).isNotNull();
      log.info("✅ FCM Service 빈이 정상적으로 생성됨");
    } else {
      log.info("⚠️ FCM_ENABLED가 false이므로 빈이 생성되지 않음 (정상)");
    }
  }

  @Test
  void testValidateToken_유효하지_않은_토큰() {
    // Given
    if (fcmService == null) {
      log.info("⚠️ FCM Service가 비활성화되어 테스트 스킵");
      return;
    }

    String invalidToken = "invalid-token-for-testing";

    // When
    log.info("토큰 검증 시작 (실패 예상)");
    boolean isValid = fcmService.validateToken(invalidToken);

    // Then
    assertThat(isValid).isFalse();
    log.info("✅ 유효하지 않은 토큰 검증 완료 (실패 확인)");
  }
}
