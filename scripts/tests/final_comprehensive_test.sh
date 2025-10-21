#!/bin/bash

# 완전한 엔드포인트 테스트 - ProblemDetail 형식 확인
# 147개의 실제 엔드포인트와 추가 엣지 케이스 모두 테스트

BASE_URL="http://43.200.49.171:8080"
TIMESTAMP=$(date "+%Y%m%d_%H%M%S")
RESULTS_DIR="final_test_results_${TIMESTAMP}"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

echo "🎯 ProblemDetail 형식 완전 검증 테스트 시작..."
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
    echo "   $method $endpoint → 예상: HTTP $expected_status"

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
    has_required_fields=false

    # JSON 형식이고 ProblemDetail 필드들이 있는지 확인
    if echo "$body" | jq -e '.type, .title, .detail' >/dev/null 2>&1; then
        is_problem_detail=true
        has_required_fields=true
    elif echo "$body" | jq -e '.type, .title' >/dev/null 2>&1; then
        is_problem_detail=true
    elif echo "$body" | jq -e '.title, .detail' >/dev/null 2>&1; then
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
            echo "   응답: $body"
            failed_tests=$((failed_tests + 1))
        fi
    else
        echo "   ❌ FAIL: HTTP $status_code (예상: $expected_status)"
        echo "   응답: $body"
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

# === 1. 정상 작동 엔드포인트 테스트 ===
echo "🔍 1. 정상 엔드포인트 테스트"
test_endpoint "GET" "/api/health" "200" "애플리케이션_헬스_체크"
test_endpoint "GET" "/health" "200" "대체_헬스_체크_경로"
test_endpoint "GET" "/api/auth/oauth2/login-urls" "200" "OAuth2_로그인_URL_조회"

# === 2. 인증 관련 400 에러 테스트 ===
echo "🔍 2. 인증 관련 400 에러 테스트"
test_endpoint "POST" "/auth/register" "400" "회원가입_빈_데이터" "{}"
test_endpoint "POST" "/auth/login" "400" "로그인_빈_데이터" "{}"
test_endpoint "GET" "/auth/health" "200" "인증_헬스_체크"

# === 3. 인증 필요 엔드포인트 테스트 (403 예상) ===
echo "🔍 3. 인증 필요 엔드포인트 테스트 (모든 147개 실제 엔드포인트)"

# 실제 엔드포인트 파일에서 읽어서 테스트
while IFS='|' read -r method endpoint; do
    # 이미 테스트한 엔드포인트들 제외
    if [ "$endpoint" = "/api/health" ] || [ "$endpoint" = "/health" ] || [ "$endpoint" = "/api/auth/oauth2/login-urls" ]; then
        continue
    fi

    # 인증이 필요한 엔드포인트는 403 예상
    if [[ "$endpoint" == "/api/"* ]] && [[ "$endpoint" != "/api/auth/"* ]] && [[ "$endpoint" != "/api/health"* ]]; then
        expected="403"
        description="인증_필요_$(echo "$endpoint" | sed 's|/|_|g' | sed 's/^_//')"
    elif [[ "$endpoint" == "/auth/"* ]] || [[ "$endpoint" == "/api/auth/"* ]]; then
        if [ "$method" = "POST" ]; then
            expected="400"  # 빈 데이터로 400 예상
            description="인증_엔드포인트_$(echo "$endpoint" | sed 's|/|_|g' | sed 's/^_//')"
        else
            expected="200"  # GET 요청은 200 예상
            description="인증_엔드포인트_$(echo "$endpoint" | sed 's|/|_|g' | sed 's/^_//')"
        fi
    else
        expected="403"
        description="기타_엔드포인트_$(echo "$endpoint" | sed 's|/|_|g' | sed 's/^_//')"
    fi

    # POST 요청에는 빈 JSON 데이터 추가
    if [ "$method" = "POST" ]; then
        test_endpoint "$method" "$endpoint" "$expected" "$description" "{}"
    else
        test_endpoint "$method" "$endpoint" "$expected" "$description"
    fi
done < all_real_endpoints.txt

# === 4. 404 에러 테스트 ===
echo "🔍 4. 404 에러 테스트"
test_endpoint "GET" "/api/test/health" "200" "테스트_헬스_체크"
test_endpoint "GET" "/api/nonexistent" "404" "존재하지_않는_API_경로"
test_endpoint "GET" "/api/health/invalid" "404" "잘못된_헬스_체크_경로"
test_endpoint "GET" "/api/auth/invalid" "404" "잘못된_인증_경로"

