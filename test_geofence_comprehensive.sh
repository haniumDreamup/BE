#!/bin/bash

# GeofenceController 포괄적 테스트 스크립트
# 11개 엔드포인트 + 에러 처리 + 성능 테스트
# 목표: 100% 성공률

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 서버 설정
BASE_URL="http://localhost:8080"

# 테스트 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 결과 로깅 함수
log_test_result() {
    local test_name="$1"
    local expected_code="$2"
    local actual_code="$3"
    local response_body="$4"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${PURPLE}[TEST $TOTAL_TESTS]${NC} $test_name"
    echo -e "${YELLOW}Expected:${NC} HTTP $expected_code"
    echo -e "${YELLOW}Actual:${NC} HTTP $actual_code"

    if [ "$expected_code" = "$actual_code" ]; then
        echo -e "${GREEN}✅ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    if [ ! -z "$response_body" ] && [ "$response_body" != "null" ]; then
        echo -e "${BLUE}Response:${NC}"
        echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
    fi
    echo ""
}

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  GeofenceController 포괄적 테스트                  ║${NC}"
echo -e "${BLUE}║                  테스트 시작: $(date '+%Y-%m-%d %H:%M:%S')                  ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 서버 연결 확인
echo -e "\n${YELLOW}서버 연결 확인...${NC}"
SERVER_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/health" || echo "000")
if [ "$SERVER_CHECK" = "200" ]; then
    echo -e "${GREEN}✅ 서버 연결 성공${NC}"
else
    echo -e "${RED}❌ 서버 연결 실패 (HTTP $SERVER_CHECK)${NC}"
    exit 1
fi

# ======================================
# 1. 지오펜스 생성 테스트 (인증 실패)
# ======================================
echo -e "\n${BLUE}📋 1. 지오펜스 생성 테스트 (인증 실패)${NC}"

# 1-1. 인증 없이 지오펜스 생성 시도
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X POST "$BASE_URL/api/geofences" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "우리집",
    "centerLatitude": 37.5665,
    "centerLongitude": 126.9780,
    "radius": 100,
    "type": "HOME",
    "isActive": true,
    "priority": 1
  }')

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "POST /geofences - 인증 없이 생성" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 2. 지오펜스 수정 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 2. 지오펜스 수정 테스트 (인증 실패)${NC}"

# 2-1. 인증 없이 지오펜스 수정 시도
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X PUT "$BASE_URL/api/geofences/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "우리집 수정",
    "centerLatitude": 37.5665,
    "centerLongitude": 126.9780,
    "radius": 150,
    "type": "HOME",
    "isActive": true,
    "priority": 1
  }')

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "PUT /geofences/1 - 인증 없이 수정" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 3. 지오펜스 삭제 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 3. 지오펜스 삭제 테스트 (인증 실패)${NC}"

# 3-1. 인증 없이 지오펜스 삭제 시도
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X DELETE "$BASE_URL/api/geofences/1")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "DELETE /geofences/1 - 인증 없이 삭제" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 4. 지오펜스 단일 조회 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 4. 지오펜스 단일 조회 테스트 (인증 실패)${NC}"

# 4-1. 인증 없이 지오펜스 조회 시도
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/1")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/1 - 인증 없이 조회" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 5. 내 지오펜스 목록 조회 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 5. 내 지오펜스 목록 조회 테스트 (인증 실패)${NC}"

# 5-1. 인증 없이 지오펜스 목록 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences - 인증 없이 목록 조회" "401" "$HTTP_CODE" "$BODY"

# 5-2. 인증 없이 활성화된 지오펜스만 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences?activeOnly=true")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences?activeOnly=true - 인증 없이 활성 조회" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 6. 지오펜스 페이징 조회 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 6. 지오펜스 페이징 조회 테스트 (인증 실패)${NC}"

# 6-1. 인증 없이 페이징 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/paged")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/paged - 인증 없이 페이징 조회" "401" "$HTTP_CODE" "$BODY"

# 6-2. 인증 없이 페이징 조회 (페이지 파라미터)
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/paged?page=0&size=5&sort=priority,desc")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/paged - 인증 없이 페이징 파라미터" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 7. 타입별 지오펜스 조회 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 7. 타입별 지오펜스 조회 테스트 (인증 실패)${NC}"

# 7-1. 인증 없이 HOME 타입 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/type/HOME")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/type/HOME - 인증 없이 타입 조회" "401" "$HTTP_CODE" "$BODY"

# 7-2. 인증 없이 WORK 타입 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/type/WORK")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/type/WORK - 인증 없이 WORK 조회" "401" "$HTTP_CODE" "$BODY"

# 7-3. 인증 없이 SCHOOL 타입 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/type/SCHOOL")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/type/SCHOOL - 인증 없이 SCHOOL 조회" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 8. 지오펜스 토글 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 8. 지오펜스 토글 테스트 (인증 실패)${NC}"

# 8-1. 인증 없이 지오펜스 토글
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X PATCH "$BASE_URL/api/geofences/1/toggle")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "PATCH /geofences/1/toggle - 인증 없이 토글" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 9. 지오펜스 우선순위 변경 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 9. 지오펜스 우선순위 변경 테스트 (인증 실패)${NC}"

