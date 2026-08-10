## $(date +%Y-%m-%d) - Secure WebSocket Connections with Explicit Client Validation
**Vulnerability:** The `/mirror` WebSocket endpoint accepted connections simply if `TvEventBus.approvedClientIds` was not empty. This constituted a critical authorization bypass, allowing any local network device to connect to the mirroring stream simply if at least one legitimate user was currently approved, thereby leaking screen mirroring data.
**Learning:** Checking if a global list is non-empty does not authenticate the specific incoming connection request in Ktor WebSocket handlers.
**Prevention:** Always validate the specific client's identifier (e.g., via query parameters) against the approved list (e.g., `TvEventBus.approvedClientIds.value.contains(clientId)`) during the connection handshake.
