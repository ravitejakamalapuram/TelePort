## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-25 - Replace collectLatest with collect for high-frequency Flow streams
**Learning:** In high-frequency event streams (like processing gyroscope sensor commands at 100Hz+ over Ktor WebSockets or TV UI updates), using Kotlin Flow's `collectLatest` is a subtle but severe performance bottleneck. Every time a new emission arrives before the previous block finishes, `collectLatest` cancels the current execution coroutine and launches a new one. This causes immense Garbage Collection (GC) pressure due to continuous coroutine cancellation and allocation, and can even drop events or destabilize WebSocket state.
**Action:** Always prefer `collect` over `collectLatest` for high-frequency, sequentially processed UI or command streams to safely eliminate coroutine allocation overhead and GC thrashing.
