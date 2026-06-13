#!/usr/bin/env python3
"""Close duplicate Bolt PRs"""
import os
import json
import urllib.request
import sys

REPO = 'ravitejakamalapuram/TelePort'
token = os.environ.get('GITHUB_TOKEN')

# Keep PR #82, close all other Bolt PRs as duplicates
DUPLICATES = {
    81: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    79: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    78: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    76: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    74: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    73: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    72: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    69: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    68: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
    67: "Duplicate of #82 - same optimization (frame.readBytes() → frame.data)",
}

def close_pr(pr_num, comment):
    """Close a PR with a comment"""
    
    # First, post a comment
    comment_url = f'https://api.github.com/repos/{REPO}/issues/{pr_num}/comments'
    comment_body = f"""## Closing as Duplicate

{comment}

All 11 Bolt PRs (#82, #81, #79, #78, #76, #74, #73, #72, #69, #68, #67) make the **exact same code change** - replacing `frame.readBytes()` with `frame.data` in `LocalServerService.kt`.

**Analysis shows these are duplicates:**
- All modify the same line of code
- All achieve the same performance optimization
- Only difference is comment wording

**Keeping PR #82** as the canonical version (most detailed comment).

**Closing this PR** to avoid confusion and merge conflicts.

---

*Automated duplicate detection*"""
    
    data = json.dumps({'body': comment_body}).encode('utf-8')
    req = urllib.request.Request(comment_url, data=data, method='POST')
    req.add_header('Authorization', f'token {token}')
    req.add_header('Accept', 'application/vnd.github+json')
    req.add_header('Content-Type', 'application/json')
    
    try:
        with urllib.request.urlopen(req) as response:
            print(f'  ✅ Posted comment to PR #{pr_num}')
    except Exception as e:
        print(f'  ❌ Failed to comment on PR #{pr_num}: {e}')
        return False
    
    # Then close the PR
    close_url = f'https://api.github.com/repos/{REPO}/pulls/{pr_num}'
    data = json.dumps({'state': 'closed'}).encode('utf-8')
    req = urllib.request.Request(close_url, data=data, method='PATCH')
    req.add_header('Authorization', f'token {token}')
    req.add_header('Accept', 'application/vnd.github+json')
    req.add_header('Content-Type', 'application/json')
    
    try:
        with urllib.request.urlopen(req) as response:
            print(f'  ✅ Closed PR #{pr_num}')
            return True
    except Exception as e:
        print(f'  ❌ Failed to close PR #{pr_num}: {e}')
        return False

def main():
    print(f"Closing {len(DUPLICATES)} duplicate Bolt PRs...")
    print("Keeping PR #82 as the canonical version\n")
    
    success = 0
    for pr_num, reason in DUPLICATES.items():
        print(f"Processing PR #{pr_num}...")
        if close_pr(pr_num, reason):
            success += 1
        print()
    
    print(f"\nClosed {success}/{len(DUPLICATES)} PRs successfully")
    
    if success < len(DUPLICATES):
        return 1
    return 0

if __name__ == '__main__':
    sys.exit(main())
