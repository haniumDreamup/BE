#!/bin/bash

# HealthController 포괄적 테스트 스크립트
# 모든 엔드포인트 테스트: 성공/실패/엣지케이스 포함

BASE_URL="http://localhost:8080"
TEST_NAME="HealthController Comprehensive Test"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    HealthController 포괄적 테스트                    ║${NC}"
echo -e "${BLUE}║                  테스트 시작: $(date '+%Y-%m-%d %H:%M:%S')                  ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 테스트 결과 로깅 함수
log_test_result() {
    local test_name="$1"
    local expected_code="$2"
    local actual_code="$3"
    local response_body="$4"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "\n${PURPLE}[TEST $TOTAL_TESTS]${NC} $test_name"
    echo -e "${YELLOW}Expected:${NC} HTTP $expected_code"
    echo -e "${YELLOW}Actual:${NC} HTTP $actual_code"

    if [ "$expected_code" = "$actual_code" ]; then
        echo -e "${GREEN}✅ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))

        # JSON 응답이면 예쁘게 출력
        if echo "$response_body" | jq empty 2>/dev/null; then
            echo -e "${BLUE}Response:${NC}"
            echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
        else
            echo -e "${BLUE}Response:${NC} $response_body"
        fi
    else
        echo -e "${RED}❌ FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}Response:${NC} $response_body"
    fi
}

# 서버 상태 확인
echo -e "\n${YELLOW}서버 연결 확인...${NC}"
if curl -s --connect-timeout 5 "$BASE_URL/api/health" > /dev/null; then
    echo -e "${GREEN}✅ 서버 연결 성공${NC}"
else
    echo -e "${RED}❌ 서버 연결 실패 - 서버가 실행 중인지 확인하세요${NC}"
    exit 1
fi

# ======================================
# 1. /api/health 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 1. /api/health 엔드포인트 테스트${NC}"

# 1-1. 정상 GET 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health - 정상 요청" "200" "$HTTP_CODE" "$BODY"

# 1-2. POST 요청 (허용되지 않는 메서드)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/health - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# 1-3. PUT 요청 (허용되지 않는 메서드)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X PUT "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "PUT /api/health - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# 1-4. DELETE 요청 (허용되지 않는 메서드)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "DELETE /api/health - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# ======================================
# 2. /health 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 2. /health 엔드포인트 테스트${NC}"

# 2-1. 정상 GET 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /health - 정상 요청" "200" "$HTTP_CODE" "$BODY"

# 2-2. 특수 문자가 포함된 헤더
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -H "X-Test-Header: <script>alert('test')</script>" -X GET "$BASE_URL/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /health - 특수 문자 헤더" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 3. /api/v1/health 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 3. /api/v1/health 엔드포인트 테스트${NC}"

# 3-1. 정상 GET 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/v1/health - 정상 요청" "200" "$HTTP_CODE" "$BODY"

# 3-2. 잘못된 버전 (v2)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v2/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/v2/health - 존재하지 않는 버전" "404" "$HTTP_CODE" "$BODY"

# ======================================
# 4. /api/health/liveness 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 4. /api/health/liveness 엔드포인트 테스트${NC}"

# 4-1. 정상 GET 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/health/liveness")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health/liveness - 정상 요청" "200" "$HTTP_CODE" "$BODY"

# 4-2. HEAD 요청 (일반적으로 헬스체크에서 사용)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -I "$BASE_URL/api/health/liveness")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "HEAD /api/health/liveness - 헤더만 요청" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 5. /api/health/readiness 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 5. /api/health/readiness 엔드포인트 테스트${NC}"

# 5-1. 정상 GET 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/health/readiness")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health/readiness - 정상 요청" "200" "$HTTP_CODE" "$BODY"

# 5-2. 동시 다중 요청 (부하 테스트)
echo -e "\n${YELLOW}동시 다중 요청 테스트 (10개)...${NC}"
for i in {1..10}; do
    curl -s "$BASE_URL/api/health/readiness" &
done
wait

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/health/readiness")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health/readiness - 부하 후 요청" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 6. /api/test/health 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 6. /api/test/health 엔드포인트 테스트${NC}"

# 6-1. 정상 GET 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/health - 정상 요청" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 7. 엣지 케이스 및 보안 테스트
# ======================================
echo -e "\n${BLUE}📋 7. 엣지 케이스 및 보안 테스트${NC}"

# 7-1. 매우 긴 URL
LONG_PATH="/api/health" + $(printf "%0*d" 1000 0)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL$LONG_PATH")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET 매우 긴 URL - 에러 처리" "404" "$HTTP_CODE" "$BODY"

