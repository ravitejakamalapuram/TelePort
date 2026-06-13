#!/bin/bash
# Batch PR Review Script - Tests multiple PRs quickly

# List of PR numbers to review
PRS=(81 80 79 78 77 76 75 74 73 72 71 70 69 68 67 66)

echo "===================================================================="
echo "Batch PR Review - Testing ${#PRS[@]} PRs"
echo "===================================================================="
echo ""

# Return to main and update
git checkout main > /dev/null 2>&1
git pull origin main > /dev/null 2>&1

# Create results file
RESULTS_FILE="pr_batch_results.txt"
> $RESULTS_FILE

for PR in "${PRS[@]}"; do
    echo "──────────────────────────────────────────────────────────────────"
    echo "Testing PR #$PR"
    echo "──────────────────────────────────────────────────────────────────"
    
    # Fetch and checkout PR
    git fetch origin pull/$PR/head:pr-$PR-temp > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "❌ Failed to fetch PR #$PR"
        echo "PR #$PR: FETCH_FAILED" >> $RESULTS_FILE
        continue
    fi
    
    git checkout pr-$PR-temp > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "❌ Failed to checkout PR #$PR"
        echo "PR #$PR: CHECKOUT_FAILED" >> $RESULTS_FILE
        continue
    fi
    
    # Quick compile test (faster than full test)
    echo "  Running quick compile test..."
    ./gradlew compileDebugKotlin -q > /dev/null 2>&1
    COMPILE_RESULT=$?
    
    if [ $COMPILE_RESULT -eq 0 ]; then
        echo "  ✅ Compilation passed"
        
        # Run full tests
        echo "  Running tests..."
        ./scripts/dev.sh test > /dev/null 2>&1
        TEST_RESULT=$?
        
        if [ $TEST_RESULT -eq 0 ]; then
            echo "  ✅ Tests passed"
            echo "PR #$PR: PASS" >> $RESULTS_FILE
        else
            echo "  ❌ Tests failed"
            echo "PR #$PR: TEST_FAIL" >> $RESULTS_FILE
        fi
    else
        echo "  ❌ Compilation failed"
        echo "PR #$PR: COMPILE_FAIL" >> $RESULTS_FILE
    fi
    
    echo ""
done

# Return to main
git checkout main > /dev/null 2>&1

# Print summary
echo "===================================================================="
echo "BATCH REVIEW COMPLETE"
echo "===================================================================="
echo ""
cat $RESULTS_FILE
echo ""

# Count results
PASS_COUNT=$(grep -c "PASS" $RESULTS_FILE)
FAIL_COUNT=$(grep -c "FAIL" $RESULTS_FILE)

echo "Summary: $PASS_COUNT passed, $FAIL_COUNT failed"
echo "Full results saved to: $RESULTS_FILE"
