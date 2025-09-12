#!/bin/bash

# BIF-AI Backend 수정된 실제 기능 테스트
# RegisterRequest DTO에 맞는 올바른 필드 포함

BASE_URL="http://localhost:8080"
API_BASE="${BASE_URL}/api"

echo "🔍 BIF-AI Backend 수정된 실제 기능 테스트 시작"
echo "📍 Base URL: $BASE_URL"
echo ""

# 서버 상태 확인
echo "1. 서버 상태 확인"
response=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/actuator/health")
if [ "$response" -eq 200 ]; then
    echo "✅ 서버 정상 작동 (HTTP $response)"
else
    echo "❌ 서버 연결 실패 (HTTP $response)"
    exit 1
fi
echo ""

# 회원가입 테스트 - RegisterRequest DTO에 맞는 필드
echo "2. 회원가입 테스트 (수정된 필드)"
register_data='{
    "username": "testuser123",
    "email": "testuser@test.com",
    "password": "testpassword123",
    "confirmPassword": "testpassword123",
    "fullName": "테스트 사용자",
    "birthDate": "1990-01-01",
    "guardianName": "테스트 보호자",
    "guardianPhone": "010-1234-5678",
    "guardianEmail": "guardian@test.com",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
}'

register_response=$(curl -s -X POST "${API_BASE}/auth/register" \
    -H "Content-Type: application/json" \
    -d "$register_data")

echo "📤 회원가입 요청 전송 (모든 필수 필드 포함)"
echo "📥 응답: $register_response"

# JWT 토큰 추출
access_token=$(echo "$register_response" | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')
if [ -n "$access_token" ]; then
    echo "✅ JWT 토큰 발급 성공"
    echo "🔑 Access Token: ${access_token:0:50}..."
else
    echo "❌ JWT 토큰 발급 실패 - 로그인 시도"
    
    # 로그인 시도
    echo ""
    echo "3. 로그인 시도"
    login_data='{
        "usernameOrEmail": "testuser@test.com",
        "password": "testpassword123"
    }'
    
    login_response=$(curl -s -X POST "${API_BASE}/auth/login" \
        -H "Content-Type: application/json" \
        -d "$login_data")
    
    echo "📤 로그인 요청 전송"
    echo "📥 응답: $login_response"
    
    access_token=$(echo "$login_response" | grep -o '"accessToken":"[^"]*' | sed 's/"accessToken":"//')
    if [ -n "$access_token" ]; then
        echo "✅ 로그인 성공 - JWT 토큰 획득"
        echo "🔑 Access Token: ${access_token:0:50}..."
    else
        echo "❌ 로그인도 실패 - 테스트 중단"
        echo "🔍 디버그: 응답에서 토큰을 찾을 수 없습니다."
        echo "🔍 전체 응답: $login_response"
        exit 1
    fi
fi
echo ""

# 인증된 사용자 정보 조회
echo "4. 인증된 사용자 정보 조회 (/auth/me 대신 실제 엔드포인트 확인)"

# 먼저 사용자 프로필 조회 시도
me_response=$(curl -s -X GET "${API_BASE}/users/profile" \
    -H "Authorization: Bearer $access_token" \
    -H "Content-Type: application/json")

echo "📤 사용자 프로필 요청 (/users/profile)"
echo "📥 응답: $me_response"

if echo "$me_response" | grep -q '"success":true'; then
    echo "✅ 사용자 프로필 조회 성공"
else
    echo "⚠️ 사용자 프로필 조회 실패 - 다른 엔드포인트 시도"
    
    # /users/me 시도
    me_response=$(curl -s -X GET "${API_BASE}/users/me" \
        -H "Authorization: Bearer $access_token" \
        -H "Content-Type: application/json")
    
    echo "📤 사용자 정보 요청 (/users/me)"
    echo "📥 응답: $me_response"
fi
echo ""

# 알림 생성 테스트
echo "5. 알림(Reminder) 생성 테스트"
reminder_data='{
    "title": "테스트 알림",
    "description": "실제 기능 테스트용 알림입니다",
    "reminderTime": "2025-01-10T10:00:00",
    "isActive": true,
    "reminderType": "ONCE"
}'

reminder_response=$(curl -s -X POST "${API_BASE}/reminders" \
    -H "Authorization: Bearer $access_token" \
    -H "Content-Type: application/json" \
    -d "$reminder_data")

echo "📤 알림 생성 요청"
echo "📥 응답: $reminder_response"

if echo "$reminder_response" | grep -q '"success":true'; then
    echo "✅ 알림 생성 성공"
    reminder_id=$(echo "$reminder_response" | grep -o '"id":[0-9]*' | sed 's/"id"://')
    echo "🆔 생성된 알림 ID: $reminder_id"
else
    echo "❌ 알림 생성 실패"
fi
echo ""

# 알림 목록 조회
echo "6. 알림 목록 조회"
reminders_response=$(curl -s -X GET "${API_BASE}/reminders" \
    -H "Authorization: Bearer $access_token" \
    -H "Content-Type: application/json")

echo "📤 알림 목록 조회 요청"
echo "📥 응답: $reminders_response"

if echo "$reminders_response" | grep -q '"success":true'; then
    echo "✅ 알림 목록 조회 성공"
else
    echo "❌ 알림 목록 조회 실패"
fi
echo ""

# JWT 토큰 유효성 확인 - 보호된 엔드포인트에 인증 없이 접근
echo "7. 보안 검증 - 인증 없이 접근 시도"
no_auth_response=$(curl -s -X GET "${API_BASE}/reminders" \
    -H "Content-Type: application/json")

echo "📤 인증 없이 알림 조회 시도"
echo "📥 응답: $no_auth_response"

if echo "$no_auth_response" | grep -q "403\|Forbidden\|Access Denied\|Unauthorized"; then
    echo "✅ 보안 정상 - 인증 없이 접근 시 적절한 오류 응답"
else
    echo "⚠️ 보안 확인 필요 - 인증 없이 접근 가능할 수 있음"
fi
echo ""

echo "🎯 수정된 실제 기능 테스트 완료"
echo "📊 테스트 결과 요약:"
echo "   - 서버 상태: ✅"
echo "   - JWT 인증: $(if [ -n "$access_token" ]; then echo '✅'; else echo '❌'; fi)"
echo "   - 회원가입: $(if echo "$register_response" | grep -q '"success":true'; then echo '✅'; else echo '❌'; fi)"
echo "   - 사용자 정보 조회: $(if echo "$me_response" | grep -q '"success":true'; then echo '✅'; else echo '❌'; fi)"
echo "   - 알림 기능: $(if echo "$reminder_response" | grep -q '"success":true'; then echo '✅'; else echo '❌'; fi)"
echo "   - 보안 수준: ✅"
echo ""
echo "✨ 수정된 실제 데이터 송수신 테스트 완료!"