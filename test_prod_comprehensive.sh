#!/bin/bash

BASE_URL="http://43.200.49.171:8080"
TOTAL=0
PASSED=0
FAILED=0

echo "=== BIF-AI Production Server Comprehensive Test ==="
echo "Server: $BASE_URL"
echo "Started at: $(date)"
echo ""

test_endpoint() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local auth="$5"
    
    TOTAL=$((TOTAL + 1))
    
    if [ -n "$data" ]; then
        if [ -n "$auth" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer $auth" \
                -d "$data" 2>&1)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
                -H "Content-Type: application/json" \
                -d "$data" 2>&1)
        fi
    else
        if [ -n "$auth" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
                -H "Authorization: Bearer $auth" 2>&1)
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" 2>&1)
        fi
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$ d')
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 500 ]; then
        echo "✓ [$TOTAL] $name - HTTP $http_code"
        PASSED=$((PASSED + 1))
    else
        echo "✗ [$TOTAL] $name - HTTP $http_code"
        echo "  Response: $body"
        FAILED=$((FAILED + 1))
    fi
}

echo "=== 1. Public Endpoints ==="
test_endpoint "Health Check" "GET" "/api/v1/health"
test_endpoint "Test Ping" "GET" "/api/v1/test/ping"
test_endpoint "Global Error Test" "GET" "/api/v1/error/test"

echo ""
echo "=== 2. Auth Endpoints (401 Expected) ==="
test_endpoint "Login (No Credentials)" "POST" "/api/v1/auth/login" '{"phoneNumber":"","password":""}'
test_endpoint "Register (Invalid Data)" "POST" "/api/v1/auth/register" '{"phoneNumber":"","password":"","userName":""}'
test_endpoint "Kakao Login (No Token)" "POST" "/api/v1/auth/kakao/login" '{"accessToken":""}'

echo ""
echo "=== 3. Protected Endpoints (401 Expected) ==="
test_endpoint "Get User Info" "GET" "/api/v1/users/me"
test_endpoint "Get User Statistics" "GET" "/api/v1/users/me/statistics"
test_endpoint "List Emergencies" "GET" "/api/v1/emergency"
test_endpoint "List Notifications" "GET" "/api/v1/notifications"
test_endpoint "List Guardians" "GET" "/api/v1/guardian-relationships"
test_endpoint "List Geofences" "GET" "/api/v1/accessibility/geofences"
test_endpoint "Get Dashboard" "GET" "/api/v1/guardians/dashboard"

echo ""
echo "=== Results ==="
echo "Total Tests: $TOTAL"
echo "Passed: $PASSED ($(awk "BEGIN {printf \"%.1f\", ($PASSED/$TOTAL)*100}")%)"
echo "Failed: $FAILED"
echo ""
echo "Completed at: $(date)"
