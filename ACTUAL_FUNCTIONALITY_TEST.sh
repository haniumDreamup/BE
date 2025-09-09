#!/bin/bash

# BIF-AI Backend 실제 기능 테스트 스크립트
BASE_URL="http://localhost:8080/api/v1"
RESULTS_DIR="actual_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "🔥 BIF-AI Backend 실제 기능 테스트 시작..."
echo "📁 결과 저장 경로: $RESULTS_DIR"

# 로그 파일
LOG_FILE="$RESULTS_DIR/actual_test.log"
SUMMARY_FILE="$RESULTS_DIR/summary.txt"

# 테스트 결과 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 로그 함수
log_test() {
    local test_name="$1"
    local status="$2"
    local response="$3"
    
    echo "[$status] $test_name" | tee -a "$LOG_FILE"
    if [ "$status" = "PASS" ]; then
        ((PASSED_TESTS++))
        echo "✅ $test_name" >> "$SUMMARY_FILE"
    else
        ((FAILED_TESTS++))
        echo "❌ $test_name" >> "$SUMMARY_FILE"
        echo "   Error: $response" >> "$SUMMARY_FILE"
    fi
    ((TOTAL_TESTS++))
}

# JWT 토큰 변수
JWT_TOKEN=""

echo "=== 1단계: 회원가입 및 로그인 테스트 ===" | tee -a "$LOG_FILE"

# 1. 회원가입 테스트
echo "Testing: 회원가입..." | tee -a "$LOG_FILE"
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!@#",
    "name": "테스트 사용자",
    "birth": "1990-01-01",
    "gender": "MALE",
    "cognitiveLevel": "MILD",
    "guardianName": "보호자",
    "guardianPhone": "010-1234-5678"
  }' -w "%{http_code}")

HTTP_CODE="${REGISTER_RESPONSE: -3}"
RESPONSE_BODY="${REGISTER_RESPONSE%???}"

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    log_test "회원가입" "PASS" "$HTTP_CODE"
else
    log_test "회원가입" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
fi

# 2. 로그인 테스트 및 JWT 토큰 획득
echo "Testing: 로그인..." | tee -a "$LOG_FILE"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!@#"
  }' -w "%{http_code}")

HTTP_CODE="${LOGIN_RESPONSE: -3}"
RESPONSE_BODY="${LOGIN_RESPONSE%???}"

