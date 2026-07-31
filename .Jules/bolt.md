## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-01 - Decouple High-Frequency Hardware Callbacks from Network Pipelines
**Learning:** High-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) bridged directly to expensive network pipelines (like WebSocket broadcasts) cause severe GC pressure, thread pool contention, and network flooding.
**Action:** Accumulate raw sensor changes over a small interval and throttle emitting to ~60Hz (16ms) using `android.os.SystemClock.uptimeMillis()` to decouple the hardware frequency from the processing pipeline.
## 2026-06-29 - Broken Sensor Throttling Pattern
**Learning:** In GyroSensorTracker, a time-based throttle (`currentTime - lastEmitTime >= INTERVAL`) was nested alongside an `else if (accumulatedDx != 0f)` catch-all designed to flush remaining movement when the user stops. This caused the throttle to be completely bypassed during normal movement because the time constraint was ignored if any movement had accumulated, resulting in heavy network/GC pressure from rapid WebSocket emission.
**Action:** When implementing time-based throttling for high-frequency hardware sensors, ensure the time check is the strict outermost condition. If you need to flush state after a stationary period, handle it by updating `lastEmitTime` independently of the emission condition rather than bypassing the time check entirely.
## YYYY-MM-DD - [Do not replace collectLatest with collect blindly]
**Learning:** `collectLatest` is often intentional in high-frequency streams (e.g., WebSockets, UI state) to drop outdated intermediate states when processing/network I/O takes time. Forcing `collect` ensures every intermediate state is processed, which can create a massive backlog, flood the network, and cause severe performance regressions.
**Action:** Do not blindly replace `collectLatest` with `collect` as a generic optimization.

## 2024-07-31 - Avoid eager string interpolation in high-frequency logs
**Learning:** String templates (like `"... $var"`) are eagerly evaluated. Using them inside high-frequency event flow collectors causes continuous string allocation and GC thrashing, even if the logger's output is suppressed.
**Action:** Always wrap such logging in conditional checks (e.g., `if (event !is HighFrequencyEvent)`) to prevent unnecessary allocations.
