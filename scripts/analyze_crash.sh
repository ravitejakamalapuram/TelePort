#!/usr/bin/env bash
# ============================================================================
# analyze_crash.sh — Analyze crash data with Gemini AI and create fix PRs.
#
# This script:
#   1. Reads a JSON file containing crash data (produced by fetch_crashes.py).
#   2. For each crash, constructs a detailed prompt and sends it to Gemini
#      (either via the `gemini` CLI or the REST API as a fallback).
#   3. If the response contains actionable file changes, applies them on a
#      new branch and opens a pull request via the `gh` CLI.
#
# Usage:
#   bash scripts/analyze_crash.sh /tmp/crashes.json
#
# Required environment variables (for the API fallback):
#   GEMINI_API_KEY   — Google AI Studio API key
# Optional:
#   GH_TOKEN         — GitHub token for `gh` CLI (auto-set in Actions)
#   DRY_RUN          — set to "true" to skip branch/PR creation
# ============================================================================
set -euo pipefail

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
log() {
  echo "[$(date '+%Y-%m-%dT%H:%M:%S%z')] $*" >&2
}

die() {
  log "FATAL: $*"
  exit 1
}

# Ensure a command is available; warn (don't die) if missing.
require_warn() {
  if ! command -v "$1" &>/dev/null; then
    log "WARNING: '$1' is not installed. Some features may be unavailable."
    return 1
  fi
  return 0
}

# ---------------------------------------------------------------------------
# Input validation
# ---------------------------------------------------------------------------
CRASH_JSON="${1:-}"
if [[ -z "$CRASH_JSON" ]]; then
  die "Usage: $0 <crash-data.json>"
fi
if [[ ! -f "$CRASH_JSON" ]]; then
  die "File not found: $CRASH_JSON"
fi

# Quick sanity: the file must parse as JSON.
if ! python3 -c "import json, sys; json.load(open(sys.argv[1]))" "$CRASH_JSON" 2>/dev/null; then
  die "Invalid JSON: $CRASH_JSON"
fi

CRASH_COUNT=$(python3 -c "import json,sys; print(len(json.load(open(sys.argv[1]))))" "$CRASH_JSON")
if [[ "$CRASH_COUNT" -eq 0 ]]; then
  log "No crashes to process. Exiting."
  exit 0
fi
log "Processing $CRASH_COUNT crash(es) …"

# ---------------------------------------------------------------------------
# Detect available AI tooling
# ---------------------------------------------------------------------------
USE_GEMINI_CLI=false
if command -v gemini &>/dev/null; then
  USE_GEMINI_CLI=true
  log "Gemini CLI detected — will use it for analysis."
else
  log "Gemini CLI not found. Will attempt REST API fallback."
  if [[ -z "${GEMINI_API_KEY:-}" ]]; then
    die "Neither 'gemini' CLI nor GEMINI_API_KEY is available. Cannot proceed."
  fi
fi

HAS_GH=false
if command -v gh &>/dev/null; then
  HAS_GH=true
fi

DRY_RUN="${DRY_RUN:-false}"

# ---------------------------------------------------------------------------
# call_gemini <prompt>
#   Sends the prompt to Gemini and prints the response to stdout.
# ---------------------------------------------------------------------------
call_gemini() {
  local prompt="$1"

  if [[ "$USE_GEMINI_CLI" == "true" ]]; then
    log "Invoking Gemini CLI …"
    echo "$prompt" | gemini -p 2>/dev/null || {
      log "Gemini CLI invocation failed; falling back to API."
      _call_gemini_api "$prompt"
    }
  else
    _call_gemini_api "$prompt"
  fi
}

