## 2025-05-24 - Fix WebSocket authorization bypass
**Vulnerability:** LocalServerService allows unauthorized mirroring via Ktor WebSockets.
**Learning:** Checking for an empty list of approved clients bypasses specific authorization validation.
**Prevention:** Validate the specific connecting client identifier against the approved list for authorization.
