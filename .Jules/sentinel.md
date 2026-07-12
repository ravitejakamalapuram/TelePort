## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.

## 2026-07-12 - WebView URI Scheme Bypass in Link Clicks
**Vulnerability:** The WebView sanitized the initial `loadUrl()` call to prevent malicious schemes (`javascript:`, `file:`, `intent:`), but failed to override `shouldOverrideUrlLoading` in the `WebViewClient`. This allowed a malicious page to execute arbitrary schemes when a user clicked a link.
**Learning:** Sanitizing entry points is not enough. You must also secure navigation events within the WebView by explicitly overriding `shouldOverrideUrlLoading` and enforcing an allowlist of safe schemes (e.g., `http`, `https`).
**Prevention:** Always implement `shouldOverrideUrlLoading` when configuring a WebView and validate the `request.url.scheme` against a strict allowlist. Avoid redundant string conversions by using the parsed `Uri` provided in the request.
