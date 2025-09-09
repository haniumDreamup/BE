#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
TIMESTAMP=$(date +%s)

echo "🔍 JWT 디버깅 테스트..."

# 1. 회원가입
echo "1️⃣ 새 사용자 회원가입..."
curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"debug_$TIMESTAMP\",
    \"email\": \"debug_$TIMESTAMP@test.com\", 
    \"password\": \"Test123!@#\",
    \"confirmPassword\": \"Test123!@#\",
    \"fullName\": \"디버그 사용자\",
    \"birthDate\": \"1990-01-01\",
    \"guardianName\": \"보호자\",
    \"guardianPhone\": \"010-1234-5678\",
    \"guardianEmail\": \"guardian_$TIMESTAMP@test.com\",
    \"agreeToTerms\": true,
    \"agreeToPrivacyPolicy\": true,
    \"agreeToMarketing\": false
  }" -w "HTTP: %{http_code}\n" | head -3

# 2. 로그인 및 토큰 획득
echo -e "\n2️⃣ 로그인..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"usernameOrEmail\": \"debug_$TIMESTAMP@test.com\",
    \"password\": \"Test123!@#\"
  }")

echo "로그인 응답:"
echo "$LOGIN_RESPONSE" | jq '.' 2>/dev/null | head -10

# JWT 토큰 추출
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.d.data.accessToken' 2>/dev/null)

if [ -n "$JWT_TOKEN" ] && [ "$JWT_TOKEN" != "null" ]; then
    echo -e "\n✅ JWT 토큰: ${JWT_TOKEN:0:50}..."
    
    echo -e "\n3️⃣ 즉시 인증 테스트..."
    
    # 즉시 API 호출
    echo "현재 사용자 조회:"
    USER_RESPONSE=$(curl -s -X GET "$BASE_URL/users/current" \
      -H "Authorization: Bearer $JWT_TOKEN" \
      -w "\nHTTP: %{http_code}")
    
    echo "$USER_RESPONSE" | head -10
    
else
    echo "❌ JWT 토큰 추출 실패"
fi
