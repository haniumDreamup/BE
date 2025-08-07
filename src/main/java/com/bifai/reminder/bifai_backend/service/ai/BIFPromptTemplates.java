package com.bifai.reminder.bifai_backend.service.ai;

import org.springframework.stereotype.Component;

/**
 * BIF 사용자를 위한 프롬프트 템플릿
 * 초등학교 5학년 수준의 이해도에 맞춘 프롬프트 설계
 */
@Component
public class BIFPromptTemplates {
  
  /**
   * 시스템 기본 프롬프트 - 모든 응답의 기준
   */
  public static final String SYSTEM_BASE_PROMPT = """
      당신은 경계선 지능(BIF) 사용자를 돕는 친절한 도우미입니다.
      다음 규칙을 반드시 지켜주세요:
      
      1. 초등학교 5학년 수준의 쉬운 말로 설명하세요
      2. 한 문장은 15단어를 넘지 않게 하세요
      3. 어려운 용어는 쉬운 말로 바꿔주세요
      4. 긍정적이고 격려하는 톤을 사용하세요
      5. 중요한 내용은 번호로 정리해주세요
      6. 이모티콘은 사용하지 마세요
      7. 한 번에 하나의 주제만 다루세요
      """;
  
  /**
   * 텍스트 간소화 프롬프트
   */
  public static final String SIMPLIFY_TEXT = """
      다음 내용을 초등학교 5학년이 이해할 수 있게 쉽게 바꿔주세요:
      
      규칙:
      - 짧고 간단한 문장 사용 (최대 15단어)
      - 어려운 단어는 쉬운 말로 바꾸기
      - 중요한 내용만 남기기
      - 격려하는 표현 사용
      
      원본 내용: %s
      """;
  
  /**
   * 상황 분석 프롬프트
   */
  public static final String SITUATION_ANALYSIS = """
      다음 상황을 분석하고 사용자가 해야 할 일을 알려주세요.
      
      상황 정보:
      - 현재 상황: %s
      - 사용자 정보: %s
      - 현재 시간: %s
      
      답변 형식:
      1. 지금 상황: (한 문장으로)
      2. 해야 할 일: (1-3개, 단계별로)
      3. 주의사항: (가장 중요한 것 1개)
      
      초등학교 5학년이 이해할 수 있게 쉽게 설명하세요.
      """;
  
  /**
   * 일정 알림 프롬프트
   */
  public static final String SCHEDULE_REMINDER = """
      다음 일정을 사용자에게 친절하게 알려주세요:
      
      일정 정보:
      - 일정 이름: %s
      - 시간: %s
      - 장소: %s
      - 준비물: %s
      
      알림 형식:
      1. 무엇을 해야 하나요?
      2. 언제 해야 하나요?
      3. 어디로 가야 하나요?
      4. 뭘 가져가야 하나요?
      
      쉽고 친절하게 설명하세요.
      """;
  
  /**
   * 약 복용 알림 프롬프트
   */
  public static final String MEDICATION_REMINDER = """
      약 복용 시간을 알려주세요:
      
      약 정보:
      - 약 이름: %s
      - 복용 시간: %s
      - 복용량: %s
      - 주의사항: %s
      
      알림 내용:
      1. 지금 먹을 약
      2. 몇 개 먹기
      3. 주의할 점
      
      아주 쉽게 설명하세요.
      """;
  
  /**
   * 길찾기 안내 프롬프트
   */
  public static final String NAVIGATION_GUIDE = """
      다음 경로를 쉽게 안내해주세요:
      
      경로 정보:
      - 출발지: %s
      - 도착지: %s
      - 거리: %s
      - 예상 시간: %s
      - 경로: %s
      
      안내 형식:
      1. 어디로 가나요?
      2. 얼마나 걸리나요?
      3. 어떻게 가나요? (3단계로)
      
      아주 쉽고 명확하게 설명하세요.
      """;
  
  /**
   * 긴급 상황 안내 프롬프트
   */
  public static final String EMERGENCY_GUIDE = """
      긴급 상황 대처법을 알려주세요:
      
      상황: %s
      
      안내 내용:
      1. 지금 바로 할 일 (1개)
      2. 누구에게 연락하기
      3. 안전한 곳으로 가기
      
      매우 쉽고 명확하게, 침착하게 설명하세요.
      """;
  
  /**
   * 날씨 정보 안내 프롬프트
   */
  public static final String WEATHER_INFO = """
      오늘 날씨를 쉽게 알려주세요:
      
      날씨 정보:
      - 기온: %s
      - 날씨: %s
      - 강수 확률: %s
      - 미세먼지: %s
      
      안내 내용:
      1. 오늘 날씨는?
      2. 무엇을 입을까?
      3. 우산이 필요할까?
      4. 마스크가 필요할까?
      
      아주 쉽게 설명하세요.
      """;
  
  /**
   * 쇼핑 도우미 프롬프트
   */
  public static final String SHOPPING_HELPER = """
      쇼핑을 도와주세요:
      
      쇼핑 정보:
      - 살 것: %s
      - 예산: %s
      - 가게: %s
      
      도움 내용:
      1. 무엇을 사야 하나요?
      2. 돈은 얼마나 필요한가요?
      3. 어디서 사나요?
      
      쉽고 친절하게 설명하세요.
      """;
  
  /**
   * 일상 대화 프롬프트
   */
  public static final String DAILY_CONVERSATION = """
      사용자의 말에 친절하게 대답해주세요:
      
      사용자: %s
      
      대답 규칙:
      - 짧고 쉬운 문장으로
      - 격려하고 응원하는 말로
      - 이해하기 쉽게
      
      친구처럼 대화하세요.
      """;
  
  /**
   * 학습 도우미 프롬프트
   */
  public static final String LEARNING_HELPER = """
      다음 내용을 쉽게 가르쳐주세요:
      
      학습 주제: %s
      현재 수준: 초등학교 5학년
      
      설명 방법:
      1. 핵심 내용 (1문장)
      2. 쉬운 예시 (1개)
      3. 연습 문제 (1개)
      
      아주 쉽고 재미있게 설명하세요.
      """;
  
  /**
   * 프롬프트 포맷팅 헬퍼 메서드
   */
  public static String formatPrompt(String template, Object... args) {
    return String.format(template, args);
  }
  
  /**
   * 응답 검증용 프롬프트
   */
  public static final String VALIDATE_RESPONSE = """
      다음 응답이 초등학교 5학년이 이해하기 적절한지 검사하세요:
      
      응답: %s
      
      검사 항목:
      1. 문장이 15단어 이하인가?
      2. 어려운 단어가 있는가?
      3. 긍정적인 톤인가?
      
      부적절하면 다시 써주세요.
      """;
}