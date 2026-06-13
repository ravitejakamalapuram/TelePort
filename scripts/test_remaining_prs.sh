#!/bin/bash
# Test remaining PRs (Palette + Feature)

PRS=(80 77 71 70 66 75)

echo "Testing remaining ${#PRS[@]} PRs..."
echo "===================================="

git checkout main > /dev/null 2>&1

for PR in "${PRS[@]}"; do
    echo ""
    echo "PR #$PR:"
    
    git fetch origin pull/$PR/head:pr-$PR-test > /dev/null 2>&1
    git checkout pr-$PR-test > /dev/null 2>&1
    
    # Quick compile check
    ./gradlew compileDebugKotlin -q 2>&1 | grep -q "BUILD SUCCESSFUL"
    if [ $? -eq 0 ]; then
        echo "  ✅ Compiles"
        
        # Run tests
        ./scripts/dev.sh test > /tmp/pr_${PR}_test.log 2>&1
        if [ $? -eq 0 ]; then
            echo "  ✅ Tests pass"
            echo "  STATUS: READY_TO_MERGE"
        else
            echo "  ❌ Tests fail"
            echo "  STATUS: TEST_FAIL"
            tail -20 /tmp/pr_${PR}_test.log | grep -E "error:|FAILED"
        fi
    else
        echo "  ❌ Compilation fails"
        echo "  STATUS: COMPILE_FAIL"
    fi
done

git checkout main > /dev/null 2>&1
echo ""
echo "===================================="
echo "Testing complete!"
