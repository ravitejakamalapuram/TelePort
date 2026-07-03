## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-04 - High Frequency Flow Collection
**Learning:** In high-frequency Kotlin Flow streams (like 100Hz+ sensor data or command streams), using `collectLatest` causes severe GC pressure, memory allocation overhead, and potential dropped frames because it continually cancels and restarts a new coroutine on every emission.
**Action:** Prefer `collect` over `collectLatest` for high-frequency event streams to process emissions sequentially and avoid coroutine allocation/cancellation overhead.
