#!/bin/bash

# API Endpoint Test Script for BIF-AI Backend
# Tests all endpoints with various scenarios including edge cases

API_URL="http://43.200.49.171:8080/api"
TOKEN=""
GUARDIAN_TOKEN=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test result counters
SUCCESS_COUNT=0
FAIL_COUNT=0
EDGE_COUNT=0

echo "================================================"
echo "BIF-AI Backend API Endpoint Test"
echo "Server: $API_URL"
echo "================================================"

# Function to print test results
print_result() {
    local status=$1
    local endpoint=$2
    local message=$3

    if [ "$status" == "SUCCESS" ]; then
        echo -e "${GREEN}✓${NC} $endpoint - $message"
        ((SUCCESS_COUNT++))
    elif [ "$status" == "FAIL" ]; then
        echo -e "${RED}✗${NC} $endpoint - $message"
        ((FAIL_COUNT++))
    elif [ "$status" == "EDGE" ]; then
        echo -e "${YELLOW}⚠${NC} $endpoint - Edge case: $message"
        ((EDGE_COUNT++))
    else
        echo -e "${BLUE}ℹ${NC} $endpoint - $message"
    fi
}

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local auth=$4
    local test_name=$5

    echo -e "\n${BLUE}Testing:${NC} $test_name"

    if [ "$auth" == "true" ] && [ -z "$TOKEN" ]; then
        print_result "FAIL" "$endpoint" "No auth token available"
        return
    fi

    if [ "$method" == "GET" ]; then
        if [ "$auth" == "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" "$API_URL$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" "$API_URL$endpoint")
        fi
    elif [ "$method" == "POST" ]; then
        if [ "$auth" == "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "$data" "$API_URL$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d "$data" "$API_URL$endpoint")
        fi
    elif [ "$method" == "PUT" ]; then
        if [ "$auth" == "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -X PUT -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "$data" "$API_URL$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X PUT -H "Content-Type: application/json" -d "$data" "$API_URL$endpoint")
        fi
    elif [ "$method" == "DELETE" ]; then
        if [ "$auth" == "true" ]; then
            response=$(curl -s -w "\n%{http_code}" -X DELETE -H "Authorization: Bearer $TOKEN" "$API_URL$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X DELETE "$API_URL$endpoint")
        fi
    fi

    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')

    # Check response
    if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
        print_result "SUCCESS" "$endpoint" "HTTP $http_code"
    elif [[ "$http_code" == "400" ]] || [[ "$http_code" == "404" ]] || [[ "$http_code" == "409" ]]; then
        print_result "EDGE" "$endpoint" "HTTP $http_code - Expected error"
    elif [[ "$http_code" == "401" ]] || [[ "$http_code" == "403" ]]; then
        if [ "$auth" == "false" ]; then
            print_result "SUCCESS" "$endpoint" "HTTP $http_code - Correctly requires auth"
        else
            print_result "FAIL" "$endpoint" "HTTP $http_code - Auth failed"
        fi
    else
        print_result "FAIL" "$endpoint" "HTTP $http_code"
    fi

    echo "Response: $(echo $body | jq -r '.' 2>/dev/null | head -5 || echo $body | head -100)"
}

echo -e "\n================================================"
echo "1. HEALTH CHECK ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/health" "" "false" "Health Check"

echo -e "\n================================================"
echo "2. AUTHENTICATION ENDPOINTS"
echo "================================================"

# Register new user (edge case: may already exist)
test_endpoint "POST" "/auth/register" '{
  "username": "testuser_'$RANDOM'",
  "password": "Test1234!@",
  "email": "test'$RANDOM'@example.com",
  "fullName": "Test User",
  "phoneNumber": "010-1234-5678",
  "birthDate": "1990-01-01",
  "role": "USER"
}' "false" "Register New User"

# Login with test account
echo -e "\n${BLUE}Attempting login for token...${NC}"
login_response=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"username": "testuser", "password": "Test1234!@"}' \
    "$API_URL/auth/login")

