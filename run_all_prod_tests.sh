#!/bin/bash

PROD_URL="http://43.200.49.171:8080"

echo "=== Converting test scripts to production server ==="
for script in test_*_100.sh; do
    if [ -f "$script" ]; then
        echo "Processing: $script"
        sed "s|BASE_URL=\"http://localhost:8080\"|BASE_URL=\"$PROD_URL\"|g" "$script" > "prod_$script"
        chmod +x "prod_$script"
    fi
done

echo ""
echo "=== Running all production tests ==="
echo "Target Server: $PROD_URL"
echo "Started at: $(date)"
echo ""

TOTAL_SCRIPTS=0
PASSED_SCRIPTS=0
FAILED_SCRIPTS=0

for script in prod_test_*_100.sh; do
    if [ -f "$script" ]; then
        TOTAL_SCRIPTS=$((TOTAL_SCRIPTS + 1))
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Running: $script"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        
        if ./"$script" 2>&1 | tee "/tmp/${script}.log"; then
            if grep -q "100.0%" "/tmp/${script}.log" 2>/dev/null || grep -q "Success Rate: 100%" "/tmp/${script}.log" 2>/dev/null; then
                echo "✓ $script PASSED"
                PASSED_SCRIPTS=$((PASSED_SCRIPTS + 1))
            else
                echo "✗ $script FAILED (not 100%)"
                FAILED_SCRIPTS=$((FAILED_SCRIPTS + 1))
            fi
        else
            echo "✗ $script FAILED (script error)"
            FAILED_SCRIPTS=$((FAILED_SCRIPTS + 1))
        fi
        echo ""
    fi
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "=== Final Results ==="
echo "Total Scripts: $TOTAL_SCRIPTS"
echo "Passed (100%): $PASSED_SCRIPTS"
echo "Failed: $FAILED_SCRIPTS"
echo "Completed at: $(date)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
