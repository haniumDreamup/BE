package com.bifai.reminder.bifai_backend.controller;

import com.bifai.reminder.bifai_backend.dto.ProblemDetail;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring Boot 기본 에러 처리를 커스터마이징
 * 404, 405 등의 에러를 ProblemDetail 형식으로 반환
 */
@Slf4j
@RestController
public class GlobalErrorController implements ErrorController {

  @RequestMapping("/error")
  public ResponseEntity<ProblemDetail> handleError(HttpServletRequest request) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    String requestURI = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    String method = request.getMethod();

    if (status != null) {
      int statusCode = Integer.parseInt(status.toString());
      HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

      log.warn("🔍 Spring Boot 에러 처리: {} {} - HTTP {}", method, requestURI, statusCode);

      ProblemDetail problemDetail;

      switch (statusCode) {
        case 404:
          problemDetail = ProblemDetail.forBifUser(
              "페이지를 찾을 수 없음",
              "요청하신 페이지를 찾을 수 없습니다",
              "주소를 확인하고 다시 시도해 주세요",
              404
          );
          break;
        case 405:
          problemDetail = ProblemDetail.forBifUser(
              "지원하지 않는 방법",
              "이 방법으로는 요청할 수 없습니다",
              "다른 방법으로 시도해 주세요",
              405
          );
          break;
        case 500:
          problemDetail = ProblemDetail.forServerError();
          break;
        default:
          problemDetail = ProblemDetail.forBifUser(
              "알 수 없는 오류",
              "예상치 못한 문제가 발생했습니다",
              "잠시 후 다시 시도해 주세요",
              statusCode
          );
          break;
      }

      return ResponseEntity.status(httpStatus).body(problemDetail);
    }

    // 상태 코드를 알 수 없는 경우 기본 서버 에러 반환
    log.error("🔍 알 수 없는 에러 상태: {} {}", method, requestURI);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ProblemDetail.forServerError());
  }
}