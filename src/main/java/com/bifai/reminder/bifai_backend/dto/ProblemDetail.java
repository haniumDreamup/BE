package com.bifai.reminder.bifai_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * RFC 9457 Problem Details for HTTP APIs
 * BIF 사용자를 위한 친화적 에러 응답 형식
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
  
  /**
   * 문제 유형을 식별하는 URI
   */
  private final URI type;
  
  /**
   * 사용자 친화적인 문제 제목 (5학년 수준)
   */
  private final String title;
  
  /**
   * HTTP 상태 코드
   */
  private final Integer status;
  
  /**
   * 구체적인 문제 설명
   */
  private final String detail;
  
  /**
   * 문제가 발생한 URI 인스턴스
   */
  private final URI instance;
  
  /**
   * BIF 사용자를 위한 행동 지침
   */
  private final String userAction;
  
  /**
   * 문제 발생 시간
   */
  private final OffsetDateTime timestamp;
  
  /**
   * 추가적인 문제 관련 정보
   */
  private final Map<String, Object> extensions;
  
  /**
   * 일반적인 에러를 위한 빌더 메서드
   */
  public static ProblemDetail forStatusAndDetail(int status, String detail) {
    return ProblemDetail.builder()
        .status(status)
        .detail(detail)
        .timestamp(OffsetDateTime.now())
        .build();
  }
  
  /**
   * BIF 특화 에러를 위한 빌더 메서드
   */
  public static ProblemDetail forBifUser(String title, String detail, String userAction, int status) {
    return ProblemDetail.builder()
        .type(URI.create("https://bifai.app/problems/" + title.toLowerCase().replace(" ", "-")))
        .title(title)
        .detail(detail)
        .userAction(userAction)
        .status(status)
        .timestamp(OffsetDateTime.now())
        .build();
  }
  
  /**
   * 인증 관련 에러를 위한 빌더 메서드
   */
  public static ProblemDetail forAuthentication(String detail) {
    return ProblemDetail.builder()
        .type(URI.create("https://bifai.app/problems/authentication"))
        .title("로그인이 필요해요")
        .detail(detail)
        .userAction("다시 로그인해 주세요")
        .status(401)
        .timestamp(OffsetDateTime.now())
        .build();
  }
  
  /**
   * 검증 에러를 위한 빌더 메서드
   */
  public static ProblemDetail forValidation(String detail) {
    return ProblemDetail.builder()
        .type(URI.create("https://bifai.app/problems/validation"))
        .title("입력 정보를 확인해 주세요")
        .detail(detail)
        .userAction("빨간색으로 표시된 부분을 다시 입력해 주세요")
        .status(400)
        .timestamp(OffsetDateTime.now())
        .build();
  }
  
  /**
   * 리소스를 찾을 수 없을 때를 위한 빌더 메서드
   */
  public static ProblemDetail forNotFound(String resource) {
    return ProblemDetail.builder()
        .type(URI.create("https://bifai.app/problems/not-found"))
        .title("찾을 수 없어요")
        .detail(resource + "을(를) 찾을 수 없습니다")
        .userAction("다시 시도해 주시거나 보호자에게 문의해 주세요")
        .status(404)
        .timestamp(OffsetDateTime.now())
        .build();
  }
  
  /**
   * 서버 에러를 위한 빌더 메서드
   */
  public static ProblemDetail forServerError() {
    return ProblemDetail.builder()
        .type(URI.create("https://bifai.app/problems/server-error"))
        .title("일시적인 문제가 발생했어요")
        .detail("서버에 일시적인 문제가 생겼습니다")
        .userAction("잠시 후 다시 시도해 주세요")
        .status(500)
        .timestamp(OffsetDateTime.now())
        .build();
  }
}