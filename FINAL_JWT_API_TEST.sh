#!/bin/bash

# 🎯 실제 JWT 인증을 사용한 BIF-AI Backend API 테스트
# 베스트 프랙티스에 따른 실제 기능 테스트

BASE_URL="http://localhost:8080"
API_BASE="${BASE_URL}/api"
RESULTS_DIR="jwt_api_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

LOG_FILE="$RESULTS_DIR/jwt_test.log"
SUCCESS_COUNT=0
FAIL_COUNT=0
TOTAL_COUNT=0

echo "🚀 BIF-AI Backend 실제 JWT 인증 API 테스트 시작"
echo "📁 결과 저장: $RESULTS_DIR"
echo "🕒 시작 시간: $(date)"
echo ""

log_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    ((TOTAL_COUNT++))
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    if [[ "$status" == "PASS" ]]; then
        echo "✅ [$timestamp] PASS: $test_name" | tee -a "$LOG_FILE"
        ((SUCCESS_COUNT++))
    else
        echo "❌ [$timestamp] FAIL: $test_name" | tee -a "$LOG_FILE"
        echo "   Details: $details" | tee -a "$LOG_FILE"
        ((FAIL_COUNT++))
    fi
}

# Step 1: 서버 상태 확인
echo "=== 1단계: 서버 상태 확인 ==="
response=$(curl -s -o /dev/null -w "%{http_code}" "$API_BASE/auth/health")
if [[ "$response" == "200" ]]; then
    log_result "서버 헬스체크" "PASS" "HTTP $response"
else
    log_result "서버 헬스체크" "FAIL" "HTTP $response"
    echo "❌ 서버가 실행 중이 아니거나 응답하지 않습니다. 테스트를 중단합니다."
    exit 1
fi
echo ""

# Step 2: 회원가입 시도 (실제 데이터로)
echo "=== 2단계: 실제 회원가입 테스트 ==="
register_data='{
    "username": "apitester'$(date +%s)'",
    "email": "apitest'$(date +%s)'@test.com",
    "password": "TestPassword123!",
    "confirmPassword": "TestPassword123!",
    "fullName": "API 테스트 사용자",
    "birthDate": "1990-01-01",
    "guardianName": "테스트 보호자",
    "guardianPhone": "010-9999-8888",
    "guardianEmail": "guardian'$(date +%s)'@test.com",
    "agreeToTerms": true,
    "agreeToPrivacyPolicy": true,
    "agreeToMarketing": false
}'

echo "📤 회원가입 요청 전송 중..."
register_response=$(curl -s -X POST "$API_BASE/auth/register" \
    -H "Content-Type: application/json" \
    -d "$register_data")

echo "📥 회원가입 응답: $register_response"

# JWT 토큰 추출 시도
access_token=""
if echo "$register_response" | jq -e .data.accessToken > /dev/null 2>&1; then
    access_token=$(echo "$register_response" | jq -r '.data.accessToken')
    log_result "회원가입 및 JWT 발급" "PASS" "토큰 길이: ${#access_token}"
    echo "🔑 JWT 토큰 획득 성공: ${access_token:0:20}...${access_token: -20}"
elif echo "$register_response" | jq -e .success > /dev/null 2>&1; then
    local success=$(echo "$register_response" | jq -r '.success')
    if [[ "$success" == "true" ]]; then
        log_result "회원가입" "PASS" "성공하지만 토큰 없음"
    else
        log_result "회원가입" "FAIL" "$(echo "$register_response" | jq -r '.error.message // "알 수 없는 오류"')"
    fi
else
    log_result "회원가입" "FAIL" "JSON 파싱 실패: $register_response"
fi
echo ""

