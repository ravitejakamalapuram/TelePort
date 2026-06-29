## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .

## 2024-05-24 - Throttling High-Frequency Hardware Callbacks
**Learning:** Decoupling high-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) from expensive network/allocation pipelines prevents GC pressure, thread pool contention, and WebSocket network flooding.
**Action:** Accumulate raw sensor changes over a small interval and throttle emitting to ~60Hz (e.g., 16ms) using `android.os.SystemClock.uptimeMillis()` rather than directly bridging hardware callbacks.
