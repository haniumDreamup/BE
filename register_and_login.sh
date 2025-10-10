#!/bin/bash

BASE_URL="http://43.200.49.171:8080"

echo "=== 회원가입 및 로그인 테스트 ==="
echo ""

# 랜덤 사용자 생성
RANDOM_NUM=$RANDOM
USERNAME="testuser${RANDOM_NUM}"
EMAIL="test${RANDOM_NUM}@test.com"
PASSWORD="Test1234!@#\$"

echo "사용자 정보:"
echo "  Username: $USERNAME"
echo "  Email: $EMAIL"
echo "  Password: $PASSWORD"
echo ""

# 회원가입
echo "1. 회원가입 시도..."
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USERNAME\",
    \"email\": \"$EMAIL\",
    \"password\": \"$PASSWORD\",
    \"confirmPassword\": \"$PASSWORD\",
    \"fullName\": \"테스트 사용자\",
    \"birthDate\": \"1990-01-01\",
    \"gender\": \"MALE\",
    \"languagePreference\": \"ko\",
    \"agreeToTerms\": true,
    \"agreeToPrivacyPolicy\": true,
    \"agreeToMarketing\": false
  }")

echo "회원가입 응답:"
echo "$REGISTER_RESPONSE" | jq .
echo ""

# 로그인
echo "2. 로그인 시도..."
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"usernameOrEmail\": \"$USERNAME\",
    \"password\": \"$PASSWORD\"
  }")

echo "로그인 응답:"
echo "$LOGIN_RESPONSE" | jq .
echo ""

# JWT 토큰 추출
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.d.data.accessToken // .d.accessToken // .data.accessToken // empty')

if [ -z "$JWT_TOKEN" ] || [ "$JWT_TOKEN" = "null" ]; then
  echo "❌ JWT 토큰 추출 실패"
  exit 1
fi

echo "✅ JWT 토큰 획득 성공!"
echo "Token: $JWT_TOKEN"
echo ""

# 토큰 저장
echo "$JWT_TOKEN" > /tmp/jwt_token.txt
echo "✅ 토큰이 /tmp/jwt_token.txt에 저장되었습니다"
echo ""

# 3. Emergency Contact API 테스트
echo "3. Emergency Contact API 테스트..."
EC_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/emergency-contacts/user/1" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "Emergency Contact 응답:"
echo "$EC_RESPONSE" | jq .
echo ""

# 4. Guardian Dashboard API 테스트
echo "4. Guardian Dashboard API 테스트..."
GD_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/guardian/dashboard/daily-summary/1?guardianId=1" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "Guardian Dashboard 응답:"
echo "$GD_RESPONSE" | jq .
echo ""

# 5. Admin API 테스트
echo "5. Admin API 테스트..."
ADMIN_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/admin/statistics" \
  -H "Authorization: Bearer $JWT_TOKEN")

echo "Admin 응답:"
echo "$ADMIN_RESPONSE" | jq .