_call_gemini_api() {
  local prompt="$1"

  if [[ -z "${GEMINI_API_KEY:-}" ]]; then
    log "ERROR: GEMINI_API_KEY is not set. Cannot call the Gemini API."
    return 1
  fi

  # Build the JSON payload.  We use python3 to safely escape the prompt.
  local payload
  payload=$(python3 -c "
import json, sys
prompt = sys.stdin.read()
print(json.dumps({
    'contents': [{'parts': [{'text': prompt}]}],
    'generationConfig': {
        'temperature': 0.2,
        'maxOutputTokens': 8192,
    }
}))
" <<< "$prompt")

  local api_url="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}"

  local response
  response=$(curl -s -w "\n%{http_code}" \
    -X POST "$api_url" \
    -H "Content-Type: application/json" \
    -d "$payload") || {
    log "ERROR: curl request to Gemini API failed."
    return 1
  }

  local http_code
  http_code=$(echo "$response" | tail -n1)
  local body
  body=$(echo "$response" | sed '$d')

  if [[ "$http_code" -ne 200 ]]; then
    log "ERROR: Gemini API returned HTTP $http_code"
    log "$body"
    return 1
  fi

  # Extract the text from the response JSON.
  python3 -c "
import json, sys
data = json.loads(sys.stdin.read())
parts = data.get('candidates', [{}])[0].get('content', {}).get('parts', [])
for p in parts:
    if 'text' in p:
        print(p['text'])
" <<< "$body"
}

# ---------------------------------------------------------------------------
# build_prompt <crash_json_object>
#   Constructs the analysis prompt for a single crash.
# ---------------------------------------------------------------------------
build_prompt() {
  local crash_obj="$1"

  local title subtitle stack_trace event_count affected_users device_model device_os
  title=$(echo "$crash_obj"       | python3 -c "import json,sys; print(json.load(sys.stdin).get('title',''))")
  subtitle=$(echo "$crash_obj"    | python3 -c "import json,sys; print(json.load(sys.stdin).get('subtitle',''))")
  stack_trace=$(echo "$crash_obj" | python3 -c "import json,sys; print(json.load(sys.stdin).get('stack_trace',''))")
  event_count=$(echo "$crash_obj" | python3 -c "import json,sys; print(json.load(sys.stdin).get('event_count',0))")
  affected_users=$(echo "$crash_obj" | python3 -c "import json,sys; print(json.load(sys.stdin).get('affected_users',0))")
  device_model=$(echo "$crash_obj"   | python3 -c "import json,sys; print(json.load(sys.stdin).get('device_info',{}).get('model','unknown'))")
  device_os=$(echo "$crash_obj"      | python3 -c "import json,sys; print(json.load(sys.stdin).get('device_info',{}).get('os_version','unknown'))")

  cat <<PROMPT
You are an expert Android developer reviewing a production crash report from Firebase Crashlytics for the TelePort Android application.

## Crash Information
- **Title**: ${title}
- **Subtitle**: ${subtitle}
- **Occurrences**: ${event_count} event(s), affecting ${affected_users} user(s)
- **Device**: ${device_model} running Android ${device_os}

## Stack Trace
\`\`\`
${stack_trace}
\`\`\`

## Instructions
1. **Root Cause Analysis**: Explain the most likely root cause of this crash.
2. **Affected Code**: Identify the specific file(s) and line(s) that are likely responsible.
3. **Proposed Fix**: Provide a concrete code fix.  For each file you change, output a
   fenced code block with the filename as the info-string, e.g.:

   \`\`\`app/src/main/java/com/example/MyActivity.kt
   // full or partial file content with the fix applied
   \`\`\`

4. **Testing Recommendations**: Suggest how to verify the fix (unit test, manual steps, etc.).
5. **Severity Assessment**: Rate the severity (Critical / High / Medium / Low) and explain why.

Please be concise and actionable.
PROMPT
}

# ---------------------------------------------------------------------------
# apply_ai_changes <ai_response> <issue_id>
#   Parses fenced code blocks from the AI response, writes them to disk,
#   and returns 0 if any files were modified.
# ---------------------------------------------------------------------------
apply_ai_changes() {
  local ai_response="$1"
  local issue_id="$2"
  local changed=0

  # Extract fenced code blocks whose info-string looks like a file path.
  # We rely on python3 for robust multi-line parsing.
  local files_json
  files_json=$(python3 -c "
import re, json, sys

text = sys.stdin.read()
# Match fenced blocks:  \`\`\`path/to/File.ext  …  \`\`\`
pattern = r'\`\`\`([\w./-]+\.\w+)\n(.*?)\`\`\`'
matches = re.findall(pattern, text, re.DOTALL)

results = []
for path, content in matches:
    # Only consider paths that look like project source files.
    if '/' in path and not path.startswith('http'):
        results.append({'path': path, 'content': content})

print(json.dumps(results))
" <<< "$ai_response") || {
    log "WARNING: Failed to parse AI response for file changes."
    return 1
  }

  local count
  count=$(python3 -c "import json,sys; print(len(json.load(sys.stdin)))" <<< "$files_json")

  if [[ "$count" -eq 0 ]]; then
    log "No file changes detected in the AI response for issue $issue_id."
    return 1
  fi

  log "Detected $count file change(s) in the AI response."

  # Write each file.
  python3 -c "
import json, os, sys

files = json.loads(sys.stdin.read())
for f in files:
    path = f['path']
    os.makedirs(os.path.dirname(path) or '.', exist_ok=True)
    with open(path, 'w') as fh:
        fh.write(f['content'])
    print(f'  ✏️  Wrote {path}', file=sys.stderr)
" <<< "$files_json"

  return 0
}

# ---------------------------------------------------------------------------
# Main loop — iterate over each crash
# ---------------------------------------------------------------------------
TIMESTAMP=$(date '+%Y%m%d%H%M%S')
ORIGINAL_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "main")

# Extract individual crash objects into an indexed temp directory.
WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

python3 -c "
import json, sys, os
crashes = json.load(open(sys.argv[1]))
work = sys.argv[2]
for i, c in enumerate(crashes):
    with open(os.path.join(work, f'{i}.json'), 'w') as f:
        json.dump(c, f)
" "$CRASH_JSON" "$WORK_DIR"

TOTAL_ANALYZED=0
TOTAL_PRS=0

for crash_file in "$WORK_DIR"/*.json; do
  crash_obj=$(cat "$crash_file")
  issue_id=$(echo "$crash_obj" | python3 -c "import json,sys; print(json.load(sys.stdin).get('issue_id','unknown'))")
  title=$(echo "$crash_obj"    | python3 -c "import json,sys; print(json.load(sys.stdin).get('title','Unknown crash'))")

  log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  log "Analyzing crash: $title (issue: $issue_id)"
  log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # 1. Build the prompt.
  prompt=$(build_prompt "$crash_obj")

  # 2. Call Gemini.
  ai_response=$(call_gemini "$prompt") || {
    log "WARNING: Failed to get AI analysis for issue $issue_id. Skipping."
    continue
  }
  TOTAL_ANALYZED=$((TOTAL_ANALYZED + 1))

  # Save the analysis for reference.
  analysis_file="$WORK_DIR/analysis_${issue_id}.md"
  echo "$ai_response" > "$analysis_file"
  log "Analysis saved to $analysis_file"

  # If dry-run, stop here.
  if [[ "$DRY_RUN" == "true" ]]; then
    log "[DRY RUN] Skipping branch creation and PR for issue $issue_id."
    echo "---"
    echo "$ai_response"
    echo "---"
    continue
  fi

  # 3. Create a fix branch.
  branch_name="fix/crash-${issue_id}-${TIMESTAMP}"
  log "Creating branch: $branch_name"
  git checkout -b "$branch_name" "$ORIGINAL_BRANCH" 2>/dev/null || {
    log "WARNING: Could not create branch $branch_name. Skipping PR creation."
    continue
  }

  # 4. Attempt to apply file changes from the AI response.
  if apply_ai_changes "$ai_response" "$issue_id"; then
    # Stage and commit.
    git add -A
    git commit -m "fix: auto-fix crash — ${title}

Issue ID: ${issue_id}
Generated by the Crash Monitor pipeline.
" || {
      log "WARNING: git commit failed (perhaps no changes). Skipping PR."
      git checkout "$ORIGINAL_BRANCH" 2>/dev/null || true
      continue
    }

    # 5. Push and create a PR.
    if [[ "$HAS_GH" == "true" ]]; then
      log "Pushing branch and creating PR …"
      git push origin "$branch_name" 2>/dev/null || {
        log "WARNING: git push failed for $branch_name."
        git checkout "$ORIGINAL_BRANCH" 2>/dev/null || true
        continue
      }

      pr_body=$(cat <<EOF
## 🤖 Auto-Generated Crash Fix

**Crash Title**: ${title}
**Issue ID**: \`${issue_id}\`
**Pipeline**: Crash Monitor & Auto-Fix

---

### AI Analysis

${ai_response}

---

> ⚠️ **This PR was generated automatically.** Please review carefully before merging.
EOF
      )

      gh pr create \
        --title "fix: auto-fix crash — ${title}" \
        --body "$pr_body" \
        --base main \
        --head "$branch_name" \
        --label "bug,crash,automated" && {
          TOTAL_PRS=$((TOTAL_PRS + 1))
          log "✅ PR created for issue $issue_id."
        } || {
          log "WARNING: 'gh pr create' failed for $branch_name."
        }
    else
      log "WARNING: 'gh' CLI not available. Branch pushed but no PR created."
      git push origin "$branch_name" 2>/dev/null || true
    fi
  else
    log "No actionable file changes found for issue $issue_id. Cleaning up branch."
    git checkout "$ORIGINAL_BRANCH" 2>/dev/null || true
    git branch -D "$branch_name" 2>/dev/null || true
    continue
  fi

  # Return to the original branch for the next iteration.
  git checkout "$ORIGINAL_BRANCH" 2>/dev/null || true
done

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "Done.  Analyzed: $TOTAL_ANALYZED / $CRASH_COUNT  |  PRs created: $TOTAL_PRS"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
