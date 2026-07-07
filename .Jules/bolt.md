## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-28 - Throttle high-frequency sensor callbacks
**Learning:** High-frequency hardware interrupts (like gyroscope events at 100Hz+) cause severe GC pressure, thread pool contention, and network flooding when bridged directly to WebSocket sends.
**Action:** Always decouple raw sensor changes from expensive downstream pipelines by accumulating deltas and throttling emission to a sensible rate (e.g., 60Hz/16ms) using `SystemClock.uptimeMillis()`.
