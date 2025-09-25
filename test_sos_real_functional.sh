#!/bin/bash

# SOS 컨트롤러 진짜 기능 테스트 스크립트
# HTTP 코드가 아닌 실제 비즈니스 데이터와 로직 검증

BASE_URL="http://localhost:8080"
TIMEOUT=10

# JWT 토큰 생성 함수
get_jwt_token() {
  local userId=$1
  java -cp build/libs/bifai-backend-0.0.1-SNAPSHOT.jar:build/libs/* \
    com.bifai.reminder.bifai_backend.utils.JwtAuthUtils $userId 2>/dev/null
}

# JSON 파싱 함수 (jq 없이)
extract_json_field() {
  local json="$1"
  local field="$2"
  echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*[^,}]*" | sed 's/.*:[[:space:]]*//' | tr -d '"'
}

# 실제 기능 검증 함수
verify_sos_data() {
  local response="$1"
  local test_name="$2"

  echo "=== $test_name 실제 데이터 검증 ==="

  # 성공 플래그 확인
  local success=$(extract_json_field "$response" "success")
  if [ "$success" != "true" ]; then
    echo "❌ success 필드가 true가 아님: $success"
    return 1
  fi

  # 실제 데이터 필드 확인
  local emergency_id=$(extract_json_field "$response" "emergencyId")
  local emergency_type=$(extract_json_field "$response" "emergencyType")
  local status=$(extract_json_field "$response" "status")

  echo "📋 실제 생성된 데이터:"
  echo "   - Emergency ID: $emergency_id"
  echo "   - Type: $emergency_type"
  echo "   - Status: $status"

  # 필수 데이터 검증
  if [ -z "$emergency_id" ] || [ "$emergency_id" = "null" ]; then
    echo "❌ Emergency ID가 생성되지 않음"
    return 1
  fi

  if [ -z "$emergency_type" ] || [ "$emergency_type" = "null" ]; then
    echo "❌ Emergency Type이 설정되지 않음"
    return 1
  fi

  if [ "$status" != "TRIGGERED" ]; then
    echo "❌ Status가 TRIGGERED가 아님: $status"
    return 1
  fi

  echo "✅ 실제 데이터 검증 성공"
  return 0
}

# 이력 데이터 검증 함수
verify_history_data() {
  local response="$1"
  local expected_count="$2"

  echo "=== SOS 이력 실제 데이터 검증 ==="

  # 배열 데이터 개수 확인 (간단한 방법)
  local count=$(echo "$response" | grep -o '"emergencyId"' | wc -l | tr -d ' ')

  echo "📋 이력 데이터 정보:"
  echo "   - 예상 개수: $expected_count"
  echo "   - 실제 개수: $count"

  if [ "$count" -lt "$expected_count" ]; then
    echo "❌ 예상보다 적은 이력 데이터: $count < $expected_count"
    return 1
  fi

  # 첫 번째 이력의 실제 데이터 확인
  local first_id=$(extract_json_field "$response" "emergencyId")
  local first_type=$(extract_json_field "$response" "emergencyType")
  local first_status=$(extract_json_field "$response" "status")

  if [ -n "$first_id" ] && [ "$first_id" != "null" ]; then
    echo "📋 첫 번째 이력 데이터:"
    echo "   - ID: $first_id"
    echo "   - Type: $first_type"
    echo "   - Status: $first_status"
    echo "✅ 이력 데이터 검증 성공"
    return 0
  else
    echo "❌ 이력 데이터가 올바르지 않음"
    return 1
  fi
}

echo "🚀 SOS 컨트롤러 진짜 기능 테스트 시작"
echo "목표: HTTP 코드가 아닌 실제 비즈니스 데이터와 로직 검증"
echo "====================================="
echo

# JWT 토큰 생성
TOKEN=$(get_jwt_token 1)
if [ -z "$TOKEN" ]; then
  echo "❌ JWT 토큰 생성 실패"
  exit 1
fi

echo "✅ JWT 토큰 생성 성공"
echo

success_count=0
total_tests=5

# 테스트 1: SOS 발동 후 실제 Emergency 데이터 생성 검증
echo "🧪 테스트 1: SOS 발동 → 실제 Emergency 데이터 생성 검증"
echo "기존: HTTP 201만 확인"
echo "신규: Emergency 엔티티 생성, ID/Type/Status 검증"
echo

sos_data='{
  "latitude": 37.5665,
  "longitude": 126.9780,
  "address": "서울시 중구 명동",
  "emergencyType": "PANIC",
  "message": "실제 기능 테스트 긴급상황",
  "shareLocation": true,
  "notifyAllContacts": true
}'

