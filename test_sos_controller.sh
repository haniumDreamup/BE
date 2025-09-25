#!/bin/bash

# EmergencyController SOS 종합 테스트 스크립트
# 4개 긴급 상황 엔드포인트 테스트

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
echo "EmergencyController SOS 테스트 시작"
echo "4개 긴급 상황 엔드포인트 테스트"
echo "========================================"
printf "\n"

# =========================
# 1. SOS 발동 테스트
# =========================
echo "${YELLOW}=== 1. SOS 발동 테스트 ===${NC}"

# 1. 인증 없이 SOS 발동 (401 예상)
sos_data='{"latitude": 37.123, "longitude": 127.456, "emergencyType": "PANIC", "message": "도움이 필요합니다", "notifyAllContacts": true, "shareLocation": true}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - 인증 없음 (401)" "$sos_data"

# 2. 잘못된 데이터 형식 (401 예상 - 인증 우선)
invalid_sos_data='{"latitude": "invalid", "longitude": "invalid", "emergencyType": "UNKNOWN"}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - 잘못된 데이터 (401 인증 우선)" "$invalid_sos_data"

# 3. 필수 필드 누락 (401 예상 - 인증 우선)
incomplete_data='{"message": "도움 필요"}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - 필수 필드 누락 (401 인증 우선)" "$incomplete_data"

# =========================
# 2. SOS 취소 테스트
# =========================
echo "${YELLOW}=== 2. SOS 취소 테스트 ===${NC}"

# 4. 인증 없이 SOS 취소 (401 예상)
test_endpoint "PUT" "/api/v1/emergency/sos/123/cancel" "401" "SOS 취소 - 인증 없음 (401)"

# 5. 잘못된 응급 ID 형식 (401 예상 - 인증 우선)
test_endpoint "PUT" "/api/v1/emergency/sos/invalid/cancel" "401" "SOS 취소 - 잘못된 ID 형식 (401 인증 우선)"

# 6. 음수 응급 ID (401 예상 - 인증 우선)
test_endpoint "PUT" "/api/v1/emergency/sos/-1/cancel" "401" "SOS 취소 - 음수 ID (401 인증 우선)"

# 7. 존재하지 않는 응급 ID (401 예상 - 인증 우선)
test_endpoint "PUT" "/api/v1/emergency/sos/999999/cancel" "401" "SOS 취소 - 존재하지 않는 ID (401 인증 우선)"

# =========================
# 3. SOS 이력 조회 테스트
# =========================
echo "${YELLOW}=== 3. SOS 이력 조회 테스트 ===${NC}"

# 8. 인증 없이 SOS 이력 조회 (401 예상)
test_endpoint "GET" "/api/v1/emergency/sos/history" "401" "SOS 이력 조회 - 인증 없음 (401)"

# =========================
# 4. 빠른 SOS 테스트
# =========================
echo "${YELLOW}=== 4. 빠른 SOS 테스트 ===${NC}"

# 9. 인증 없이 빠른 SOS (401 예상)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.123&longitude=127.456" "401" "빠른 SOS - 인증 없음 (401)"

# 10. 잘못된 좌표 형식 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=invalid&longitude=invalid" "401" "빠른 SOS - 잘못된 좌표 (401 인증 우선)"

# 11. 좌표 누락 (400 예상)
test_endpoint "POST" "/api/v1/emergency/sos/quick" "400" "빠른 SOS - 좌표 누락 (400)"

# 12. 일부 좌표 누락 (400 예상)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.123" "400" "빠른 SOS - 경도 누락 (400)"

# =========================
# 5. 메서드 검증 및 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 5. 메서드 검증 및 엣지 케이스 테스트 ===${NC}"

# 13. 잘못된 HTTP 메서드 - GET으로 SOS 발동 시도 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - GET 메서드 (401 인증 우선)"

# 14. 잘못된 HTTP 메서드 - DELETE로 SOS 발동 시도 (401 예상 - 인증 우선)
test_endpoint "DELETE" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - DELETE 메서드 (401 인증 우선)"

# 15. 잘못된 HTTP 메서드 - POST로 SOS 취소 시도 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/emergency/sos/123/cancel" "401" "SOS 취소 - POST 메서드 (401 인증 우선)"

# 16. 존재하지 않는 엔드포인트 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/v1/emergency/sos/nonexistent" "401" "존재하지 않는 SOS 엔드포인트 (401 인증 우선)"

# 17. 루트 SOS 경로 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/sos" "401" "루트 SOS 경로 (401 인증 우선)"

# =========================
# 6. 경계값 및 보안 테스트
# =========================
echo "${YELLOW}=== 6. 경계값 및 보안 테스트 ===${NC}"

# 18. 매우 큰 좌표값 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=999999&longitude=999999" "401" "빠른 SOS - 큰 좌표값 (401 인증 우선)"

# 19. 매우 작은 좌표값 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=-999999&longitude=-999999" "401" "빠른 SOS - 작은 좌표값 (401 인증 우선)"

# 20. 빈 데이터로 SOS 발동 시도 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "401" "SOS 발동 - 빈 데이터 (401 인증 우선)" ""

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
    printf "${YELLOW}📝 SOS 엔드포인트는 긴급 상황을 위한 중요한 API입니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 SOS 엔드포인트는 긴급 상황을 위한 중요한 API입니다${NC}\n"
    exit 1
fi