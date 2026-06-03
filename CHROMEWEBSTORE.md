# Chrome Web Store Listing — TelePort Cast & Remote

> Last Updated: 2026-06-02

## Store Listing

**Extension Name**
TelePort Cast & Remote

**Short Description**
Beam tabs, cast screens, and control your TelePort TV directly from Chrome.

**Detailed Description**
TelePort Cast & Remote enables seamless screen sharing, tab casting, and remote control capabilities for your TelePort TV system. Instantly send your active tab, full screen, or media stream to your local receiver over Wi-Fi without any configuration.

**Category**
Productivity

**Single Purpose**
Enables users to stream browser tabs, screens, and media directly to local TelePort TV devices over the local network.

**Primary Language**
English

## Graphics & Assets

| Asset | Dimensions | Status | Filename |
|---|---|---|---|
| Store Icon | 128×128 PNG | ✅ Ready | chrome-extension/icons/icon-128.png |

## Permissions Justification

Every permission in manifest.json needs a justification. The review team reads these.

| Permission | Type | Justification |
|---|---|---|
| `tabs` | permissions | Used to query current browser tab details to initialize tab screen-casting. |
| `contextMenus` | permissions | Used to register cast shortcuts in the browser context menu. |
| `storage` | permissions | Used to store configurations and paired receiver settings locally. |
| `tabCapture` | permissions | Used to capture the video and audio stream of the active tab for local network streaming. |
| `offscreen` | permissions | Used to spawn background audio-processing or screen-capture context helpers. |
| `http://*/*` | host_permissions | Used to capture and stream web content to the local cast receiver device. |
| `https://*/*` | host_permissions | Used to capture and stream web content to the local cast receiver device. |

## Privacy & Data Use

### Data Collection
**Does the extension collect user data?** No

All extension preferences and inputs are stored locally on the device and never sent off-device.

### Data Use Certification
- [x] Data is NOT sold to third parties
- [x] Data is NOT used for purposes unrelated to the extension's core functionality
- [x] Data is NOT used for creditworthiness or lending purposes

## Privacy Policy
Privacy Policy available in `PRIVACY.md` in the project root. Recommended to host via GitHub Pages.

## Version History

| Version | Date | Changes | Status |
|---|---|---|---|
| 1.0.0 | 2026-06-02 | Initial release with tab and screen casting capabilities. | Active |
