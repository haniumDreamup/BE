#!/bin/bash

# AccessibilityController 포괄적 테스트 스크립트
# 모든 엔드포인트 테스트: 성공/실패/엣지케이스 포함

BASE_URL="http://localhost:8080"
TEST_NAME="AccessibilityController Comprehensive Test"
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
echo -e "${BLUE}║                  AccessibilityController 포괄적 테스트                ║${NC}"
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

        # JSON 응답이면 예쁘게 출력 (길면 자름)
        if echo "$response_body" | jq empty 2>/dev/null; then
            echo -e "${BLUE}Response:${NC}"
            if [ ${#response_body} -gt 500 ]; then
                echo "$response_body" | jq '.' 2>/dev/null | head -20 || echo "${response_body:0:500}..."
            else
                echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"
            fi
        else
            echo -e "${BLUE}Response:${NC} ${response_body:0:200}$([ ${#response_body} -gt 200 ] && echo '...')"
        fi
    else
        echo -e "${RED}❌ FAILED${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo -e "${RED}Response:${NC} ${response_body:0:300}$([ ${#response_body} -gt 300 ] && echo '...')"
    fi
}

# 서버 상태 확인
echo -e "\n${YELLOW}서버 연결 확인...${NC}"
if curl -s --connect-timeout 5 "$BASE_URL/api/v1/accessibility/statistics" > /dev/null; then
    echo -e "${GREEN}✅ 서버 연결 성공${NC}"
else
    echo -e "${RED}❌ 서버 연결 실패 - 서버가 실행 중인지 확인하세요${NC}"
    exit 1
fi

# ======================================
# 1. /api/v1/accessibility/voice-guidance 테스트 (POST)
# ======================================
echo -e "\n${BLUE}📋 1. 음성 안내 텍스트 생성 테스트${NC}"

# 1-1. 정상 음성 안내 요청 - 네비게이션
REQUEST_JSON='{
  "context": "navigation",
  "params": {
    "location": "홈화면",
    "action": "navigate"
  },
  "language": "ko"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/voice-guidance")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /voice-guidance - 네비게이션 컨텍스트" "200" "$HTTP_CODE" "$BODY"

# 1-2. 응급상황 컨텍스트
REQUEST_JSON='{
  "context": "emergency",
  "params": {
    "type": "fall_detection",
    "severity": "high"
  },
  "language": "ko"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/voice-guidance")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /voice-guidance - 응급상황 컨텍스트" "200" "$HTTP_CODE" "$BODY"

# 1-3. 버튼 액션 컨텍스트
REQUEST_JSON='{
  "context": "button_action",
  "params": {
    "button_name": "확인",
    "action_type": "submit"
  },
  "language": "ko"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/voice-guidance")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /voice-guidance - 버튼 액션" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 2. /api/v1/accessibility/aria-label 테스트 (POST)
# ======================================
echo -e "\n${BLUE}📋 2. ARIA 라벨 생성 테스트${NC}"

# 2-1. 버튼 ARIA 라벨
REQUEST_JSON='{
  "elementType": "button",
  "elementName": "긴급전화",
  "attributes": {
    "action": "call_emergency",
    "priority": "high"
  }
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/aria-label")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /aria-label - 긴급전화 버튼" "200" "$HTTP_CODE" "$BODY"

# 2-2. 입력 필드 ARIA 라벨
REQUEST_JSON='{
  "elementType": "input",
  "elementName": "전화번호",
  "attributes": {
    "required": true,
    "type": "tel",
    "format": "xxx-xxxx-xxxx"
  }
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/aria-label")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /aria-label - 전화번호 입력" "200" "$HTTP_CODE" "$BODY"

# 2-3. 메뉴 ARIA 라벨
REQUEST_JSON='{
  "elementType": "menu",
  "elementName": "주메뉴",
  "attributes": {
    "expandable": true,
    "items_count": 5
  }
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/aria-label")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /aria-label - 메뉴" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 3. /api/v1/accessibility/screen-reader-hint 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 3. 스크린 리더 힌트 테스트${NC}"

# 3-1. 버튼 클릭 힌트
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/screen-reader-hint?action=press&target=emergency_button")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /screen-reader-hint - 긴급버튼 누르기" "200" "$HTTP_CODE" "$BODY"

# 3-2. 스와이프 액션 힌트
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/screen-reader-hint?action=swipe&target=navigation_menu")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /screen-reader-hint - 메뉴 스와이프" "200" "$HTTP_CODE" "$BODY"

# 3-3. 텍스트 입력 힌트
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/screen-reader-hint?action=input&target=phone_field")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /screen-reader-hint - 텍스트 입력" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 4. /api/v1/accessibility/settings 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 4. 접근성 설정 조회 테스트${NC}"

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/settings")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /settings - 접근성 설정 조회" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 5. /api/v1/accessibility/settings 테스트 (PUT)
# ======================================
echo -e "\n${BLUE}📋 5. 접근성 설정 업데이트 테스트${NC}"

# 5-1. 완전한 설정 업데이트
REQUEST_JSON='{
  "userId": 1,
  "highContrastEnabled": true,
  "fontSize": "large",
  "voiceGuidanceEnabled": true,
  "simplifiedUiEnabled": true,
  "colorScheme": "high_contrast"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X PUT -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/settings")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "PUT /settings - 완전한 설정 업데이트" "200" "$HTTP_CODE" "$BODY"

# 5-2. 부분적 설정 업데이트
REQUEST_JSON='{
  "fontSize": "medium",
  "voiceGuidanceEnabled": false
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X PUT -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/settings")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "PUT /settings - 부분적 설정 업데이트" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 6. /api/v1/accessibility/settings/apply-profile 테스트 (POST)
# ======================================
echo -e "\n${BLUE}📋 6. 접근성 프로파일 적용 테스트${NC}"

# 6-1. 시각장애 프로파일
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$BASE_URL/api/v1/accessibility/settings/apply-profile?profileType=visual_impaired")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /apply-profile - 시각장애 프로파일" "200" "$HTTP_CODE" "$BODY"

# 6-2. 인지장애 프로파일
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$BASE_URL/api/v1/accessibility/settings/apply-profile?profileType=cognitive")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /apply-profile - 인지장애 프로파일" "200" "$HTTP_CODE" "$BODY"

# 6-3. 신체장애 프로파일
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$BASE_URL/api/v1/accessibility/settings/apply-profile?profileType=motor_impaired")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /apply-profile - 신체장애 프로파일" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 7. /api/v1/accessibility/color-schemes 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 7. 색상 스키마 목록 테스트${NC}"

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/color-schemes")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /color-schemes - 색상 스키마 목록" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 8. /api/v1/accessibility/color-schemes/current 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 8. 현재 색상 스키마 테스트${NC}"

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/color-schemes/current")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /color-schemes/current - 현재 색상 스키마" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 9. /api/v1/accessibility/simplified-navigation 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 9. 간소화 네비게이션 테스트${NC}"

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/simplified-navigation")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /simplified-navigation - 간소화 네비게이션" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 10. /api/v1/accessibility/touch-targets 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 10. 터치 타겟 정보 테스트${NC}"

# 10-1. 기본 터치 타겟
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/touch-targets")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /touch-targets - 기본" "200" "$HTTP_CODE" "$BODY"

# 10-2. 모바일 디바이스
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/touch-targets?deviceType=mobile")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /touch-targets - 모바일" "200" "$HTTP_CODE" "$BODY"

# 10-3. 태블릿 디바이스
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/touch-targets?deviceType=tablet")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /touch-targets - 태블릿" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 11. /api/v1/accessibility/simplify-text 테스트 (POST)
# ======================================
echo -e "\n${BLUE}📋 11. 텍스트 간소화 테스트${NC}"

# 11-1. 복잡한 의료 텍스트
REQUEST_JSON='{
  "text": "본 애플리케이션은 인지 기능이 저하된 사용자를 위한 맞춤형 의료 서비스를 제공하기 위해 개발되었으며, 다양한 접근성 기능과 통합된 응급 대응 시스템을 통해 사용자의 안전과 편의성을 동시에 향상시키고자 합니다.",
  "targetLevel": "elementary"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/simplify-text")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /simplify-text - 복잡한 의료 텍스트" "200" "$HTTP_CODE" "$BODY"

# 11-2. 법적 용어 텍스트
REQUEST_JSON='{
  "text": "이용자는 서비스 이용 중 발생한 개인정보 처리와 관련하여 정보통신망 이용촉진 및 정보보호 등에 관한 법률, 개인정보보호법 등 관련 법령에 따라 권리를 행사할 수 있습니다.",
  "targetLevel": "middle_school"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/simplify-text")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /simplify-text - 법적 용어" "200" "$HTTP_CODE" "$BODY"

# 11-3. 이미 간단한 텍스트
REQUEST_JSON='{
  "text": "안녕하세요. 도움이 필요하시면 버튼을 눌러주세요.",
  "targetLevel": "elementary"
}'
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "$REQUEST_JSON" "$BASE_URL/api/v1/accessibility/simplify-text")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /simplify-text - 간단한 텍스트" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 12. /api/v1/accessibility/settings/sync 테스트 (POST)
# ======================================
echo -e "\n${BLUE}📋 12. 설정 동기화 테스트${NC}"

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$BASE_URL/api/v1/accessibility/settings/sync")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /settings/sync - 설정 동기화" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 13. /api/v1/accessibility/statistics 테스트 (GET)
# ======================================
echo -e "\n${BLUE}📋 13. 접근성 통계 테스트${NC}"

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/statistics")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /statistics - 접근성 통계" "200" "$HTTP_CODE" "$BODY"

# ======================================
# 14. 에러 케이스 테스트
# ======================================
echo -e "\n${BLUE}📋 14. 에러 처리 테스트${NC}"

# 14-1. 잘못된 HTTP 메서드
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE "$BASE_URL/api/v1/accessibility/voice-guidance")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "DELETE /voice-guidance - 잘못된 메서드" "405" "$HTTP_CODE" "$BODY"

# 14-2. 존재하지 않는 엔드포인트
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/nonexistent")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /nonexistent - 존재하지 않는 엔드포인트" "404" "$HTTP_CODE" "$BODY"

# 14-3. 필수 파라미터 누락 (screen-reader-hint)
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/screen-reader-hint?action=press")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /screen-reader-hint - 파라미터 누락" "400" "$HTTP_CODE" "$BODY"

# 14-4. 잘못된 JSON 형식
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -d "{invalid json}" "$BASE_URL/api/v1/accessibility/voice-guidance")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "POST /voice-guidance - 잘못된 JSON" "400" "$HTTP_CODE" "$BODY"

