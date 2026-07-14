## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.

## 2024-05-18 - Unsafe WebView Navigation
**Vulnerability:** WebView allowed arbitrary navigation (including `javascript:`, `intent:`, `file:`) when links were clicked, because only the initial `loadUrl` was sanitized.
**Learning:** `loadUrl` sanitization is insufficient. You must implement `shouldOverrideUrlLoading` in `WebViewClient` to validate all subsequent navigations against a scheme allowlist.
**Prevention:** Always implement an allowlist of safe schemes (`http`, `https`, `about`, `data`) in `shouldOverrideUrlLoading` for webviews rendering untrusted content.
