## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .

## 2026-07-05 - Prevent GC and coroutine cancellation overhead on high-frequency flow collections
**Learning:** In high-frequency Kotlin Flow collections (e.g., 100Hz+ event streams like sensor commands or state updates), using `collectLatest` continuously cancels and restarts coroutines on every emission, causing severe GC pressure, memory allocation overhead, and potential dropped frames.
**Action:** Prefer `collect` over `collectLatest` to process high-frequency streams without the massive coroutine cancellation overhead.
