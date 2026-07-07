## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .

## 2026-07-07 - collectLatest vs collect in High-Frequency Flows
**Learning:** Using `collectLatest` on a `StateFlow` or `SharedFlow` with high-frequency updates (e.g. 100Hz gyroscope data) causes extreme GC pressure, dropped frames, and interrupted network operations. Each new emission cancels the ongoing suspension inside the collector (such as Ktor's `WebSocketSession.send`), resulting in `CancellationException` and disconnected WebSockets.
**Action:** Always prefer `collect` over `collectLatest` for handling continuous, high-frequency streams where every event must be processed sequentially without cancelling the current execution block.
