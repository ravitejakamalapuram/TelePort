# GitHub Actions Self-Hosted Runner Troubleshooting Guide

## Quick Diagnosis

Run these commands to quickly identify the issue:

```bash
# Check if runner service is running
launchctl list | grep actions.runner

# Check if runner process is active
ps aux | grep "Runner.Listener" | grep TelePort

# Check runner status in GitHub
gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | {name, status, busy}'

# Check for queued workflows
gh run list --json status,name | jq '.[] | select(.status=="queued")'
```

---

## Common Issues

### 1. Jobs Stuck in "Queued" Status

**Symptoms:**
- Workflows show "queued" status indefinitely
- Runner shows "online" in GitHub but doesn't pick up jobs
- No errors in GitHub Actions UI

**Root Cause:**
Network timeout or stale broker sessions preventing the runner from receiving new job messages.

**Solution:**
```bash
# Quick fix
./scripts/fix-runner.sh

# Or manually
cd ~/github-runners/TelePort/Runner1
./svc.sh stop
sleep 5
./svc.sh start
```

**Verification:**
```bash
# Wait 10 seconds, then check
sleep 10
gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | select(.name=="TelePort-Runner1")'
```

---

### 2. Runner Offline or Not Found

**Symptoms:**
- Runner doesn't appear in GitHub settings
- Runner status shows "offline"
- Service not listed in `launchctl`

**Root Cause:**
Runner service not running or crashed.

**Solution:**
```bash
# Check service status
cd ~/github-runners/TelePort/Runner1
./svc.sh status

# If not running, start it
./svc.sh start

# If service doesn't exist, reinstall
./svc.sh install
./svc.sh start
```

---

### 3. Network Timeout Errors in Logs

**Symptoms:**
- Logs show: `The HTTP request timed out after 00:01:40`
- Logs show: `Socket Error: HostNotFound`
- Errors connecting to `tokenghub.actions.githubusercontent.com`

**Root Cause:**
Network connectivity issues or firewall blocking GitHub Actions endpoints.

**Solution:**
```bash
# Test connectivity
ping broker.actions.githubusercontent.com
ping tokenghub.actions.githubusercontent.com

# Check DNS resolution
nslookup broker.actions.githubusercontent.com

# Restart runner to establish fresh connections
./scripts/fix-runner.sh
```

---

### 4. Runner Process Crashes or Exits

**Symptoms:**
- Service shows "Started" but no process running
- Runner keeps restarting
- Error code 3 in logs

**Root Cause:**
Corrupted work directory or authentication issues.

**Solution:**
```bash
cd ~/github-runners/TelePort/Runner1

# Stop service
./svc.sh stop

# Clear work directory
rm -rf _work/*

# Check credentials
ls -la .credentials*

# Restart service
./svc.sh start

# Check logs
tail -50 _diag/Runner_*.log | grep -i error
```

---

### 5. Authentication or Registration Errors

**Symptoms:**
- Runner can't connect to GitHub
- 401 Unauthorized errors
- "Invalid token" messages

**Root Cause:**
Expired or invalid authentication token.

**Solution:**
```bash
# Remove existing runner
cd ~/github-runners/TelePort/Runner1
./config.sh remove

# Get new registration token
gh api --method POST /repos/ravitejakamalapuram/TelePort/actions/runners/registration-token --jq '.token'

# Re-configure with new token
./config.sh --url https://github.com/ravitejakamalapuram/TelePort --token <NEW_TOKEN> --name TelePort-Runner1

# Install and start service
./svc.sh install
./svc.sh start
```

---

## Monitoring & Maintenance

### Daily Checks
```bash
# Check runner health
gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | {name, status, busy}'
```

### Weekly Checks
```bash
# Review recent workflow runs
gh run list --limit 20

# Check for any queued jobs
gh run list --json status | jq 'map(select(.status=="queued")) | length'
```

### Monthly Maintenance
```bash
# Restart runner to clear sessions
./scripts/fix-runner.sh

# Clean old diagnostic logs
cd ~/github-runners/TelePort/Runner1
ls -t _diag/Runner_*.log | tail -n +10 | xargs rm --
```

---

## Log Analysis

### View Recent Errors
```bash
cd ~/github-runners/TelePort/Runner1
tail -100 _diag/Runner_*.log | grep -i "error\|warn\|exception"
```

### Search for Specific Issues
```bash
# Network issues
grep -i "timeout\|socket\|connection" _diag/Runner_*.log

# Authentication issues
grep -i "auth\|token\|credential" _diag/Runner_*.log

# Job processing
grep -i "job\|message\|broker" _diag/Runner_*.log
```

---

## Emergency Recovery

If nothing else works, completely reset the runner:

```bash
# 1. Stop and uninstall service
cd ~/github-runners/TelePort/Runner1
./svc.sh stop
./svc.sh uninstall

# 2. Remove runner from GitHub
RUNNER_ID=$(gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | select(.name=="TelePort-Runner1") | .id')
gh api --method DELETE /repos/ravitejakamalapuram/TelePort/actions/runners/$RUNNER_ID

# 3. Clean runner directory
rm -rf _work/* _diag/* .credentials*

# 4. Get fresh token and reconfigure
TOKEN=$(gh api --method POST /repos/ravitejakamalapuram/TelePort/actions/runners/registration-token --jq '.token')
./config.sh --url https://github.com/ravitejakamalapuram/TelePort --token $TOKEN --name TelePort-Runner1

# 5. Reinstall service
./svc.sh install
./svc.sh start
./svc.sh status
```

---

## Support Resources

- **GitHub Actions Runner Docs**: https://docs.github.com/en/actions/hosting-your-own-runners
- **Runner Logs**: `~/github-runners/TelePort/Runner1/_diag/`
- **Service Status**: `launchctl list | grep actions.runner`
- **GitHub Status**: https://www.githubstatus.com/