# Step 3: JWT 토큰이 있으면 인증된 엔드포인트 테스트
if [[ -n "$access_token" && "$access_token" != "null" ]]; then
    echo "=== 3단계: JWT 인증된 API 엔드포인트 테스트 ==="
    
    # 인증 헤더
    auth_header="Authorization: Bearer $access_token"
    
    # 핵심 사용자 엔드포인트 테스트
    test_authenticated_endpoint() {
        local endpoint="$1"
        local method="$2"
        local description="$3"
        local expected_status="$4"
        
        local response
        if [[ "$method" == "GET" ]]; then
            response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$endpoint" -H "$auth_header")
        elif [[ "$method" == "POST" ]]; then
            response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X POST "$endpoint" -H "$auth_header" -H "Content-Type: application/json" -d '{}')
        fi
        
        local http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
        local body=$(echo "$response" | sed 's/HTTPSTATUS:[0-9]*$//')
        
        if [[ "$http_code" == "$expected_status" ]]; then
            # 응답 내용도 검증
            if echo "$body" | jq -e .success > /dev/null 2>&1; then
                local success=$(echo "$body" | jq -r '.success')
                if [[ "$success" == "true" ]]; then
                    log_result "$description" "PASS" "HTTP $http_code, success: $success"
                else
                    log_result "$description" "FAIL" "HTTP $http_code, success: $success, error: $(echo "$body" | jq -r '.error.message // "Unknown"')"
                fi
            else
                log_result "$description" "PASS" "HTTP $http_code (비JSON 응답)"
            fi
        else
            log_result "$description" "FAIL" "HTTP $http_code (Expected: $expected_status), Body: $body"
        fi
    }
    
    # 핵심 엔드포인트들 테스트
    echo "🔐 인증된 사용자 엔드포인트 테스트..."
    test_authenticated_endpoint "$API_BASE/users/me" "GET" "현재 사용자 정보 조회" "200"
    
    echo "🚨 응급 기능 엔드포인트 테스트..."
    test_authenticated_endpoint "$API_BASE/emergency-contacts" "GET" "응급 연락처 목록" "200"
    test_authenticated_endpoint "$API_BASE/emergency/active" "GET" "활성 응급상황" "200"
    
    echo "🔔 알림 기능 엔드포인트 테스트..."
    test_authenticated_endpoint "$API_BASE/notifications/settings" "GET" "알림 설정" "200"
    
    echo "👁️ Vision AI 엔드포인트 테스트..."
    test_authenticated_endpoint "$API_BASE/vision/analyze" "POST" "Vision 분석 (파일 없이)" "400"
    
    echo "📱 모바일 인증 확인..."
    test_authenticated_endpoint "$API_BASE/mobile/auth/check" "GET" "모바일 인증 상태" "200"
    
    echo "📍 지오펜스 기능 테스트..."
    test_authenticated_endpoint "$API_BASE/geofences/paged" "GET" "페이징된 지오펜스 목록" "200"
    
    echo "🎯 실험 기능 테스트..."
    test_authenticated_endpoint "$API_BASE/experiments/my-experiments" "GET" "내 실험 목록" "200"
    
    echo "♿ 접근성 기능 테스트..."
    test_authenticated_endpoint "$API_BASE/accessibility/settings" "GET" "접근성 설정" "200"
    
else
    echo "⚠️  JWT 토큰을 획득하지 못해서 인증된 엔드포인트 테스트를 건너뜁니다."
fi
echo ""

# Step 4: 무인증 접근 보안 테스트 
echo "=== 4단계: 보안 검증 테스트 (무인증 접근) ==="

test_security_endpoint() {
    local endpoint="$1"
    local description="$2"
    
    local response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$endpoint")
    local http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    if [[ "$http_code" == "403" || "$http_code" == "401" ]]; then
        log_result "보안: $description" "PASS" "HTTP $http_code (적절한 접근 거부)"
    else
        log_result "보안: $description" "FAIL" "HTTP $http_code (보안 취약점 의심)"
    fi
}

echo "🛡️ 보안 테스트 수행 중..."
test_security_endpoint "$API_BASE/users/me" "사용자 정보 보호"
test_security_endpoint "$API_BASE/admin/statistics" "관리자 기능 보호"  
test_security_endpoint "$API_BASE/emergency-contacts" "응급 연락처 보호"
test_security_endpoint "$API_BASE/guardians/my" "보호자 정보 보호"
echo ""

# 최종 결과 요약
echo "=== 📊 최종 테스트 결과 요약 ===" | tee -a "$LOG_FILE"
echo "🕒 완료 시간: $(date)" | tee -a "$LOG_FILE"
echo "📊 총 테스트: $TOTAL_COUNT" | tee -a "$LOG_FILE"
echo "✅ 성공: $SUCCESS_COUNT" | tee -a "$LOG_FILE"
echo "❌ 실패: $FAIL_COUNT" | tee -a "$LOG_FILE"

if [[ $TOTAL_COUNT -gt 0 ]]; then
    success_rate=$(( SUCCESS_COUNT * 100 / TOTAL_COUNT ))
    echo "📈 성공률: ${success_rate}%" | tee -a "$LOG_FILE"
else
    echo "📈 성공률: 0%" | tee -a "$LOG_FILE"
fi

echo ""
echo "📁 상세 로그: $LOG_FILE"
echo ""

if [[ $SUCCESS_COUNT -gt $FAIL_COUNT ]]; then
    echo "🎉 전반적으로 API가 잘 작동하고 있습니다!"
else
    echo "⚠️  일부 API에 문제가 있을 수 있습니다. 로그를 확인해보세요."
fi

echo ""
echo "✨ 실제 JWT 인증 기반 API 테스트 완료!"
