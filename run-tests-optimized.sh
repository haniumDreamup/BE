#!/bin/bash

# Optimized Test Runner Script for BIF-AI Backend
# This script runs tests with optimizations to reduce execution time

echo "🚀 Starting optimized test execution..."
echo "================================"

# Clean previous test outputs
rm -rf build/test-results
rm -rf build/reports/tests

# Set JVM options for faster test execution
export GRADLE_OPTS="-Xmx2g -Xms512m -XX:+UseParallelGC -XX:MaxMetaspaceSize=512m"

# Run tests with optimizations
echo "📝 Running tests with optimizations..."
./gradlew clean test \
  --parallel \
  --max-workers=4 \
  --no-daemon \
  -Dspring.profiles.active=test \
  -Dspring.jpa.show-sql=false \
  -Dlogging.level.root=WARN \
  -Dlogging.level.org.hibernate=WARN \
  -Dlogging.level.org.springframework=WARN \
  -x processTestResources \
  --continue

# Check test results
if [ $? -eq 0 ]; then
    echo "✅ All tests passed successfully!"
else
    echo "❌ Some tests failed. Check the report at:"
    echo "   file://$(pwd)/build/reports/tests/test/index.html"
fi

# Show test summary
echo ""
echo "📊 Test Summary:"
echo "================================"
if [ -f build/test-results/test/TEST-*.xml ]; then
    tests_run=$(grep -h "tests=" build/test-results/test/TEST-*.xml | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
    failures=$(grep -h "failures=" build/test-results/test/TEST-*.xml | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
    errors=$(grep -h "errors=" build/test-results/test/TEST-*.xml | sed 's/.*errors="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
    
    echo "Total tests run: ${tests_run:-0}"
    echo "Failures: ${failures:-0}"
    echo "Errors: ${errors:-0}"
fi

echo "================================"
echo "✨ Test execution completed!"