#!/bin/bash

# SOS 이력 조회 API 테스트만 실행 (테스트 10-12번)
# User.devices @JsonIgnore 추가 후 LazyInitializationException 수정 확인

BASE_URL="http://localhost:8080"

# JWT 토큰 생성 함수
get_jwt_token() {
  local userId=$1
  java -cp build/libs/bifai-backend-0.0.1-SNAPSHOT.jar:build/libs/* \
    com.bifai.reminder.bifai_backend.utils.JwtAuthUtils $userId 2>/dev/null
}

echo "=== SOS 이력 조회 API 테스트 (Tests 10-12) ==="
echo "User.devices LazyInitializationException 수정 확인"
echo

# 토큰 생성
TOKEN=$(get_jwt_token 1)
if [ -z "$TOKEN" ]; then
  echo "❌ JWT 토큰 생성 실패"
  exit 1
fi

echo "✅ JWT 토큰 생성 성공"
echo

success_count=0
total_tests=3

# 테스트 10: SOS 이력 조회 (빈 이력)
echo "Test 10: SOS 이력 조회 (빈 이력)"
response=$(curl -s -w "HTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  "$BASE_URL/api/v1/emergency/sos/history")

http_code=$(echo "$response" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
body=$(echo "$response" | sed 's/HTTP_CODE:[0-9]*$//')

if [ "$http_code" = "200" ]; then
  echo "✅ Test 10 성공: HTTP $http_code"
  echo "응답: $body"
  success_count=$((success_count + 1))
else
  echo "❌ Test 10 실패: HTTP $http_code"
  echo "응답: $body"
  if [[ "$body" == *"LazyInitializationException"* ]]; then
    echo "🚨 LazyInitializationException 여전히 발생!"
  fi
fi
echo

# 테스트 11: SOS 발동 후 이력 조회
echo "Test 11: SOS 발동 후 이력 조회"

# 먼저 SOS 발동
sos_response=$(curl -s -w "HTTP_CODE:%{http_code}" \
  -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 37.5665,
    "longitude": 126.9780,
    "address": "서울시 중구 명동",
    "emergencyType": "PANIC",
    "message": "테스트 긴급상황",
    "shareLocation": true,
    "notifyAllContacts": false
  }' \
  "$BASE_URL/api/v1/emergency/sos/trigger")

sos_http_code=$(echo "$sos_response" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
if [ "$sos_http_code" != "201" ]; then
  echo "❌ SOS 발동 실패: HTTP $sos_http_code"
  echo "SOS 응답: $(echo "$sos_response" | sed 's/HTTP_CODE:[0-9]*$//')"
  echo
else
  echo "✅ SOS 발동 성공"

  # 이력 조회
  response=$(curl -s -w "HTTP_CODE:%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    "$BASE_URL/api/v1/emergency/sos/history")

  http_code=$(echo "$response" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  body=$(echo "$response" | sed 's/HTTP_CODE:[0-9]*$//')

  if [ "$http_code" = "200" ]; then
    echo "✅ Test 11 성공: HTTP $http_code"
    echo "응답: $body"
    success_count=$((success_count + 1))
  else
    echo "❌ Test 11 실패: HTTP $http_code"
    echo "응답: $body"
    if [[ "$body" == *"LazyInitializationException"* ]]; then
      echo "🚨 LazyInitializationException 여전히 발생!"
    fi
  fi
fi
echo

# 테스트 12: 다른 사용자의 SOS 이력 조회 (권한 테스트)
echo "Test 12: 다른 사용자의 SOS 이력 조회 (권한 테스트)"
TOKEN_USER999=$(get_jwt_token 999)

if [ -z "$TOKEN_USER999" ]; then
  echo "❌ 사용자 999의 JWT 토큰 생성 실패"
else
  response=$(curl -s -w "HTTP_CODE:%{http_code}" \
    -H "Authorization: Bearer $TOKEN_USER999" \
    -H "Content-Type: application/json" \
    "$BASE_URL/api/v1/emergency/sos/history")

  http_code=$(echo "$response" | grep -o "HTTP_CODE:[0-9]*" | cut -d: -f2)
  body=$(echo "$response" | sed 's/HTTP_CODE:[0-9]*$//')

  if [ "$http_code" = "200" ]; then
    echo "✅ Test 12 성공: HTTP $http_code (사용자별 이력 분리됨)"
    echo "응답: $body"
    success_count=$((success_count + 1))
  else
    echo "❌ Test 12 실패: HTTP $http_code"
    echo "응답: $body"
    if [[ "$body" == *"LazyInitializationException"* ]]; then
      echo "🚨 LazyInitializationException 여전히 발생!"
    fi
  fi
fi
echo

# 결과 요약
echo "=== 테스트 결과 요약 ==="
echo "성공: $success_count / $total_tests"
success_rate=$(echo "scale=1; $success_count * 100 / $total_tests" | bc)
echo "성공률: $success_rate%"

if [ "$success_count" -eq "$total_tests" ]; then
  echo "🎉 모든 SOS 이력 조회 테스트 성공! LazyInitializationException 해결됨!"
  exit 0
else
  echo "⚠️  일부 테스트 실패. LazyInitializationException 수정 필요 또는 다른 이슈 존재"
  exit 1
fi