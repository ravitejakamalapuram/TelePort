## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.
## 2026-07-15 - WebView Scheme Allowlist Bypass
**Vulnerability:** WebView allowlist for safe schemes failed open when the parsed URI scheme was null (e.g. from whitespace-padded URLs like ` javascript:alert()`).
**Learning:** `android.net.Uri.parse` returns null for the scheme if the URL string has leading whitespace, bypassing simple scheme-based allowlists that return false (allow) when scheme is null.
**Prevention:** Always trim URL strings before parsing, and ensure `shouldOverrideUrlLoading` fails closed by returning `true` (block) when the scheme is null.
## 2026-07-30 - WebSocket Mirror Connection Authentication Bypass
**Vulnerability:** The `/mirror` endpoint simply checked `!TvEventBus.approvedClientIds.value.isEmpty()` without authenticating the specific connection. This meant that once *any* user was approved to control the TV, *any other* device on the local network could connect to the mirror stream without approval.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting. Always implement explicit authorization state checks within WebSocket endpoints by validating the specific connecting client's identifier (e.g., via query parameters) against an approved list before accepting connections. Merely checking if the approved list is non-empty creates an authorization bypass.
**Prevention:** Pass a unique `clientId` (or token) during initial connection that gets approved by the user. Then, require that same `clientId` for all subsequent privileged connections and explicitly check that it exists in the approved list.
