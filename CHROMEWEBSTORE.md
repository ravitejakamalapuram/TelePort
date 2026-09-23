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

TelePort Cast & Remote is the Chrome companion for the TelePort Android TV app. Send the page you are viewing to your TV, or mirror a tab's video to it, over your local Wi-Fi network.

Features:
- Send page to TV: open the current tab's URL in the TelePort browser on your TV.
- Mirror tab: stream the current tab's video to the TV.
- Right-click menu shortcuts for sending or mirroring the current page.
- Remote control: move the TV cursor, click, scroll, go back, play/pause, type text and toggle the TV's dark mode from the popup.
- Local-only: the extension talks directly to your TV at the IP address you enter; no cloud servers, accounts or analytics.

How to use:
1. Install the TelePort app on your Android TV and note its IP address.
2. Open the extension from the Chrome toolbar and enter the TV's IP address.
3. Send or mirror the current tab to your TV.
```

---

## 3. Permissions Justifications (Required for Review)

Google review requires specific plain-English justification for each declared permission:

| Permission | Used in Code? | Sample Evidence | Required? | Risk | Plain-English Review Justification |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `tabs` | Yes | popup/popup.js:213 | Yes | MEDIUM | Reads the URL of the active tab when the user clicks 'Send to TV' (popup or right-click menu) so that URL can be opened on the user's TelePort TV, and identifies the tab to mirror. |
| `contextMenus` | Yes | service-worker.js:157 | Yes | LOW | Adds two right-click menu items, 'Send active page to TelePort TV' and 'Mirror active tab to TelePort TV', as shortcuts for the extension's casting actions. |
| `storage` | Yes | popup/popup.js:45 | Yes | LOW | Stores the TV's local IP address the user enters (chrome.storage.local) and the current casting state (chrome.storage.session). |
| `tabCapture` | Yes | service-worker.js:58 | Yes | HIGH | Captures the video of the current tab, only after the user starts 'Mirror tab', so it can be streamed to the user's TV on the local network. |
| `offscreen` | Yes | service-worker.js:63 | Yes | MEDIUM | Hosts the offscreen document that encodes the captured tab video (WebCodecs) and streams it to the TV over a WebSocket, since service workers cannot access media streams. |
| `host_permissions` (`http://*/*`, `https://*/*`) | Yes | service-worker.js:26 | Yes | HIGH | The extension connects to the TelePort TV app at the local IP address the user enters (e.g. ws://192.168.x.x:8080). That address is user-configured and not known in advance, so a fixed host list is not possible. No page content is read or modified. |

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
