# TelePort - Production Release Guide

**App Version:** 1.0.0 (versionCode: 6)  
**Release Type:** First Production Release (Free App)  
**Date:** 2026-06-09

---

## ✅ Changes Made for Production

### 1. **Removed AdMob Test ID**
- ❌ Removed test AdMob App ID from `AndroidManifest.xml`
- ❌ Removed AdMob ad services configuration
- ✅ App is now free with no ads

### 2. **Removed Monetization Dependencies**
- ❌ Removed `play-services-ads` (AdMob SDK)
- ❌ Removed `user-messaging-platform` (GDPR/CCPA consent)
- ❌ Removed `billing-ktx` (Google Play Billing)
- ✅ Reduced APK size by ~4MB

### 3. **Feature Configuration**
- ✅ `ENABLE_PAID_SUBSCRIPTIONS = false`
- ✅ `ENABLE_ADVERTISEMENTS = false`
- ✅ **All premium features unlocked for all users**

### 4. **Version Bump**
- `versionCode`: 5 → **6**
- `versionName`: 1.0 → **1.0.0**

---

## 🚀 How to Build Production AAB

### Step 1: Clean Build
```bash
./gradlew clean
```

### Step 2: Build Release AAB
```bash
./gradlew bundleRelease
```

### Step 3: Locate the AAB
The signed AAB will be created at:
```
app/build/outputs/bundle/release/app-release.aab
```

### Step 4: Verify the Build
Check the file size (should be ~15-20MB):
```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
```

---

## 📦 Upload to Google Play Console

### Step 1: Go to Production Track
1. Open [Google Play Console](https://play.google.com/console/)
2. Select your app: **TelePort**
3. Navigate to **Release** → **Production**
4. Click **Create new release**

### Step 2: Upload AAB
1. Click **Upload** or drag `app-release.aab`
2. Wait for upload to complete
3. Google Play will generate optimized APKs for different devices

### Step 3: Add Release Notes
```
Version 1.0.0 - First Production Release

🎉 TelePort is now live!

Features:
• Full-featured Android TV web browser
• Mobile remote control with air mouse gestures
• QR code instant pairing
• Multi-tab browsing with ad blocker
• Automatic video extraction and native playback
• Zero-cloud dependency - works completely offline

All features are FREE and unlocked for everyone!

Enjoy browsing on your TV! 📺
```

### Step 4: Review and Roll Out
1. Review the release details
2. Click **Review release**
3. Click **Start rollout to Production**
4. Choose rollout percentage:
   - **Staged rollout:** 20% → 50% → 100% (safer)
   - **Full rollout:** 100% (faster)

---

## 📋 Pre-Release Checklist

Before uploading to production, ensure these are complete in Play Console:

- [x] **Store Listing** - Name, description, screenshots, icon
- [x] **Content Rating** - IARC questionnaire completed
- [x] **Data Safety** - Data collection practices declared
- [x] **Target Audience** - Age range selected
- [x] **App Access** - Special requirements documented
- [x] **Privacy Policy** - URL added and accessible
- [x] **Production Questionnaire** - Submitted and approved

---

## 🔍 Post-Release Monitoring

### Firebase Crashlytics
Monitor crashes after release:
```bash
# Fetch recent crashes
python3 scripts/fetch_crashes.py
```

### Play Console Metrics
Monitor these metrics daily for the first week:
- Crash rate (should be <1%)
- ANR rate (should be <0.5%)
- Install/uninstall rate
- User reviews and ratings

### Version Rollback
If critical issues are found:
1. Go to **Production** → **Releases**
2. Click **Halt rollout** (for staged rollout)
3. Fix issues in new version
4. Upload patched AAB as version 1.0.1 (versionCode: 7)

---

## 🎯 Future Monetization (Optional)

When ready to add monetization in a future update:

### Option 1: Add Subscriptions
1. Create subscription products in Play Console
2. Set `ENABLE_PAID_SUBSCRIPTIONS = true`
3. Add back `billing-ktx` dependency
4. Release as version 1.1.0

### Option 2: Add Ads
1. Create AdMob account and get production App ID
2. Add production App ID to `AndroidManifest.xml`
3. Set `ENABLE_ADVERTISEMENTS = true`
4. Add back AdMob dependencies
5. Release as version 1.1.0

### Option 3: Both
Freemium model with ads for free users and subscription to remove ads.

---

## ✅ Summary

**Current Status:** Production-ready free app  
**All Features:** Unlocked for all users  
**No Ads:** Clean, ad-free experience  
**No Subscriptions:** Completely free  
**Ready to Upload:** YES ✅

Good luck with your launch! 🚀
