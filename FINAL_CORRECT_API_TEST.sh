#!/bin/bash

BASE_URL="http://localhost:8080/api"

echo "🎯 완전히 올바른 필드명으로 최종 테스트..."

# 1. 완전히 올바른 회원가입 테스트
echo "Testing: 완전히 올바른 회원가입 (모든 필수 필드 포함)..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser123",
    "email": "test@example.com", 
    "password": "Test123!@#",
    "confirmPassword": "Test123!@#",
    "fullName": "테스트 사용자",
    "birthDate": "1990-01-01",
    "guardianName": "보호자",
    "guardianPhone": "010-1234-5678",
    "guardianEmail": "guardian@example.com",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
  }' -w "%{http_code}")

HTTP_CODE="${REGISTER_RESPONSE: -3}"
RESPONSE_BODY="${REGISTER_RESPONSE%???}"

echo "회원가입 결과: HTTP $HTTP_CODE"
echo "응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✅ 회원가입 성공!"
    
    # 2. 로그인 테스트
    echo ""
    echo "Testing: 로그인..."
    LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "usernameOrEmail": "test@example.com",
        "password": "Test123!@#"
      }' -w "%{http_code}")
    
    HTTP_CODE="${LOGIN_RESPONSE: -3}"
    RESPONSE_BODY="${LOGIN_RESPONSE%???}"
    
    echo "로그인 결과: HTTP $HTTP_CODE"
    echo "응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ 로그인 성공!"
        
        # JWT 토큰 추출 (여러 경로 시도)
        JWT_TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.data.accessToken // .d.accessToken // .accessToken // empty' 2>/dev/null)
        if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
            echo "✅ JWT 토큰 획득: ${JWT_TOKEN:0:30}..."
            
            # 3. 인증된 API 테스트들
            echo ""
            echo "=== 인증된 API 테스트 시작 ==="
            
            # 현재 사용자 정보 조회
            echo "Testing: 현재 사용자 정보 조회..."
            USER_INFO_RESPONSE=$(curl -s -X GET "$BASE_URL/users/current" \
              -H "Authorization: Bearer $JWT_TOKEN" -w "%{http_code}")
            
            HTTP_CODE="${USER_INFO_RESPONSE: -3}"
            echo "현재 사용자 조회: HTTP $HTTP_CODE $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "❌")"
            
            # 알림 목록 조회
            echo "Testing: 알림 목록 조회..."
            REMINDERS_RESPONSE=$(curl -s -X GET "$BASE_URL/reminders" \
              -H "Authorization: Bearer $JWT_TOKEN" -w "%{http_code}")
            
            HTTP_CODE="${REMINDERS_RESPONSE: -3}"
            echo "알림 목록 조회: HTTP $HTTP_CODE $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "❌")"
            
            # 알림 생성
            echo "Testing: 알림 생성..."
            CREATE_REMINDER_RESPONSE=$(curl -s -X POST "$BASE_URL/reminders" \
              -H "Authorization: Bearer $JWT_TOKEN" \
              -H "Content-Type: application/json" \
              -d '{
                "title": "테스트 알림",
                "content": "이것은 테스트 알림입니다",
                "reminderTime": "2024-12-31T23:59:59",
                "repeatType": "NONE"
              }' -w "%{http_code}")
            
            HTTP_CODE="${CREATE_REMINDER_RESPONSE: -3}"
            echo "알림 생성: HTTP $HTTP_CODE $([ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ] && echo "✅" || echo "❌")"
            
            # 응급 연락처 조회
            echo "Testing: 응급 연락처 조회..."
            EMERGENCY_RESPONSE=$(curl -s -X GET "$BASE_URL/emergency-contacts" \
              -H "Authorization: Bearer $JWT_TOKEN" -w "%{http_code}")
            
            HTTP_CODE="${EMERGENCY_RESPONSE: -3}"
            echo "응급 연락처 조회: HTTP $HTTP_CODE $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "❌")"
            
            # 사용자 설정 조회
            echo "Testing: 사용자 설정 조회..."
            PREFERENCES_RESPONSE=$(curl -s -X GET "$BASE_URL/users/preferences" \
              -H "Authorization: Bearer $JWT_TOKEN" -w "%{http_code}")
            
            HTTP_CODE="${PREFERENCES_RESPONSE: -3}"
            echo "사용자 설정 조회: HTTP $HTTP_CODE $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "❌")"
            
            echo ""
            echo "🎯 인증된 API 테스트 완료!"
            
        else
            echo "❌ JWT 토큰 추출 실패"
            echo "응답 구조 분석:"
            echo "$RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
        fi
    else
        echo "❌ 로그인 실패"
    fi
else
    echo "❌ 회원가입 실패 - 필드 검증 문제"
    echo "응답 분석:"
    echo "$RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
fi

echo ""
echo "🔍 헬스체크 테스트..."
HEALTH_RESPONSE=$(curl -s -X GET "$BASE_URL/auth/health" -w "%{http_code}")
HTTP_CODE="${HEALTH_RESPONSE: -3}"
echo "인증 헬스체크: HTTP $HTTP_CODE $([ "$HTTP_CODE" = "200" ] && echo "✅" || echo "❌")"