# 7-2. SQL Injection 시도 (URL 파라미터)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/health?id=1';DROP TABLE users;--")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health - SQL Injection 시도" "200" "$HTTP_CODE" "$BODY"

# 7-3. 큰 헤더 전송
BIG_HEADER=$(printf "X-Big-Header: %0*d" 8000 0)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -H "$BIG_HEADER" -X GET "$BASE_URL/api/health" 2>/dev/null)
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health - 큰 헤더 전송" "200" "$HTTP_CODE" "$BODY"

# 7-4. 특수 문자가 포함된 User-Agent
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -H "User-Agent: <script>alert('xss')</script>" -X GET "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health - XSS User-Agent" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 8. 응답 내용 검증 테스트
# ======================================
echo -e "\n${BLUE}📋 8. 응답 내용 검증 테스트${NC}"

# 8-1. JSON 구조 검증
RESPONSE=$(curl -s "$BASE_URL/api/health")
if echo "$RESPONSE" | jq -e '.status' > /dev/null 2>&1; then
    STATUS_VALUE=$(echo "$RESPONSE" | jq -r '.status')
    if [ "$STATUS_VALUE" = "UP" ]; then
        log_test_result "JSON 구조 검증 - status 필드" "PASS" "PASS" "status: $STATUS_VALUE"
    else
        log_test_result "JSON 구조 검증 - status 필드" "UP" "$STATUS_VALUE" "$RESPONSE"
    fi
else
    log_test_result "JSON 구조 검증 - status 필드" "VALID JSON" "INVALID JSON" "$RESPONSE"
fi

# 8-2. 응답 시간 측정
START_TIME=$(date +%s%3N)
RESPONSE=$(curl -s "$BASE_URL/api/health")
END_TIME=$(date +%s%3N)
RESPONSE_TIME=$((END_TIME - START_TIME))

if [ $RESPONSE_TIME -lt 1000 ]; then  # 1초 미만
    log_test_result "응답 시간 검증 (<1000ms)" "FAST" "FAST" "${RESPONSE_TIME}ms"
else
    log_test_result "응답 시간 검증 (<1000ms)" "FAST" "SLOW" "${RESPONSE_TIME}ms"
fi

# ======================================
# 9. HTTP 메서드별 테스트
# ======================================
echo -e "\n${BLUE}📋 9. HTTP 메서드별 상세 테스트${NC}"

# OPTIONS 요청 (CORS 프리플라이트)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X OPTIONS "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "OPTIONS /api/health - CORS 프리플라이트" "200" "$HTTP_CODE" "$BODY"

# PATCH 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X PATCH "$BASE_URL/api/health")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "PATCH /api/health - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# ======================================
# 10. 잘못된 경로 테스트
# ======================================
echo -e "\n${BLUE}📋 10. 잘못된 경로 테스트${NC}"

# 잘못된 하위 경로
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/health/nonexistent")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/health/nonexistent - 잘못된 하위 경로" "404" "$HTTP_CODE" "$BODY"

# 대소문자 구분 확인
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/HEALTH")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/HEALTH - 대문자 경로" "404" "$HTTP_CODE" "$BODY"

# ======================================
# 최종 결과 출력
# ======================================
echo -e "\n${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                           테스트 결과 요약                           ║${NC}"
echo -e "${BLUE}╠══════════════════════════════════════════════════════════════════╣${NC}"
echo -e "${BLUE}║${NC} 총 테스트: ${YELLOW}$TOTAL_TESTS${NC}개"
echo -e "${BLUE}║${NC} 성공: ${GREEN}$PASSED_TESTS${NC}개"
echo -e "${BLUE}║${NC} 실패: ${RED}$FAILED_TESTS${NC}개"

if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo -e "${BLUE}║${NC} 성공률: ${YELLOW}$SUCCESS_RATE%${NC}"
fi

echo -e "${BLUE}║${NC} 테스트 완료: $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"

if [ $SUCCESS_RATE -eq 100 ]; then
    echo -e "\n${GREEN}🎉 모든 테스트가 성공했습니다!${NC}"
    exit 0
elif [ $SUCCESS_RATE -ge 90 ]; then
    echo -e "\n${YELLOW}⚠️  대부분의 테스트가 성공했습니다.${NC}"
    exit 0
else
    echo -e "\n${RED}❌ 일부 테스트가 실패했습니다. 로그를 확인하세요.${NC}"
    exit 1
fi