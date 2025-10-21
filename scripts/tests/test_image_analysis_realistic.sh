#!/bin/bash

# ImageAnalysisController 현실적 테스트 스크립트
# Spring Security 및 Multipart 처리를 고려한 실용적 테스트

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
        if [ ${#response_body} -lt 200 ]; then
            printf "   응답 본문: $response_body\n"
        else
            printf "   응답 본문: ${response_body:0:200}...\n"
        fi
    fi

    printf "\n"
}

echo "=============================================="
echo "ImageAnalysisController 현실적 테스트"
echo "Spring Security 및 Multipart 기반 실용적 검증"
echo "=============================================="
printf "\n"

# =========================
# 1. Spring Security 인증 테스트 (401 응답)
# =========================
echo "${YELLOW}=== 1. Spring Security 인증 테스트 ===${NC}"

# 1. POST /analyze - 인증 필요 (TestSecurityConfig에서 .authenticated() 설정)
test_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 인증 없음 (401 by Security)"

# 2. POST /quick-analyze - 인증 필요
test_endpoint "POST" "/api/images/quick-analyze" "401" "빠른 분석 - 인증 없음 (401 by Security)"

# 3. GET /analysis/{id} - 인증 필요
test_endpoint "GET" "/api/images/analysis/1" "401" "분석 결과 조회 - 인증 없음 (401 by Security)"

# =========================
# 2. 경로 매개변수 검증 (Spring이 처리)
# =========================
echo "${YELLOW}=== 2. 경로 매개변수 검증 테스트 ===${NC}"

# 4. 잘못된 analysisId 형식 - Spring이 400으로 처리
test_endpoint "GET" "/api/images/analysis/invalid" "400" "분석 결과 - 잘못된 ID 형식 (400 by Spring)"

# 5. 쿼리 파라미터 오류 - Spring이 400으로 처리
test_endpoint "POST" "/api/images/quick-analyze?latitude=invalid&longitude=invalid" "400" "빠른 분석 - 잘못된 좌표 (400 by Spring)"

# =========================
# 3. HTTP 메서드 검증 (Spring이 처리)
# =========================
echo "${YELLOW}=== 3. HTTP 메서드 검증 ===${NC}"

# 6. 잘못된 HTTP 메서드 - GET으로 POST 엔드포인트 호출
test_endpoint "GET" "/api/images/analyze" "405" "이미지 분석 - GET 메서드 (405 Method Not Allowed)"

# 7. 잘못된 HTTP 메서드 - DELETE으로 POST 엔드포인트 호출
test_endpoint "DELETE" "/api/images/analyze" "405" "이미지 분석 - DELETE 메서드 (405 Method Not Allowed)"

# 8. 잘못된 HTTP 메서드 - POST로 GET 엔드포인트 호출
test_endpoint "POST" "/api/images/analysis/1" "405" "분석 결과 - POST 메서드 (405 Method Not Allowed)"

# 9. 잘못된 HTTP 메서드 - PUT으로 POST 엔드포인트 호출
test_endpoint "PUT" "/api/images/quick-analyze" "405" "빠른 분석 - PUT 메서드 (405 Method Not Allowed)"

# =========================
# 4. 엔드포인트 존재 검증 (Spring이 처리)
# =========================
echo "${YELLOW}=== 4. 엔드포인트 존재 검증 ===${NC}"

# 10. 존재하지 않는 엔드포인트
test_endpoint "GET" "/api/images/nonexistent" "404" "존재하지 않는 엔드포인트 (404 Not Found)"

# 11. 루트 경로 - 매핑되지 않은 경로
test_endpoint "GET" "/api/images" "404" "루트 Images 경로 (404 Not Found)"

# 12. 잘못된 하위 경로
test_endpoint "GET" "/api/images/invalid/path" "404" "잘못된 하위 경로 (404 Not Found)"

# 13. 매개변수 없는 analysis 경로
test_endpoint "GET" "/api/images/analysis" "404" "매개변수 없는 analysis (404 Not Found)"

# =========================
# 5. Content-Type 검증 (Multipart 관련)
# =========================
echo "${YELLOW}=== 5. Content-Type 및 Multipart 검증 ===${NC}"

# 14. 잘못된 Content-Type - JSON으로 multipart 엔드포인트 호출
test_endpoint "POST" "/api/images/analyze" "415" "이미지 분석 - 잘못된 Content-Type (415 Unsupported Media Type)" '{}' "Content-Type: application/json"

# 15. Content-Type 누락 - multipart가 필요한 엔드포인트
test_endpoint "POST" "/api/images/analyze" "400" "이미지 분석 - Content-Type 누락 (400 Bad Request)" '' ""

# 16. 빠른 분석 - 잘못된 Content-Type
test_endpoint "POST" "/api/images/quick-analyze" "415" "빠른 분석 - 잘못된 Content-Type (415 Unsupported Media Type)" '{}' "Content-Type: application/json"

# =========================
# 6. 경계 케이스 테스트
# =========================
echo "${YELLOW}=== 6. 경계 케이스 테스트 ===${NC}"

# 17. 매우 큰 analysisId
test_endpoint "GET" "/api/images/analysis/999999999" "401" "분석 결과 - 큰 ID (401 인증 우선)"

# 18. 음수 analysisId
test_endpoint "GET" "/api/images/analysis/-1" "401" "분석 결과 - 음수 ID (401 인증 우선)"

# 19. 0 analysisId
test_endpoint "GET" "/api/images/analysis/0" "401" "분석 결과 - 0 ID (401 인증 우선)"

# =========================
# 7. URL 인코딩 및 특수문자 테스트
# =========================
echo "${YELLOW}=== 7. URL 인코딩 및 특수문자 테스트 ===${NC}"

# 20. 특수문자가 포함된 경로
test_endpoint "GET" "/api/images/analysis/test%20id" "400" "분석 결과 - 특수문자 ID (400 Bad Request)"

# =========================
# 결과 요약
# =========================
echo "=============================================="
echo "           테스트 결과 요약"
echo "=============================================="
printf "총 테스트: ${BLUE}$TOTAL_TESTS${NC}\n"
printf "통과: ${GREEN}$PASSED_TESTS${NC}\n"
printf "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}\n"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    printf "\n${GREEN}🎉 모든 테스트 통과! (100%% 성공률)${NC}\n"
    printf "${YELLOW}📝 ImageAnalysisController는 Spring Security와 올바르게 통합되었습니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 일부 테스트가 예상과 다른 결과를 보입니다${NC}\n"
    exit 1
fi