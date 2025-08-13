package com.bifai.reminder.bifai_backend.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA JSON 변환기
 * Map을 JSON 문자열로 변환하여 데이터베이스에 저장
 */
@Converter
@Slf4j
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {
  
  private static final ObjectMapper objectMapper = new ObjectMapper();
  
  @Override
  public String convertToDatabaseColumn(Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "{}";
    }
    
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      log.error("Failed to convert map to JSON string", e);
      return "{}";
    }
  }
  
  @Override
  public Map<String, Object> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return new HashMap<>();
    }
    
    try {
      return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
    } catch (IOException e) {
      log.error("Failed to convert JSON string to map", e);
      return new HashMap<>();
    }
  }
}