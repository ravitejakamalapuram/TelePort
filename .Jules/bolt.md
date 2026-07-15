## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-01 - Decouple High-Frequency Hardware Callbacks from Network Pipelines
**Learning:** High-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) bridged directly to expensive network pipelines (like WebSocket broadcasts) cause severe GC pressure, thread pool contention, and network flooding.
**Action:** Accumulate raw sensor changes over a small interval and throttle emitting to ~60Hz (16ms) using `android.os.SystemClock.uptimeMillis()` to decouple the hardware frequency from the processing pipeline.
## 2026-07-02 - Use collect instead of collectLatest in high-frequency Flows
**Learning:** In high-frequency Kotlin Flow collections (e.g., 100Hz+ event streams like sensor commands or websocket updates), `collectLatest` cancels the ongoing suspending operation (like `send()` or UI updates) when a new emission occurs. This causes severe GC pressure, memory allocation overhead, and drops frames or throws `CancellationException` disrupting connections.
**Action:** Prefer `collect` over `collectLatest` to safely process each emission and avoid coroutine cancellation overhead.