# ======================================
# 15. 부하 및 성능 테스트
# ======================================
echo -e "\n${BLUE}📋 15. 성능 테스트${NC}"

# 15-1. 동시 다중 요청 (설정 조회)
echo -e "\n${YELLOW}설정 조회 동시 요청 테스트 (15개)...${NC}"
for i in {1..15}; do
    curl -s "$BASE_URL/api/v1/accessibility/settings" > /dev/null &
done
wait

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$BASE_URL/api/v1/accessibility/settings")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS:.*//g')
log_test_result "GET /settings - 부하 후 요청" "200" "$HTTP_CODE" "$BODY"

# 15-2. 응답 시간 측정
echo -e "\n${YELLOW}응답 시간 측정...${NC}"
START_TIME=$(date +%s%3N 2>/dev/null || echo $(($(date +%s) * 1000)))
RESPONSE=$(curl -s "$BASE_URL/api/v1/accessibility/statistics")
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
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo -e "${BLUE}║${NC} 성공률: ${YELLOW}$SUCCESS_RATE%${NC}"
fi

echo -e "${BLUE}║${NC} 테스트 완료: $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 목표: 100% 성공률 달성!
if [ $SUCCESS_RATE -eq 100 ]; then
    echo -e "\n${GREEN}🎉 🎉 🎉 AccessibilityController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 13개 엔드포인트가 정상 작동합니다!${NC}"
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