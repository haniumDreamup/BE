package com.bifai.reminder.bifai_backend.security;

import com.bifai.reminder.bifai_backend.dto.ProblemDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패 시 처리하는 EntryPoint
 * BIF 사용자를 위한 단순하고 명확한 에러 메시지 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BifAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(HttpServletRequest request, 
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("인증 실패: {}", authException.getMessage());
        
        // BIF 사용자를 위한 단순한 에러 메시지
        String errorMessage = "로그인이 필요합니다. 다시 로그인해주세요.";

        // 요청 경로에 따른 추가 안내
        String requestPath = request.getRequestURI();
        if (requestPath.contains("/guardian")) {
            errorMessage = "보호자 로그인이 필요합니다.";
        }

        // ProblemDetail 형식으로 에러 반환
        ProblemDetail problemDetail = ProblemDetail.forAuthentication(errorMessage);
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}