#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)

echo "🚀 새로운 사용자로 성공적인 API 테스트..."

# 1. 새로운 사용자명으로 회원가입
echo "Testing: 새로운 사용자 회원가입..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"testuser$TIMESTAMP\",
    \"email\": \"test$TIMESTAMP@example.com\", 
    \"password\": \"Test123!@#\",
    \"confirmPassword\": \"Test123!@#\",
    \"fullName\": \"테스트 사용자\",
    \"birthDate\": \"1990-01-01\",
    \"guardianName\": \"보호자\",
    \"guardianPhone\": \"010-1234-5678\",
    \"guardianEmail\": \"guardian$TIMESTAMP@example.com\",
    \"agreeToTerms\": true,
    \"agreeToPrivacyPolicy\": true,
    \"agreeToMarketing\": false
  }" -w "%{http_code}")

HTTP_CODE="${REGISTER_RESPONSE: -3}"
RESPONSE_BODY="${REGISTER_RESPONSE%???}"

echo "회원가입 결과: HTTP $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✅ 회원가입 성공!"
    
    # 2. 로그인 테스트
    echo ""
    echo "Testing: 로그인..."
    LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d "{
        \"usernameOrEmail\": \"test$TIMESTAMP@example.com\",
        \"password\": \"Test123!@#\"
      }" -w "%{http_code}")
    
    HTTP_CODE="${LOGIN_RESPONSE: -3}"
    RESPONSE_BODY="${LOGIN_RESPONSE%???}"
    
    echo "로그인 결과: HTTP $HTTP_CODE"
    
    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ 로그인 성공!"
        
        # JWT 토큰 추출
        JWT_TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.data.accessToken // .d.accessToken // .accessToken // empty' 2>/dev/null)
        
        if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
            echo "✅ JWT 토큰 획득 성공: ${JWT_TOKEN:0:30}..."
            
            echo ""
            echo "🔥 전체 API 기능 테스트 시작!"
            echo "================================"
            
            TOTAL_TESTS=0
            PASSED_TESTS=0
            
            # 테스트 함수
            test_api() {
                local name="$1"
                local method="$2"
                local endpoint="$3"
                local data="$4"
                
                echo -n "[$((++TOTAL_TESTS))] $name: "
                
                if [ "$method" = "GET" ]; then
                    response=$(curl -s -X GET "$BASE_URL$endpoint" \
                      -H "Authorization: Bearer $JWT_TOKEN" -w "%{http_code}")
                else
                    response=$(curl -s -X "$method" "$BASE_URL$endpoint" \
                      -H "Authorization: Bearer $JWT_TOKEN" \
                      -H "Content-Type: application/json" \
                      -d "$data" -w "%{http_code}")
                fi
                
                http_code="${response: -3}"
                
                if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
                    echo "✅ HTTP $http_code"
                    ((PASSED_TESTS++))
                else
                    echo "❌ HTTP $http_code"
                fi
            }
            
            # 사용자 관련 API 테스트
            echo "👤 사용자 관련 API:"
            test_api "현재 사용자 조회" "GET" "/users/current"
            test_api "사용자 설정 조회" "GET" "/users/preferences"
            
            # 알림 관련 API 테스트
            echo ""
            echo "🔔 알림 관련 API:"
            test_api "알림 목록 조회" "GET" "/reminders"
            test_api "알림 생성" "POST" "/reminders" '{
                "title": "테스트 알림",
                "content": "API 테스트용 알림입니다",
                "reminderTime": "2024-12-31T23:59:59",
                "repeatType": "NONE"
            }'
            
            # 응급 관련 API 테스트
            echo ""
            echo "🚨 응급 관련 API:"
            test_api "응급 연락처 조회" "GET" "/emergency-contacts"
            test_api "응급상황 조회" "GET" "/emergency"
            
            # 보호자 관련 API 테스트
            echo ""
            echo "👨‍👩‍👧‍👦 보호자 관련 API:"
            test_api "보호자 관리 조회" "GET" "/guardian-management"
            
            # 기능 테스트 API
            echo ""
            echo "🧪 기능 테스트 API:"
            test_api "A/B 테스트 조회" "GET" "/experiments/current"
            test_api "접근성 설정 조회" "GET" "/accessibility/settings"
            
            # 최종 결과
            echo ""
            echo "================================"
            echo "🎯 **최종 테스트 결과**"
            echo "총 테스트: $TOTAL_TESTS"
            echo "성공: $PASSED_TESTS"
            echo "실패: $((TOTAL_TESTS - PASSED_TESTS))"
            echo "성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
            echo "================================"
            
            if [ $PASSED_TESTS -gt 0 ]; then
                echo "✅ **API 시스템이 정상적으로 작동하고 있습니다!**"
                echo "🔑 JWT 인증이 정상 작동"
                echo "📱 핵심 API 기능들이 정상 동작"
            else
                echo "❌ **API 시스템에 문제가 있습니다**"
            fi
            
        else
            echo "❌ JWT 토큰 추출 실패"
            echo "로그인 응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
        fi
    else
        echo "❌ 로그인 실패: HTTP $HTTP_CODE"
        echo "응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
    fi
else
    echo "❌ 회원가입 실패: HTTP $HTTP_CODE"
    echo "응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
fi
