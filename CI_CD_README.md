# CI/CD Documentation - TelePort Project

This directory contains all documentation related to the TelePort project's CI/CD pipeline using GitHub Actions with self-hosted runners.

---

## 📋 Quick Links

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [CI_CD_STATUS.md](CI_CD_STATUS.md) | Current status and health of CI/CD | Check pipeline status, recent fixes |
| [RUNNER_TROUBLESHOOTING.md](RUNNER_TROUBLESHOOTING.md) | Complete troubleshooting guide | Jobs stuck in queue, runner offline |
| [RUNNER_SETUP.md](RUNNER_SETUP.md) | Initial runner setup instructions | Setting up new runner, fresh install |
| [CI_CD_FIX_SUMMARY_2026-06-22.md](CI_CD_FIX_SUMMARY_2026-06-22.md) | Latest fix details | Understanding recent fixes |

---

## 🚨 Quick Fix for Common Issues

### Jobs Stuck in Queue?
```bash
./scripts/fix-runner.sh
```

### Runner Offline?
```bash
cd ~/github-runners/TelePort/Runner1
./svc.sh start
```

### Need to Check Status?
```bash
gh api repos/ravitejakamalapuram/TelePort/actions/runners --jq '.runners[] | {name, status, busy}'
```

---

## 🔧 Available Scripts

### `scripts/fix-runner.sh`
**Purpose**: Restart runner service to clear network issues  
**When to use**: Jobs queued, network timeouts, stale sessions  
**Usage**: `./scripts/fix-runner.sh`

### `scripts/setup-runner.sh`
**Purpose**: Automated runner installation and setup  
**When to use**: Fresh install, complete runner reset  
**Usage**: `./scripts/setup-runner.sh`

---

## 📊 Pipeline Overview

### CI Workflow (`.github/workflows/ci.yml`)
- **Triggers**: Push to main, Pull requests
- **Runner**: Self-hosted `TelePort-Runner1`
- **Actions**:
  - Runs unit tests
  - Builds debug APK
  - Validates Chrome Extension

### CD Workflow (`.github/workflows/cd.yml`)
- **Triggers**: Push to main, Manual dispatch
- **Runner**: Self-hosted `TelePort-Runner1`
- **Actions**:
  - Generates changelog
  - Bumps version and creates tags
  - Builds release APK/AAB
  - Packages Chrome Extension
  - Deploys to Google Play Store

---

## 🏥 Health Monitoring

### Daily Check
```bash
# Quick health check
gh run list --limit 5
```

### Weekly Check
```bash
# Check for queued jobs
gh run list --json status | jq 'map(select(.status=="queued")) | length'

# View runner status
gh api repos/ravitejakamalapuram/TelePort/actions/runners \
  --jq '.runners[] | {name, status, busy}'
```

### Monthly Maintenance
```bash
# Proactive restart to prevent issues
./scripts/fix-runner.sh

# Clean old logs
cd ~/github-runners/TelePort/Runner1
ls -t _diag/Runner_*.log | tail -n +10 | xargs rm --
```

---

## 📝 Common Commands

### Check Workflow Runs
```bash
# List recent runs
gh run list --limit 10

# View specific run
gh run view <run-id>

# View run logs
gh run view <run-id> --log
```

### Runner Management
```bash
# Check service status
cd ~/github-runners/TelePort/Runner1
./svc.sh status

# Stop service
./svc.sh stop

# Start service
./svc.sh start

# Restart service
./svc.sh stop && sleep 3 && ./svc.sh start
```

### View Logs
```bash
# Live tail runner logs
tail -f ~/github-runners/TelePort/Runner1/_diag/Runner_*.log

# Search for errors
grep -i error ~/github-runners/TelePort/Runner1/_diag/Runner_*.log

# Last 100 lines
tail -100 ~/github-runners/TelePort/Runner1/_diag/Runner_*.log
```

---

## 🎯 Current Status

**Last Updated**: 2026-06-22  
**CI Status**: ✅ Operational  
**CD Status**: ✅ Operational  
**Runner ID**: 24  
**Runner Status**: Online  
**Last Fix**: Network timeout issues (2026-06-22)

---

## 📚 Issue History

### 2026-06-22: Network Timeout & Queued Jobs
- **Problem**: Jobs stuck in queue, network timeouts
- **Solution**: Restarted runner service
- **Details**: [CI_CD_FIX_SUMMARY_2026-06-22.md](CI_CD_FIX_SUMMARY_2026-06-22.md)

### 2026-06-06: Runner Stale Session
- **Problem**: CD jobs queued, runner appeared online but inactive
- **Solution**: Removed and re-registered runner
- **Details**: See [CI_CD_STATUS.md](CI_CD_STATUS.md) - Historical section

---

## 🆘 Getting Help

1. **Check Status**: [CI_CD_STATUS.md](CI_CD_STATUS.md)
2. **Try Quick Fix**: `./scripts/fix-runner.sh`
3. **Consult Troubleshooting**: [RUNNER_TROUBLESHOOTING.md](RUNNER_TROUBLESHOOTING.md)
4. **Review Logs**: `tail -100 ~/github-runners/TelePort/Runner1/_diag/Runner_*.log | grep -i error`
5. **Check GitHub Status**: https://www.githubstatus.com/

---

## 🔐 Security Notes

- Runner credentials stored in: `~/github-runners/TelePort/Runner1/.credentials*`
- Service runs as current user (not root)
- Runner has access to repository secrets for builds and deployments
- Logs may contain sensitive information - handle with care

---

## 🚀 Future Improvements

- [ ] Set up monitoring alerts for queued jobs
- [ ] Automated monthly runner restarts via cron
- [ ] Runner health dashboard
- [ ] Backup runner for high availability
- [ ] Metrics collection for job execution times

---

**Maintained by**: TelePort Development Team  
**Support**: See troubleshooting guide or check workflow run logs
