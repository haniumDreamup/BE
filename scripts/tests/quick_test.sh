#!/bin/bash

BASE_URL="http://43.200.49.171:8080"

echo "=== 핵심 엔드포인트 빠른 테스트 ==="

# 공개 엔드포인트들
echo "1. Health 체크"
curl -s "$BASE_URL/api/health" | head -c 100
echo -e "\n"

echo "2. OAuth2 로그인 URLs"  
curl -s "$BASE_URL/api/auth/oauth2/login-urls" | head -c 200
echo -e "\n"

# 인증이 필요한 엔드포인트들 - 403이 예상됨
echo "3. 접근성 설정 (403 예상)"
curl -s -w "%{http_code}" "$BASE_URL/api/accessibility/settings" | tail -c 3
echo -e "\n"

echo "4. 통계 API (403 예상)"
curl -s -w "%{http_code}" "$BASE_URL/api/statistics/geofence" | tail -c 3
echo -e "\n"

echo "5. 실험 목록 (403 예상)"  
curl -s -w "%{http_code}" "$BASE_URL/api/experiments" | tail -c 3
echo -e "\n"

echo "6. WebSocket 연결 (403 예상)"
curl -s -w "%{http_code}" "$BASE_URL/ws" | tail -c 3
echo -e "\n"

echo "=== 테스트 완료 ==="
