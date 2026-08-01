## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.
## 2026-07-15 - WebView Scheme Allowlist Bypass
**Vulnerability:** WebView allowlist for safe schemes failed open when the parsed URI scheme was null (e.g. from whitespace-padded URLs like ` javascript:alert()`).
**Learning:** `android.net.Uri.parse` returns null for the scheme if the URL string has leading whitespace, bypassing simple scheme-based allowlists that return false (allow) when scheme is null.
**Prevention:** Always trim URL strings before parsing, and ensure `shouldOverrideUrlLoading` fails closed by returning `true` (block) when the scheme is null.

## 2024-08-01 - Fix WebSocket Authorization Bypass
**Vulnerability:** The `/mirror` WebSocket endpoint in `LocalServerService.kt` allowed any local network client to connect simply by checking if `TvEventBus.approvedClientIds.value.isEmpty()`. This created an authorization bypass where any device could intercept the mirror stream if at least one client was approved.
**Learning:** Merely checking if an approved list is non-empty does not validate the specific client connecting. Even in local network apps, endpoints must authenticate the specific client session.
**Prevention:** Always require clients to transmit a unique identifier (like `clientId`) during connection (e.g. via query parameters) and validate that specific identifier against the server's approved list before accepting the connection.
