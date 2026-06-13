#!/bin/bash
# Analyze Bolt PRs for duplication

echo "Analyzing Bolt PRs for duplication..."
echo "======================================================================"

# Bolt PRs
BOLT_PRS=(82 81 79 78 76 74 73 72 69 68 67)

git checkout main > /dev/null 2>&1

for PR in "${BOLT_PRS[@]}"; do
    echo ""
    echo "PR #$PR:"
    
    # Fetch PR
    git fetch origin pull/$PR/head:pr-$PR-temp > /dev/null 2>&1
    
    # Show what files changed
    echo "  Files changed:"
    git diff main...pr-$PR-temp --stat | grep -E "LocalServerService|WebSocket" | sed 's/^/    /'
    
    # Show the actual change in LocalServerService
    echo "  Key changes in LocalServerService.kt:"
    git diff main...pr-$PR-temp -- app/src/main/java/com/teleport/app/tv/server/LocalServerService.kt | \
        grep -A3 -B3 "readBytes\|frame.data\|ByteArray" | head -15 | sed 's/^/    /'
done

echo ""
echo "======================================================================"