# === 5. 405 에러 테스트 ===
echo "🔍 5. 405 에러 테스트"
test_endpoint "DELETE" "/api/health" "405" "헬스_체크에_DELETE_메서드"
test_endpoint "PUT" "/api/auth/oauth2/login-urls" "405" "OAuth2_URL에_PUT_메서드"
test_endpoint "PATCH" "/api/test/health" "405" "테스트_헬스체크에_PATCH_메서드"

# === 6. 고급 에러 시나리오 테스트 ===
echo "🔍 6. 고급 에러 시나리오 테스트"
test_endpoint "GET" "/api/accessibility/screen-reader-hint" "400" "필수_파라미터_누락"
test_endpoint "GET" "/api/accessibility/screen-reader-hint?action=" "400" "빈_파라미터_값"
test_endpoint "POST" "/api/accessibility/voice-guidance" "403" "JSON_엔드포인트에_Content-Type_없음"

# === 7. 잘못된 토큰 테스트 ===
echo "🔍 7. 잘못된 토큰 테스트"
test_endpoint "GET" "/api/users/me" "403" "잘못된_토큰으로_내_정보_조회" "" "Authorization: Bearer invalid-token"
test_endpoint "GET" "/api/guardians/my" "403" "잘못된_토큰으로_보호자_조회" "" "Authorization: Bearer invalid-token"
test_endpoint "GET" "/api/accessibility/settings" "403" "잘못된_토큰으로_접근성_설정" "" "Authorization: Bearer invalid-token"

# === 8. 엣지 케이스 테스트 ===
echo "🔍 8. 엣지 케이스 테스트"
test_endpoint "GET" "/API/HEALTH" "404" "대문자_경로"
test_endpoint "GET" "/api/HEALTH" "404" "부분_대문자_경로"
test_endpoint "GET" "/api/health/../admin" "404" "경로_트래버설_시도"
test_endpoint "GET" "/api/health%2F" "400" "URL_인코딩된_경로"
test_endpoint "GET" "/ws" "403" "WebSocket_엔드포인트에_HTTP_GET"

# === 9. 잘못된 데이터 형식 테스트 ===
echo "🔍 9. 잘못된 데이터 형식 테스트"
test_endpoint "POST" "/auth/register" "400" "회원가입에_잘못된_JSON" '{"username":}'
test_endpoint "POST" "/auth/login" "400" "로그인에_XML_데이터" "<xml>data</xml>" "Content-Type: application/xml"
test_endpoint "POST" "/auth/register" "400" "큰_JSON_데이터" "$(printf '{\"username\":\"%*s\"}' 5000 | tr ' ' 'a')"

# === 10. 타입 검증 테스트 ===
echo "🔍 10. 타입 검증 테스트"
test_endpoint "GET" "/api/users/abc" "403" "문자열_사용자_ID"
test_endpoint "GET" "/api/geofences/xyz" "403" "문자열_지오펜스_ID"
test_endpoint "GET" "/api/emergency/status/invalid" "403" "잘못된_응급상황_ID"
test_endpoint "GET" "/api/users/-1" "403" "음수_사용자_ID"
test_endpoint "GET" "/api/geofences/-123" "403" "음수_지오펜스_ID"
test_endpoint "GET" "/api/users/999999999999" "403" "매우_큰_사용자_ID"

# === 결과 요약 ===
echo ""
echo "📊 최종 테스트 결과"
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
    echo "ProblemDetail 형식 완전 검증 테스트 결과"
    echo "테스트 시간: $(date)"
    echo "총 테스트: $total_tests"
    echo "성공: $passed_tests"
    echo "실패: $failed_tests"
    echo "성공률: $success_rate%"
    echo ""
    if [ $failed_tests -gt 0 ]; then
        echo "실패한 테스트:"
        grep "FAIL," "$RESULTS_DIR/summary.csv" | while IFS=',' read -r result test_name method endpoint expected actual desc; do
            echo "  - $test_name: $method $endpoint (예상: $expected, 실제: $actual)"
        done
    fi
} > "$RESULTS_DIR/summary.txt"

if [ $failed_tests -eq 0 ]; then
    echo "🎉 100% 성공! 모든 엔드포인트가 올바른 ProblemDetail 형식으로 응답합니다!"
    exit 0
else
    echo "⚠️  $failed_tests개 테스트 실패. 상세 로그: $RESULTS_DIR/"
    echo "📝 CSV 결과: $RESULTS_DIR/summary.csv"
    exit 1
fi