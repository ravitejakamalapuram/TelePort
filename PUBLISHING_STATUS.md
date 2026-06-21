# 📦 TelePort Publishing Status

**Last Updated:** June 21, 2026

---

## 📊 Current Status Overview

| Product | Build Status | Publishing Status | Auto-Deploy |
|---------|-------------|-------------------|-------------|
| **Android App** | ✅ Working | 🟡 Submitted for review | 🟡 Ready (pending approval) |
| **Chrome Extension** | ✅ Working | ❌ Manual only | ❌ Not configured |

---

## 📱 Android App

### Status: **Submitted for Google Play Review**

**What's Working:**
- ✅ Self-hosted CI/CD runner active
- ✅ Builds APK and AAB on every push to `main`
- ✅ Uploads to Play Store alpha track
- ✅ Privacy policy hosted at GitHub Pages
- ✅ Demo video available
- ✅ Accessibility declaration submitted

**Pending:**
- ⏳ **Google Play review in progress** (24-48 hours)
- Once approved, CD workflow will fully auto-deploy

**Next Steps:**
1. Wait for Google Play review results
2. If approved → automatic deployments will work
3. If rejected → check email for required changes

**Resources:**
- Privacy Policy: https://ravitejakamalapuram.github.io/TelePort/privacy.html
- Demo Video: https://ravitejakamalapuram.github.io/TelePort/docs/demo_video.mp4
- Setup Guide: `PLAY_CONSOLE_SETUP.md`

---

## 🌐 Chrome Extension

### Status: **Manual Publishing Only**

**What's Working:**
- ✅ Extension builds automatically on every push
- ✅ Packaged as `extension.zip` in GitHub Releases
- ✅ Available for manual download

**Not Working:**
- ❌ **No auto-publish to Chrome Web Store**
- ❌ Missing Chrome Web Store API credentials

**Why It's Not Working:**
Before migrating to shared workflows, the CD pipeline only **packaged** the extension and uploaded it to GitHub Releases. It **never auto-published** to Chrome Web Store.

The shared workflow **has** a Chrome Web Store publish action (`publish-cws`), but it requires 4 secrets that were never set up:
- `CHROME_EXTENSION_ID`
- `CHROME_CLIENT_ID`
- `CHROME_CLIENT_SECRET`
- `CHROME_REFRESH_TOKEN`

**Next Steps:**
1. Follow setup guide in `CHROME_WEBSTORE_SETUP.md`
2. Add the 4 required secrets to GitHub
3. Uncomment the `publish-chrome` job in `.github/workflows/cd.yml`
4. Push to enable auto-publishing

**Current Workaround:**
1. Download `extension.zip` from [GitHub Releases](https://github.com/ravitejakamalapuram/TelePort/releases/latest)
2. Manually upload to [Chrome Web Store Dashboard](https://chrome.google.com/webstore/devconsole)

**Resources:**
- Setup Guide: `CHROME_WEBSTORE_SETUP.md`
- Latest Release: https://github.com/ravitejakamalapuram/TelePort/releases/latest

---

## 🔧 CI/CD Infrastructure

### Self-Hosted Runner: **Active**

**Location:** `~/github-runners/TelePort/Runner1`

**Status:**
- ✅ Online and processing jobs
- ✅ Auto-updated to latest version (v2.335.1)
- ✅ Running as macOS service

**Commands:**
```bash
# Check status
cd ~/github-runners/TelePort/Runner1 && ./svc.sh status

# View logs
tail -f ~/github-runners/TelePort/Runner1/_diag/Runner_*.log

# Restart if needed
cd ~/github-runners/TelePort/Runner1
./svc.sh stop
./svc.sh start
```

**Workflows Using Self-Hosted Runner:**
- ✅ CI (tests and builds)
- ✅ CD (release and deploy)

---

## 📝 Summary

### ✅ What's Automated:
1. **CI Tests** - Run on every PR and push
2. **Android Build** - APK/AAB created on every push to `main`
3. **Play Store Upload** - AAB uploaded to alpha track
4. **Chrome Extension Package** - ZIP created and uploaded to GitHub Releases
5. **GitHub Releases** - Auto-created with changelog

### ❌ What's Manual:
1. **Chrome Web Store Publishing** - Requires setup (see `CHROME_WEBSTORE_SETUP.md`)
2. **Play Store Approval** - One-time review (in progress)

### 🎯 To Achieve Full Automation:
1. ✅ Wait for Play Store approval (~24-48 hours)
2. ⏳ Set up Chrome Web Store credentials (15 minutes)

---

## 🚀 Future State (After Setup)

**Every push to `main` will:**
1. ✅ Build and test code
2. ✅ Create GitHub Release with changelog
3. ✅ Deploy Android app to Play Store alpha track
4. ✅ Publish Chrome Extension to Chrome Web Store
5. ✅ Update metadata and documentation

**Zero manual intervention required!** 🎉

---

## 📞 Quick Reference

| Need | File |
|------|------|
| Android publishing status | This file |
| Play Store setup | `PLAY_CONSOLE_SETUP.md` |
| Chrome Web Store setup | `CHROME_WEBSTORE_SETUP.md` |
| Demo video guide | `DEMO_VIDEO_GUIDE.md` |
| Runner setup | `RUNNER_SETUP_COMPLETE.md` |
| CI workflow | `.github/workflows/ci.yml` |
| CD workflow | `.github/workflows/cd.yml` |

---

**Need help?** Check the relevant setup guide above or open an issue.
