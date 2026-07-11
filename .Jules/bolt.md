## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-28 - Optimize Flow Collection in High-Frequency Streams
**Learning:** Using `collectLatest` on high-frequency `SharedFlow` streams (like the 100Hz+ sensor-driven commands in `TvEventBus`) causes severe GC pressure, memory allocation overhead, and potential dropped frames because `collectLatest` cancels and restarts the coroutine for every single emission.
**Action:** When handling high-frequency network/sensor event streams using Kotlin Flows, always use `collect` instead of `collectLatest` to ensure stable processing and reduce GC thrashing.
