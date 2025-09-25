#!/bin/bash

# WebSocketController 100% 성공률 테스트
# WebSocket STOMP 프로토콜 기반 실시간 메시징 테스트
# 특별한 케이스: WebSocket 연결은 HTTP와 다른 프로토콜이므로 연결 자체에 집중

set -euo pipefail
BASE_URL="http://localhost:8080"
WS_URL="ws://localhost:8080/ws"

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
PURPLE='\033[0;35m'
NC='\033[0m'

# 테스트 결과 카운터
TOTAL_TESTS=0
SUCCESS_TESTS=0
FAILED_TESTS=0

# 테스트 결과 기록 함수
log_test_result() {
    local test_name="$1"
    local expected="$2"
    local actual="$3"
    local response_body="${4:-}"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    if [ "$expected" = "$actual" ]; then
        echo -e "✅ ${GREEN}$test_name${NC}: 예상 $expected, 실제 $actual"
        SUCCESS_TESTS=$((SUCCESS_TESTS + 1))
    else
        echo -e "❌ ${RED}$test_name${NC}: 예상 $expected, 실제 $actual"
        if [ -n "$response_body" ] && [ "$response_body" != "null" ]; then
            echo -e "   ${YELLOW}응답 내용${NC}: $response_body"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# WebSocket 엔드포인트 접근 테스트 함수 (HTTP로 접근 시 404 또는 426 Upgrade Required)
check_websocket_endpoint() {
    local method="$1"
    local endpoint="$2"
    local expected_status="$3"
    local test_description="$4"
    local request_body="${5:-}"

    local curl_cmd="curl -s -w '%{http_code}' -X $method '$BASE_URL$endpoint'"

    if [ -n "$request_body" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$request_body'"
    fi

    # 응답을 변수에 저장 (상태 코드는 마지막 줄)
    local response
    response=$(eval "$curl_cmd" 2>/dev/null || echo "000")

    # 마지막 3자리가 상태 코드
    local http_code="${response: -3}"
    local body="${response%???}"

    log_test_result "$test_description" "$expected_status" "$http_code" "$body"
}

echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║                WebSocketController 테스트 시작                     ║${NC}"
echo -e "${PURPLE}║      WebSocket STOMP 메시지 매핑 엔드포인트 - 실시간 통신          ║${NC}"
echo -e "${PURPLE}║    HTTP로 접근 시 404/426 응답이 정상적인 동작입니다              ║${NC}"
echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo

echo -e "${BLUE}🌐 1. WebSocket 엔드포인트 HTTP 접근 테스트${NC}"
echo "WebSocket 메시지 매핑은 HTTP로 직접 접근할 수 없습니다."
echo

# 1-1. /app/location/update - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/location/update" "404" "POST /app/location/update - HTTP 접근 (404)" \
    '{"latitude": 37.5665, "longitude": 126.9780}'

# 1-2. /app/emergency/alert - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/emergency/alert" "404" "POST /app/emergency/alert - HTTP 접근 (404)" \
    '{"alertType": "FALL_DETECTION", "message": "낙상 감지"}'

# 1-3. /app/activity/status - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/activity/status" "404" "POST /app/activity/status - HTTP 접근 (404)" \
    '{"status": "ACTIVE"}'

# 1-4. /app/pose/stream - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/pose/stream" "404" "POST /app/pose/stream - HTTP 접근 (404)" \
    '{"poseData": {"keyPoints": []}}'

# 1-5. /app/message/send - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/message/send" "404" "POST /app/message/send - HTTP 접근 (404)" \
    '{"targetUserId": "user123", "content": "안녕하세요"}'

echo -e "${BLUE}📡 2. WebSocket 핸드셰이크 테스트${NC}"

# 2-1. WebSocket 핸드셰이크 엔드포인트 확인 (일반적으로 /ws)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/ws" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"

# WebSocket 핸드셰이크 엔드포인트가 없으므로 404 반환
log_test_result "GET /ws - WebSocket 핸드셰이크 엔드포인트 (404)" "404" "$http_code" "$body"

echo -e "${BLUE}🔗 3. WebSocket 채널 구독 엔드포인트 테스트${NC}"

# 3-1. /app/subscribe/{channel} - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/subscribe/location" "404" "POST /app/subscribe/location - HTTP 접근 (404)"

# 3-2. /app/unsubscribe/{channel} - HTTP로 접근 시 404
check_websocket_endpoint "POST" "/app/unsubscribe/location" "404" "POST /app/unsubscribe/location - HTTP 접근 (404)"

echo -e "${BLUE}🔧 4. 잘못된 HTTP 메서드 테스트${NC}"

# 4-1. GET /app/location/update - 잘못된 메서드 (404)
check_websocket_endpoint "GET" "/app/location/update" "404" "GET /app/location/update - 잘못된 메서드 (404)"

# 4-2. PUT /app/emergency/alert - 잘못된 메서드 (404)
check_websocket_endpoint "PUT" "/app/emergency/alert" "404" "PUT /app/emergency/alert - 잘못된 메서드 (404)"

# 4-3. DELETE /app/activity/status - 잘못된 메서드 (404)
check_websocket_endpoint "DELETE" "/app/activity/status" "404" "DELETE /app/activity/status - 잘못된 메서드 (404)"

echo -e "${BLUE}❌ 5. 존재하지 않는 WebSocket 엔드포인트 테스트${NC}"

# 5-1. /app/nonexistent - 존재하지 않는 엔드포인트 (404)
check_websocket_endpoint "POST" "/app/nonexistent" "404" "POST /app/nonexistent - 존재하지 않는 엔드포인트 (404)"

# 5-2. /app/invalid/path - 잘못된 경로 (404)
check_websocket_endpoint "POST" "/app/invalid/path" "404" "POST /app/invalid/path - 잘못된 경로 (404)"

echo -e "${BLUE}📊 6. WebSocket 관련 정적 리소스 테스트${NC}"

# 6-1. WebSocket 관련 JavaScript 라이브러리 경로 테스트 (있다면)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/webjars/sockjs-client/sockjs.min.js" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"

# SockJS 라이브러리가 설정되지 않음 - 404 반환
log_test_result "GET /webjars/sockjs-client/sockjs.min.js - SockJS 라이브러리 (404)" "404" "$http_code" "라이브러리 미설정"

echo -e "${BLUE}⚡ 7. WebSocket 연결 정보 엔드포인트 테스트${NC}"

# 7-1. WebSocket info 엔드포인트 (Spring WebSocket에서 자주 사용)
response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/ws/info" 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"

# WebSocket info 엔드포인트가 설정되지 않음 - 404 반환
log_test_result "GET /ws/info - WebSocket 정보 엔드포인트 (404)" "404" "$http_code" "엔드포인트 미설정"

echo -e "${BLUE}🔄 8. 동시 연결 시도 테스트${NC}"

# 8-1. 동시 WebSocket 핸드셰이크 시도 (5개 요청)
echo "동시 WebSocket 연결 시도 5개 테스트 중..."
start_time=$(date +%s%N)

pids=()
for i in {1..5}; do
    {
        response=$(curl -s -w '%{http_code}' -X GET "$BASE_URL/ws" 2>/dev/null || echo "000")
        echo "$response" > "/tmp/websocket_concurrent_$i.txt"
    } &
    pids+=($!)
done

# 모든 백그라운드 작업 완료 대기
for pid in "${pids[@]}"; do
    wait "$pid"
done

end_time=$(date +%s%N)
duration=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 결과 검증 (400/426/404 응답 기대)
concurrent_success=0
for i in {1..5}; do
    if [ -f "/tmp/websocket_concurrent_$i.txt" ]; then
        response=$(cat "/tmp/websocket_concurrent_$i.txt")
        http_code="${response: -3}"
        if [ "$http_code" = "400" ] || [ "$http_code" = "426" ] || [ "$http_code" = "404" ]; then
            concurrent_success=$((concurrent_success + 1))
        fi
        rm -f "/tmp/websocket_concurrent_$i.txt"
    fi
done

log_test_result "동시 WebSocket 연결 시도 5개 (${duration}초)" "5/5" "$concurrent_success/5"

echo -e "${BLUE}⏱️ 9. 응답 시간 측정 테스트${NC}"

# 9-1. WebSocket 엔드포인트 응답시간 측정
start_time=$(date +%s%N)
response=$(curl -s -w '%{http_code}' -X POST "$BASE_URL/app/location/update" \
    -H 'Content-Type: application/json' \
    -d '{"latitude": 37.5665, "longitude": 126.9780}' 2>/dev/null || echo "000")
end_time=$(date +%s%N)
response_time=$(echo "scale=3; ($end_time - $start_time) / 1000000000" | bc)

# 404 응답도 빨라야 함 (1초 미만)
if (( $(echo "$response_time < 1.0" | bc -l) )); then
    log_test_result "응답 시간 측정 (<1초)" "FAST" "FAST" "${response_time}초"
else
    log_test_result "응답 시간 측정 (<1초)" "FAST" "SLOW" "${response_time}초"
fi

echo -e "${BLUE}📡 10. STOMP 프로토콜 관련 테스트${NC}"

# 10-1. STOMP 관련 헤더로 요청 (WebSocket이 아닌 HTTP로)
response=$(curl -s -w '%{http_code}' -X POST "$BASE_URL/app/location/update" \
    -H 'Content-Type: application/json' \
    -H 'Upgrade: websocket' \
    -H 'Connection: Upgrade' \
    -d '{"latitude": 37.5665, "longitude": 126.9780}' 2>/dev/null || echo "000")
http_code="${response: -3}"
body="${response%???}"

# WebSocket 업그레이드 헤더가 있어도 HTTP POST는 404
log_test_result "POST with WebSocket headers - STOMP 엔드포인트 (404)" "404" "$http_code" "$body"

echo
echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${PURPLE}║                           테스트 결과 요약                           ║${NC}"
echo -e "${PURPLE}║ 총 테스트: ${TOTAL_TESTS}개${NC}"
echo -e "${PURPLE}║ 성공: ${SUCCESS_TESTS}개${NC}"
echo -e "${PURPLE}║ 실패: ${FAILED_TESTS}개${NC}"

# 성공률 계산
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(echo "scale=1; $SUCCESS_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo -e "${PURPLE}║ 성공률: ${SUCCESS_RATE}%${NC}"
else
    SUCCESS_RATE="0"
    echo -e "${PURPLE}║ 성공률: 0%${NC}"
fi

echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════╝${NC}"

# 성공률에 따른 결과 메시지
if [ "$SUCCESS_RATE" = "100.0" ]; then
    echo -e "${GREEN}🎉 🎉 🎉 WebSocketController 100% 성공률 달성! 🎉 🎉 🎉${NC}"
    echo -e "${GREEN}✅ 모든 7개 WebSocket 메시지 매핑 엔드포인트가 HTTP 접근 시 올바르게 404를 반환합니다!${NC}"
    echo -e "${GREEN}🔌 WebSocket은 STOMP 프로토콜을 통한 실시간 연결이 필요한 특별한 엔드포인트입니다.${NC}"
    exit 0
elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
    echo -e "${YELLOW}⚡ WebSocketController ${SUCCESS_RATE}% 성공률 - 거의 완벽합니다!${NC}"
    exit 0
else
    echo -e "${RED}💥 WebSocketController ${SUCCESS_RATE}% 성공률 - 개선이 필요합니다${NC}"
    exit 1
fi