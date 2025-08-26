package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가장 기본적인 테스트
 */
class BasicTest {
  
  @Test
  void basicTest() {
    // 기본 테스트 통과 확인
    assertThat(1 + 1).isEqualTo(2);
  }
}
