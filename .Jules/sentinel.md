## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.
## 2026-07-15 - WebView Scheme Allowlist Bypass
**Vulnerability:** WebView allowlist for safe schemes failed open when the parsed URI scheme was null (e.g. from whitespace-padded URLs like ` javascript:alert()`).
**Learning:** `android.net.Uri.parse` returns null for the scheme if the URL string has leading whitespace, bypassing simple scheme-based allowlists that return false (allow) when scheme is null.
**Prevention:** Always trim URL strings before parsing, and ensure `shouldOverrideUrlLoading` fails closed by returning `true` (block) when the scheme is null.
## 2024-05-24 - Missing Authorization Check on WebSocket Endpoint
**Vulnerability:** The `/mirror` WebSocket endpoint in `LocalServerService.kt` only checked if the `approvedClientIds` set was non-empty, allowing any device on the local network to connect and stream video frames if at least one authorized client existed.
**Learning:** Merely checking if an approved list is non-empty creates an authorization bypass. Explicit authorization state checks must validate the specific connecting client's identifier.
**Prevention:** Always implement explicit authorization state checks within WebSocket endpoints by validating the specific connecting client's identifier (e.g., via query parameters) against an approved list before accepting connections.
