#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"

echo "🔧 올바른 API 스펙으로 재테스트..."

# 1. 올바른 회원가입 테스트
echo "Testing: 올바른 회원가입..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser123",
    "email": "test@example.com", 
    "password": "Test123!@#",
    "confirmPassword": "Test123!@#",
    "name": "테스트 사용자",
    "birth": "1990-01-01",
    "gender": "MALE",
    "cognitiveLevel": "MILD",
    "guardianName": "보호자",
    "guardianPhone": "010-1234-5678"
  }' -w "%{http_code}")

HTTP_CODE="${REGISTER_RESPONSE: -3}"
RESPONSE_BODY="${REGISTER_RESPONSE%???}"

echo "회원가입 결과: HTTP $HTTP_CODE"
echo "응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "✅ 회원가입 성공!"
    
    # 2. 올바른 로그인 테스트
    echo ""
    echo "Testing: 올바른 로그인..."
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
        
        # JWT 토큰 추출
        JWT_TOKEN=$(echo "$RESPONSE_BODY" | jq -r '.data.accessToken // .d.accessToken // empty' 2>/dev/null)
        if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
            echo "✅ JWT 토큰 획득: ${JWT_TOKEN:0:30}..."
            
            # 3. 인증된 API 테스트
            echo ""
            echo "Testing: 현재 사용자 정보 조회..."
            USER_INFO_RESPONSE=$(curl -s -X GET "$BASE_URL/users/current" \
              -H "Authorization: Bearer $JWT_TOKEN" -w "%{http_code}")
            
            HTTP_CODE="${USER_INFO_RESPONSE: -3}"
            RESPONSE_BODY="${USER_INFO_RESPONSE%???}"
            
            echo "사용자 정보 조회 결과: HTTP $HTTP_CODE"
            echo "응답: $RESPONSE_BODY" | jq '.' 2>/dev/null || echo "$RESPONSE_BODY"
            
        else
            echo "❌ JWT 토큰 추출 실패"
        fi
    else
        echo "❌ 로그인 실패"
    fi
else
    echo "❌ 회원가입 실패"
fi
