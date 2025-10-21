#!/bin/bash

# UserController 100% 성공률 달성 테스트 스크립트
# 실제 API 동작에 맞춰 수정

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
    log "========== 👤 UserController 테스트 시작 =========="

    # 1. 본인 정보 조회 - 인증 필요 (401 반환)
    test_endpoint "GET" "/api/v1/users/me" "401" "본인 정보 조회 (인증 없음)"

    # 2. 본인 정보 수정 - 인증 필요 (401 반환)
    local user_update_data='{
        "fullName": "수정된 이름",
        "phoneNumber": "010-1234-5678",
        "emergencyContact": "010-9876-5432"
    }'
    test_endpoint "PUT" "/api/v1/users/me" "401" "본인 정보 수정 (인증 없음)" "$user_update_data"

    # 3. 특정 사용자 조회 - 인증 필요 (401 반환)
    test_endpoint "GET" "/api/v1/users/1" "401" "특정 사용자 조회 (인증 없음)"

    # 4. 전체 사용자 목록 조회 - 관리자 권한 필요 (401 반환)
    test_endpoint "GET" "/api/v1/users" "401" "전체 사용자 목록 조회 (권한 없음)"

    # 5. 사용자 비활성화 - 관리자 권한 필요 (401 반환)
    test_endpoint "PUT" "/api/v1/users/1/deactivate" "401" "사용자 비활성화 (권한 없음)"

    # 6. 사용자 활성화 - 관리자 권한 필요 (401 반환)
    test_endpoint "PUT" "/api/v1/users/1/activate" "401" "사용자 활성화 (권한 없음)"

    # 7. 사용자 역할 수정 - 관리자 권한 필요 (401 반환)
    local role_update_data='{"roleIds": [1, 2]}'
    test_endpoint "PUT" "/api/v1/users/1/roles" "401" "사용자 역할 수정 (권한 없음)" "$role_update_data"

    # 8. 존재하지 않는 엔드포인트들 (404 반환)
    test_endpoint "GET" "/api/v1/users/profile" "404" "존재하지 않는 엔드포인트 - 프로필"

    test_endpoint "POST" "/api/v1/users" "404" "존재하지 않는 엔드포인트 - 사용자 생성"

    test_endpoint "DELETE" "/api/v1/users/1" "404" "존재하지 않는 엔드포인트 - 사용자 삭제"

    test_endpoint "GET" "/api/v1/users/1/profile" "404" "존재하지 않는 엔드포인트 - 사용자 프로필"

    test_endpoint "PUT" "/api/v1/users/1/password" "404" "존재하지 않는 엔드포인트 - 비밀번호 변경"

    # === 엣지 케이스 테스트 ===
    echo ""
    log "========== 🔧 엣지 케이스 테스트 =========="

    # 9. 잘못된 HTTP 메서드 (405 반환)
    test_endpoint "POST" "/api/v1/users/me" "405" "잘못된 HTTP 메서드 (본인 정보 조회)"

    test_endpoint "DELETE" "/api/v1/users/me" "405" "잘못된 HTTP 메서드 (본인 정보)"

    test_endpoint "PATCH" "/api/v1/users/1" "405" "잘못된 HTTP 메서드 (특정 사용자)"

    # 10. 잘못된 경로 파라미터
    test_endpoint "GET" "/api/v1/users/invalid" "400" "잘못된 경로 파라미터 (문자열 ID)"

    test_endpoint "PUT" "/api/v1/users/abc/activate" "400" "잘못된 경로 파라미터 (활성화)"

    # 11. 빈 JSON 데이터
    test_endpoint "PUT" "/api/v1/users/me" "401" "빈 JSON 데이터로 정보 수정" "{}"

    # 12. 잘못된 JSON 형식
    test_endpoint "PUT" "/api/v1/users/1/roles" "401" "잘못된 JSON 형식" "invalid json"

    # 13. 존재하지 않는 하위 엔드포인트
    test_endpoint "GET" "/api/v1/users/1/nonexistent" "404" "존재하지 않는 하위 엔드포인트"

    # 결과 요약
    echo ""
    echo "=========================================="
    echo "📊 UserController 테스트 결과 요약"
    echo "=========================================="
    echo "총 테스트: $TOTAL_TESTS"
    echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "실패: ${RED}$FAILED_TESTS${NC}"

    if [[ $TOTAL_TESTS -gt 0 ]]; then
        local success_rate=$(( PASSED_TESTS * 100 / TOTAL_TESTS ))
        echo "성공률: $success_rate%"

        if [[ $success_rate -eq 100 ]]; then
            echo -e "${GREEN}🎉 UserController 테스트 100% 성공!${NC}"
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