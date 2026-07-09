## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-29 - High-Frequency Event Collection
**Learning:** Using `collectLatest` on high-frequency `Flow` events (like TV commands mapping to AirMouse sensor streams running at 100Hz+) causes severe performance degradation, high GC pressure, and dropped frames because `collectLatest` cancels and restarts its internal coroutine block upon every new emission.
**Action:** Replace `collectLatest` with `collect` for processing high-frequency continuous event streams that only perform non-suspending synchronous work, avoiding the cancellation overhead.
