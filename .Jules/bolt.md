## 2026-06-12 - AdMob/Billing CI Dependencies
**Learning:** In the TelePort app, the code references AdMob (, ) and Play Billing (), but these were omitted from  causing CI to fail compilation.
**Action:** Always verify dependencies are present when using external Google services. Additionally, resolving  manifest merger conflicts requires adding a specific property directly to the .
## 2026-07-04 - Decouple Sensor Hardware Interrupts from Network Transmission
**Learning:** High-frequency hardware sensors (like `Sensor.TYPE_GYROSCOPE` at ~100Hz+) can overwhelm the networking pipeline if their events are transmitted directly without throttling. Passing continuous events creates heavy GC pressure, thread pool contention, and WebSocket network flooding, ultimately degrading performance.
**Action:** Decouple the hardware interrupt from the expensive network/allocation pipeline by accumulating the raw sensor changes over a small interval and throttling the emission (e.g., to ~60Hz or 16ms) using `android.os.SystemClock.uptimeMillis()`.
