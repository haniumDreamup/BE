#!/bin/bash

# Error Handling Improvement Test Script
# Tests the new ErrorCode enum, GlobalExceptionHandler, and structured logging

echo "🧪 Testing Improved Error Handling System"
echo "=========================================="

BASE_URL="http://localhost:8080/api/v1"

# Test 1: Validation Error (should now be handled by GlobalExceptionHandler)
echo "1️⃣ Testing validation error handling..."
response=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ab",
    "email": "invalid-email",
    "password": "123",
    "confirmPassword": "456",
    "fullName": "",
    "agreeToTerms": false,
    "agreeToPrivacyPolicy": false
  }')

echo "Response: $response"
echo "Expected: Should show detailed validation errors with user-friendly messages"
echo ""

# Test 2: Missing required fields (should trigger VALIDATION_002)
echo "2️⃣ Testing missing required fields..."
response=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{}')

echo "Response: $response"
echo "Expected: Should show VALIDATION_002 error code"
echo ""

# Test 3: Invalid login credentials (should trigger AUTH_002)
echo "3️⃣ Testing invalid login credentials..."
response=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "nonexistent@example.com",
    "password": "wrongpassword",
    "rememberMe": false
  }')

echo "Response: $response"
echo "Expected: Should show AUTH_002 error code with friendly message"
echo ""

# Test 4: Malformed JSON (should trigger VALIDATION_001)
echo "4️⃣ Testing malformed JSON..."
response=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{invalid json}')

echo "Response: $response"
echo "Expected: Should show JSON parsing error with friendly message"
echo ""

# Test 5: Valid registration (should show improved logging)
echo "5️⃣ Testing valid registration (check server logs for structured logging)..."
unique_email="test_$(date +%s)@example.com"
response=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"testuser$(date +%s)\",
    \"email\": \"$unique_email\",
    \"password\": \"validPassword123!\",
    \"confirmPassword\": \"validPassword123!\",
    \"fullName\": \"Test User\",
    \"agreeToTerms\": true,
    \"agreeToPrivacyPolicy\": true,
    \"agreeToMarketing\": false
  }")

echo "Response: $response"
echo "Expected: Should show success with structured logging in server logs"
echo ""

echo "✅ Error handling test completed!"
echo "👀 Check server logs to see:"
echo "   - Structured logging with MDC (traceId, operation, executionTime)"
echo "   - User-friendly error messages"
echo "   - Proper ErrorCode classification"
echo "   - Security event logging"