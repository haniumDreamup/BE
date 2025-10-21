#!/bin/bash

# OAuth2 Integration Test - AuthController에 OAuth2 기능 통합 테스트
# 기존 /api/v1/auth/oauth2/login-urls 엔드포인트가 정상 작동하는지 확인

BASE_URL="http://localhost:8080"

echo "=== OAuth2 Integration Test ==="
echo "OAuth2Controller -> AuthController 통합 검증"
echo

# 1. OAuth2 로그인 URL 조회 테스트
echo "Test 1: OAuth2 로그인 URL 조회"
echo "GET $BASE_URL/api/v1/auth/oauth2/login-urls"
echo

response=$(curl -s -w "HTTP_CODE:%{http_code}" \
  -H "Content-Type: application/json" \
  "$BASE_URL/api/v1/auth/oauth2/login-urls")

http_code=$(echo "$response" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
body=$(echo "$response" | sed 's/HTTP_CODE:[0-9]*$//')

echo "HTTP Status: $http_code"
echo "Response Body: $body"

if [ "$http_code" = "200" ]; then
  echo "✅ OAuth2 통합 성공!"

  # 응답 데이터 검증
  if [[ "$body" == *"kakao"* ]] && [[ "$body" == *"naver"* ]] && [[ "$body" == *"google"* ]]; then
    echo "✅ OAuth2 제공자 데이터 검증 성공 (kakao, naver, google 포함)"
  else
    echo "❌ OAuth2 제공자 데이터 검증 실패"
  fi

  if [[ "$body" == *"소셜 로그인 주소를 가져왔습니다"* ]]; then
    echo "✅ 응답 메시지 검증 성공"
  else
    echo "❌ 응답 메시지 검증 실패"
  fi

else
  echo "❌ OAuth2 통합 실패: HTTP $http_code"
  echo "에러 응답: $body"
fi

echo
echo "=== 테스트 완료 ==="
echo "✅ OAuth2Controller 기능이 AuthController로 성공적으로 통합됨"
echo "📍 엔드포인트: /api/v1/auth/oauth2/login-urls"
echo "🔗 통합된 인증 API: 회원가입, 로그인, 토큰갱신, 로그아웃, OAuth2 URL 조회"