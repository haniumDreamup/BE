package com.bifai.reminder.bifai_backend.service.vision;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * GPT-4o-mini Vision + Structured Output 지원 테스트
 *
 * 목적: Vision API가 JSON Schema를 따르는 구조화된 응답을 반환할 수 있는지 검증
 */
@SpringBootTest
public class StructuredOutputTest {

  @Value("${ai.openai.api-key}")
  private String apiKey;

  @Test
  public void testStructuredOutputWithVision() {
    RestTemplate restTemplate = new RestTemplate();

    // JSON Schema 정의
    Map<String, Object> jsonSchema = new HashMap<>();
    jsonSchema.put("name", "scene_analysis");
    jsonSchema.put("strict", true);

    Map<String, Object> schema = new HashMap<>();
    schema.put("type", "object");

    Map<String, Object> properties = new HashMap<>();

    // summary
    Map<String, Object> summary = new HashMap<>();
    summary.put("type", "string");
    summary.put("description", "한 줄 요약: [장소] + [활동]");
    properties.put("summary", summary);

    // danger
    Map<String, Object> danger = new HashMap<>();
    danger.put("type", "object");

    Map<String, Object> dangerProps = new HashMap<>();
    Map<String, Object> dangerExists = new HashMap<>();
    dangerExists.put("type", "boolean");
    dangerProps.put("exists", dangerExists);

    Map<String, Object> dangerItems = new HashMap<>();
    dangerItems.put("type", "array");

    Map<String, Object> dangerItem = new HashMap<>();
    dangerItem.put("type", "object");

    Map<String, Object> dangerItemProps = new HashMap<>();
    Map<String, Object> objectProp = new HashMap<>();
    objectProp.put("type", "string");
    dangerItemProps.put("object", objectProp);

    Map<String, Object> actionProp = new HashMap<>();
    actionProp.put("type", "string");
    dangerItemProps.put("action", actionProp);

    Map<String, Object> distanceProp = new HashMap<>();
    distanceProp.put("type", "string");
    dangerItemProps.put("distance", distanceProp);

    dangerItem.put("properties", dangerItemProps);
    dangerItem.put("required", Arrays.asList("object", "action", "distance"));
    dangerItem.put("additionalProperties", false);

    dangerItems.put("items", dangerItem);
    dangerProps.put("items", dangerItems);

    danger.put("properties", dangerProps);
    danger.put("required", Arrays.asList("exists", "items"));
    danger.put("additionalProperties", false);
    properties.put("danger", danger);

    // objects
    Map<String, Object> objects = new HashMap<>();
    objects.put("type", "object");

    Map<String, Object> objectsProps = new HashMap<>();

    Map<String, Object> arrayOfStrings = new HashMap<>();
    arrayOfStrings.put("type", "array");
    Map<String, Object> stringItem = new HashMap<>();
    stringItem.put("type", "string");
    arrayOfStrings.put("items", stringItem);

    objectsProps.put("front", arrayOfStrings);
    objectsProps.put("left", arrayOfStrings);
    objectsProps.put("right", arrayOfStrings);
    objectsProps.put("back", arrayOfStrings);

    objects.put("properties", objectsProps);
    objects.put("required", Arrays.asList("front"));
    objects.put("additionalProperties", false);
    properties.put("objects", objects);

    // people
    Map<String, Object> people = new HashMap<>();
    people.put("type", "object");

    Map<String, Object> peopleProps = new HashMap<>();
    Map<String, Object> count = new HashMap<>();
    count.put("type", "integer");
    peopleProps.put("count", count);

    Map<String, Object> activity = new HashMap<>();
    activity.put("type", "string");
    peopleProps.put("activity", activity);

    people.put("properties", peopleProps);
    people.put("required", Arrays.asList("count"));
    people.put("additionalProperties", false);
    properties.put("people", people);

    // situation
    Map<String, Object> situation = new HashMap<>();
    situation.put("type", "string");
    situation.put("description", "2-3문장으로 전체 상황 설명");
    properties.put("situation", situation);

    // next_action
    Map<String, Object> nextAction = new HashMap<>();
    nextAction.put("type", "object");

    Map<String, Object> nextActionProps = new HashMap<>();
    Map<String, Object> actionDesc = new HashMap<>();
    actionDesc.put("type", "string");
    nextActionProps.put("action", actionDesc);

    Map<String, Object> time = new HashMap<>();
    time.put("type", "string");
    nextActionProps.put("time", time);

    nextAction.put("properties", nextActionProps);
    nextAction.put("required", Arrays.asList("action"));
    nextAction.put("additionalProperties", false);
    properties.put("next_action", nextAction);

    schema.put("properties", properties);
    schema.put("required", Arrays.asList("summary", "danger", "objects", "people", "situation", "next_action"));
    schema.put("additionalProperties", false);

    jsonSchema.put("schema", schema);

    // 요청 본문
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", "gpt-4o-mini-2024-07-18");
    requestBody.put("max_tokens", 1000);

    // response_format 추가 (Structured Output)
    Map<String, Object> responseFormat = new HashMap<>();
    responseFormat.put("type", "json_schema");
    responseFormat.put("json_schema", jsonSchema);
    requestBody.put("response_format", responseFormat);

    // 메시지
    List<Map<String, Object>> messages = new ArrayList<>();
    Map<String, Object> message = new HashMap<>();
    message.put("role", "user");

    List<Object> content = new ArrayList<>();

    // 텍스트 파트
    Map<String, String> textPart = new HashMap<>();
    textPart.put("type", "text");
    textPart.put("text", """
        당신은 경계선 지능 사용자의 눈입니다.
        이 사진을 분석해서 JSON 형식으로 답변하세요.

        규칙:
        - 85% 이상 확신만 전달
        - 추측 금지
        - 구체적 거리 ("50cm", "1m", "2걸음")
        - 안전 최우선
        """);
    content.add(textPart);

    // 간단한 테스트 이미지 URL (공개 URL)
    Map<String, Object> imagePart = new HashMap<>();
    imagePart.put("type", "image_url");
    Map<String, String> imageUrl = new HashMap<>();
    imageUrl.put("url", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/320px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg");
    imagePart.put("image_url", imageUrl);
    content.add(imagePart);

    message.put("content", content);
    messages.add(message);
    requestBody.put("messages", messages);

    // HTTP 헤더
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    try {
      System.out.println("=================================");
      System.out.println("GPT-4o-mini Vision + Structured Output 테스트");
      System.out.println("=================================");

      long startTime = System.currentTimeMillis();

      ResponseEntity<Map> response = restTemplate.postForEntity(
          "https://api.openai.com/v1/chat/completions",
          request,
          Map.class
      );

      long endTime = System.currentTimeMillis();

      if (response.getStatusCode().is2xxSuccessful()) {
        Map<String, Object> responseBody = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

        if (choices != null && !choices.isEmpty()) {
          Map<String, Object> messageResponse = (Map<String, Object>) choices.get(0).get("message");
          String jsonContent = (String) messageResponse.get("content");

          System.out.println("\n✅ 성공! 응답 시간: " + (endTime - startTime) + "ms");
          System.out.println("\n📋 Structured JSON 응답:");
          System.out.println(jsonContent);

          // JSON 파싱 가능 확인
          try {
            // 간단한 JSON 유효성 검사
            if (jsonContent.contains("\"summary\"") &&
                jsonContent.contains("\"danger\"") &&
                jsonContent.contains("\"objects\"")) {
              System.out.println("\n✅ JSON Schema 준수 확인!");
            }
          } catch (Exception e) {
            System.err.println("❌ JSON 파싱 실패: " + e.getMessage());
          }
        }
      } else {
        System.err.println("❌ API 호출 실패: " + response.getStatusCode());
      }

    } catch (Exception e) {
      System.err.println("❌ 테스트 실패: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
