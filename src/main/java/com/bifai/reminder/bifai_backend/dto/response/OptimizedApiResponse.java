package com.bifai.reminder.bifai_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 최적화된 API 응답 래퍼
 * 불필요한 필드 제거 및 조건부 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 제외
public class OptimizedApiResponse<T> {
  
  @JsonProperty("s") // 짧은 키 이름으로 응답 크기 감소
  private boolean success;
  
  @JsonProperty("d")
  private T data;
  
  @JsonProperty("m")
  private String message;
  
  @JsonProperty("t")
  private LocalDateTime timestamp;
  
  @JsonProperty("e")
  private ErrorInfo error;
  
  @JsonProperty("p")
  private PageInfo pageInfo;
  
  @JsonProperty("c")
  private CacheInfo cacheInfo;
  
  /**
   * 성공 응답 생성
   */
  public static <T> OptimizedApiResponse<T> success(T data) {
    return OptimizedApiResponse.<T>builder()
      .success(true)
      .data(data)
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 메시지와 함께 성공 응답
   */
  public static <T> OptimizedApiResponse<T> success(T data, String message) {
    return OptimizedApiResponse.<T>builder()
      .success(true)
      .data(data)
      .message(message)
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 페이징 성공 응답
   */
  public static <T> OptimizedApiResponse<List<T>> success(Page<T> page) {
    return OptimizedApiResponse.<List<T>>builder()
      .success(true)
      .data(page.getContent())
      .pageInfo(PageInfo.from(page))
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 캐시 정보와 함께 응답
   */
  public static <T> OptimizedApiResponse<T> successWithCache(T data, boolean fromCache, long ttl) {
    return OptimizedApiResponse.<T>builder()
      .success(true)
      .data(data)
      .cacheInfo(CacheInfo.builder()
        .cached(fromCache)
        .ttl(ttl)
        .build())
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 에러 응답
   */
  public static <T> OptimizedApiResponse<T> error(String code, String message) {
    return OptimizedApiResponse.<T>builder()
      .success(false)
      .error(ErrorInfo.builder()
        .code(code)
        .message(message)
        .build())
      .timestamp(LocalDateTime.now())
      .build();
  }
  
  /**
   * 에러 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ErrorInfo {
    @JsonProperty("c")
    private String code;
    
    @JsonProperty("m")
    private String message;
    
    @JsonProperty("f")
    private List<FieldError> fieldErrors;
    
    @Data
    @AllArgsConstructor
    public static class FieldError {
      @JsonProperty("f")
      private String field;
      
      @JsonProperty("m")
      private String message;
    }
  }
  
  /**
   * 페이지 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PageInfo {
    @JsonProperty("p")
    private int page;
    
    @JsonProperty("s")
    private int size;
    
    @JsonProperty("t")
    private long total;
    
    @JsonProperty("tp")
    private int totalPages;
    
    @JsonProperty("f")
    private boolean first;
    
    @JsonProperty("l")
    private boolean last;
    
    public static PageInfo from(Page<?> page) {
      return PageInfo.builder()
        .page(page.getNumber())
        .size(page.getSize())
        .total(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .first(page.isFirst())
        .last(page.isLast())
        .build();
    }
  }
  
  /**
   * 캐시 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CacheInfo {
    @JsonProperty("c")
    private boolean cached;
    
    @JsonProperty("t")
    private long ttl; // Time to live in seconds
    
    @JsonProperty("k")
    private String key;
  }
}