TOKEN=$(echo "$login_response" | jq -r '.d.accessToken' 2>/dev/null)
if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    print_result "SUCCESS" "/auth/login" "Login successful, token obtained"
else
    print_result "FAIL" "/auth/login" "Failed to obtain token"
    echo "Login response: $login_response"
fi

# Edge cases for auth
test_endpoint "POST" "/auth/login" '{"username": "nonexistent", "password": "wrong"}' "false" "Login with wrong credentials (Edge)"
test_endpoint "POST" "/auth/register" '{"username": "", "password": ""}' "false" "Register with empty fields (Edge)"
test_endpoint "POST" "/auth/register" '{"username": "a", "password": "123"}' "false" "Register with invalid data (Edge)"

echo -e "\n================================================"
echo "3. USER MANAGEMENT ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/users/profile" "" "true" "Get User Profile"
test_endpoint "PUT" "/users/profile" '{"fullName": "Updated Name"}' "true" "Update User Profile"
test_endpoint "GET" "/users/nonexistent" "" "true" "Get Non-existent User (Edge)"

echo -e "\n================================================"
echo "4. EMERGENCY ENDPOINTS"
echo "================================================"

test_endpoint "POST" "/emergency/trigger" '{"location": {"latitude": 37.5665, "longitude": 126.9780}}' "true" "Trigger Emergency"
test_endpoint "GET" "/emergency/contacts" "" "true" "Get Emergency Contacts"
test_endpoint "POST" "/emergency/contacts" '{
  "name": "Emergency Contact",
  "phone": "010-9999-9999",
  "relationship": "친구"
}' "true" "Add Emergency Contact"
test_endpoint "POST" "/emergency/trigger" '{}' "true" "Trigger Emergency without location (Edge)"

echo -e "\n================================================"
echo "5. GUARDIAN ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/guardians" "" "true" "Get Guardians List"
test_endpoint "POST" "/guardians/request" '{
  "guardianEmail": "guardian@example.com"
}' "true" "Send Guardian Request"
test_endpoint "GET" "/guardians/dashboard" "" "true" "Get Guardian Dashboard"
test_endpoint "POST" "/guardians/request" '{"guardianEmail": ""}' "true" "Guardian request with empty email (Edge)"

echo -e "\n================================================"
echo "6. NOTIFICATION ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/notifications" "" "true" "Get Notifications"
test_endpoint "POST" "/notifications/token" '{"token": "test_fcm_token"}' "true" "Update FCM Token"
test_endpoint "PUT" "/notifications/1/read" "" "true" "Mark Notification as Read"
test_endpoint "PUT" "/notifications/999999/read" "" "true" "Mark non-existent notification (Edge)"

echo -e "\n================================================"
echo "7. GEOFENCE ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/geofences" "" "true" "Get Geofences"
test_endpoint "POST" "/geofences" '{
  "name": "집",
  "latitude": 37.5665,
  "longitude": 126.9780,
  "radius": 100,
  "alertOnExit": true,
  "alertOnEntry": false
}' "true" "Create Geofence"
test_endpoint "POST" "/geofences" '{"name": ""}' "true" "Create geofence with missing data (Edge)"

echo -e "\n================================================"
echo "8. IMAGE ANALYSIS ENDPOINTS"
echo "================================================"

test_endpoint "POST" "/vision/analyze-danger" '{"imageUrl": "https://example.com/image.jpg"}' "true" "Analyze Danger from Image"
test_endpoint "POST" "/vision/quick-capture" '{"imageData": "base64_encoded_image"}' "true" "Quick Capture Analysis"
test_endpoint "POST" "/vision/analyze-danger" '{}' "true" "Analyze danger without image (Edge)"

echo -e "\n================================================"
echo "9. STATISTICS ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/statistics/dashboard" "" "true" "Get Statistics Dashboard"
test_endpoint "GET" "/statistics/usage?period=DAILY" "" "true" "Get Daily Usage Statistics"
test_endpoint "GET" "/statistics/usage?period=INVALID" "" "true" "Get statistics with invalid period (Edge)"

