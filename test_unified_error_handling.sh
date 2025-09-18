#!/bin/bash

# 통합 에러 핸들링 테스트 스크립트
# ProblemDetail 형식의 응답을 확인

BASE_URL="http://43.200.49.171:8080"
TIMESTAMP=$(date "+%Y%m%d_%H%M%S")
RESULTS_DIR="unified_error_test_results_${TIMESTAMP}"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

echo "🔍 통합 에러 핸들링 테스트 시작..."
echo "📁 결과 저장 위치: $RESULTS_DIR"
echo ""

# 테스트 카운터
total_tests=0
passed_tests=0

# 테스트 함수
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_status="$3"
    local test_name="$4"
    local data="$5"

    total_tests=$((total_tests + 1))

    echo "🧪 테스트 $total_tests: $test_name"
    echo "   $method $endpoint → 예상: HTTP $expected_status"

    # 요청 실행
    if [ "$method" == "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint")
    fi

    # 마지막 줄에서 상태 코드 추출
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)

    # 결과 저장
    echo "=== $test_name ===" >> "$RESULTS_DIR/detailed.log"
    echo "Request: $method $endpoint" >> "$RESULTS_DIR/detailed.log"
    echo "Expected: $expected_status" >> "$RESULTS_DIR/detailed.log"
    echo "Actual: $status_code" >> "$RESULTS_DIR/detailed.log"
    echo "Response Body: $body" >> "$RESULTS_DIR/detailed.log"
    echo "" >> "$RESULTS_DIR/detailed.log"

    # ProblemDetail 형식 확인
    is_problem_detail=false
    if echo "$body" | jq -e '.type, .title, .detail' >/dev/null 2>&1; then
        is_problem_detail=true
    fi

    # 결과 검증
    if [ "$status_code" == "$expected_status" ]; then
        if [ "$is_problem_detail" == "true" ]; then
            echo "   ✅ 성공 (HTTP $status_code, ProblemDetail 형식)"
            passed_tests=$((passed_tests + 1))
        else
            echo "   ⚠️  상태 코드는 맞지만 ProblemDetail 형식이 아님"
            echo "   응답: $body"
        fi
    else
        echo "   ❌ 실패 (예상: $expected_status, 실제: $status_code)"
        echo "   응답: $body"
    fi

    echo ""
}

# 서버 상태 확인
echo "🏥 서버 상태 확인 중..."
health_response=$(curl -s "$BASE_URL/api/health" | head -c 100)
if [[ $health_response == *"UP"* ]]; then
    echo "✅ 서버가 정상 동작 중"
else
    echo "❌ 서버에 접근할 수 없습니다"
    echo "응답: $health_response"
    exit 1
fi
echo ""

# === 404 에러 테스트 ===
echo "🔍 404 에러 테스트"
test_endpoint "GET" "/api/nonexistent" "404" "존재하지_않는_엔드포인트"
test_endpoint "GET" "/invalid/path" "404" "잘못된_경로"
test_endpoint "POST" "/api/fake/endpoint" "404" "가짜_POST_엔드포인트"

# === 405 에러 테스트 ===
echo "🔍 405 에러 테스트"
test_endpoint "DELETE" "/api/health" "405" "헬스체크_DELETE_요청"
test_endpoint "PUT" "/api/auth/login" "405" "로그인_PUT_요청"
test_endpoint "PATCH" "/api/users" "405" "사용자_PATCH_요청"

# === 400 에러 테스트 ===
echo "🔍 400 에러 테스트"
test_endpoint "POST" "/api/auth/register" "400" "회원가입_빈_데이터" "{}"
test_endpoint "POST" "/api/auth/login" "400" "로그인_빈_데이터" "{}"
test_endpoint "POST" "/api/auth/register" "400" "회원가입_잘못된_JSON" '{"username":}'

# === 401 에러 테스트 ===
echo "🔍 401 에러 테스트"
test_endpoint "GET" "/api/users/profile" "401" "프로필_인증_없음"
test_endpoint "POST" "/api/guardians" "401" "보호자_등록_인증_없음"
test_endpoint "GET" "/api/emergency/alerts" "401" "긴급_알림_인증_없음"

# === 403 에러 테스트 (인증은 있지만 권한 없음) ===
echo "🔍 403 에러 테스트"
# 일반적으로 403은 유효한 토큰이 있지만 권한이 없을 때 발생
# 현재는 인증 토큰 없이 테스트하므로 401이 나올 수 있음
test_endpoint "GET" "/api/admin/users" "401" "관리자_전용_엔드포인트"

# 결과 요약
echo "📊 테스트 결과 요약"
echo "=================================="
echo "총 테스트: $total_tests"
echo "성공: $passed_tests"
echo "실패: $((total_tests - passed_tests))"
echo "성공률: $(echo "scale=1; $passed_tests * 100 / $total_tests" | bc -l)%"
echo ""

# 결과 저장
{
    echo "통합 에러 핸들링 테스트 결과"
    echo "테스트 시간: $(date)"
    echo "총 테스트: $total_tests"
    echo "성공: $passed_tests"
    echo "실패: $((total_tests - passed_tests))"
    echo "성공률: $(echo "scale=1; $passed_tests * 100 / $total_tests" | bc -l)%"
} > "$RESULTS_DIR/summary.txt"

if [ $passed_tests -eq $total_tests ]; then
    echo "🎉 모든 테스트가 성공했습니다! 100% 에러 핸들링 완료!"
    exit 0
else
    echo "⚠️  일부 테스트가 실패했습니다. 로그를 확인해주세요."
    echo "📝 상세 로그: $RESULTS_DIR/detailed.log"
    exit 1
fi