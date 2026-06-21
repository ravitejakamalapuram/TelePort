# ✅ Self-Hosted Runner Setup Complete

**Date:** June 21, 2026  
**Status:** ✅ **OPERATIONAL**

## Summary

Successfully configured both CI and CD workflows to use self-hosted runner and reconnected the local GitHub Actions runner.

---

## Changes Made

### 1. **CI Workflow** (`.github/workflows/ci.yml`)
```yaml
Changed: runs-on: ubuntu-latest
To:      runs-on: self-hosted
```

### 2. **CD Workflow** (`.github/workflows/cd.yml`)
```yaml
Status: Already configured for self-hosted
runner-type: 'self-hosted'
```

### 3. **Self-Hosted Runner Reconnection**

**Location:** `~/github-runners/TelePort/Runner1`

**Actions Performed:**
1. ✅ Stopped existing service
2. ✅ Uninstalled old service configuration
3. ✅ Removed expired runner registration
4. ✅ Generated fresh registration token via GitHub API
5. ✅ Reconfigured runner with new token
6. ✅ Installed service (`./svc.sh install`)
7. ✅ Started service (`./svc.sh start`)

**Runner Details:**
- **Name:** TelePort-Runner1
- **Labels:** self-hosted
- **Group:** Default
- **Status:** ✅ Running (PID: 62546)
- **Version:** 2.334.0 → Auto-updating to 2.335.1

---

## Current Status

### ✅ Runner Health
```
status actions.runner.ravitejakamalapuram-TelePort.TelePort-Runner1:
Started: 62546 0 actions.runner.ravitejakamalapuram-TelePort.TelePort-Runner1
```

### ✅ Jobs Processing
- **CD Job:** Running (`release / Release & Deploy`)
- **CI Job:** Queued (will run next)
- **Worker Process:** Active (PID: 63406)

### 📊 Workflow Status
```bash
$ gh run list --limit 3
CD  - queued     (ci: Change CI workflow to use self-hosted runner)
CI  - queued     (ci: Change CI workflow to use self-hosted runner)
CI  - success    (revert: Change CD workflow back to self-hosted runner)
```

---

## How It Works

### **Workflow Execution Flow:**

```
Push to main
    ↓
GitHub triggers CI + CD workflows
    ↓
Jobs assigned to: self-hosted runners
    ↓
~/github-runners/TelePort/Runner1
    ↓
Runner.Listener picks up jobs
    ↓
Spawns Runner.Worker processes
    ↓
Executes workflow steps locally
```

### **Benefits of Self-Hosted:**

1. ✅ **Full control** over build environment
2. ✅ **Access to local secrets** (signing keys, credentials)
3. ✅ **Faster builds** (cached dependencies, no cold starts)
4. ✅ **Consistent environment** between CI/CD
5. ✅ **No GitHub Actions minute costs**

---

## Monitoring

### Check Runner Status
```bash
cd ~/github-runners/TelePort/Runner1
./svc.sh status
```

### View Runner Logs
```bash
cd ~/github-runners/TelePort/Runner1/_diag
tail -f Runner_*.log
```

### List Active Jobs
```bash
gh run list --limit 5
```

### Watch Runner Process
```bash
ps aux | grep "Runner\|Worker"
```

---

## Service Management

### Start Runner
```bash
cd ~/github-runners/TelePort/Runner1
./svc.sh start
```

### Stop Runner
```bash
cd ~/github-runners/TelePort/Runner1
./svc.sh stop
```

### Restart Runner
```bash
cd ~/github-runners/TelePort/Runner1
./svc.sh stop && ./svc.sh start
```

### Check Logs
```bash
tail -f ~/Library/Logs/actions.runner.ravitejakamalapuram-TelePort.TelePort-Runner1/Runner_*.log
```

---

## Auto-Updates

The runner automatically updates itself when GitHub releases new versions:
- **Current:** 2.334.0
- **Available:** 2.335.1
- **Status:** Auto-updating in progress

---

## Next Steps

✅ **Everything is configured and running!**

The runner will now:
1. Pick up all CI/CD jobs automatically
2. Execute them on your local machine
3. Update itself when needed
4. Restart on system boot (LaunchAgent configured)

You can monitor workflows at:
https://github.com/ravitejakamalapuram/TelePort/actions
