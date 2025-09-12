#!/bin/bash

# API 테스트 스크립트
# 사용법: ./test-api.sh

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "🚀 BIF-AI API 테스트 시작..."

# 1. 헬스체크
echo -n "1. 헬스체크 테스트... "
if curl -s "$BASE_URL/actuator/health" | grep -q "UP"; then
    echo -e "${GREEN}✓ 성공${NC}"
else
    echo -e "${RED}✗ 실패${NC}"
    echo "서버가 실행 중인지 확인하세요."
    exit 1
fi

# 2. 회원가입
echo -n "2. 회원가입 테스트... "
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "testuser'$(date +%s)'",
        "email": "test'$(date +%s)'@test.com",
        "password": "Test1234!",
        "fullName": "테스트 유저"
    }')

if echo "$REGISTER_RESPONSE" | grep -q "success.*true"; then
    echo -e "${GREEN}✓ 성공${NC}"
    USERNAME=$(echo "$REGISTER_RESPONSE" | grep -o '"username":"[^"]*' | cut -d'"' -f4)
else
    echo -e "${RED}✗ 실패${NC}"
    echo "$REGISTER_RESPONSE"
fi

# 3. 로그인
echo -n "3. 로그인 테스트... "
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "'$USERNAME'",
        "password": "Test1234!"
    }')

if echo "$LOGIN_RESPONSE" | grep -q "accessToken"; then
    echo -e "${GREEN}✓ 성공${NC}"
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
else
    echo -e "${RED}✗ 실패${NC}"
    echo "$LOGIN_RESPONSE"
fi

# 4. 프로필 조회 (인증 필요)
echo -n "4. 프로필 조회 테스트... "
PROFILE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/users/profile" \
    -H "Authorization: Bearer $TOKEN")

if echo "$PROFILE_RESPONSE" | grep -q "$USERNAME"; then
    echo -e "${GREEN}✓ 성공${NC}"
else
    echo -e "${RED}✗ 실패${NC}"
fi

# 5. 일정 생성
echo -n "5. 일정 생성 테스트... "
SCHEDULE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/schedules" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "title": "테스트 일정",
        "description": "API 테스트용 일정",
        "startTime": "2025-09-01T10:00:00",
        "location": "테스트 장소"
    }')

if echo "$SCHEDULE_RESPONSE" | grep -q "success.*true"; then
    echo -e "${GREEN}✓ 성공${NC}"
else
    echo -e "${RED}✗ 실패${NC}"
fi

# 6. OAuth2 URL 조회 (인증 불필요)
echo -n "6. OAuth2 URL 조회 테스트... "
OAUTH_RESPONSE=$(curl -s -X GET "$BASE_URL/api/auth/oauth2/login-urls")

if echo "$OAUTH_RESPONSE" | grep -q "kakao"; then
    echo -e "${GREEN}✓ 성공${NC}"
else
    echo -e "${RED}✗ 실패${NC}"
fi

echo ""
echo "✅ 기본 API 테스트 완료!"
echo ""
echo "📝 추가 테스트를 위해서는:"
echo "1. Swagger UI 접속: http://localhost:8080/swagger-ui.html"
echo "2. API_TEST_CHECKLIST.md 참조"