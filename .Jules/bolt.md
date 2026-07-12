## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-29 - Optimize Flow Collection for High-Frequency Events
**Learning:** In high-frequency Kotlin Flow streams (e.g., sensor data updates or UI commands), using `collectLatest` is an anti-pattern. `collectLatest` launches a new coroutine for every emission and cancels the previous one, leading to severe GC pressure, memory allocation overhead, and potential event dropping when events arrive rapidly (e.g., at 100Hz).
**Action:** When handling sequential, high-frequency events where processing is fast and non-suspending, always use `collect` instead of `collectLatest` to process events in order without the overhead of continuous coroutine allocation and cancellation.
## 2026-07-12 - Decouple Throttling from State Checks
**Learning:** When throttling high-frequency sensor deltas, evaluating the time-based emission check inside a value-based condition (e.g., `if (delta != 0)`) prevents accumulators from flushing correctly when the sensor stops moving. This traps stale deltas and causes delayed emissions/jumps when the sensor moves again.
**Action:** Always place the time-based emission logic outside the non-zero delta check to guarantee residual accumulations are flushed immediately when the throttle interval expires.
