package com.bifai.reminder.bifai_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 간단한 컴파일 테스트
 */
@SpringBootTest
public class SimpleCompilationTest {

    @Test
    void contextLoads() {
        // 스프링 컨텍스트가 로드되는지 확인
        assertTrue(true, "컨텍스트 로드 성공");
    }
    
    @Test
    void simpleTest() {
        // 간단한 테스트
        int result = 2 + 2;
        assertTrue(result == 4, "기본 연산 테스트");
    }
}