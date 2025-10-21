#!/bin/bash

# Accessibility Controller 직접 테스트 - Production
BASE_URL="http://43.200.49.171:8080"
TIMESTAMP=$(date +%s)

echo "==================================="
echo "Accessibility Controller 직접 테스트"
echo "==================================="

# 1. 회원가입
echo ""
echo "1. 회원가입..."
REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "acctest'"$TIMESTAMP"'",
    "email": "acctest'"$TIMESTAMP"'@test.com",
    "password": "TestPass1234",
    "confirmPassword": "TestPass1234",
    "fullName": "접근성 테스트",
    "languagePreference": "ko",
    "languagePreferenceSecondary": "ko",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
  }')

JWT_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.data.accessToken // .d.data.accessToken // empty')

if [ -z "$JWT_TOKEN" ]; then
  echo "❌ 회원가입 실패 - JWT 토큰을 찾을 수 없음"
  echo "$REGISTER_RESPONSE" | jq .
  exit 1
fi

echo "✅ 회원가입 성공"
echo "JWT Token: ${JWT_TOKEN:0:30}..."

# 2. Accessibility 설정 조회
echo ""
echo "2. Accessibility 설정 조회..."
ACC_RESULT=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/accessibility/settings" \
  -H "Authorization: Bearer $JWT_TOKEN")

HTTP_CODE=$(echo "$ACC_RESULT" | tail -1)
BODY=$(echo "$ACC_RESULT" | head -n -1)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ Accessibility 조회 성공!"
  echo "$BODY" | jq .
else
  echo "❌ Accessibility 조회 실패"
  echo "$BODY" | jq . 2>/dev/null || echo "$BODY"
fi

echo ""
echo "==================================="
echo "테스트 완료"
echo "==================================="
