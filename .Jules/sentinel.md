## 2026-06-21 - Unauthorized WebSocket Mirror Connection
**Vulnerability:** The `/mirror` WebSocket endpoint allowed any device on the local network to establish a connection and stream video frames without requiring prior approval from the TV host, leading to unauthorized streaming.
**Learning:** In local network Ktor applications, standard CSWSH protections (Origin checks) do not sufficiently prevent unauthorized local devices from connecting.
**Prevention:** Always implement explicit authorization state checks (e.g., verifying against an `approvedClientIds` list) within all sensitive WebSocket endpoints before accepting connections or processing frames.
## 2026-07-15 - WebView Scheme Allowlist Bypass
**Vulnerability:** WebView allowlist for safe schemes failed open when the parsed URI scheme was null (e.g. from whitespace-padded URLs like ` javascript:alert()`).
**Learning:** `android.net.Uri.parse` returns null for the scheme if the URL string has leading whitespace, bypassing simple scheme-based allowlists that return false (allow) when scheme is null.
**Prevention:** Always trim URL strings before parsing, and ensure `shouldOverrideUrlLoading` fails closed by returning `true` (block) when the scheme is null.
## 2026-07-20 - Unauthorized Mirror Connection Bypass
**Vulnerability:** The `/mirror` WebSocket endpoint only checked if `TvEventBus.approvedClientIds.value.isEmpty()`, allowing any device on the network to connect and stream if *any* device was approved.
**Learning:** Checking for the existence of any approved state is insufficient for endpoint authorization. This created an Insecure Direct Object Reference (IDOR) equivalent where holding a valid session wasn't explicitly tied to the connecting identity.
**Prevention:** Always authenticate individual WebSocket connections by generating an unguessable unique token (e.g. UUIDv4) on the approved client, passing it as a query parameter during connection, and explicitly validating it against the allowed list.
