package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 가장 기본적인 테스트
 */
class BasicTest {
  
  @Test
  void basicTest() {
    assertThat(1 + 1).isEqualTo(2);
  }
  
  @Test
  void stringTest() {
    String text = "Hello BIF-AI";
    assertThat(text).contains("BIF");
  }
}