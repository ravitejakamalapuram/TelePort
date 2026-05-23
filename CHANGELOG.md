# Changelog

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
