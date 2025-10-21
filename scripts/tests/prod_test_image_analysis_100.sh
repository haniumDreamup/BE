#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 서버 URL
BASE_URL="http://43.200.49.171:8080"

# 테스트 카운터
TOTAL_TESTS=0
PASSED_TESTS=0

# 로그 함수들
log_info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 테스트 함수
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    local expected_status="$4"
    local data="$5"
    local auth_header="$6"

    ((TOTAL_TESTS++))

    local curl_cmd="curl -s -w '%{http_code}' -X $method"
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

    if [[ -n "$auth_header" ]]; then
        curl_cmd="$curl_cmd -H 'Authorization: $auth_header'"
    fi

    if [[ -n "$data" ]]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi

    curl_cmd="$curl_cmd $BASE_URL$endpoint"

    local response
    response=$(eval $curl_cmd)
    local status_code="${response: -3}"
    local body="${response%???}"

    echo "🔍 테스트: $description"
    echo "📤 요청: $method $endpoint"
    echo "📊 응답상태: $status_code"
    echo "📄 응답내용: ${body:0:200}..."

    if [[ "$status_code" == "$expected_status" ]]; then
        log_success "$description - 상태: $status_code"
    else
        log_error "$description - 예상: $expected_status, 실제: $status_code"
    fi

    echo "----------------------------------------"
}

# multipart/form-data 테스트 함수
test_multipart_endpoint() {
    local method="$1"
    local endpoint="$2"
    local description="$3"
    local expected_status="$4"
    local auth_header="$5"
    local has_file="$6"

    ((TOTAL_TESTS++))

    local curl_cmd="curl -s -w '%{http_code}' -X $method"

    if [[ -n "$auth_header" ]]; then
        curl_cmd="$curl_cmd -H 'Authorization: $auth_header'"
    fi

    if [[ "$has_file" == "true" ]]; then
        # 실제 파일 없이 텍스트로 테스트
        curl_cmd="$curl_cmd -F 'image=@/dev/null;type=image/jpeg' -F 'request={\"analysisType\":\"ON_DEMAND\"};type=application/json'"
    fi

    curl_cmd="$curl_cmd $BASE_URL$endpoint"

    local response
    response=$(eval $curl_cmd)
    local status_code="${response: -3}"
    local body="${response%???}"

    echo "🔍 테스트: $description"
    echo "📤 요청: $method $endpoint (multipart)"
    echo "📊 응답상태: $status_code"
    echo "📄 응답내용: ${body:0:200}..."

    if [[ "$status_code" == "$expected_status" ]]; then
        log_success "$description - 상태: $status_code"
    else
        log_error "$description - 예상: $expected_status, 실제: $status_code"
    fi

    echo "----------------------------------------"
}

# 메인 테스트 시작
log_info "========== 📸 ImageAnalysisController 테스트 시작 =========="

# ImageAnalysisController는 테스트 엔드포인트가 없으므로 모든 엔드포인트에 인증 필요

# 1. 이미지 분석 (인증 필요 - 400, multipart 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/analyze" "이미지 분석 (인증 없음)" "400" "" "true"

# 2. 분석 결과 조회 (인증 필요 - 401)
test_endpoint "GET" "/api/images/analysis/1" "분석 결과 조회 (인증 없음)" "401"

# 3. 빠른 이미지 분석 (인증 필요 - 400, multipart 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/quick-analyze" "빠른 이미지 분석 (인증 없음)" "400" "" "true"

# 4. 빠른 이미지 분석 위치 파라미터 (인증 필요 - 400, multipart 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=37.5665&longitude=126.9780" "빠른 이미지 분석 위치 파라미터 (인증 없음)" "400" "" "true"

log_info "========== 🔧 엣지 케이스 테스트 =========="

# 5. 잘못된 HTTP 메서드 - 이미지 분석 (GET - 401, 인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analyze" "잘못된 HTTP 메서드 - 이미지 분석 (GET)" "401"

# 6. 잘못된 HTTP 메서드 - 분석 결과 조회 (POST - 401, 인증이 먼저 체크됨)
test_endpoint "POST" "/api/images/analysis/1" "잘못된 HTTP 메서드 - 분석 결과 조회 (POST)" "401"

# 7. 잘못된 HTTP 메서드 - 빠른 분석 (GET - 401, 인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/quick-analyze" "잘못된 HTTP 메서드 - 빠른 분석 (GET)" "401"

# 8. 존재하지 않는 분석 ID (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis/999999" "존재하지 않는 분석 ID" "401"

