#!/bin/bash

BASE_URL="http://43.200.49.171:8080"

echo "=== 프로덕션 서버 실제 인증 테스트 ==="

# 1. 회원가입
echo "1. 회원가입..."
REGISTER=$(curl -s -X POST "$BASE_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser001",
    "email": "test001@example.com",
    "password": "Test1234!",
    "confirmPassword": "Test1234!",
    "name": "테스트사용자",
    "phoneNumber": "01012345678",
    "birthDate": "1990-01-01",
    "gender": "MALE",
    "role": "ELDERLY"
  }')
echo "✅ 회원가입: $(echo $REGISTER | jq -r '.d.success // .success')"

# 2. 로그인
echo -e "\n2. 로그인..."
LOGIN=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser001",
    "password": "Test1234!"
  }')

TOKEN=$(echo $LOGIN | jq -r '.d.data.accessToken // .data.accessToken // empty')

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
  echo "✅ 로그인 성공! 토큰 획득"
  
  echo -e "\n3. 인증된 API 테스트..."
  echo "- 내 정보 조회:"
  curl -s "$BASE_URL/api/v1/users/me" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.d.data.username // "실패"'
  
  echo "- 통계 조회:"
  curl -s "$BASE_URL/api/v1/users/me/statistics" \
    -H "Authorization: Bearer $TOKEN" | jq -r '.d.success // "실패"'
  
  echo -e "\n✅ 프로덕션 서버 정상 동작 확인!"
else
  echo "❌ 로그인 실패"
  echo $LOGIN | jq '.'
fi
