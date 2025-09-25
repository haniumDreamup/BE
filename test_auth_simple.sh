#!/bin/bash

# 간단한 인증 테스트 - 401/400 상태 코드 검증

BASE_URL="http://localhost:8080"

echo "=== 간단한 인증 및 유효성 검증 테스트 ==="

# 1. 인증이 필요한 엔드포인트 - 401 테스트
echo "1. 통계 엔드포인트 인증 없이 접근:"
curl -s -w "\nStatus: %{http_code}\n\n" "$BASE_URL/api/statistics/geofence"

# 2. 잘못된 날짜 형식 - 400 테스트
echo "2. 잘못된 날짜 형식:"
curl -s -w "\nStatus: %{http_code}\n\n" "$BASE_URL/api/statistics/geofence?startDate=invalid-date"

# 3. 공개 엔드포인트 확인
echo "3. 공개 엔드포인트 (health):"
curl -s -w "\nStatus: %{http_code}\n\n" "$BASE_URL/api/health"
