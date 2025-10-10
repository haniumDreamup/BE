#!/bin/bash

# JWT 토큰 획득 스크립트
BASE_URL="http://43.200.49.171:8080"

echo "=== JWT 토큰 획득 중 ==="
echo ""

# testuser 로그인
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"usernameOrEmail\":\"testuser\",\"password\":\"Test1234!@#\$\"}")

echo "응답: $RESPONSE"
echo ""

# JWT 토큰 추출
JWT_TOKEN=$(echo "$RESPONSE" | jq -r '.d.accessToken // .data.accessToken // empty')

if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "null" ]; then
  echo "❌ 로그인 실패"
  echo "응답 내용:"
  echo "$RESPONSE" | jq .
  exit 1
fi

echo "✅ 로그인 성공!"
echo "JWT 토큰: $JWT_TOKEN"
echo ""

# 토큰 파일로 저장
echo "$JWT_TOKEN" > /tmp/jwt_token.txt
echo "토큰이 /tmp/jwt_token.txt에 저장되었습니다"
echo ""

# 토큰 테스트 - 사용자 정보 조회
echo "=== 토큰 검증 중 ==="
USER_INFO=$(curl -s -X GET "${BASE_URL}/api/v1/users/me" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "사용자 정보: $USER_INFO" | jq .
