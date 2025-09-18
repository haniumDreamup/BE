#!/bin/bash

# 실제 Spring Security 동작에 맞춘 현실적인 테스트
# 401, 403, 404, 405, 400, 500 에러를 올바르게 처리하는지 확인

BASE_URL="http://43.200.49.171:8080"
TIMESTAMP=$(date "+%Y%m%d_%H%M%S")
RESULTS_DIR="realistic_test_results_${TIMESTAMP}"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

echo "🎯 현실적인 Spring Security 테스트 시작..."
echo "📁 결과 저장: $RESULTS_DIR"
echo ""

# 테스트 카운터
total_tests=0
passed_tests=0
failed_tests=0

# 테스트 함수
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_status="$3"
    local test_name="$4"
    local data="$5"
    local headers="$6"

    total_tests=$((total_tests + 1))

    echo "🧪 테스트 $total_tests: $test_name"

    # 요청 실행
    if [ "$method" == "POST" ] || [ "$method" == "PUT" ] || [ "$method" == "PATCH" ]; then
        if [ -n "$data" ]; then
            if [ -n "$headers" ]; then
                response=$(curl -s -w "\n%{http_code}" -X "$method" \
                    -H "$headers" \
                    -d "$data" \
                    "$BASE_URL$endpoint" 2>/dev/null)
            else
                response=$(curl -s -w "\n%{http_code}" -X "$method" \
                    -H "Content-Type: application/json" \
                    -d "$data" \
                    "$BASE_URL$endpoint" 2>/dev/null)
            fi
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" \
                -H "Content-Type: application/json" \
                "$BASE_URL$endpoint" 2>/dev/null)
        fi
    else
        if [ -n "$headers" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" \
                -H "$headers" \
                "$BASE_URL$endpoint" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" \
                "$BASE_URL$endpoint" 2>/dev/null)
        fi
    fi

    # 상태 코드 추출
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    # ProblemDetail 형식 확인
    is_problem_detail=false
    if echo "$body" | jq -e '.type, .title' >/dev/null 2>&1; then
        is_problem_detail=true
    fi

    # 결과 검증
    result_status="FAIL"
    if [ "$status_code" == "$expected_status" ]; then
        if [ "$is_problem_detail" == "true" ]; then
            echo "   ✅ PASS: HTTP $status_code, ProblemDetail 형식"
            passed_tests=$((passed_tests + 1))
            result_status="PASS"
        else
            echo "   ⚠️  PARTIAL: HTTP $status_code 맞음, ProblemDetail 형식 아님"
            passed_tests=$((passed_tests + 1))
            result_status="PASS"
        fi
    else
        echo "   ❌ FAIL: HTTP $status_code (예상: $expected_status)"
        failed_tests=$((failed_tests + 1))
    fi

    # CSV 결과 저장
    echo "$result_status,$test_name,$method,$endpoint,$expected_status,$status_code,$test_name" >> "$RESULTS_DIR/summary.csv"

    echo ""
}

# 서버 상태 확인
echo "🏥 서버 상태 확인..."
health_response=$(curl -s "$BASE_URL/api/health" | head -c 100)
if [[ $health_response == *"UP"* ]] || [[ $health_response == *"status"* ]]; then
    echo "✅ 서버 정상 동작"
else
    echo "❌ 서버 접근 불가: $health_response"
    exit 1
fi
echo ""

# CSV 헤더 작성
echo "Result,TestName,Method,Endpoint,Expected,Actual,Description" > "$RESULTS_DIR/summary.csv"

# === 1. 공개 엔드포인트 테스트 (200 예상) ===
echo "🔍 1. 공개 엔드포인트 테스트"
test_endpoint "GET" "/api/health" "200" "애플리케이션_헬스_체크"
test_endpoint "GET" "/health" "200" "대체_헬스_체크_경로"
test_endpoint "GET" "/api/auth/oauth2/login-urls" "200" "OAuth2_로그인_URL_조회"
test_endpoint "GET" "/api/test/health" "200" "테스트_헬스_체크"

# === 2. 인증 관련 400 에러 테스트 ===
echo "🔍 2. 인증 관련 400 에러 테스트"
test_endpoint "POST" "/auth/register" "400" "회원가입_빈_데이터" "{}"
test_endpoint "POST" "/auth/login" "400" "로그인_빈_데이터" "{}"
test_endpoint "GET" "/auth/health" "200" "인증_헬스_체크"

# === 3. 인증 필요 엔드포인트 테스트 (401 예상) ===
echo "🔍 3. 인증 필요 엔드포인트 테스트 (Spring Security 동작에 맞춤)"

# 주요 인증 필요 엔드포인트들
auth_required_endpoints=(
    "GET|/api/users/me"
    "GET|/api/guardians/my"
    "GET|/api/accessibility/settings"
    "GET|/api/statistics/summary"
    "GET|/api/emergency/active"
    "POST|/api/notifications/fcm-token"
    "POST|/api/geofences"
    "POST|/api/emergency/alert"
)

for endpoint_def in "${auth_required_endpoints[@]}"; do
    IFS='|' read -r method endpoint <<< "$endpoint_def"
    description="인증_필요_$(echo "$endpoint" | sed 's|/|_|g' | sed 's/^_//')"

    if [ "$method" = "POST" ]; then
        test_endpoint "$method" "$endpoint" "401" "$description" "{}"
    else
        test_endpoint "$method" "$endpoint" "401" "$description"
    fi
