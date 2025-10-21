#!/bin/bash

# ImageAnalysisController 종합 테스트 스크립트
# 3개 웨어러블 카메라 이미지 분석 엔드포인트 테스트

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 이미지 파일 생성 (1x1 픽셀 PNG)
create_test_image() {
    local filename=$1
    # Base64로 인코딩된 1x1 PNG 이미지 생성
    echo "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==" | base64 -d > "$filename"
}

# 테스트 함수 (일반 JSON)
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

# multipart 테스트 함수
test_multipart_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description=$4
    local image_file=$5
    local additional_params=$6

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    printf "${BLUE}테스트 $TOTAL_TESTS: $description${NC}\n"

    if [ "$method" = "POST" ] && [ -n "$image_file" ]; then
        if [ -f "$image_file" ]; then
            response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
                -F "image=@$image_file" $additional_params)
        else
            response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
                $additional_params)
        fi
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint")
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
echo "ImageAnalysisController 테스트 시작"
echo "3개 웨어러블 카메라 이미지 분석 엔드포인트 테스트"
echo "========================================"
printf "\n"

# 테스트 이미지 파일 생성
create_test_image "test_image.png"

# =========================
# 1. 이미지 분석 테스트 (POST /api/images/analyze)
# =========================
echo "${YELLOW}=== 1. 이미지 분석 테스트 ===${NC}"

# 1. 인증 없이 이미지 분석 (401 예상)
test_multipart_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 인증 없음 (401)" "test_image.png"

# 2. 이미지 파일 없이 분석 요청 (401 예상 - 인증 우선)
test_multipart_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 파일 없음 (401 인증 우선)" ""

# 3. 잘못된 콘텐츠 타입 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 잘못된 콘텐츠 타입 (401)" '{"test": "data"}'

# =========================
# 2. 빠른 이미지 분석 테스트 (POST /api/images/quick-analyze)
# =========================
echo "${YELLOW}=== 2. 빠른 이미지 분석 테스트 ===${NC}"

# 4. 인증 없이 빠른 분석 (401 예상)
test_multipart_endpoint "POST" "/api/images/quick-analyze" "401" "빠른 분석 - 인증 없음 (401)" "test_image.png"

# 5. 위치 정보와 함께 빠른 분석 (401 예상 - 인증 우선)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=37.123&longitude=127.456" "401" "빠른 분석 - 위치 정보 포함 (401 인증 우선)" "test_image.png"

# 6. 잘못된 위치 좌표 (401 예상 - 인증 우선)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=invalid&longitude=invalid" "401" "빠른 분석 - 잘못된 좌표 (401 인증 우선)" "test_image.png"

# =========================
# 3. 분석 결과 조회 테스트 (GET /api/images/analysis/{analysisId})
# =========================
echo "${YELLOW}=== 3. 분석 결과 조회 테스트 ===${NC}"

# 7. 인증 없이 분석 결과 조회 (401 예상)
test_endpoint "GET" "/api/images/analysis/1" "401" "분석 결과 조회 - 인증 없음 (401)"

# 8. 잘못된 분석 ID 형식 (401 예상 - 인증 우선)
test_endpoint "GET" "/api/images/analysis/invalid" "401" "분석 결과 조회 - 잘못된 ID 형식 (401 인증 우선)"

# 9. 음수 분석 ID (401 예상 - 인증 우선)
test_endpoint "GET" "/api/images/analysis/-1" "401" "분석 결과 조회 - 음수 ID (401 인증 우선)"

# 10. 존재하지 않는 분석 ID (401 예상 - 인증 우선)
test_endpoint "GET" "/api/images/analysis/999999" "401" "분석 결과 조회 - 존재하지 않는 ID (401 인증 우선)"

# =========================
# 4. HTTP 메서드 검증 테스트
# =========================
echo "${YELLOW}=== 4. HTTP 메서드 검증 테스트 ===${NC}"

# 11. 잘못된 HTTP 메서드 - GET으로 이미지 분석 시도 (401 또는 405 예상)
test_endpoint "GET" "/api/images/analyze" "401" "이미지 분석 - GET 메서드 (401)"

# 12. 잘못된 HTTP 메서드 - DELETE로 이미지 분석 시도 (401 또는 405 예상)
test_endpoint "DELETE" "/api/images/analyze" "401" "이미지 분석 - DELETE 메서드 (401)"

# 13. 잘못된 HTTP 메서드 - POST로 분석 결과 조회 시도 (401 또는 405 예상)
test_endpoint "POST" "/api/images/analysis/1" "401" "분석 결과 조회 - POST 메서드 (401)"

# 14. 잘못된 HTTP 메서드 - PUT으로 빠른 분석 시도 (401 또는 405 예상)
test_endpoint "PUT" "/api/images/quick-analyze" "401" "빠른 분석 - PUT 메서드 (401)"

# =========================
# 5. 엣지 케이스 및 보안 테스트
# =========================
echo "${YELLOW}=== 5. 엣지 케이스 및 보안 테스트 ===${NC}"

# 15. 존재하지 않는 엔드포인트 (401 또는 404 예상)
test_endpoint "GET" "/api/images/nonexistent" "404" "존재하지 않는 이미지 엔드포인트 (404)"

# 16. 루트 이미지 경로 (401 또는 404 예상)
test_endpoint "GET" "/api/images" "404" "루트 이미지 경로 (404)"

# 17. 빈 데이터로 이미지 분석 시도 (401 예상 - 인증 우선)
test_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 빈 데이터 (401 인증 우선)" ""

# 18. Content-Type 없이 요청 (401 예상 - 인증 우선)
test_multipart_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - Content-Type 없음 (401 인증 우선)" ""

# =========================
# 6. 파일 크기 및 형식 테스트
# =========================
echo "${YELLOW}=== 6. 파일 크기 및 형식 테스트 ===${NC}"

# 19. 빈 파일로 분석 시도 (401 예상 - 인증 우선)
touch empty_file.txt
test_multipart_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 빈 파일 (401 인증 우선)" "empty_file.txt"

# 20. 텍스트 파일로 분석 시도 (401 예상 - 인증 우선)
echo "not an image" > text_file.txt
test_multipart_endpoint "POST" "/api/images/analyze" "401" "이미지 분석 - 텍스트 파일 (401 인증 우선)" "text_file.txt"

# 정리
rm -f test_image.png empty_file.txt text_file.txt

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
    printf "${YELLOW}📝 Image Analysis API는 웨어러블 카메라를 위한 중요한 AI 분석 API입니다${NC}\n"
    exit 0
else
    success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    printf "\n${YELLOW}⚠️  성공률: $success_rate%%${NC}\n"
    printf "${YELLOW}📝 Image Analysis API는 웨어러블 카메라를 위한 중요한 AI 분석 API입니다${NC}\n"
    exit 1
fi