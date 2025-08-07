package com.bifai.reminder.bifai_backend.config;

import com.bifai.reminder.bifai_backend.security.jwt.JwtTokenProvider;
import com.bifai.reminder.bifai_backend.security.userdetails.BifUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * WebSocket 인증 채널 인터셉터
 * STOMP CONNECT 시 JWT 토큰 검증 및 인증 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  
  private final JwtTokenProvider jwtTokenProvider;
  private final BifUserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    
    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      // CONNECT 시 인증 처리
      String authToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
      
      if (authToken != null && authToken.startsWith(BEARER_PREFIX)) {
        String jwt = authToken.substring(BEARER_PREFIX.length());
        
        try {
          // JWT 토큰 검증
          if (jwtTokenProvider.validateToken(jwt)) {
            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            
            if (username != null) {
              // 사용자 정보 조회
              UserDetails userDetails = userDetailsService.loadUserByUsername(username);
              
              // 인증 객체 생성
              Authentication authentication = new UsernamePasswordAuthenticationToken(
                  userDetails,
                  null,
                  userDetails.getAuthorities()
              );
              
              accessor.setUser(authentication);
              SecurityContextHolder.getContext().setAuthentication(authentication);
              
              log.info("WebSocket 연결 인증 성공: {}", username);
            } else {
              throw new IllegalArgumentException("토큰에서 사용자 정보를 추출할 수 없습니다");
            }
          } else {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
          }
        } catch (Exception e) {
          log.error("WebSocket 인증 실패: {}", e.getMessage());
          throw new IllegalArgumentException("인증 실패: " + e.getMessage());
        }
      } else {
        log.warn("WebSocket 연결 시도 - 인증 토큰 없음");
        throw new IllegalArgumentException("인증 토큰이 필요합니다");
      }
    }
    
    return message;
  }

  @Override
  public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
    
    if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
      Authentication auth = (Authentication) accessor.getUser();
      if (auth != null) {
        log.info("WebSocket 연결 종료: {}", auth.getName());
      }
    }
  }

}