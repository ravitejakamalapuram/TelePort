## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-29 - Optimize Flow Collection for High-Frequency Events
**Learning:** In high-frequency Kotlin Flow streams (e.g., sensor data updates or UI commands), using `collectLatest` is an anti-pattern. `collectLatest` launches a new coroutine for every emission and cancels the previous one, leading to severe GC pressure, memory allocation overhead, and potential event dropping when events arrive rapidly (e.g., at 100Hz).
**Action:** When handling sequential, high-frequency events where processing is fast and non-suspending, always use `collect` instead of `collectLatest` to process events in order without the overhead of continuous coroutine allocation and cancellation.
## 2026-06-30 - Decouple High-Frequency Sensor Interrupts
**Learning:** High-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) can overwhelm expensive network/allocation pipelines, leading to severe GC pressure, thread pool contention, and WebSocket network flooding.
**Action:** Decouple these fast events from the pipeline by accumulating the state/data (like smoothed cursor changes) over a small interval and throttling the emission (e.g., to ~60Hz or 16ms) using `android.os.SystemClock.uptimeMillis()` rather than directly bridging hardware callbacks.
