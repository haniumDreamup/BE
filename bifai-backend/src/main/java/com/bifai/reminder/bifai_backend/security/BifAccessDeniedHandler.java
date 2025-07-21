package com.bifai.reminder.bifai_backend.security;

import com.bifai.reminder.bifai_backend.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 권한 없음 처리 핸들러
 * BIF 사용자를 위한 이해하기 쉬운 권한 에러 메시지 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BifAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        log.warn("접근 권한 없음: {}", accessDeniedException.getMessage());
        
        // BIF 사용자를 위한 단순한 에러 메시지
        String errorMessage = "이 기능을 사용할 권한이 없습니다. 보호자에게 문의하세요.";
        
        // 요청 경로에 따른 추가 안내
        String requestPath = request.getRequestURI();
        if (requestPath.contains("/admin")) {
            errorMessage = "관리자만 사용할 수 있는 기능입니다.";
        } else if (requestPath.contains("/guardian")) {
            errorMessage = "보호자만 사용할 수 있는 기능입니다.";
        }
        
        // API 응답 형식에 맞춰 에러 반환
        ApiResponse<Void> errorResponse = ApiResponse.error(errorMessage);
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}