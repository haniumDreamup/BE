#!/bin/bash

# EmergencyController SOS 기능 테스트 스크립트
# 4개 긴급 상황 엔드포인트의 실제 기능 테스트

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

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

    status_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | sed '$d')

    printf "   요청: $method $BASE_URL$endpoint\n"
    printf "   예상 상태: $expected_status, 실제 상태: $status_code\n"

    if [ "$status_code" = "$expected_status" ]; then
        printf "   ${GREEN}✓ 통과${NC}\n"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        if [ -n "$response_body" ] && [ "$response_body" != "null" ]; then
            printf "   응답: $response_body\n"
        fi
    else
        printf "   ${RED}✗ 실패${NC}\n"
        printf "   응답 본문: $response_body\n"
    fi

    printf "\n"
}

echo "========================================"
echo "EmergencyController SOS 기능 테스트 시작"
echo "4개 긴급 상황 엔드포인트 실제 기능 테스트"
echo "========================================"
printf "\n"

# =========================
# 1. SOS 발동 기능 테스트 (POST /api/v1/emergency/sos/trigger)
# =========================
echo "${YELLOW}=== 1. SOS 발동 기능 테스트 ===${NC}"

# 1. 정상적인 SOS 발동 (실제 기능 테스트)
valid_sos_data='{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "emergencyType": "PANIC",
  "message": "도움이 필요합니다",
  "notifyAllContacts": true,
  "shareLocation": true
}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "200" "정상 SOS 발동 (200 성공)" "$valid_sos_data"

# 2. 최소한의 SOS 발동 (필수 필드만)
minimal_sos_data='{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "emergencyType": "PANIC"
}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "200" "최소 필드 SOS 발동 (200)" "$minimal_sos_data"

# 3. 잘못된 데이터 형식 (400 검증)
invalid_sos_data='{"latitude": "invalid", "longitude": "invalid", "emergencyType": "UNKNOWN"}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "400" "잘못된 SOS 데이터 형식 (400 검증 에러)" "$invalid_sos_data"

# 4. 필수 필드 누락 (400 검증)
incomplete_data='{"message": "도움 필요"}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "400" "필수 필드 누락 (400 검증 에러)" "$incomplete_data"

# 5. 잘못된 긴급 상황 타입
invalid_emergency_type='{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "emergencyType": "INVALID_TYPE"
}'
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "400" "잘못된 긴급 상황 타입 (400)" "$invalid_emergency_type"

# =========================
# 2. SOS 취소 기능 테스트 (PUT /api/v1/emergency/sos/{id}/cancel)
# =========================
echo "${YELLOW}=== 2. SOS 취소 기능 테스트 ===${NC}"

# 6. 정상적인 SOS 취소
test_endpoint "PUT" "/api/v1/emergency/sos/1/cancel" "200" "정상 SOS 취소 (200)"

# 7. 존재하지 않는 응급 ID
test_endpoint "PUT" "/api/v1/emergency/sos/999999/cancel" "404" "존재하지 않는 응급 ID (404)"

# 8. 잘못된 응급 ID 형식
test_endpoint "PUT" "/api/v1/emergency/sos/invalid/cancel" "400" "잘못된 응급 ID 형식 (400)"

# 9. 음수 응급 ID
test_endpoint "PUT" "/api/v1/emergency/sos/-1/cancel" "400" "음수 응급 ID (400)"

# =========================
# 3. SOS 이력 조회 기능 테스트 (GET /api/v1/emergency/sos/history)
# =========================
echo "${YELLOW}=== 3. SOS 이력 조회 기능 테스트 ===${NC}"

# 10. 정상적인 SOS 이력 조회
test_endpoint "GET" "/api/v1/emergency/sos/history" "200" "정상 SOS 이력 조회 (200)"

# 11. 페이징 파라미터를 포함한 이력 조회
test_endpoint "GET" "/api/v1/emergency/sos/history?page=0&size=10" "200" "페이징 파라미터 포함 SOS 이력 조회 (200)"

# 12. 잘못된 페이징 파라미터
test_endpoint "GET" "/api/v1/emergency/sos/history?page=-1&size=0" "400" "잘못된 페이징 파라미터 (400)"

# =========================
# 4. 빠른 SOS 기능 테스트 (POST /api/v1/emergency/sos/quick)
# =========================
echo "${YELLOW}=== 4. 빠른 SOS 기능 테스트 ===${NC}"

# 13. 정상적인 빠른 SOS
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665&longitude=126.9780" "200" "정상 빠른 SOS (200)"

# 14. 추가 파라미터를 포함한 빠른 SOS
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665&longitude=126.9780&message=긴급상황" "200" "파라미터 포함 빠른 SOS (200)"

# 15. 좌표 누락 (400 예상)
test_endpoint "POST" "/api/v1/emergency/sos/quick" "400" "좌표 누락 빠른 SOS (400)"

# 16. 일부 좌표 누락 (400 예상)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=37.5665" "400" "경도 누락 빠른 SOS (400)"

# 17. 잘못된 좌표 형식 (400 예상)
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=invalid&longitude=invalid" "400" "잘못된 좌표 형식 빠른 SOS (400)"

# =========================
# 5. HTTP 메서드 및 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 5. HTTP 메서드 및 엣지 케이스 테스트 ===${NC}"

# 18. 잘못된 HTTP 메서드 - GET으로 SOS 발동 시도
test_endpoint "GET" "/api/v1/emergency/sos/trigger" "405" "SOS 발동 - GET 메서드 (405 Method Not Allowed)"

# 19. 잘못된 HTTP 메서드 - DELETE로 SOS 발동 시도
test_endpoint "DELETE" "/api/v1/emergency/sos/trigger" "405" "SOS 발동 - DELETE 메서드 (405)"

# 20. 잘못된 HTTP 메서드 - POST로 SOS 취소 시도
test_endpoint "POST" "/api/v1/emergency/sos/1/cancel" "405" "SOS 취소 - POST 메서드 (405)"

# 21. 존재하지 않는 SOS 엔드포인트
test_endpoint "GET" "/api/v1/emergency/sos/nonexistent" "404" "존재하지 않는 SOS 엔드포인트 (404)"

# 22. 경계값 테스트 - 매우 큰 좌표값
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=999999&longitude=999999" "400" "매우 큰 좌표값 (400)"

# 23. 경계값 테스트 - 매우 작은 좌표값
test_endpoint "POST" "/api/v1/emergency/sos/quick?latitude=-999999&longitude=-999999" "400" "매우 작은 좌표값 (400)"

# 24. 빈 데이터로 SOS 발동 시도
test_endpoint "POST" "/api/v1/emergency/sos/trigger" "400" "빈 데이터 SOS 발동 (400)" ""

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
    printf "${YELLOW}📝 SOS API 기능이 정상적으로 작동합니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 일부 SOS API 기능에 문제가 있습니다${NC}\n"
    exit 1
fi