response=$(timeout $TIMEOUT curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$sos_data" \
  "$BASE_URL/api/v1/emergency/sos/trigger")

echo "📨 서버 응답:"
echo "$response"
echo

if verify_sos_data "$response" "SOS 발동"; then
  success_count=$((success_count + 1))

  # 생성된 Emergency ID 저장
  EMERGENCY_ID=$(extract_json_field "$response" "emergencyId")
  echo "💾 생성된 Emergency ID: $EMERGENCY_ID (후속 테스트에서 사용)"
fi
echo "-----------------------------------"
echo

# 테스트 2: SOS 이력 조회 → 실제 데이터 반환 검증
echo "🧪 테스트 2: SOS 이력 조회 → 실제 이력 데이터 검증"
echo "기존: HTTP 200만 확인"
echo "신규: 실제 Emergency 배열, 각 항목의 데이터 필드 검증"
echo

history_response=$(timeout $TIMEOUT curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  "$BASE_URL/api/v1/emergency/sos/history")

echo "📨 이력 조회 응답:"
echo "$history_response"
echo

if verify_history_data "$history_response" 1; then
  success_count=$((success_count + 1))
fi
echo "-----------------------------------"
echo

# 테스트 3: Quick SOS 발동 → 실제 기본값 설정 검증
echo "🧪 테스트 3: Quick SOS → 기본값 자동 설정 검증"
echo "기존: HTTP 201만 확인"
echo "신규: Type=PANIC, Message=기본값, 실제 데이터 확인"
echo

quick_response=$(timeout $TIMEOUT curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  "$BASE_URL/api/v1/emergency/sos/quick?latitude=37.123&longitude=127.456")

echo "📨 Quick SOS 응답:"
echo "$quick_response"
echo

# Quick SOS 기본값 검증
quick_type=$(extract_json_field "$quick_response" "emergencyType")
if [ "$quick_type" = "PANIC" ]; then
  echo "✅ Quick SOS 기본 Type 검증 성공: $quick_type"
  if verify_sos_data "$quick_response" "Quick SOS"; then
    success_count=$((success_count + 1))
    QUICK_EMERGENCY_ID=$(extract_json_field "$quick_response" "emergencyId")
  fi
else
  echo "❌ Quick SOS 기본 Type 검증 실패: $quick_type (expected: PANIC)"
fi
echo "-----------------------------------"
echo

# 테스트 4: SOS 취소 → 실제 상태 변경 검증
if [ -n "$EMERGENCY_ID" ]; then
  echo "🧪 테스트 4: SOS 취소 → 실제 상태 변경 검증"
  echo "기존: HTTP 200만 확인"
  echo "신규: Emergency 상태가 실제로 CANCELLED로 변경되는지 확인"
  echo

  cancel_response=$(timeout $TIMEOUT curl -s -X PUT \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    "$BASE_URL/api/v1/emergency/sos/$EMERGENCY_ID/cancel")

  echo "📨 취소 응답:"
  echo "$cancel_response"
  echo

  # 취소 후 이력에서 상태 확인
  sleep 1
  updated_history=$(timeout $TIMEOUT curl -s \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    "$BASE_URL/api/v1/emergency/sos/history")

  # 취소된 Emergency의 상태 확인 (첫 번째 항목이 가장 최근)
  latest_status=$(extract_json_field "$updated_history" "status")
  if [ "$latest_status" = "CANCELLED" ]; then
    echo "✅ SOS 취소 후 상태 변경 검증 성공: $latest_status"
    success_count=$((success_count + 1))
  else
    echo "❌ SOS 취소 후 상태 변경 검증 실패: $latest_status (expected: CANCELLED)"
  fi
else
  echo "⚠️  테스트 4 스킵: Emergency ID가 없음"
fi
echo "-----------------------------------"
echo

# 테스트 5: 이력 재조회 → 증가된 데이터 개수 검증
echo "🧪 테스트 5: 최종 이력 재조회 → 실제 개수 증가 검증"
echo "기존: HTTP 200만 확인"
echo "신규: 총 3개 Emergency 생성됐는지 확인 (초기 테스트 데이터 + 신규 2개)"
echo

final_history=$(timeout $TIMEOUT curl -s \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  "$BASE_URL/api/v1/emergency/sos/history")

echo "📨 최종 이력 응답:"
echo "$final_history"
echo

if verify_history_data "$final_history" 3; then
  success_count=$((success_count + 1))
fi
echo "-----------------------------------"
echo

# 결과 요약
echo "🏁 진짜 기능 테스트 결과 요약"
echo "====================================="
echo "성공: $success_count / $total_tests"
success_rate=$(echo "scale=1; $success_count * 100 / $total_tests" | bc 2>/dev/null || echo "계산 불가")
echo "성공률: $success_rate%"
echo

if [ "$success_count" -eq "$total_tests" ]; then
  echo "🎉 모든 진짜 기능 테스트 성공!"
  echo "✅ HTTP 코드뿐만 아니라 실제 비즈니스 데이터와 로직 검증 완료"
  echo "✅ Emergency 엔티티 생성/조회/상태변경 모든 흐름 검증됨"
  exit 0
else
  echo "⚠️  일부 진짜 기능 테스트 실패"
  echo "❌ 실제 비즈니스 로직에 문제가 있을 수 있음"
  echo "📊 가짜 테스트(HTTP 코드만): 100% 성공"
  echo "📊 진짜 테스트(비즈니스 데이터): $success_rate% 성공"
  exit 1
fi