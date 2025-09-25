#!/bin/bash

# TestController 포괄적 테스트 스크립트
# 모든 엔드포인트 테스트: 성공/실패/엣지케이스 포함

BASE_URL="http://localhost:8080"
TEST_NAME="TestController Comprehensive Test"
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
echo -e "${BLUE}║                    TestController 포괄적 테스트                     ║${NC}"
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
if curl -s --connect-timeout 5 "$BASE_URL/api/test/health" > /dev/null; then
    echo -e "${GREEN}✅ 서버 연결 성공${NC}"
else
    echo -e "${RED}❌ 서버 연결 실패 - 서버가 실행 중인지 확인하세요${NC}"
    exit 1
fi

# ======================================
# 1. /api/test/date 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 1. /api/test/date 엔드포인트 테스트${NC}"

# 1-1. 유효한 날짜 파라미터
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-01-01")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 유효한 날짜 (2024-01-01)" "200" "$HTTP_CODE" "$BODY"

# 1-2. 날짜 파라미터 없음 (옵셔널)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 날짜 파라미터 없음" "200" "$HTTP_CODE" "$BODY"

# 1-3. 잘못된 날짜 형식 (400 에러 테스트용)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=invalid-date")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 잘못된 날짜 형식" "400" "$HTTP_CODE" "$BODY"

# 1-4. 잘못된 ISO 형식 (MM/DD/YYYY)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=12/31/2024")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - MM/DD/YYYY 형식" "400" "$HTTP_CODE" "$BODY"

# 1-5. 공백이 포함된 날짜
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-01-01%20extra")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 공백 포함 날짜" "400" "$HTTP_CODE" "$BODY"

# 1-6. 존재하지 않는 날짜
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-02-30")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 존재하지 않는 날짜 (2월 30일)" "400" "$HTTP_CODE" "$BODY"

# 1-7. 윤년 체크
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-02-29")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 윤년 날짜 (2024-02-29)" "200" "$HTTP_CODE" "$BODY"

# 1-8. POST 요청 (허용되지 않는 메서드)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$BASE_URL/api/test/date")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/date - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# ======================================
# 2. /api/test/echo 엔드포인트 테스트
# ======================================
echo -e "\n${BLUE}📋 2. /api/test/echo 엔드포인트 테스트${NC}"

# 2-1. 정상 메시지 에코
TEST_MESSAGE="Hello BIF-AI Backend"
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "\"$TEST_MESSAGE\"" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 정상 메시지" "200" "$HTTP_CODE" "$BODY"

# 2-2. 빈 메시지
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d '""' "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 빈 메시지" "200" "$HTTP_CODE" "$BODY"

# 2-3. 메시지 없음 (required = false)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 메시지 없음" "200" "$HTTP_CODE" "$BODY"

# 2-4. 긴 메시지 (5000자)
LONG_MESSAGE=$(printf "%0*d" 5000 0)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "\"$LONG_MESSAGE\"" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 긴 메시지 (5000자)" "200" "$HTTP_CODE" "$BODY"

# 2-5. 특수 문자가 포함된 메시지
SPECIAL_MESSAGE="<script>alert('XSS')</script> & HTML \"entities\" 'test'"
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "\"$SPECIAL_MESSAGE\"" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 특수 문자" "200" "$HTTP_CODE" "$BODY"

# 2-6. 유니코드 메시지
UNICODE_MESSAGE="안녕하세요 🌟 UTF-8 테스트 😊 中文测试"
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "\"$UNICODE_MESSAGE\"" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 유니코드" "200" "$HTTP_CODE" "$BODY"

# 2-7. 잘못된 Content-Type
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: text/plain" -d "plain text message" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 잘못된 Content-Type" "400" "$HTTP_CODE" "$BODY"

# 2-8. 잘못된 JSON
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "{invalid json" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 잘못된 JSON" "400" "$HTTP_CODE" "$BODY"

# 2-9. GET 요청 (허용되지 않는 메서드)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/echo - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# ======================================
# 3. 엣지 케이스 및 보안 테스트
# ======================================
echo -e "\n${BLUE}📋 3. 엣지 케이스 및 보안 테스트${NC}"

# 3-1. SQL Injection 시도 (날짜 파라미터)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-01-01';DROP TABLE users;--")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - SQL Injection 시도" "400" "$HTTP_CODE" "$BODY"

# 3-2. 매우 긴 URL 파라미터
LONG_DATE_PARAM=$(printf "2024-01-%0*d" 1000 1)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=$LONG_DATE_PARAM" 2>/dev/null)
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 매우 긴 파라미터" "400" "$HTTP_CODE" "$BODY"