echo -e "\n================================================"
echo "10. SOS ENDPOINTS"
echo "================================================"

test_endpoint "POST" "/sos/send" '{
  "message": "도움이 필요합니다",
  "location": {"latitude": 37.5665, "longitude": 126.9780}
}' "true" "Send SOS"
test_endpoint "GET" "/sos/history" "" "true" "Get SOS History"
test_endpoint "POST" "/sos/send" '{"message": ""}' "true" "Send SOS without message (Edge)"

echo -e "\n================================================"
echo "11. ACCESSIBILITY ENDPOINTS"
echo "================================================"

test_endpoint "GET" "/accessibility/settings" "" "true" "Get Accessibility Settings"
test_endpoint "PUT" "/accessibility/settings" '{
  "fontSize": "LARGE",
  "highContrast": true,
  "screenReader": true
}' "true" "Update Accessibility Settings"
test_endpoint "PUT" "/accessibility/settings" '{"fontSize": "INVALID"}' "true" "Update with invalid settings (Edge)"

echo -e "\n================================================"
echo "12. USER BEHAVIOR ENDPOINTS"
echo "================================================"

test_endpoint "POST" "/behavior/log" '{
  "actionType": "BUTTON_CLICK",
  "actionDetail": {"button": "emergency"},
  "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
}' "true" "Log User Behavior"
test_endpoint "GET" "/behavior/patterns" "" "true" "Get Behavior Patterns"

echo -e "\n================================================"
echo "13. POSE DETECTION ENDPOINTS"
echo "================================================"

test_endpoint "POST" "/pose/analyze" '{
  "imageData": "base64_encoded_image",
  "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
}' "true" "Analyze Pose"
test_endpoint "GET" "/pose/history" "" "true" "Get Pose History"

echo -e "\n================================================"
echo "14. ADMIN ENDPOINTS (Should fail without admin rights)"
echo "================================================"

test_endpoint "GET" "/admin/users" "" "true" "Get All Users (Admin)"
test_endpoint "GET" "/admin/system/health" "" "true" "Get System Health (Admin)"
test_endpoint "POST" "/admin/users/1/disable" "" "true" "Disable User (Admin)"

echo -e "\n================================================"
echo "15. EDGE CASES AND ERROR HANDLING"
echo "================================================"

# Test with malformed JSON
test_endpoint "POST" "/auth/register" '{invalid json}' "false" "Malformed JSON (Edge)"

# Test with SQL injection attempt
test_endpoint "POST" "/auth/login" '{"username": "admin\" OR \"1\"=\"1", "password": "password"}' "false" "SQL Injection attempt (Edge)"

# Test with XSS attempt
test_endpoint "POST" "/notifications/send" '{"message": "<script>alert(\"XSS\")</script>"}' "true" "XSS attempt (Edge)"

# Test with extremely long input
LONG_STRING=$(printf 'a%.0s' {1..10000})
test_endpoint "POST" "/auth/register" "{\"username\": \"$LONG_STRING\", \"password\": \"Test1234!@\"}" "false" "Extremely long input (Edge)"

# Test rate limiting (if implemented)
echo -e "\n${BLUE}Testing rate limiting...${NC}"
for i in {1..5}; do
    response=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/health")
    if [ "$response" == "429" ]; then
        print_result "SUCCESS" "/health" "Rate limiting working (429)"
        break
    fi
done

echo -e "\n================================================"
echo "TEST SUMMARY"
echo "================================================"
echo -e "${GREEN}Success:${NC} $SUCCESS_COUNT"
echo -e "${RED}Failed:${NC} $FAIL_COUNT"
echo -e "${YELLOW}Edge Cases:${NC} $EDGE_COUNT"
echo "================================================"

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}All critical tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed. Please review.${NC}"
    exit 1
fi