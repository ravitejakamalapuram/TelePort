# Chrome Web Store Listing — TelePort Cast & Remote

> Last Updated: 2026-05-26

## Store Listing

**Extension Name**  
TelePort Cast & Remote

**Short Description**  
Beam tabs, cast screens, and control your TelePort TV directly from Chrome.

**Detailed Description**  
TelePort Cast & Remote is the companion Chrome extension for the TelePort local-first ecosystem. It allows you to control and cast content directly from your Chrome browser to any Android TV running the TelePort receiver app.

Key Features:
- Beam Page: Instantly send the URL of the active tab to your TelePort TV for native full-screen viewing.
- Tab Mirroring (Cast): Capture and stream the active tab's video and audio in real-time over your local Wi-Fi network.
- Context Menu Shortcuts: Right-click anywhere on a webpage to quickly beam the page or start mirroring your tab.
- Zero Cloud Dependencies: Communicates directly with your TV receiver over the local network via WebSockets, ensuring fast transmission and complete data privacy.

How to Use:
1. Ensure your Android TV is running the TelePort receiver app.
2. Click the TelePort extension icon in your Chrome toolbar.
3. Enter your TV's local IP address (shown on the TV screen) and save it.
4. Click "Beam URL" to send the current tab to the TV, or "Start Cast" to mirror the tab's audio and video.
5. Alternatively, right-click on any page and choose the TelePort context menu options.

Privacy & Security:
This extension operates entirely locally. No browsing history, personal data, or captured media is ever transmitted to external servers or cloud services. All communication is point-to-point over your local network.

**Category**  
Productivity

**Single Purpose**  
Beam browser tabs and cast media streams to a TelePort TV receiver on the local network.

**Primary Language**  
English


## Graphics & Assets

| Asset | Dimensions | Status | Filename |
|-------|-----------|--------|----------|
| Store Icon [REQUIRED] | 128×128 PNG | ✅ Ready | `chrome-extension/icons/icon-128.png` |
| Screenshot 1 [REQUIRED] | 1280×800 or 640×400 | ⬜ Not created | `store-assets/screenshot-popup.png` |
| Screenshot 2 [RECOMMENDED] | 1280×800 or 640×400 | ⬜ Not created | `store-assets/screenshot-casting.png` |

### Screenshot Notes
- **Screenshot 1 (Popup Interface)**: Shows the extension popup with the TV IP address field, connection state, and the "Beam URL" & "Start Cast" control buttons.
- **Screenshot 2 (Casting Indicator)**: Displays the browser window during casting with the red recording badge and active transmission.


## Permissions Justification

| Permission | Type | Justification |
|------------|------|---------------|
| `tabs` | permissions | Query active tabs to retrieve the current URL and page title to beam or mirror to the TV. |
| `contextMenus` | permissions | Create right-click context menu options for quick access to "Send page to TV" and "Mirror tab". |
| `storage` | permissions | Persist the TV's IP address locally on the device to maintain connection across browser restarts. |
| `tabCapture` | permissions | Capture the active tab's video and audio media stream for real-time mirroring to the TV. |
| `offscreen` | permissions | Run a background offscreen document to process and stream the captured tab media frames to the TV receiver via WebSockets. |
| `http://*/*` | host_permissions | Capture tabs and execute context menu commands on HTTP web pages. |
| `https://*/*` | host_permissions | Capture tabs and execute context menu commands on HTTPS web pages. |


## Privacy & Data Use

### Data Collection

**Does the extension collect user data?** No

All data stays on the local device or is transmitted directly to the user's TV on the local network. No remote databases, analytics endpoints, or external third-party services are accessed.

### Data Use Certification
- [x] Data is NOT sold to third parties
- [x] Data is NOT used for purposes unrelated to the extension's core functionality
- [x] Data is NOT used for creditworthiness or lending purposes


## Privacy Policy

**Privacy Policy URL**  
`https://ravitejakamalapuram.github.io/TelePort/privacy-policy.html` *(Recommended placeholder — should be hosted on a public GitHub Pages URL or developer website)*

### Privacy Policy Content
```markdown
# Privacy Policy for TelePort Cast & Remote

Last updated: 2026-05-26

TelePort Cast & Remote (the "Extension") values your privacy. This privacy policy explains our practices regarding user data.

## What Data We Collect & Process
The Extension does not collect, record, or store any personal data, user credentials, or browsing history. It does not track user behavior or capture analytics.

For the purpose of casting, the Extension utilizes:
1. **TV IP Address**: Stored locally on your device via `chrome.storage.local` to establish a network connection with your TV receiver.
2. **Tab Media Stream**: The video and audio of the active browser tab are captured via `chrome.tabCapture` and transmitted directly to your TV over your local Wi-Fi network. This stream is transient, is never saved, and never leaves your local network.

## How Data is Transmitted
All communication between the Extension and the TelePort TV receiver is point-to-point and happens entirely within your local Wi-Fi network. No data is sent to external servers, cloud services, or third parties.

## Third-Party Services
This Extension does not integrate any third-party SDKs, analytics packages, or advertising services.

## Contact
If you have any questions or feedback regarding this policy, please contact raviteja369.k@gmail.com.
```


## Distribution

**Visibility**: Public  
**Regions**: All regions  
**Pricing**: Free  


## Developer Info

**Publisher Name**  
Raviteja Kamalapuram

**Contact Email**  
raviteja369.k@gmail.com

**Support URL / Email**  
`https://github.com/ravitejakamalapuram/TelePort/issues`

**Homepage URL**  
`https://github.com/ravitejakamalapuram/TelePort`


## Version History

| Version | Date | Changes | Status |
|---------|------|---------|--------|
| 1.0.0 | 2026-05-26 | Initial release of TelePort Cast & Remote extension. | Draft |


## Review Notes

### Known Issues / Limitations
- Casting requires both the computer and the Android TV to be on the same local subnet.
- The Ktor server on the Android TV must be running for connection or beaming to succeed.
