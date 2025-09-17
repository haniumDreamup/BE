#!/bin/bash

API_URL="http://43.200.49.171:8080/api"
echo "=== BIF-AI Backend Quick API Test ==="

# 1. Health Check
echo -e "\n1. Health Check:"
curl -s "$API_URL/health" | jq .

# 2. Auth - Register (Edge case: already exists)
echo -e "\n2. Register Test User:"
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!@","email":"test@example.com","fullName":"Test User","phoneNumber":"010-1234-5678","birthDate":"1990-01-01","role":"USER"}' \
  "$API_URL/auth/register" | jq .

# 3. Auth - Login
echo -e "\n3. Login:"
LOGIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test1234!@"}' \
  "$API_URL/auth/login")
echo "$LOGIN_RESPONSE" | jq .

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.d.accessToken // empty')
echo "Token: ${TOKEN:0:50}..."

if [ -z "$TOKEN" ]; then
  echo "Failed to get token, stopping tests"
  exit 1
fi

# 4. User Profile (Authenticated)
echo -e "\n4. Get User Profile:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/users/profile" | jq .

# 5. User Profile Update
echo -e "\n5. Update User Profile:"
curl -s -X PUT -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"fullName":"Updated Test User"}' \
  "$API_URL/users/profile" | jq .

# 6. Notifications
echo -e "\n6. Get Notifications:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/notifications" | jq .

# 7. Emergency Contacts
echo -e "\n7. Get Emergency Contacts:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/emergency/contacts" | jq .

# 8. Add Emergency Contact
echo -e "\n8. Add Emergency Contact:"
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Test Contact","phone":"010-9999-9999","relationship":"친구"}' \
  "$API_URL/emergency/contacts" | jq .

# 9. Guardians
echo -e "\n9. Get Guardians:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/guardians" | jq .

# 10. Geofences
echo -e "\n10. Get Geofences:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/geofences" | jq .

# 11. Statistics Dashboard
echo -e "\n11. Statistics Dashboard:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/statistics/dashboard" | jq .

# 12. SOS History
echo -e "\n12. SOS History:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/sos/history" | jq .

# 13. Accessibility Settings
echo -e "\n13. Accessibility Settings:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/accessibility/settings" | jq .

# 14. Behavior Patterns
echo -e "\n14. Behavior Patterns:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/behavior/patterns" | jq .

# 15. Pose History
echo -e "\n15. Pose History:"
curl -s -H "Authorization: Bearer $TOKEN" "$API_URL/pose/history" | jq .

echo -e "\n=== Edge Cases ==="

# Edge Case 1: Invalid endpoint
echo -e "\n16. Invalid Endpoint:"
curl -s "$API_URL/invalid" | jq . || echo "No JSON response"

# Edge Case 2: Missing auth
echo -e "\n17. Protected endpoint without auth:"
curl -s "$API_URL/users/profile" | jq .

# Edge Case 3: Invalid JSON
echo -e "\n18. Invalid JSON:"
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{invalid json}' \
  "$API_URL/emergency/contacts" | jq . || echo "No JSON response"

# Edge Case 4: Empty required fields
echo -e "\n19. Empty required fields:"
curl -s -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"","phone":""}' \
  "$API_URL/emergency/contacts" | jq .

echo -e "\n=== Test Complete ==="