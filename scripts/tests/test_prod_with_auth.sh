#!/bin/bash

BASE_URL="http://43.200.49.171:8080"

echo "=== 프로덕션 서버 인증 플로우 테스트 ==="

# 1. 회원가입
echo "1. 회원가입 시도..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "01012345678",
    "password": "testPassword123!",
    "userName": "테스트유저"
  }')
echo "회원가입 응답: $REGISTER_RESPONSE"

# 2. 로그인
echo -e "\n2. 로그인 시도..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "01012345678",
    "password": "testPassword123!"
  }')
echo "로그인 응답: $LOGIN_RESPONSE"

# JWT 토큰 추출
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken // .accessToken // empty')

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "✅ JWT 토큰 획득 성공!"
  
  # 3. 인증된 요청
  echo -e "\n3. 인증된 요청 테스트..."
  curl -s -X GET "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  
  echo -e "\n4. 통계 조회..."
  curl -s -X GET "$BASE_URL/api/v1/users/me/statistics" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
else
  echo "❌ 토큰 획득 실패"
fi
