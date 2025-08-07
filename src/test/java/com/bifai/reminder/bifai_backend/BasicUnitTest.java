package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가장 기본적인 단위 테스트
 * Spring 컨텍스트 없이 실행
 */
class BasicUnitTest {
  
  @Test
  void basicTest() {
    // given
    int a = 1;
    int b = 2;
    
    // when
    int result = a + b;
    
    // then
    assertThat(result).isEqualTo(3);
  }
}