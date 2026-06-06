# GitHub Actions Self-Hosted Runner Setup for TelePort

## Problem Identified
The self-hosted runner `TelePort-Runner1` was showing as "online" in GitHub but was NOT picking up any workflow jobs. All CD and self-hosted workflows were stuck in "queued" state indefinitely.

**Root Cause**: The runner service was not functioning properly - likely due to expired authentication token or stuck process.

**Solution**: Remove and re-register the runner with a fresh token.

---

## Quick Fix: Re-register the Runner

The old runner has been removed from GitHub. Follow these steps to set up a new runner:

### Step 1: Download Runner (if not already downloaded)

```bash
# Create a folder for the runner
mkdir -p ~/actions-runner && cd ~/actions-runner

# Download the latest runner for macOS x64
curl -o actions-runner-osx-x64-2.334.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.334.0/actions-runner-osx-x64-2.334.0.tar.gz

# Verify hash (optional but recommended)
echo "73a979ff7e9ce8a70244f3a959d896870be486fac92bb08ed90684f961474e0d  actions-runner-osx-x64-2.334.0.tar.gz" | shasum -a 256 -c

# Extract the installer
tar xzf ./actions-runner-osx-x64-2.334.0.tar.gz
```

### Step 2: Configure the Runner

Use this **fresh registration token** (expires at 2026-06-06 19:03:48 IST):

```bash
# Configure the runner
./config.sh --url https://github.com/ravitejakamalapuram/TelePort --token ABXMNXMLQZ4OMKHDLQ5QJP3KEQQ3Y --name TelePort-Runner1 --labels self-hosted,macOS,X64

# When prompted:
# - Runner group: Press Enter for default
# - Runner name: TelePort-Runner1 (or press Enter to use default)
# - Work folder: Press Enter for default (_work)
```

### Step 3: Start the Runner

**Option A: Run as a service (recommended for always-on runner)**
```bash
# Install and start the service
sudo ./svc.sh install
sudo ./svc.sh start

# Check status
sudo ./svc.sh status
```

**Option B: Run interactively (for testing)**
```bash
# Run the runner in the foreground
./run.sh
```

---

## Verify Runner is Working

After starting the runner, verify it's registered and online:

```bash
# Check if runner appears in GitHub
gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | {name, status, busy}'
```

Expected output:
```json
{
  "busy": false,
  "name": "TelePort-Runner1",
  "status": "online"
}
```

Then trigger the test workflow:
```bash
gh workflow run test-runner.yml
sleep 5
gh run list --workflow=test-runner.yml --limit 1
```

The workflow should show status "in_progress" or "completed", NOT "queued".

---

## If Token Expires

If the registration token expires before you can use it (expires in ~1 hour), generate a new one:

```bash
gh api --method POST /repos/ravitejakamalapuram/TelePort/actions/runners/registration-token --jq '{token: .token, expires_at: .expires_at}'
```

---

## Troubleshooting

### Runner shows "online" but doesn't pick up jobs
1. Restart the runner service:
   ```bash
   sudo ./svc.sh stop
   sudo ./svc.sh start
   ```

2. Check runner logs:
   ```bash
   # If running as service
   tail -f ~/actions-runner/_diag/*.log
   
   # If running interactively, logs appear in terminal
   ```

3. Remove and re-register:
   ```bash
   # Stop service
   sudo ./svc.sh stop
   sudo ./svc.sh uninstall
   
   # Remove runner from GitHub
   gh api --method DELETE /repos/ravitejakamalapuram/TelePort/actions/runners/{runner-id}
   
   # Get new token and reconfigure
   gh api --method POST /repos/ravitejakamalapuram/TelePort/actions/runners/registration-token --jq '.token'
   ./config.sh --url https://github.com/ravitejakamalapuram/TelePort --token <NEW_TOKEN>
   ```

### Check what jobs are waiting
```bash
gh run list --status queued --limit 5
```

---

## Once Fixed

After the runner is working:

1. Cancel all queued CD runs:
   ```bash
   gh run list --workflow=cd.yml --status queued --json databaseId --jq '.[].databaseId' | xargs -I {} gh run cancel {}
   ```

2. Trigger a fresh CD run:
   ```bash
   gh workflow run cd.yml
   ```

3. Clean up test workflows (optional):
   ```bash
   rm .github/workflows/test-runner.yml
   rm .github/workflows/test-cd-ubuntu.yml
   git add -A && git commit -m "chore: remove runner test workflows" && git push
   ```

---

## Summary

- ✅ Old broken runner removed from GitHub
- ✅ Fresh registration token generated
- ⏳ Waiting for runner to be re-registered and started
- ⏳ Once runner is online, CD pipeline will work
