#!/bin/bash

BASE_URL="http://43.200.49.171:8080"

echo "=== Testing Production Server ==="
echo "Server: $BASE_URL"
echo ""

echo "1. Health Check:"
curl -s "$BASE_URL/api/v1/health" | jq '.' || echo "Failed"
echo ""

echo "2. Test Controller:"
curl -s "$BASE_URL/api/v1/test/ping" | jq '.' || echo "Failed"
echo ""
