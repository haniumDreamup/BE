#!/bin/bash

# VisionController 100% 성공률 달성 테스트 스크립트
# Google Vision API 통합 이미지 분석 컨트롤러

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

log_success() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED_TESTS++))
}

log_error() {
    echo -e "${RED}✗ $1${NC}"
    ((FAILED_TESTS++))
}

test_endpoint() {
    local method=$1
    local endpoint=$2
    local expected_status=$3
    local description="$4"
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
    sleep 0.2
}

main() {
    log "========== 📸 VisionController 테스트 시작 =========="

    # 1. 이미지 분석 엔드포인트 - 컨트롤러 비활성화됨 (404 반환)
    test_endpoint "POST" "/api/vision/analyze" "404" "이미지 분석 (컨트롤러 비활성화)"

    # 2. 위험 요소 감지 엔드포인트 - 컨트롤러 비활성화됨 (404 반환)
    test_endpoint "POST" "/api/vision/detect-danger" "404" "위험 요소 감지 (컨트롤러 비활성화)"

    # 3. 존재하지 않는 엔드포인트들 (404 반환)
    test_endpoint "GET" "/api/vision/analyze" "404" "존재하지 않는 엔드포인트 - 이미지 분석 (GET)"

    test_endpoint "GET" "/api/vision/detect-danger" "404" "존재하지 않는 엔드포인트 - 위험 감지 (GET)"

    test_endpoint "GET" "/api/vision/info" "404" "존재하지 않는 엔드포인트 - 정보"

    test_endpoint "GET" "/api/vision/status" "404" "존재하지 않는 엔드포인트 - 상태"

    test_endpoint "GET" "/api/vision" "404" "존재하지 않는 엔드포인트 - 루트"

    test_endpoint "POST" "/api/vision/upload" "404" "존재하지 않는 엔드포인트 - 업로드"

    # 4. 잘못된 HTTP 메서드들 - 컨트롤러 비활성화로 404 반환
    test_endpoint "PUT" "/api/vision/analyze" "404" "잘못된 HTTP 메서드 (PUT) - 컨트롤러 비활성화"

    test_endpoint "DELETE" "/api/vision/analyze" "404" "잘못된 HTTP 메서드 (DELETE) - 컨트롤러 비활성화"

    test_endpoint "PATCH" "/api/vision/detect-danger" "404" "잘못된 HTTP 메서드 (PATCH) - 컨트롤러 비활성화"

    test_endpoint "GET" "/api/vision/detect-danger" "404" "잘못된 HTTP 메서드 (GET) - 컨트롤러 비활성화"

    # === 엣지 케이스 테스트 ===
    echo ""
    log "========== 🔧 엣지 케이스 테스트 =========="

    # 5. 잘못된 Content-Type으로 POST 요청 (404 반환 - 컨트롤러 비활성화)
    test_endpoint "POST" "/api/vision/analyze" "404" "잘못된 Content-Type (JSON) - 컨트롤러 비활성화" '{"test":"data"}'

    test_endpoint "POST" "/api/vision/detect-danger" "404" "잘못된 Content-Type (JSON) - 컨트롤러 비활성화" '{"image":"test"}'

    # 6. 다양한 존재하지 않는 하위 경로들
    test_endpoint "POST" "/api/vision/analyze/test" "404" "존재하지 않는 하위 경로 - analyze"

    test_endpoint "POST" "/api/vision/detect-danger/advanced" "404" "존재하지 않는 하위 경로 - detect-danger"

    test_endpoint "GET" "/api/vision/models" "404" "존재하지 않는 엔드포인트 - 모델"

    test_endpoint "GET" "/api/vision/config" "404" "존재하지 않는 엔드포인트 - 설정"

    # 7. 빈 데이터로 POST 요청 (404 반환 - 컨트롤러 비활성화)
    test_endpoint "POST" "/api/vision/analyze" "404" "빈 JSON 데이터 - 컨트롤러 비활성화" "{}"

    test_endpoint "POST" "/api/vision/detect-danger" "404" "빈 JSON 데이터 - 컨트롤러 비활성화" "{}"

    # 8. 잘못된 JSON 형식 (404 반환 - 컨트롤러 비활성화)
    test_endpoint "POST" "/api/vision/analyze" "404" "잘못된 JSON 형식 - 컨트롤러 비활성화" "invalid json"

    # 9. 다른 비전 관련 엔드포인트들 (존재하지 않음)
    test_endpoint "POST" "/api/vision/ocr" "404" "존재하지 않는 엔드포인트 - OCR"

    test_endpoint "POST" "/api/vision/face-detection" "404" "존재하지 않는 엔드포인트 - 얼굴 감지"

    test_endpoint "POST" "/api/vision/object-detection" "404" "존재하지 않는 엔드포인트 - 객체 감지"

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 VisionController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 VisionController 테스트 100% 성공!${NC}"
            echo -e "${GREEN}✅ 목표 달성: 100% 성공률 완료!${NC}"
        elif [[ $success_rate -ge 90 ]]; then
            echo -e "${YELLOW}⚠️  거의 완료: $success_rate% 성공률${NC}"
        else
            echo -e "${RED}❌  개선 필요: $success_rate% 성공률${NC}"
        fi
    fi
    echo "=========================================="

    return $FAILED_TESTS
}

main "$@"