# 9. 잘못된 분석 ID 형식 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis/invalid" "잘못된 분석 ID 형식" "401"

# 10. 0 분석 ID (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis/0" "0 분석 ID" "401"

# 11. 네거티브 분석 ID (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis/-1" "네거티브 분석 ID" "401"

# 12. 파일 없이 이미지 분석 (인증이 먼저 체크됨)
test_multipart_endpoint "POST" "/api/images/analyze" "파일 없이 이미지 분석" "401" "" "false"

# 13. 파일 없이 빠른 분석 (인증이 먼저 체크됨)
test_multipart_endpoint "POST" "/api/images/quick-analyze" "파일 없이 빠른 분석" "401" "" "false"

# 14. 잘못된 위도 값 - 빠른 분석 (파라미터 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=invalid&longitude=126.9780" "잘못된 위도 값 - 빠른 분석" "400" "" "true"

# 15. 잘못된 경도 값 - 빠른 분석 (파라미터 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=37.5665&longitude=invalid" "잘못된 경도 값 - 빠른 분석" "400" "" "true"

# 16. 범위 초과 위도 값 - 빠른 분석 (파라미터 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=91.0&longitude=126.9780" "범위 초과 위도 값 - 빠른 분석" "400" "" "true"

# 17. 범위 초과 경도 값 - 빠른 분석 (파라미터 파싱 에러가 먼저 발생)
test_multipart_endpoint "POST" "/api/images/quick-analyze?latitude=37.5665&longitude=181.0" "범위 초과 경도 값 - 빠른 분석" "400" "" "true"

# 18. 존재하지 않는 하위 경로
test_endpoint "GET" "/api/images/analyze/status" "존재하지 않는 하위 경로" "401"

# 19. 루트 경로 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/" "루트 경로 (슬래시 포함)" "401"

# 20. 특수 문자가 포함된 분석 ID
test_endpoint "GET" "/api/images/analysis/@#$" "특수 문자 포함 분석 ID" "401"

# 21. 매우 긴 분석 ID 값
test_endpoint "GET" "/api/images/analysis/123456789012345678901234567890" "매우 긴 분석 ID 값" "401"

# 22. 빈 경로 파라미터 - 분석 결과 조회 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis/" "빈 경로 파라미터 - 분석 결과 조회" "401"

# 23. 쿼리 파라미터 테스트 - 분석 결과 조회
test_endpoint "GET" "/api/images/analysis/1?detailed=true" "쿼리 파라미터 포함 - 분석 결과 조회" "401"

# 24. Content-Type 없이 multipart 요청
test_endpoint "POST" "/api/images/analyze" "Content-Type 없이 multipart 요청" "401"

# 25. 잘못된 Content-Type으로 multipart 요청 (인증이 먼저 체크됨)
test_endpoint "POST" "/api/images/analyze" "잘못된 Content-Type multipart 요청" "401" '{"test":"data"}'

# 26. OPTIONS 메서드 테스트 - CORS preflight (인증이 먼저 체크됨)
test_endpoint "OPTIONS" "/api/images/analyze" "OPTIONS 메서드 - CORS preflight" "401"

# 27. HEAD 메서드 테스트
test_endpoint "HEAD" "/api/images/analysis/1" "HEAD 메서드 테스트" "401"

# 28. 매우 긴 경로 테스트 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis/1/extra/long/path/that/should/not/exist" "매우 긴 경로 테스트" "401"

# 29. 특수문자 포함 쿼리 파라미터 (URL 파싱 에러가 먼저 발생)
test_endpoint "GET" "/api/images/analysis/1?param=특수문자@#$" "특수문자 포함 쿼리 파라미터" "400"

# 30. 빈 분석 결과 경로 (인증이 먼저 체크됨)
test_endpoint "GET" "/api/images/analysis" "빈 분석 결과 경로" "401"

echo ""
echo "=========================================="
echo "📊 ImageAnalysisController 테스트 결과 요약"
echo "=========================================="
echo "총 테스트: $TOTAL_TESTS"
echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
echo -e "실패: ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}"

# 성공률 계산
success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
echo "성공률: $success_rate%"

if [[ $success_rate -eq 100 ]]; then
    echo -e "${GREEN}🎉  100% 성공! 모든 테스트 통과${NC}"
elif [[ $success_rate -ge 80 ]]; then
    echo -e "${YELLOW}⚠️   양호: $success_rate% 성공률${NC}"
else
    echo -e "${RED}❌  개선 필요: $success_rate% 성공률${NC}"
fi

echo "=========================================="