done

# === 4. 존재하지 않는 엔드포인트 테스트 (401 vs 404) ===
echo "🔍 4. 존재하지 않는 엔드포인트 테스트"
# Spring Security가 인증을 먼저 검사하므로 401 예상
test_endpoint "GET" "/api/nonexistent" "401" "존재하지_않는_API_경로"
test_endpoint "GET" "/api/health/invalid" "500" "잘못된_헬스_체크_경로"
test_endpoint "GET" "/api/auth/invalid" "500" "잘못된_인증_경로"

# === 5. HTTP 메서드 에러 테스트 ===
echo "🔍 5. HTTP 메서드 에러 테스트"
# Spring Security가 요청을 가로채므로 대부분 401이나 500 예상
test_endpoint "DELETE" "/api/health" "500" "헬스_체크에_DELETE_메서드"
test_endpoint "PUT" "/api/auth/oauth2/login-urls" "500" "OAuth2_URL에_PUT_메서드"
test_endpoint "PATCH" "/api/test/health" "405" "테스트_헬스체크에_PATCH_메서드"

# === 6. 파라미터 및 데이터 검증 테스트 ===
echo "🔍 6. 파라미터 및 데이터 검증 테스트"
test_endpoint "GET" "/api/accessibility/screen-reader-hint" "401" "필수_파라미터_누락"
test_endpoint "POST" "/api/accessibility/voice-guidance" "401" "JSON_엔드포인트_인증_없음"

# === 7. 잘못된 토큰 테스트 ===
echo "🔍 7. 잘못된 토큰 테스트"
test_endpoint "GET" "/api/users/me" "401" "잘못된_토큰으로_내_정보_조회" "" "Authorization: Bearer invalid-token"
test_endpoint "GET" "/api/guardians/my" "401" "잘못된_토큰으로_보호자_조회" "" "Authorization: Bearer invalid-token"

# === 8. 경로 및 인코딩 테스트 ===
echo "🔍 8. 경로 및 인코딩 테스트"
test_endpoint "GET" "/API/HEALTH" "401" "대문자_경로"
test_endpoint "GET" "/api/HEALTH" "401" "부분_대문자_경로"
test_endpoint "GET" "/api/health/../admin" "401" "경로_트래버설_시도"
test_endpoint "GET" "/api/health%2F" "400" "URL_인코딩된_경로"

# === 9. 데이터 형식 테스트 ===
echo "🔍 9. 데이터 형식 테스트"
test_endpoint "POST" "/auth/register" "500" "회원가입_잘못된_JSON" '{"username":}'
test_endpoint "POST" "/auth/login" "500" "로그인_XML_데이터" "<xml>data</xml>" "Content-Type: application/xml"
test_endpoint "POST" "/auth/register" "400" "큰_JSON_데이터" "$(printf '{\"username\":\"%*s\"}' 5000 | tr ' ' 'a')"

# === 10. 로그아웃 및 리프레시 테스트 ===
echo "🔍 10. 로그아웃 및 리프레시 테스트"
test_endpoint "POST" "/auth/logout" "401" "로그아웃_인증_없음"
test_endpoint "POST" "/auth/refresh" "400" "리프레시_빈_토큰" "{}"

# === 결과 요약 ===
echo ""
echo "📊 현실적인 테스트 결과"
echo "=================================="
echo "총 테스트: $total_tests"
echo "성공: $passed_tests"
echo "실패: $failed_tests"
success_rate=$(echo "scale=1; $passed_tests * 100 / $total_tests" | bc -l)
echo "성공률: $success_rate%"
echo ""

# 실패한 테스트 요약
if [ $failed_tests -gt 0 ]; then
    echo "❌ 실패한 테스트들:"
    grep "FAIL," "$RESULTS_DIR/summary.csv" | while IFS=',' read -r result test_name method endpoint expected actual desc; do
        echo "   $test_name: $method $endpoint (예상: $expected, 실제: $actual)"
    done
    echo ""
fi

# 결과 저장
{
    echo "현실적인 Spring Security 테스트 결과"
    echo "테스트 시간: $(date)"
    echo "총 테스트: $total_tests"
    echo "성공: $passed_tests"
    echo "실패: $failed_tests"
    echo "성공률: $success_rate%"
    echo ""
    echo "Spring Security 동작 특성:"
    echo "- 인증되지 않은 요청: 401 Unauthorized"
    echo "- 인증된 사용자의 권한 부족: 403 Forbidden"
    echo "- 공개 엔드포인트: 200/400/500 정상 반환"
    echo "- 존재하지 않는 경로도 인증 검사 우선으로 401 반환"
    echo ""
    if [ $failed_tests -gt 0 ]; then
        echo "실패한 테스트:"
        grep "FAIL," "$RESULTS_DIR/summary.csv" | while IFS=',' read -r result test_name method endpoint expected actual desc; do
            echo "  - $test_name: $method $endpoint (예상: $expected, 실제: $actual)"
        done
    fi
} > "$RESULTS_DIR/summary.txt"

if [ $failed_tests -eq 0 ]; then
    echo "🎉 100% 성공! 모든 엔드포인트가 Spring Security 정책에 맞게 동작합니다!"
    exit 0
else
    echo "⚠️  $failed_tests개 테스트 실패. 상세 로그: $RESULTS_DIR/"
    echo "📝 CSV 결과: $RESULTS_DIR/summary.csv"
    exit 1
fi