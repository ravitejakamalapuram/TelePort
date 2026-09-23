# Chrome Web Store Listing & Publishing Record

*Last Updated: 2026-09-20*

---

## 1. Extension Information
- **Name**: TelePort Cast & Remote
- **Extension ID**: `oimdkmacklcheogoakhdgilcedkcbikb`
- **Publisher ID**: `9637cb78-fa33-49dd-a4cb-91066ff182e3`
- **Version**: `1.0.0`
- **Manifest Version**: `MV3`
- **Language**: `en`
- **Category**: `Productivity`

---

## 2. Store Listing Copy

### Short Description (max 132 characters)
> Beam tabs, cast screens, and control your TelePort TV directly from Chrome.

### Detailed Description
```markdown
TelePort Cast & Remote

Beam tabs, cast screens, and control your TelePort TV directly from Chrome.

Key Features:
- Local-first and private: all data operations run strictly inside your browser and local network.
- High performance: fast processing for developer workflows.
- Clean and intuitive interface designed for modern productivity.

How to use:
1. Open the extension from the Chrome toolbar.
2. Select your local TelePort TV device.
3. Cast tabs or screens directly.
```

---

## 3. Permissions Justifications (Required for Review)

Google review requires specific plain-English justification for each declared permission:

| Permission | Used in Code? | Sample Evidence | Required? | Risk | Plain-English Review Justification |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `tabs` | Yes | popup/popup.js:213 | Yes | MEDIUM | Required to inspect tab URLs, monitor tab lifecycle, or coordinate multi-tab workflows. |
| `contextMenus` | Yes | service-worker.js:157 | Yes | LOW | Adds custom action items to the Chrome right-click context menu. |
| `storage` | Yes | popup/popup.js:45 | Yes | LOW | Required to locally persist user settings, configurations, and application state across sessions. |
| `tabCapture` | Yes | service-worker.js:58 | Yes | HIGH | Captures tab visual stream upon explicit user request for media casting or recording. |
| `offscreen` | Yes | service-worker.js:63 | Yes | MEDIUM | Required to perform DOM, audio, or clipboard processing not supported in service workers. |

---

## 4. Privacy & Data Use Disclosure

- **Privacy Policy URL**: `https://ravitejakamalapuram.github.io/teleport.html`

---

## 5. Store Assets Checklist

- [x] Extension Icon (128×128 PNG): `icons/icon-128.png`
- [ ] Primary Screenshot (1280×800 PNG): `chrome-store/assets/screenshots/01-main-screen.png`
- [ ] Promotional Tile (440×280 PNG): Optional but recommended for featured placement
- [ ] Marquee Promo (1400×560 PNG): Optional

---

## 6. Pre-Publish Checklist

- [x] Manifest V3 compliance verified
- [x] No `eval()` or remotely hosted code
- [x] No secrets, private keys, or API tokens in package
- [x] Distributable archive contains `manifest.json` at root
- [ ] Extension registered in Chrome Web Store Developer Dashboard
- [ ] CWS API OAuth credentials configured (`.env`)
- [ ] Final human confirmation obtained before submission

---

## 7. Release History

| Version | Date | Status | Package ZIP | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `1.0.0` | 2026-09-20 | Draft / Ready | `chrome-store/builds/teleport-cast---remote-v1.0.0.zip` | Automated build & verification passed |
