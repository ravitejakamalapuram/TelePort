# Changelog

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
