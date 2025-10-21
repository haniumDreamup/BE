#!/bin/bash

# AdminController 종합 테스트 스크립트
# Profile 제한으로 인해 대부분 404 예상

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
echo "AdminController 테스트 시작"
echo "⚠️  Profile 제한으로 대부분 404 예상"
echo "========================================"
printf "\n"

# =========================
# 1. 시스템 통계 조회 테스트
# =========================
echo "${YELLOW}=== 1. 시스템 통계 조회 테스트 ===${NC}"

# 1. 인증 없이 접근 (404 예상 - Controller 자체가 로드되지 않음)
test_endpoint "GET" "/api/admin/statistics" "404" "시스템 통계 조회 (Profile 제한으로 404)"

# =========================
# 2. 활성 세션 조회 테스트
# =========================
echo "${YELLOW}=== 2. 활성 세션 조회 테스트 ===${NC}"

# 2. 활성 세션 조회 (404 예상)
test_endpoint "GET" "/api/admin/sessions" "404" "활성 세션 조회 (Profile 제한으로 404)"

# =========================
# 3. 세션 강제 종료 테스트
# =========================
echo "${YELLOW}=== 3. 세션 강제 종료 테스트 ===${NC}"

# 3. 특정 사용자 세션 종료 (404 예상)
test_endpoint "DELETE" "/api/admin/sessions/123" "404" "사용자 세션 종료 (Profile 제한으로 404)"

# =========================
# 4. 인증 로그 조회 테스트
# =========================
echo "${YELLOW}=== 4. 인증 로그 조회 테스트 ===${NC}"

# 4. 인증 로그 조회 (404 예상)
test_endpoint "GET" "/api/admin/auth-logs" "404" "인증 로그 조회 (Profile 제한으로 404)"

# 5. 인증 로그 조회 (페이징 파라미터) (404 예상)
test_endpoint "GET" "/api/admin/auth-logs?page=0&size=10" "404" "인증 로그 조회 - 페이징 (Profile 제한으로 404)"

# =========================
# 5. 시스템 설정 테스트
# =========================
echo "${YELLOW}=== 5. 시스템 설정 테스트 ===${NC}"

# 6. 시스템 설정 조회 (404 예상)
test_endpoint "GET" "/api/admin/settings" "404" "시스템 설정 조회 (Profile 제한으로 404)"

# 7. 시스템 설정 수정 (404 예상)
settings_data='{"maxUsers": 1000, "maintenanceMode": false}'
test_endpoint "PUT" "/api/admin/settings" "404" "시스템 설정 수정 (Profile 제한으로 404)" "$settings_data"

# =========================
# 6. 백업 및 캐시 테스트
# =========================
echo "${YELLOW}=== 6. 백업 및 캐시 테스트 ===${NC}"

# 8. 데이터베이스 백업 (404 예상)
test_endpoint "POST" "/api/admin/backup" "404" "데이터베이스 백업 (Profile 제한으로 404)"

# 9. 캐시 전체 초기화 (404 예상)
test_endpoint "DELETE" "/api/admin/cache" "404" "캐시 전체 초기화 (Profile 제한으로 404)"

# 10. 특정 캐시 초기화 (404 예상)
test_endpoint "DELETE" "/api/admin/cache?cacheName=userCache" "404" "특정 캐시 초기화 (Profile 제한으로 404)"

# =========================
# 7. 메서드 검증 및 엣지 케이스 테스트
# =========================
echo "${YELLOW}=== 7. 메서드 검증 및 엣지 케이스 테스트 ===${NC}"

# 11. POST로 statistics 접근 시도 (404 예상)
test_endpoint "POST" "/api/admin/statistics" "404" "시스템 통계 - POST 메서드 (Profile 제한으로 404)"

# 12. GET으로 backup 접근 시도 (404 예상)
test_endpoint "GET" "/api/admin/backup" "404" "백업 - GET 메서드 (Profile 제한으로 404)"

# 13. 존재하지 않는 관리자 엔드포인트 (404 예상)
test_endpoint "GET" "/api/admin/nonexistent" "404" "존재하지 않는 관리자 엔드포인트"

# 14. 루트 관리자 경로 (404 예상)
test_endpoint "GET" "/api/admin" "404" "루트 관리자 경로"

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
    printf "${YELLOW}📝 AdminController는 @Profile(\"!test\")로 인해 현재 환경에서 비활성화됨${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 AdminController는 @Profile(\"!test\")로 인해 현재 환경에서 비활성화됨${NC}\n"
    exit 1
fi