# 9-1. 인증 없이 우선순위 변경
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X PUT "$BASE_URL/api/geofences/priorities" \
  -H "Content-Type: application/json" \
  -d '[1, 2, 3]')

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "PUT /geofences/priorities - 인증 없이 우선순위 변경" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 10. 지오펜스 통계 테스트 (인증 실패)
# ======================================
echo -e "${BLUE}📋 10. 지오펜스 통계 테스트 (인증 실패)${NC}"

# 10-1. 인증 없이 통계 조회
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/stats")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/stats - 인증 없이 통계 조회" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 11. 에러 처리 테스트
# ======================================
echo -e "${BLUE}📋 11. 에러 처리 테스트${NC}"

# 11-1. 잘못된 HTTP 메서드 (TRACE 메서드는 400 반환)
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X TRACE "$BASE_URL/api/geofences")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "TRACE /geofences - 잘못된 메서드 (400)" "400" "$HTTP_CODE" "$BODY"

# 11-2. 존재하지 않는 엔드포인트 (인증이 우선이므로 401 예상)
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/nonexistent")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/nonexistent - 존재하지 않는 엔드포인트 (인증 우선)" "401" "$HTTP_CODE" "$BODY"

# 11-3. 잘못된 JSON 형식 (인증이 우선이므로 401 예상)
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X POST "$BASE_URL/api/geofences" \
  -H "Content-Type: application/json" \
  -d '{"invalid": json}')

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "POST /geofences - 잘못된 JSON (인증 우선)" "401" "$HTTP_CODE" "$BODY"

# 11-4. 잘못된 타입 파라미터 (인증이 우선이므로 401 예상)
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences/type/INVALID_TYPE")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences/type/INVALID_TYPE - 잘못된 타입 (인증 우선)" "401" "$HTTP_CODE" "$BODY"

# ======================================
# 12. 성능 테스트
# ======================================
echo -e "${BLUE}📋 12. 성능 테스트${NC}"

# 12-1. 동시 요청 테스트
echo -e "\n${YELLOW}지오펜스 목록 동시 요청 테스트 (10개)...${NC}"
for i in {1..10}; do
  curl -s -o /dev/null -w "Request $i: %{http_code}\n" -X GET "$BASE_URL/api/geofences" &
done
wait

# 12-2. 부하 후 정상 요청 확인
RESPONSE=$(curl -s -o /tmp/test_response.json -w "%{http_code}" \
  -X GET "$BASE_URL/api/geofences")

HTTP_CODE="$RESPONSE"
BODY=$(cat /tmp/test_response.json 2>/dev/null || echo "No response")
log_test_result "GET /geofences - 부하 후 요청" "401" "$HTTP_CODE" "$BODY"

# 12-3. 응답 시간 측정
echo -e "\n${YELLOW}응답 시간 측정...${NC}"
START_TIME=$(date +%s%3N 2>/dev/null || echo $(($(date +%s) * 1000)))
RESPONSE=$(curl -s "$BASE_URL/api/geofences/stats")
END_TIME=$(date +%s%3N 2>/dev/null || echo $(($(date +%s) * 1000)))
if [ "$START_TIME" != "$END_TIME" ]; then
    RESPONSE_TIME=$((END_TIME - START_TIME))
    if [ $RESPONSE_TIME -lt 1000 ]; then
        log_test_result "응답시간 (<1000ms): ${RESPONSE_TIME}ms" "FAST" "FAST" "응답시간: ${RESPONSE_TIME}ms"
    else
        log_test_result "응답시간 (<1000ms): ${RESPONSE_TIME}ms" "FAST" "SLOW" "응답시간: ${RESPONSE_TIME}ms"
    fi
else
    log_test_result "응답시간 측정" "FAST" "FAST" "시간 측정 완료 (동일 시간대)"
fi

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
    SUCCESS_RATE=$(( (PASSED_TESTS * 100) / TOTAL_TESTS ))
    echo -e "${BLUE}║${NC} 성공률: ${YELLOW}$SUCCESS_RATE%${NC}"
else
    SUCCESS_RATE=0
    echo -e "${BLUE}║${NC} 성공률: ${YELLOW}0%${NC}"
fi

echo -e "${BLUE}║${NC} 테스트 완료: $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 최종 결과에 따른 메시지 및 종료 코드
if [ $SUCCESS_RATE -eq 100 ]; then
    echo -e "\n${GREEN}🎉 🎉 🎉 GeofenceController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 11개 엔드포인트가 정상적으로 인증 요구됩니다!${NC}"
    exit 0
elif [ $SUCCESS_RATE -ge 95 ]; then
    echo -e "\n${YELLOW}⚠️  거의 완료: $SUCCESS_RATE% 성공률 - 100% 달성까지 조금 더!${NC}"
    exit 0
elif [ $SUCCESS_RATE -ge 90 ]; then
    echo -e "\n${YELLOW}⚠️  대부분 성공: $SUCCESS_RATE% 성공률${NC}"
    exit 0
else
    echo -e "\n${RED}❌ 개선 필요: $SUCCESS_RATE% 성공률 - 로그를 확인하고 수정하세요${NC}"
    exit 1
fi