# 3-3. 여러 날짜 파라미터
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-01-01&date=2024-02-01")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 중복 파라미터" "200" "$HTTP_CODE" "$BODY"

# 3-4. 잘못된 헤더
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -H "Invalid-Header: <script>alert('xss')</script>" -X GET "$BASE_URL/api/test/date?date=2024-01-01")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 악의적 헤더" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 4. 동시 요청 및 부하 테스트
# ======================================
echo -e "\n${BLUE}📋 4. 동시 요청 및 부하 테스트${NC}"

# 4-1. 동시 다중 요청 (date 엔드포인트)
echo -e "\n${YELLOW}날짜 엔드포인트 동시 요청 테스트 (20개)...${NC}"
for i in {1..20}; do
    curl -s "$BASE_URL/api/test/date?date=2024-01-$(printf "%02d" $((i % 28 + 1)))" &
done
wait

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/date?date=2024-01-01")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/date - 부하 후 요청" "200" "$HTTP_CODE" "$BODY"

# 4-2. 동시 다중 요청 (echo 엔드포인트)
echo -e "\n${YELLOW}에코 엔드포인트 동시 요청 테스트 (15개)...${NC}"
for i in {1..15}; do
    curl -s -X POST -H "Content-Type: application/json" -d "\"Message $i\"" "$BASE_URL/api/test/echo" &
done
wait

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "\"Test after load\"" "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /api/test/echo - 부하 후 요청" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 5. 응답 시간 및 성능 테스트
# ======================================
echo -e "\n${BLUE}📋 5. 응답 시간 및 성능 테스트${NC}"

# 5-1. 날짜 엔드포인트 응답 시간
START_TIME=$(date +%s%3N 2>/dev/null || date +%s000)
RESPONSE=$(curl -s "$BASE_URL/api/test/date?date=2024-01-01")
END_TIME=$(date +%s%3N 2>/dev/null || date +%s000)
RESPONSE_TIME=$((END_TIME - START_TIME))

if [ $RESPONSE_TIME -lt 500 ]; then  # 500ms 미만
    log_test_result "날짜 엔드포인트 응답 시간 (<500ms)" "FAST" "FAST" "${RESPONSE_TIME}ms"
else
    log_test_result "날짜 엔드포인트 응답 시간 (<500ms)" "FAST" "SLOW" "${RESPONSE_TIME}ms"
fi

# 5-2. 에코 엔드포인트 응답 시간
START_TIME=$(date +%s%3N 2>/dev/null || date +%s000)
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "\"Performance test\"" "$BASE_URL/api/test/echo")
END_TIME=$(date +%s%3N 2>/dev/null || date +%s000)
RESPONSE_TIME=$((END_TIME - START_TIME))

if [ $RESPONSE_TIME -lt 500 ]; then  # 500ms 미만
    log_test_result "에코 엔드포인트 응답 시간 (<500ms)" "FAST" "FAST" "${RESPONSE_TIME}ms"
else
    log_test_result "에코 엔드포인트 응답 시간 (<500ms)" "FAST" "SLOW" "${RESPONSE_TIME}ms"
fi

# ======================================
# 6. HTTP 메서드별 상세 테스트
# ======================================
echo -e "\n${BLUE}📋 6. HTTP 메서드별 상세 테스트${NC}"

# OPTIONS 요청 (CORS 프리플라이트)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X OPTIONS "$BASE_URL/api/test/date")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "OPTIONS /api/test/date - CORS 프리플라이트" "200" "$HTTP_CODE" "$BODY"

# HEAD 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -I "$BASE_URL/api/test/date?date=2024-01-01")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "HEAD /api/test/date - 헤더만 요청" "200" "$HTTP_CODE" "$BODY"

# PUT/PATCH/DELETE 요청
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X PUT "$BASE_URL/api/test/echo")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "PUT /api/test/echo - 메서드 불허용" "405" "$HTTP_CODE" "$BODY"

# ======================================
# 7. 잘못된 경로 테스트
# ======================================
echo -e "\n${BLUE}📋 7. 잘못된 경로 테스트${NC}"

# 존재하지 않는 하위 경로
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/test/nonexistent")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/test/nonexistent - 존재하지 않는 경로" "404" "$HTTP_CODE" "$BODY"

# 대소문자 구분 확인
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/TEST/date")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /api/TEST/date - 대문자 경로" "404" "$HTTP_CODE" "$BODY"

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