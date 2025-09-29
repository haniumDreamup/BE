#!/bin/bash

echo "=== Flutter API Testing Environment ==="
echo "Testing connectivity to production backend: http://43.200.49.171:8080"
echo

# Test basic connectivity
echo "1. Testing health endpoint..."
curl -s -o /dev/null -w "Status: %{http_code}\n" http://43.200.49.171:8080/api/v1/health

# Flutter setup
if [ -d "/app" ] && [ -f "/app/pubspec.yaml" ]; then
    echo
    echo "2. Setting up Flutter app..."
    cd /app

    export API_BASE_URL=http://43.200.49.171:8080
    echo "API_BASE_URL set to: $API_BASE_URL"

    # Get dependencies
    flutter pub get

    # Run tests
    echo
    echo "3. Running Flutter tests..."
    flutter test --dart-define=API_BASE_URL=http://43.200.49.171:8080 || true

    # Build for web (since we can't run Android emulator on ARM Mac easily)
    echo
    echo "4. Building Flutter web version..."
    flutter build web --dart-define=API_BASE_URL=http://43.200.49.171:8080

    echo
    echo "Build completed. You can serve the web build to test API connectivity."
else
    echo "No Flutter app found in /app"
    echo "Keeping container running for manual testing..."

    # Keep testing API endpoints
    while true; do
        echo
        echo "=== API Connectivity Test $(date) ==="
        echo "Health check:"
        curl -s http://43.200.49.171:8080/api/v1/health | head -5
        sleep 30
    done
fi