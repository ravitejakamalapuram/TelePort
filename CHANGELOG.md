# Changelog

## [2026-05-29] - Automated Release

- fix(test): correct Robolectric qualifier to mdpi for TV pairing screen to prevent cropping (2e6e832) by Raviteja Kamalapuram
- docs: fix broken logo icon on GitHub Pages (ce45d0e) by Raviteja Kamalapuram


## [2026-05-29] - Automated Release

- fix(mobile): implement custom rotatable QrScanActivity with close button (c25aff0) by Raviteja Kamalapuram


## [2026-05-28] - Automated Release

- feat: implement headless video casting and native player controls (#26) (177450d) by raviteja kamalapuram


## [2026-05-27] - Automated Release

- Fix ForegroundServiceDidNotStartInTimeException on Android 14+ by specifying service types in startForeground calls (#22) (331f03f) by raviteja kamalapuram


## [2026-05-27] - Automated Release

- 🛡️ Sentinel: [HIGH] Secure WebView against Mixed Content and Local File Access (#21) (260501d) by raviteja kamalapuram


## [2026-05-26] - Automated Release

- ⚡ Bolt: Optimize QR code generation using setPixels (#13) (40047b4) by raviteja kamalapuram


## [2026-05-26] - Automated Release

- chore: delete GitHub Actions crash-monitor workflow in favor of local scheduler (4778fc6) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- fix: resolve air remote lag, invert vertical axis, and persist tv browser screen when remote disconnects (d9f071b) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- feat: implement single source of truth for branding tokens, app name, and port settings (44b0b22) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- docs: Align landing page branding, security, and connection loader states (929c3b9) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- feat: implement Wave 4 Chrome Extension, design guidelines, and dev runner CLI (45d47f8) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- Request runtime POST_NOTIFICATIONS permission on startup for Android 13+ devices (8d70481) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- Update pairing and controller screen assets after namespace refactoring and test improvements (557e1ca) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- ci: remove transitive AD_ID permission to resolve Play Store upload policies (d439440) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- ci: fail the On Merge to Main workflow if Google Play upload fails (ebd6c7c) by Raviteja Kamalapuram


## [2026-05-25] - Automated Release

- Expose gh pr create stderr in analyze_crash.sh to diagnose failure (92e7699) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- docs: update compliance video with real emulator recordings (side-by-side TV & Mobile Remote) (bfcc017) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- docs: add automatically generated compliance demo video for Play Store review (460a255) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- fix: generate all 5 screenshots (pairing & controller sub-tabs) and delete old controller screen (79c13b1) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- fix: enable native graphics for Robolectric to generate non-blank screenshots (bf04425) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- docs: add premium Privacy Policy page for Google Play Store compliance (7672777) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- feat: migrate in-app update notification to official Google Play In-App Updates SDK (1cc056f) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- ci: upgrade versionCode to 5, set up flavor dimensions, and add local in-app updates (e3546ed) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- ci: upgrade compileSdk and targetSdk to API 35 and suppress warnings (9513069) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- ci: support passing keystore parameters via Gradle project properties (8641abb) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- ci: add continue-on-error and change track to tracks for Google Play upload (b747d72) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- Complete local screen mirroring implementation and fix tests (3f63fa9) by Raviteja Kamalapuram


## [2026-05-24] - Automated Release

- ci: fix r0adkll/upload-google-play action version tag (025978c) by Raviteja Kamalapuram


## [2026-05-23] - Automated Release

- Implement Wave 2 features: Zero-Install Web Remote, Multi-Controller Co-Browsing, Smart Dark Mode, and Clipboard Sync (0c29a66) by Raviteja Kamalapuram


## [2026-05-23] - Automated Release

- ci: configure automated release tag, sign, compile, and publish release workflow (0e66e8c) by Raviteja Kamalapuram


## [2026-05-23] - Automated Release

- docs: add initial CHANGELOG.md (92a20ee) by Raviteja Kamalapuram


All notable changes to this project will be documented in this file.

## [2026-05-23] - Automated Release

- Implement TV Boot Receiver, auto-foreground on remote connection, and configure unit test options (3f917d0) by Raviteja Kamalapuram

## [1.0.0] - 2026-05-23
### Added
- Unified single-module architecture supporting runtime detection of TV or Mobile Phone devices.
- Local network pairing using mDNS service discovery and QR Code scanning fallback.
- TV Browser featuring multi-tab browsing, an ad/popup blocker, and a virtual cursor overlay.
- Mobile Companion Remote with virtual trackpad, air mouse mode, D-pad, and direct keyboard input.
- Native Media3 ExoPlayer stream extractor on TV for clean media playback.
- Custom premium app branding assets (logo, banner, and Android resources).
- Automated asset generator script (`scripts/generate_assets.py`).
- Automated JVM screenshot capture tests using Robolectric.
- GitHub Actions CI/CD workflows for automated release changelogs and screenshot captures on PR merges to `main`.
