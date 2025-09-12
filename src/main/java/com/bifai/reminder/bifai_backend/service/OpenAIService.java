package com.bifai.reminder.bifai_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * OpenAI ChatGPT 서비스
 * BIF 사용자를 위한 상황 해석 및 AI 응답 생성
 */
@Slf4j
@Service
public class OpenAIService {
  
  private final ChatClient chatClient;
  
  public OpenAIService(@Autowired(required = false) ChatClient chatClient) {
    this.chatClient = chatClient;
  }
  
  @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}")
  private String model;
  
  @Value("${spring.ai.openai.chat.options.temperature:0.7}")
  private double temperature;
  
  /**
   * 이미지 상황 해석
   */
  public Map<String, String> interpretSituation(
      List<Map<String, Object>> objects,
      String extractedText,
      String userQuestion,
      String context) {
    
    log.info("OpenAI 상황 해석 시작 - 객체: {}개, 텍스트: {}, 질문: {}", 
        objects.size(), extractedText != null ? "있음" : "없음", userQuestion);
    
    if (chatClient == null) {
      log.warn("ChatClient가 사용할 수 없습니다. 폴백 응답을 반환합니다.");
      return createFallbackResponse();
    }
    
    try {
      String systemPrompt = buildSystemPrompt();
      String userPrompt = buildUserPrompt(objects, extractedText, userQuestion, context);
      
      String response = chatClient.prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .call()
          .content();
      
      log.debug("OpenAI 응답: {}", response);
      
      return parseAIResponse(response);
      
    } catch (Exception e) {
      log.error("OpenAI 상황 해석 실패", e);
      return createFallbackResponse();
    }
  }
  
  /**
   * BIF 사용자를 위한 시스템 프롬프트 생성
   */
  private String buildSystemPrompt() {
    return """
        당신은 BIF(경계성 지적기능장애) 사용자를 돕는 AI 어시스턴트입니다.
        사용자는 IQ 70-85 범위로 인지적 지원이 필요합니다.
        
        응답 규칙:
        1. 5학년 수준의 쉬운 단어 사용
        2. 한 문장은 15단어 이내로 짧게
        3. 구체적이고 실용적인 조언 제공
        4. 긍정적이고 격려하는 톤 유지
        5. 위험상황은 명확히 경고
        
        응답 형식 (JSON):
        {
          "description": "상황 설명 (쉬운 말로)",
          "action": "구체적인 행동 지침",
          "safety": "SAFE/CAUTION/WARNING/DANGER"
        }
        
        예시:
        - "복잡한 교차로" → "길이 여러 갈래로 나뉘는 곳"
        - "즉시 대피하세요" → "지금 안전한 곳으로 가세요"
        """;
  }
  
  /**
   * 사용자 프롬프트 생성
   */
  private String buildUserPrompt(
      List<Map<String, Object>> objects,
      String extractedText,
      String userQuestion,
      String context) {
    
    StringBuilder prompt = new StringBuilder();
    prompt.append("이미지 분석 결과:\n");
    
    // 발견된 객체들
    if (!objects.isEmpty()) {
      prompt.append("발견된 것들: ");
      for (Map<String, Object> obj : objects) {
        String label = (String) obj.get("label");
        Float confidence = ((Number) obj.get("confidence")).floatValue();
        prompt.append(String.format("%s(%.0f%%), ", label, confidence * 100));
      }
      prompt.append("\n");
    }
    
    // 추출된 텍스트
    if (extractedText != null && !extractedText.trim().isEmpty()) {
      prompt.append("글자: ").append(extractedText.trim()).append("\n");
    }
    
    // 사용자 질문
    if (userQuestion != null && !userQuestion.trim().isEmpty()) {
      prompt.append("질문: ").append(userQuestion.trim()).append("\n");
    }
    
    // 상황 맥락
    if (context != null && !context.trim().isEmpty()) {
      prompt.append("상황: ").append(context.trim()).append("\n");
    }
    
    prompt.append("\n이 상황을 BIF 사용자가 이해할 수 있도록 쉽게 설명해주세요.");
    
    return prompt.toString();
  }
  
  /**
   * AI 응답 파싱
   */
  private Map<String, String> parseAIResponse(String response) {
    try {
      // JSON 형태로 응답이 왔는지 확인
      if (response.contains("{") && response.contains("}")) {
        // 간단한 JSON 파싱 (실제 프로덕션에서는 ObjectMapper 사용 권장)
        String description = extractJsonValue(response, "description");
        String action = extractJsonValue(response, "action");
        String safety = extractJsonValue(response, "safety");
        
        return Map.of(
            "description", description != null ? description : "상황을 분석하고 있어요.",
            "action", action != null ? action : "잠시 기다려주세요.",
            "safety", safety != null ? safety : "SAFE"
        );
      } else {
        // 일반 텍스트 응답 처리
        return Map.of(
            "description", response.length() > 100 ? response.substring(0, 100) + "..." : response,
            "action", "상황을 다시 확인해주세요.",
            "safety", "SAFE"
        );
      }
    } catch (Exception e) {
      log.error("AI 응답 파싱 실패", e);
      return createFallbackResponse();
    }
  }
  
  /**
   * 간단한 JSON 값 추출
   */
  private String extractJsonValue(String json, String key) {
    String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher m = p.matcher(json);
    return m.find() ? m.group(1) : null;
  }
  
  /**
   * 폴백 응답 생성
   */
  private Map<String, String> createFallbackResponse() {
    return Map.of(
        "description", "이미지를 분석했어요. 궁금한 것이 있으면 말씀해주세요.",
        "action", "필요하면 도움을 요청하세요.",
        "safety", "SAFE"
    );
  }
  
  /**
   * 의료 복용 알림 메시지 생성
   */
  public String generateMedicationReminder(String medicationName, String time, String dosage) {
    String prompt = String.format(
        "BIF 사용자를 위한 복약 알림 메시지를 만들어주세요.\n" +
        "약: %s\n시간: %s\n용량: %s\n" +
        "쉽고 친근한 말로 30자 이내로 작성해주세요.",
        medicationName, time, dosage
    );
    
    if (chatClient == null) {
      log.warn("ChatClient가 사용할 수 없습니다. 기본 메시지를 반환합니다.");
      return String.format("💊 %s 먹을 시간이에요!", medicationName);
    }
    
    try {
      return chatClient.prompt()
          .system("BIF 사용자를 위한 친근한 알림 메시지를 만드는 어시스턴트입니다. 쉬운 말로 짧게 작성하세요.")
          .user(prompt)
          .call()
          .content();
    } catch (Exception e) {
      log.error("복약 알림 메시지 생성 실패", e);
      return String.format("💊 %s 먹을 시간이에요!", medicationName);
    }
  }
  
  /**
   * 긴급 상황 설명 생성
   */
  public String generateEmergencyExplanation(String situation, String location) {
    String prompt = String.format(
        "BIF 사용자를 위한 긴급상황 설명을 만들어주세요.\n" +
        "상황: %s\n위치: %s\n" +
        "위험을 명확히 알리고 즉시 행동할 수 있는 지침을 50자 이내로 작성해주세요.",
        situation, location
    );
    
    if (chatClient == null) {
      log.warn("ChatClient가 사용할 수 없습니다. 기본 긴급 메시지를 반환합니다.");
      return "🚨 위험해요! 즉시 안전한 곳으로 피하고 도움을 요청하세요!";
    }
    
    try {
      return chatClient.prompt()
          .system("BIF 사용자를 위한 긴급상황 대응 가이드를 만드는 어시스턴트입니다. 명확하고 즉시 행동 가능한 지침을 제공하세요.")
          .user(prompt)
          .call()
          .content();
    } catch (Exception e) {
      log.error("긴급상황 설명 생성 실패", e);
      return "🚨 위험해요! 즉시 안전한 곳으로 피하고 도움을 요청하세요!";
    }
  }
}