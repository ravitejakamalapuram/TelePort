## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.
## 2026-07-15 - WebView Scheme Allowlist Bypass
**Vulnerability:** WebView allowlist for safe schemes failed open when the parsed URI scheme was null (e.g. from whitespace-padded URLs like ` javascript:alert()`).
**Learning:** `android.net.Uri.parse` returns null for the scheme if the URL string has leading whitespace, bypassing simple scheme-based allowlists that return false (allow) when scheme is null.
**Prevention:** Always trim URL strings before parsing, and ensure `shouldOverrideUrlLoading` fails closed by returning `true` (block) when the scheme is null.
## 2026-06-25 - Extracted Client Identifier Bypass
**Vulnerability:** The `/mirror` WebSocket endpoint checked if `TvEventBus.approvedClientIds` was populated, but did not actually verify that the *connecting* client was one of the approved clients.
**Learning:** Merely checking that a global authorization state is valid (e.g. `isEmpty()`) is insufficient; you must validate the specific identity or token of the client attempting to connect.
**Prevention:** Always extract and validate the specific client identifier (e.g. via `clientId` query parameter) against the approved list on every sensitive WebSocket connection.
