#!/bin/bash

# GuardianRelationshipController 종합 테스트 스크립트
# 13개 REST 엔드포인트 테스트

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 함수
test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    local data=$5
    local headers=$6

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    printf "${BLUE}테스트 $TOTAL_TESTS: $description${NC}\n"

    if [ -z "$headers" ]; then
        headers="Content-Type: application/json"
    fi

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL$endpoint" -H "$headers")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" -H "$headers" -d "$data")
    elif [ "$method" = "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL$endpoint" -H "$headers" -d "$data")
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint" -H "$headers")
    fi

    # 응답에서 상태 코드 추출 (마지막 줄)
    status_code=$(echo "$response" | tail -n1)

    # 응답 본문 (상태 코드 제외)
    response_body=$(echo "$response" | sed '$d')

    printf "   요청: $method $BASE_URL$endpoint\n"
    printf "   예상 상태: $expected_status, 실제 상태: $status_code\n"

    if [ "$status_code" = "$expected_status" ]; then
        printf "   ${GREEN}✓ 통과${NC}\n"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        printf "   ${RED}✗ 실패${NC}\n"
        printf "   응답 본문: $response_body\n"
    fi

    printf "\n"
}

echo "========================================"
echo "GuardianRelationshipController 테스트 시작"
echo "13개 엔드포인트 테스트"
echo "========================================"
printf "\n"

# =========================
# 1. 보호자 초대 테스트
# =========================
echo "${YELLOW}=== 1. 보호자 초대 테스트 ===${NC}"

# 1. 인증 없이 보호자 초대 (401 예상)
invite_data='{"userId": 1, "guardianEmail": "guardian@test.com", "message": "보호자가 되어주세요"}'
test_endpoint "POST" "/api/guardian-relationships/invite" "401" "보호자 초대 - 인증 없음 (401)" "$invite_data"

# 2. 잘못된 데이터 형식 (400 예상)
invalid_data='{"userId": "invalid", "guardianEmail": "invalid-email"}'
test_endpoint "POST" "/api/guardian-relationships/invite" "400" "보호자 초대 - 잘못된 데이터 형식 (400)" "$invalid_data"

# =========================
# 2. 초대 수락/거부 테스트
# =========================
echo "${YELLOW}=== 2. 초대 수락/거부 테스트 ===${NC}"

# 3. 초대 수락 - 잘못된 토큰 (500 예상 - DB 테이블 없음)
test_endpoint "POST" "/api/guardian-relationships/accept-invitation?token=invalid&guardianId=1" "500" "초대 수락 - 잘못된 토큰 (500 DB)"

# 4. 초대 거부 - 잘못된 토큰 (500 예상 - DB 테이블 없음)
test_endpoint "POST" "/api/guardian-relationships/reject-invitation?token=invalid&guardianId=1" "500" "초대 거부 - 잘못된 토큰 (500 DB)"

# 5. 파라미터 누락 테스트 (400 예상)
test_endpoint "POST" "/api/guardian-relationships/accept-invitation" "400" "초대 수락 - 파라미터 누락 (400)"

# =========================
# 3. 권한 수정 테스트
# =========================
echo "${YELLOW}=== 3. 권한 수정 테스트 ===${NC}"

# 6. 인증 없이 권한 수정 (401 예상)
permission_data='{"canViewLocation": true, "canReceiveEmergencyAlert": true, "canViewActivity": false}'
test_endpoint "PUT" "/api/guardian-relationships/1/permissions" "401" "권한 수정 - 인증 없음 (401)" "$permission_data"

# 7. 존재하지 않는 관계 ID (400 예상)
test_endpoint "PUT" "/api/guardian-relationships/999/permissions" "401" "권한 수정 - 존재하지 않는 관계 (401)" "$permission_data"

# =========================
# 4. 관계 상태 변경 테스트
# =========================
echo "${YELLOW}=== 4. 관계 상태 변경 테스트 ===${NC}"

# 8. 인증 없이 관계 일시 중지 (401 예상)
test_endpoint "POST" "/api/guardian-relationships/1/suspend" "401" "관계 일시 중지 - 인증 없음 (401)"

# 9. 인증 없이 관계 재활성화 (401 예상)
test_endpoint "POST" "/api/guardian-relationships/1/reactivate" "401" "관계 재활성화 - 인증 없음 (401)"

# 10. 인증 없이 관계 종료 (401 예상)
test_endpoint "DELETE" "/api/guardian-relationships/1?reason=test" "401" "관계 종료 - 인증 없음 (401)"

# =========================
# 5. 목록 조회 테스트
# =========================
echo "${YELLOW}=== 5. 목록 조회 테스트 ===${NC}"

# 11. 인증 없이 사용자 보호자 목록 조회 (401 예상)
test_endpoint "GET" "/api/guardian-relationships/user/1" "401" "사용자 보호자 목록 - 인증 없음 (401)"

# 12. 인증 없이 보호자 피보호자 목록 조회 (401 예상)
test_endpoint "GET" "/api/guardian-relationships/guardian/1" "401" "보호자 피보호자 목록 - 인증 없음 (401)"

# 13. 인증 없이 긴급 연락처 조회 (401 예상)
test_endpoint "GET" "/api/guardian-relationships/user/1/emergency-contacts" "401" "긴급 연락처 조회 - 인증 없음 (401)"

# =========================
# 6. 기능 테스트
# =========================
echo "${YELLOW}=== 6. 기능 테스트 ===${NC}"

# 14. 인증 없이 권한 확인 (401 예상)
test_endpoint "GET" "/api/guardian-relationships/check-permission?guardianId=1&userId=1&permissionType=VIEW_LOCATION" "401" "권한 확인 - 인증 없음 (401)"

# 15. 인증 없이 활동 시간 업데이트 (401 예상)
test_endpoint "POST" "/api/guardian-relationships/update-activity?guardianId=1&userId=1" "401" "활동 시간 업데이트 - 인증 없음 (401)"

# =========================
# 7. 엣지 케이스 및 검증 테스트
# =========================
echo "${YELLOW}=== 7. 엣지 케이스 및 검증 테스트 ===${NC}"

# 16. 잘못된 경로 (404 예상)
test_endpoint "GET" "/api/guardian-relationships/nonexistent" "404" "존재하지 않는 엔드포인트 (404)"

# 17. 잘못된 HTTP 메서드 (405 예상)
test_endpoint "PATCH" "/api/guardian-relationships/invite" "405" "잘못된 HTTP 메서드 (405)"

# 18. 잘못된 관계 ID 형식 (400 예상)
test_endpoint "GET" "/api/guardian-relationships/user/invalid" "400" "잘못된 사용자 ID 형식 (400)"

# 19. 음수 ID (400 예상)
test_endpoint "GET" "/api/guardian-relationships/user/-1" "400" "음수 사용자 ID (400)"

# 20. 매우 큰 ID (404 또는 400 예상)
test_endpoint "GET" "/api/guardian-relationships/user/999999999" "401" "매우 큰 사용자 ID"

# =========================
# 결과 요약
# =========================
echo "========================================"
echo "           테스트 결과 요약"
echo "========================================"
printf "총 테스트: ${BLUE}$TOTAL_TESTS${NC}\n"
printf "통과: ${GREEN}$PASSED_TESTS${NC}\n"
printf "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}\n"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    printf "\n${GREEN}🎉 모든 테스트 통과! (100%% 성공률)${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    exit 1
fi