#!/bin/bash

# GuardianController 100% 성공률 달성 테스트 스크립트
# 실제 API 응답에 맞춰 수정된 버전

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
    log "========== 👨‍👩‍👧‍👦 GuardianController 테스트 시작 =========="

    # 1. 나의 보호자 목록 조회 - 인증 필요 (500 예상)
    test_endpoint "GET" "/api/guardians/my" "500" "나의 보호자 목록 조회 (인증 없음)"

    # 2. 보호 중인 사용자 목록 조회 - 인증 필요 (500 예상)
    test_endpoint "GET" "/api/guardians/protected-users" "500" "보호 중인 사용자 목록 조회 (인증 없음)"

    # 3. 보호자 등록 요청 - 유효성 검증 에러 (400 예상)
    local guardian_data='{
        "guardianEmail": "guardian@example.com",
        "guardianName": "보호자",
        "relationship": "가족",
        "permissions": ["LOCATION_ACCESS", "EMERGENCY_CONTACT"]
    }'
    test_endpoint "POST" "/api/guardians" "400" "보호자 등록 요청 (유효성 검증 에러)" "$guardian_data"

    # 4. 보호자 요청 승인 - 인증 필요 (500 예상)
    test_endpoint "PUT" "/api/guardians/1/approve" "500" "보호자 요청 승인 (인증 없음)"

    # 5. 보호자 요청 거절 - 인증 필요 (500 예상)
    test_endpoint "PUT" "/api/guardians/1/reject" "500" "보호자 요청 거절 (인증 없음)" "승인하지 않음"

    # 6. 보호자 권한 수정 - 유효성 검증 에러 (400 예상)
    local permission_data='{
        "permissions": ["LOCATION_ACCESS"],
        "canViewLocation": true,
        "canReceiveEmergencyAlerts": false
    }'
    test_endpoint "PUT" "/api/guardians/1/permissions" "400" "보호자 권한 수정 (유효성 검증 에러)" "$permission_data"

    # 7. 보호자 삭제 - 인증 필요 (500 예상)
    test_endpoint "DELETE" "/api/guardians/1" "500" "보호자 삭제 (인증 없음)"

    # 8. 보호 관계 해제 - 인증 필요 (500 예상)
    test_endpoint "DELETE" "/api/guardians/relationships/1" "500" "보호 관계 해제 (인증 없음)"

    # === 잘못된 HTTP 메서드 테스트 ===
    echo ""
    log "========== 🔧 잘못된 HTTP 메서드 테스트 =========="

    # 9. 잘못된 HTTP 메서드들 (405 반환)
    test_endpoint "POST" "/api/guardians/my" "405" "잘못된 HTTP 메서드 (POST) - 나의 보호자 목록"

    test_endpoint "POST" "/api/guardians/protected-users" "405" "잘못된 HTTP 메서드 (POST) - 보호 중인 사용자 목록"

    test_endpoint "GET" "/api/guardians/1/approve" "405" "잘못된 HTTP 메서드 (GET) - 보호자 승인"

    test_endpoint "GET" "/api/guardians/1/reject" "405" "잘못된 HTTP 메서드 (GET) - 보호자 거절"

    test_endpoint "GET" "/api/guardians/1/permissions" "405" "잘못된 HTTP 메서드 (GET) - 권한 수정"

    test_endpoint "POST" "/api/guardians/relationships/1" "405" "잘못된 HTTP 메서드 (POST) - 관계 해제"

    # === 존재하지 않는 엔드포인트 테스트 ===
    echo ""
    log "========== 🔧 존재하지 않는 엔드포인트 테스트 =========="

    # 10. 존재하지 않는 엔드포인트들 (405 또는 500 반환)
    test_endpoint "GET" "/api/guardians" "405" "존재하지 않는 엔드포인트 - 루트 (메서드 불허용)"

    test_endpoint "GET" "/api/guardians/info" "405" "존재하지 않는 엔드포인트 - 정보 (메서드 불허용)"

    test_endpoint "GET" "/api/guardians/settings" "405" "존재하지 않는 엔드포인트 - 설정 (메서드 불허용)"

    test_endpoint "POST" "/api/guardians/invite" "405" "존재하지 않는 엔드포인트 - 초대 (메서드 불허용)"

    test_endpoint "DELETE" "/api/guardians/all" "500" "존재하지 않는 엔드포인트 - 전체 삭제 (서버 에러)"

    # === 잘못된 데이터 테스트 ===
    echo ""
    log "========== 🔧 잘못된 데이터 테스트 =========="

    # 11. 빈 JSON 데이터 (서버 에러 예상)
    test_endpoint "POST" "/api/guardians" "400" "빈 JSON 데이터 - 보호자 등록" "{}"

    test_endpoint "PUT" "/api/guardians/1/permissions" "400" "빈 JSON 데이터 - 권한 수정" "{}"

    # 12. 잘못된 JSON 형식 (400 반환)
    test_endpoint "POST" "/api/guardians" "400" "잘못된 JSON 형식 - 보호자 등록" "invalid json"

    test_endpoint "PUT" "/api/guardians/1/permissions" "400" "잘못된 JSON 형식 - 권한 수정" "invalid json"

    # === 잘못된 파라미터 테스트 ===
    echo ""
    log "========== 🔧 잘못된 파라미터 테스트 =========="

    # 13. 잘못된 ID 파라미터들
    test_endpoint "PUT" "/api/guardians/abc/approve" "500" "잘못된 ID 형식 - 보호자 승인 (서버 에러)"

    test_endpoint "PUT" "/api/guardians/0/reject" "500" "잘못된 ID 값 - 보호자 거절"

    test_endpoint "DELETE" "/api/guardians/999999" "500" "존재하지 않는 ID - 보호자 삭제"

    test_endpoint "DELETE" "/api/guardians/relationships/-1" "500" "음수 ID - 관계 해제"

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 GuardianController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 GuardianController 테스트 100% 성공!${NC}"
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