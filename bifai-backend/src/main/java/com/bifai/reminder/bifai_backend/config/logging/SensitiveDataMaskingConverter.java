package com.bifai.reminder.bifai_backend.config.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그에서 민감한 데이터를 마스킹하는 컨버터
 * 개인정보 보호를 위한 로그 필터링
 */
public class SensitiveDataMaskingConverter extends MessageConverter {
    
    // 마스킹할 패턴들
    private static final Pattern[] SENSITIVE_PATTERNS = {
        // 이메일
        Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", 
            Pattern.CASE_INSENSITIVE),
        
        // 전화번호 (한국 형식)
        Pattern.compile("(\\d{2,3})[-.\\s]?(\\d{3,4})[-.\\s]?(\\d{4})"),
        Pattern.compile("(010|011|016|017|018|019)[-.\\s]?(\\d{3,4})[-.\\s]?(\\d{4})"),
        
        // 주민등록번호 패턴
        Pattern.compile("\\d{6}[-.\\s]?[1-4]\\d{6}"),
        
        // 신용카드 번호
        Pattern.compile("\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}[-.\\s]?\\d{4}"),
        
        // 비밀번호 필드
        Pattern.compile("(password|pwd|passwd|비밀번호)\\s*[:=]\\s*[\"']?([^\"'\\s]+)[\"']?", 
            Pattern.CASE_INSENSITIVE),
        
        // JWT 토큰
        Pattern.compile("(Bearer\\s+)?([A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+)", 
            Pattern.CASE_INSENSITIVE),
        
        // API 키
        Pattern.compile("(api[_-]?key|apikey|access[_-]?token)\\s*[:=]\\s*[\"']?([^\"'\\s]+)[\"']?", 
            Pattern.CASE_INSENSITIVE),
        
        // IP 주소 (선택적)
        Pattern.compile("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b")
    };
    
    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // 각 패턴에 대해 마스킹 적용
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            message = maskPattern(message, pattern);
        }
        
        return message;
    }
    
    /**
     * 특정 패턴에 대한 마스킹 처리
     */
    private String maskPattern(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String masked = maskMatch(matcher);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * 매칭된 부분을 마스킹
     */
    private String maskMatch(Matcher matcher) {
        String fullMatch = matcher.group(0);
        
        // 이메일 마스킹
        if (fullMatch.contains("@")) {
            String email = matcher.group(1) + "@" + matcher.group(2);
            return maskEmail(email);
        }
        
        // 전화번호 마스킹
        if (fullMatch.matches(".*\\d{2,3}[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}.*")) {
            return maskPhoneNumber(fullMatch);
        }
        
        // 비밀번호 필드 마스킹
        if (fullMatch.toLowerCase().contains("password") || 
            fullMatch.toLowerCase().contains("pwd") ||
            fullMatch.contains("비밀번호")) {
            return matcher.group(1) + ": ***";
        }
        
        // JWT 토큰 마스킹
        if (fullMatch.contains(".") && fullMatch.split("\\.").length == 3) {
            return maskJwtToken(fullMatch);
        }
        
        // API 키 마스킹
        if (fullMatch.toLowerCase().contains("key") || 
            fullMatch.toLowerCase().contains("token")) {
            return matcher.group(1) + ": ***";
        }
        
        // 기본 마스킹
        return maskDefault(fullMatch);
    }
    
    /**
     * 이메일 마스킹
     */
    private String maskEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length != 2) return "***@***.***";
        
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "***@" + domain;
        }
        
        return localPart.substring(0, 2) + "***@" + domain;
    }
    
    /**
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phone) {
        return phone.replaceAll("\\d{4}$", "****");
    }
    
    /**
     * JWT 토큰 마스킹
     */
    private String maskJwtToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return "***.***.***";
        
        // 헤더는 유지, 페이로드와 시그니처는 마스킹
        return parts[0] + ".***.***";
    }
    
    /**
     * 기본 마스킹
     */
    private String maskDefault(String value) {
        if (value.length() <= 4) {
            return "***";
        }
        
        int visibleLength = Math.min(3, value.length() / 4);
        return value.substring(0, visibleLength) + "***";
    }
}