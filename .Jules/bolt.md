## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-06-30 - Sensor Callback Throttling
**Learning:** Decoupling high-frequency hardware interrupts (like gyroscope sensor events at 100Hz+) from expensive network/allocation pipelines prevents GC pressure, thread pool contention, and WebSocket network flooding. Accumulating raw sensor changes over a small interval and throttling emitting to ~60Hz (e.g., 16ms) using `android.os.SystemClock.uptimeMillis()` rather than directly bridging hardware callbacks drastically improves performance.
**Action:** Always throttle high-frequency hardware events (sensors, touch, etc.) before emitting them over a network or triggering heavy UI recompositions. Accumulate the delta to prevent data loss.