if [ "$HTTP_CODE" = "200" ]; then
    log_test "로그인" "PASS" "$HTTP_CODE"
    # JWT 토큰 추출 시도
    JWT_TOKEN=$(echo "$RESPONSE_BODY" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    if [ -n "$JWT_TOKEN" ]; then
        echo "JWT 토큰 획득 성공: ${JWT_TOKEN:0:20}..." | tee -a "$LOG_FILE"
    else
        echo "JWT 토큰 추출 실패" | tee -a "$LOG_FILE"
    fi
else
    log_test "로그인" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
fi

echo "=== 2단계: 인증이 필요한 API 테스트 ===" | tee -a "$LOG_FILE"

if [ -n "$JWT_TOKEN" ]; then
    AUTH_HEADER="Authorization: Bearer $JWT_TOKEN"
    
    # 3. 현재 사용자 정보 조회
    echo "Testing: 현재 사용자 정보 조회..." | tee -a "$LOG_FILE"
    USER_INFO_RESPONSE=$(curl -s -X GET "$BASE_URL/users/current" \
      -H "$AUTH_HEADER" -w "%{http_code}")
    
    HTTP_CODE="${USER_INFO_RESPONSE: -3}"
    RESPONSE_BODY="${USER_INFO_RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_test "사용자 정보 조회" "PASS" "$HTTP_CODE"
    else
        log_test "사용자 정보 조회" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
    fi
    
    # 4. 알림 목록 조회
    echo "Testing: 알림 목록 조회..." | tee -a "$LOG_FILE"
    REMINDERS_RESPONSE=$(curl -s -X GET "$BASE_URL/reminders" \
      -H "$AUTH_HEADER" -w "%{http_code}")
    
    HTTP_CODE="${REMINDERS_RESPONSE: -3}"
    RESPONSE_BODY="${REMINDERS_RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_test "알림 목록 조회" "PASS" "$HTTP_CODE"
    else
        log_test "알림 목록 조회" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
    fi
    
    # 5. 알림 생성 테스트
    echo "Testing: 알림 생성..." | tee -a "$LOG_FILE"
    CREATE_REMINDER_RESPONSE=$(curl -s -X POST "$BASE_URL/reminders" \
      -H "$AUTH_HEADER" \
      -H "Content-Type: application/json" \
      -d '{
        "title": "테스트 알림",
        "content": "이것은 테스트 알림입니다",
        "reminderTime": "2024-12-31T23:59:59",
        "repeatType": "NONE"
      }' -w "%{http_code}")
    
    HTTP_CODE="${CREATE_REMINDER_RESPONSE: -3}"
    RESPONSE_BODY="${CREATE_REMINDER_RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
        log_test "알림 생성" "PASS" "$HTTP_CODE"
    else
        log_test "알림 생성" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
    fi
    
    # 6. 응급 연락처 조회
    echo "Testing: 응급 연락처 조회..." | tee -a "$LOG_FILE"
    EMERGENCY_RESPONSE=$(curl -s -X GET "$BASE_URL/emergency-contacts" \
      -H "$AUTH_HEADER" -w "%{http_code}")
    
    HTTP_CODE="${EMERGENCY_RESPONSE: -3}"
    RESPONSE_BODY="${EMERGENCY_RESPONSE%???}"
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_test "응급 연락처 조회" "PASS" "$HTTP_CODE"
    else
        log_test "응급 연락처 조회" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
    fi
    
else
    echo "JWT 토큰이 없어서 인증이 필요한 API 테스트를 건너뜁니다." | tee -a "$LOG_FILE"
fi

echo "=== 3단계: 헬스체크 테스트 ===" | tee -a "$LOG_FILE"

# 7. 일반 헬스체크
echo "Testing: 일반 헬스체크..." | tee -a "$LOG_FILE"
HEALTH_RESPONSE=$(curl -s -X GET "$BASE_URL/../health" -w "%{http_code}")
HTTP_CODE="${HEALTH_RESPONSE: -3}"
RESPONSE_BODY="${HEALTH_RESPONSE%???}"

if [ "$HTTP_CODE" = "200" ]; then
    log_test "일반 헬스체크" "PASS" "$HTTP_CODE"
else
    log_test "일반 헬스체크" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
fi

# 8. 인증 헬스체크
echo "Testing: 인증 헬스체크..." | tee -a "$LOG_FILE"
AUTH_HEALTH_RESPONSE=$(curl -s -X GET "$BASE_URL/auth/health" -w "%{http_code}")
HTTP_CODE="${AUTH_HEALTH_RESPONSE: -3}"
RESPONSE_BODY="${AUTH_HEALTH_RESPONSE%???}"

if [ "$HTTP_CODE" = "200" ]; then
    log_test "인증 헬스체크" "PASS" "$HTTP_CODE"
else
    log_test "인증 헬스체크" "FAIL" "HTTP $HTTP_CODE: $RESPONSE_BODY"
fi

# 최종 결과 요약
echo "" | tee -a "$SUMMARY_FILE"
echo "=== 최종 테스트 결과 ===" | tee -a "$SUMMARY_FILE"
echo "총 테스트: $TOTAL_TESTS" | tee -a "$SUMMARY_FILE"
echo "성공: $PASSED_TESTS" | tee -a "$SUMMARY_FILE"
echo "실패: $FAILED_TESTS" | tee -a "$SUMMARY_FILE"
echo "성공률: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%" | tee -a "$SUMMARY_FILE"

echo "" | tee -a "$LOG_FILE"
echo "🎯 실제 기능 테스트 완료!" | tee -a "$LOG_FILE"
echo "📋 상세 결과는 $SUMMARY_FILE 파일을 확인하세요." | tee -a "$LOG_FILE"

# 결과 요약 출력
cat "$SUMMARY_FILE"
